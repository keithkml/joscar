/*
 *  Copyright (c) 2005, The Joust Project
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *  - Neither the name of the Joust Project nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 */

package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_ACK;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME_ACK;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME_SENDHEADER;
import net.kano.joscar.rvproto.ft.FileTransferHeader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

class ReceiveController extends TransferController {
    protected void transferInThread(final Stream stream,
            final FileTransferImpl transfer) throws IOException {
        InputStream socketIn = stream.getInputStream();

        IncomingFileTransferImpl itransfer = (IncomingFileTransferImpl) transfer;
        long icbmId = itransfer.getRvSession().getRvSessionId();
        boolean good = false;
        for (; !shouldStop() ;) {
            FileTransferHeader sendheader = FileTransferHeader
                    .readHeader(socketIn);

            if (sendheader == null) break;

            List<String> parts = sendheader.getFilename().getSegments();
            String filename;
            if (parts.size() > 0) {
                filename = parts.get(parts.size() - 1);
            } else {
                filename = itransfer.getRvSession().getScreenname()
                        + " download";
            }

            File file = new File(filename);
            boolean resume = file.exists();
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel fileChannel = raf.getChannel();

            OutputStream socketOut = stream.getOutputStream();
            long toDownload;
            if (resume) {
                FileTransferHeader outHeader = new FileTransferHeader(
                        sendheader);
                outHeader.setHeaderType(FileTransferHeader.HEADERTYPE_RESUME);
                outHeader.setIcbmMessageId(icbmId);
                long len = raf.length();
                outHeader.setBytesReceived(len);
                outHeader.setReceivedChecksum(FileTransferTools.getChecksum(
                        fileChannel, 0, len));
                outHeader.setCompression(0);
                outHeader.setEncryption(0);
                outHeader.write(socketOut);

                FileTransferHeader resumeResponse = FileTransferHeader
                        .readHeader(socketIn);
                if (resumeResponse == null) break;
                assert resumeResponse.getHeaderType()
                        == HEADERTYPE_RESUME_SENDHEADER
                        : resumeResponse.getHeaderType();
                long bytesReceived = resumeResponse.getBytesReceived();
                assert bytesReceived <= len : "sender is trying to trick us: "
                        + bytesReceived + " > " + len;
                if (bytesReceived != len) {
                    //TODO: announce that resuming failed
                }
                fileChannel.position(bytesReceived);
                raf.setLength(bytesReceived);
                toDownload = resumeResponse.getFileSize()
                        - bytesReceived;
                FileTransferHeader finalResponse = new FileTransferHeader(
                        resumeResponse);
                finalResponse.setHeaderType(HEADERTYPE_RESUME_ACK);
                finalResponse.write(socketOut);
            } else {
                FileTransferHeader outHeader = new FileTransferHeader(
                        sendheader);
                outHeader.setIcbmMessageId(icbmId);
                raf.setLength(0);
                outHeader.setHeaderType(HEADERTYPE_ACK);
                outHeader.setBytesReceived(0);
                outHeader.setReceivedChecksum(0);
                outHeader.setCompression(0);
                outHeader.setEncryption(0);
                outHeader.setFlags(0);
                outHeader.write(socketOut);
                toDownload = sendheader.getFileSize();
            }

            long startedAt = fileChannel.position();
            int downloaded = downloadFile(socketIn, fileChannel, startedAt,
                    toDownload);
            if (downloaded != toDownload) break;

            long calculatedSum = FileTransferTools.getChecksum(fileChannel, 0,
                    startedAt + downloaded);

            fileChannel.close();
            raf.close();

            FileTransferHeader doneHeader = new FileTransferHeader(sendheader);
            doneHeader.setHeaderType(FileTransferHeader.HEADERTYPE_RECEIVED);
            doneHeader.setFlags(doneHeader.getFlags()
                    | FileTransferHeader.FLAG_DONE);
            doneHeader.setIcbmMessageId(icbmId);
            doneHeader.setFilesLeft(doneHeader.getFilesLeft() - 1);
            if (doneHeader.getFilesLeft() == 0) {
                doneHeader.setPartsLeft(doneHeader.getPartsLeft() - 1);
            }
            doneHeader.setBytesReceived(startedAt + downloaded);
            doneHeader.setReceivedChecksum(calculatedSum);
            doneHeader.write(socketOut);
            if (doneHeader.getFilesLeft() == 0
                    && doneHeader.getPartsLeft() == 0) {
                good = true;
                break;
            }
        }

        if (good) {
            fireSucceeded(new StateInfo() {
            });
        } else {
            fireFailed(new UnknownTransferErrorException());
        }
    }

    private int downloadFile(InputStream socketIn, FileChannel fileChannel,
            long offset, long length) throws IOException {
        ReadableByteChannel inChannel = Channels.newChannel(socketIn);
        int downloaded = 0;
        while (downloaded < length) {
            long remaining = length - downloaded;
            long transferred = fileChannel.transferFrom(inChannel,
                    offset + downloaded, Math.min(1024, remaining));

            if (transferred == 0 || transferred == -1) break;

            //TODO: post progress event
            downloaded += transferred;
        }
        return downloaded;
    }

}
