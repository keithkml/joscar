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

import static net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransferImpl.KEY_NORMAL_REDIRECTED;
import static net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransferImpl.KEY_PROXY_REDIRECTED;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME_ACK;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_ACK;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME_SENDHEADER;
import net.kano.joscar.rvcmd.SegmentedFilename;
import net.kano.joscar.rvcmd.sendfile.FileSendAcceptRvCmd;
import net.kano.joscar.rvproto.ft.FileTransferHeader;
import net.kano.joscar.rvproto.ft.FileTransferChecksum;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

class SendController extends TransferController {
    protected void transferInThread(Stream stream, FileTransferImpl transfer)
            throws IOException {
        System.out.println("starting send");
        OutgoingFileTransfer otransfer = (OutgoingFileTransfer) transfer;
        List<File> files = otransfer.getFiles();
        List<RandomAccessFile> rafs = new ArrayList<RandomAccessFile>(files.size());
        long totalSize = 0;
        Map<RandomAccessFile,File> lastmods = new IdentityHashMap<RandomAccessFile, File>();
        Map<RandomAccessFile,Long> lengths = new IdentityHashMap<RandomAccessFile, Long>();
        for (File file : files) {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            rafs.add(raf);
            long length = raf.length();
            totalSize += length;
            lastmods.put(raf, file);
            lengths.put(raf, length);
        }

        int fileCount = rafs.size();
        int left = fileCount + 1;
        OutputStream socketOut = stream.getOutputStream();
        for (RandomAccessFile raf : rafs) {
            left--;
            File file = lastmods.get(raf);
            FileChannel fileChannel = raf.getChannel();

            long len = lengths.get(raf);
            long fileChecksum = FileTransferTools.getChecksum(fileChannel, 0, len);

            FileTransferHeader sendheader = new FileTransferHeader();
            sendheader.setDefaults();
            sendheader.setFilename(new SegmentedFilename(file.getName()));
            sendheader.setHeaderType(FileTransferHeader.HEADERTYPE_SENDHEADER);
            sendheader.setChecksum(fileChecksum);
            sendheader.setReceivedChecksum(FileTransferChecksum.CHECKSUM_EMPTY);
            sendheader.setFileCount(fileCount);
            sendheader.setFileSize(len);
            sendheader.setFilesLeft(left);
            sendheader.setLastmod(file.lastModified()/1000);
            sendheader.setPartCount(1);
            sendheader.setPartsLeft(1);
            sendheader.setResForkChecksum(FileTransferChecksum.CHECKSUM_EMPTY);
            sendheader.setResForkReceivedChecksum(FileTransferChecksum.CHECKSUM_EMPTY);
            sendheader.setListNameOffset(28);
            sendheader.setListSizeOffset(17);
            sendheader.setTotalFileSize(totalSize);
            boolean proxyRedirected = transfer.getTransferProperty(KEY_PROXY_REDIRECTED)
                    == Boolean.TRUE;
            boolean normalRedirected = transfer.getTransferProperty(KEY_NORMAL_REDIRECTED)
                    == Boolean.TRUE;
            if (proxyRedirected || normalRedirected) {
                sendheader.setIcbmMessageId(otransfer.getRvSession().getRvSessionId());
                otransfer.getRvSession().sendRv(new FileSendAcceptRvCmd());
            }
            sendheader.write(socketOut);

            InputStream in = stream.getInputStream();
            FileTransferHeader ack = FileTransferHeader.readHeader(in);
            if (ack == null) break;

            //TODO: check received icbm id

            long startAt;

            int headerType = ack.getHeaderType();
            if (headerType == HEADERTYPE_RESUME) {
                long attemptStartAt = ack.getBytesReceived();
                final long attemptChecksum = ack.getReceivedChecksum();
                long oursum = FileTransferTools.getChecksum(fileChannel, 0,
                        attemptStartAt);
                long respondStartAt;
                if (oursum != attemptChecksum) {
                    //TODO: announce resume failed
                    respondStartAt = 0;

                } else {
                    respondStartAt = attemptStartAt;
                }

                FileTransferHeader resumeSend = new FileTransferHeader(sendheader);
                resumeSend.setBytesReceived(respondStartAt);
                resumeSend.setChecksum(fileChecksum);
                resumeSend.setHeaderType(HEADERTYPE_RESUME_SENDHEADER);
                resumeSend.write(socketOut);

                FileTransferHeader resumeAck = FileTransferHeader.readHeader(in);
                if (resumeAck.getHeaderType() != HEADERTYPE_RESUME_ACK) {
                    //TODO: fire event for mismatched header
                    break;
                }
                startAt = resumeAck.getBytesReceived();

            } else if (headerType == HEADERTYPE_ACK) {
                startAt = 0;

            } else {
                //TODO: fire event for mismatched header
                break;
            }

            long toSend = len - startAt;
            int sent = sendFile(socketOut, fileChannel, startAt, toSend);
            if (sent != toSend) break;

            FileTransferHeader receivedHeader = FileTransferHeader.readHeader(in);
            if (receivedHeader == null) break;
            if (receivedHeader.getBytesReceived() != len
                    || receivedHeader.getChecksum() != fileChecksum) {
                //TODO: fire checksum failed error
                break;
            }
        }
    }

    private int sendFile(OutputStream out, FileChannel fileChannel,
            long offset, long length) throws IOException {
        WritableByteChannel outChannel = Channels.newChannel(out);
        int uploaded = 0;
        while (uploaded < length) {
            long remaining = length - uploaded;
            long transferred = fileChannel.transferTo(offset + uploaded,
                    Math.min(1024, remaining), outChannel);

            if (transferred == 0 || transferred == -1) break;

            //TODO: post progress event
            uploaded += transferred;
        }
        return uploaded;
    }
}
