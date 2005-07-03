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

package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_SENDHEADER;
import net.kano.joscar.rvproto.ft.FileTransferHeader;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_ACK;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME_ACK;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME_SENDHEADER;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.Checksummer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FailureEventException;
import static net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransferImpl.KEY_REDIRECTED;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.ProgressStatusOwner;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvSessionBasedTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.TransferPropertyHolder;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.TransferSucceededInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.TransferredFileInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.CorruptTransferEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownTransferErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ChecksummingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.FileCompleteEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ResumeChecksumFailedEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.TransferringFileEvent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

public class ReceiveController extends TransferController {
    protected void transferInThread(final StreamInfo stream,
            final TransferPropertyHolder transfer)
            throws IOException, FailureEventException {
        InputStream socketIn = stream.getInputStream();

        List<File> files = new ArrayList<File>();
        RvSessionBasedTransfer itransfer = (RvSessionBasedTransfer) transfer;
        EventPost eventBasedTransfer = transfer.getEventPost();
        long icbmId = itransfer.getRvSession().getRvSessionId();
        boolean good = false;
        boolean redirected = transfer.getTransferProperty(KEY_REDIRECTED)
                == Boolean.TRUE;
        for (; !shouldStop() ;) {
            FileTransferHeader sendheader = FileTransferHeader
                    .readHeader(socketIn);

            if (sendheader == null) break;
            assert sendheader.getHeaderType() == HEADERTYPE_SENDHEADER;
            long desiredChecksum = sendheader.getChecksum();
            if (!redirected) {
                if (sendheader.getIcbmMessageId() != icbmId) {
                    break;
                }
            }
            setConnected();

            List<String> parts = sendheader.getFilename().getSegments();
            String filename;
            if (parts.size() > 0) {
                filename = parts.get(parts.size() - 1);
            } else {
                filename = itransfer.getRvSession().getScreenname()
                        + " download";
            }

            File file = new File(filename);
            files.add(file);
            boolean attemptResume = file.exists();
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel fileChannel = raf.getChannel();

            OutputStream socketOut = stream.getOutputStream();
            long toDownload;
            if (attemptResume) {
                FileTransferHeader outHeader = new FileTransferHeader(
                        sendheader);
                outHeader.setHeaderType(HEADERTYPE_RESUME);
                outHeader.setIcbmMessageId(icbmId);
                long len = raf.length();
                outHeader.setBytesReceived(len);
                Checksummer summer = new Checksummer(fileChannel, len);
                eventBasedTransfer.fireEvent(new ChecksummingEvent(file, summer));
                outHeader.setReceivedChecksum(summer.compute());
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
                    eventBasedTransfer.fireEvent(new ResumeChecksumFailedEvent(file));
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
                // not resuming
                FileTransferHeader outHeader = new FileTransferHeader(sendheader);
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
            Receiver receiver = new Receiver(fileChannel, startedAt, toDownload);
            TransferredFileInfo info = new TransferredFileInfo(file,
                    startedAt + toDownload, startedAt);
            eventBasedTransfer.fireEvent(new TransferringFileEvent(info, receiver));
            int downloaded = receiver.downloadFile(socketIn);
            if (downloaded != toDownload) break;

            Checksummer summer = new Checksummer(fileChannel,
                    startedAt + downloaded);
            eventBasedTransfer.fireEvent(new ChecksummingEvent(file, summer));
            long calculatedSum = summer.compute();

            fileChannel.close();
            raf.close();


            boolean failed = calculatedSum != desiredChecksum;
            if (!failed) eventBasedTransfer.fireEvent(new FileCompleteEvent(info));

            FileTransferHeader doneHeader;
            try {
                doneHeader = new FileTransferHeader(sendheader);
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
            } finally {
                if (failed) {
                    fireFailed(new CorruptTransferEvent(info));
                    break;
                }
            }
            if (doneHeader.getFilesLeft() == 0
                    && doneHeader.getPartsLeft() == 0) {
                good = true;
                break;
            }
        }

        if (good) {
            fireSucceeded(new TransferSucceededInfo(files));
        } else {
            fireFailed(new UnknownTransferErrorEvent());
        }
    }

    private static class Receiver implements ProgressStatusOwner {
        private final FileChannel fileChannel;
        private final long offset;
        private final long length;
        private long position = -1;

        public Receiver(FileChannel fileChannel, long offset, long length) {
            this.fileChannel = fileChannel;
            this.offset = offset;
            this.length = length;
        }

        public int downloadFile(InputStream socketIn) throws IOException {
            ReadableByteChannel inChannel = Channels.newChannel(socketIn);
            int downloaded = 0;
            while (downloaded < length) {
                long remaining = length - downloaded;
                long transferred = fileChannel.transferFrom(inChannel,
                        offset + downloaded, Math.min(1024, remaining));

                if (transferred == 0 || transferred == -1) break;

                downloaded += transferred;
                setPosition(offset + downloaded);
            }
            return downloaded;
        }

        public long getStartPosition() {
            return offset;
        }

        public synchronized long getPosition() {
            return position;
        }

        public long getEnd() {
            return length;
        }

        private synchronized void setPosition(long position) {
            this.position = position;
        }
    }

}
