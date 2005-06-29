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

import net.kano.joscar.rvproto.ft.FileTransferChecksum;
import net.kano.joscar.rvproto.ft.FileTransferHeader;
import static net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransferManager.KEY_REQUEST_ID;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

class ReceiveFileController extends StateController {
    public void start(final FileTransferManager.FileTransfer transfer,
            StateController last) {
        StateInfo endState = last.getEndState();
        if (endState instanceof Stream) {
            final Stream stream = (Stream) endState;
            Thread receiveThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        receiveInThread(stream, transfer);

                    } catch (IOException e) {
                        e.printStackTrace();
                        fireFailed(e);
                        return;
                    }
                }
            });
            receiveThread.start();
        }
    }

    private void receiveInThread(final Stream stream,
            final FileTransferManager.FileTransfer transfer)
            throws IOException {
        InputStream socketIn = stream.getInputStream();

        long icbmId = transfer.getTransferProperty(KEY_REQUEST_ID);
        for (;;) {
            FileTransferHeader sendheader = FileTransferHeader.readHeader(socketIn);

            if (sendheader == null) break;

            List<String> parts = sendheader.getFilename().getSegments();
            String filename;
            if (parts.size() > 0) filename = parts.get(parts.size()-1);
            else filename = transfer.getRvSession().getScreenname() + " download";

            File file = new File(filename);
            boolean resume = file.exists();
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel fileChannel = raf.getChannel();

            OutputStream socketOut = stream.getOutputStream();
            long toDownload;
            if (resume) {
                FileTransferHeader outHeader = new FileTransferHeader(sendheader);
                outHeader.setIcbmMessageId(icbmId);
                outHeader.setHeaderType(FileTransferHeader.HEADERTYPE_RESUME);
                outHeader.setIcbmMessageId(icbmId);
                long len = raf.length();
                outHeader.setBytesReceived(len);
                outHeader.setReceivedChecksum(getChecksum(fileChannel, 0, len));
                outHeader.write(socketOut);

                FileTransferHeader resumeResponse = FileTransferHeader.readHeader(socketIn);
                System.out.println("got resume response: " + resumeResponse);
                assert resumeResponse.getHeaderType()
                        == FileTransferHeader.HEADERTYPE_RESUME_SENDHEADER
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
                FileTransferHeader finalResponse = new FileTransferHeader(resumeResponse);
                finalResponse.setHeaderType(FileTransferHeader.HEADERTYPE_RESUME_ACK);
                finalResponse.write(socketOut);
            } else {
                FileTransferHeader outHeader = new FileTransferHeader(sendheader);
                outHeader.setIcbmMessageId(icbmId);
                raf.setLength(0);
                outHeader.setHeaderType(FileTransferHeader.HEADERTYPE_ACK);
                outHeader.setBytesReceived(0);
                outHeader.setReceivedChecksum(0);
                outHeader.write(socketOut);
                toDownload = sendheader.getFileSize();
            }

            long startedAt = fileChannel.position();
            int downloaded = downloadFile(socketIn, fileChannel, startedAt, toDownload);
            if (downloaded != toDownload) {
                fireFailed(new IOException("Could not complete download"));
                return;
            }

            long calculatedSum = getChecksum(fileChannel, 0, startedAt + downloaded);

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
            //TODO: does startedat go here? should it be removed?
            doneHeader.setBytesReceived(startedAt + downloaded);
            doneHeader.setReceivedChecksum(calculatedSum);
            doneHeader.write(socketOut);
            if (doneHeader.getFilesLeft() == 0 && doneHeader.getPartsLeft() == 0) {
                break;
            }
        }


        fireSucceeded(new StateInfo() {
        });
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

    private long getChecksum(FileChannel fileChannel, long offset, long length)
            throws IOException {
        long oldPos = fileChannel.position();
        try {
            FileTransferChecksum summer = new FileTransferChecksum();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            fileChannel.position(offset);
            long remaining = length;
            while (remaining > 0) {
                buffer.rewind();
                buffer.limit((int) Math.min(remaining, buffer.capacity()));
                int count = fileChannel.read(buffer);
                if (count == -1) break;
                buffer.flip();
                remaining -= buffer.limit();
                summer.update(buffer.array(), buffer.arrayOffset(), buffer.limit());
            }
            if (remaining > 0) {
                throw new IOException("could not get checksum for entire file; "
                        + remaining + " failed of " + length);
            }

            return summer.getValue();
        } finally {
            fileChannel.position(oldPos);
        }
    }
}
