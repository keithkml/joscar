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

import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.SegmentedFilename;
import net.kano.joscar.rvcmd.sendfile.FileSendAcceptRvCmd;
import net.kano.joscar.rvproto.ft.FileTransferChecksum;
import net.kano.joscar.rvproto.ft.FileTransferHeader;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_ACK;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME_ACK;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME_SENDHEADER;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.Checksummer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FailureEventException;
import static net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransferImpl.KEY_REDIRECTED;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.OutgoingFileTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.ProgressStatusProvider;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvSessionBasedTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.TransferPropertyHolder;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ChecksummingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.CorruptTransferEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.FileCompleteEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ResumeChecksumFailedEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.TransferredFileInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.TransferringFileEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.TransferSucceededInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

//TOLATER: reverse lookup proxies and check for *.aol.com
public class SendController extends TransferController {
    protected void transferInThread(StreamInfo stream, TransferPropertyHolder transfer)
            throws IOException, FailureEventException {
        OutgoingFileTransfer otransfer = (OutgoingFileTransfer) transfer;
        RvSessionBasedTransfer rvTransfer = (RvSessionBasedTransfer) transfer;
        EventPost eventPost = transfer.getEventPost();
        List<File> files = otransfer.getFiles();
        List<RandomAccessFile> rafs = new ArrayList<RandomAccessFile>(files.size());
        long totalSize = 0;
        Map<RandomAccessFile,File> lastmods = new IdentityHashMap<RandomAccessFile, File>();
        Map<RandomAccessFile,Long> lengths = new IdentityHashMap<RandomAccessFile, Long>();
        Map<File, String> names = otransfer.getNameMappings();
        try {
            for (File file : files) {
                RandomAccessFile raf = null;
                boolean okay = false;
                try {
                    raf = new RandomAccessFile(file, "r");
                    rafs.add(raf);
                    okay = true;
                } finally {
                    if (!okay && raf != null) raf.close();
                }
                long length = raf.length();
                totalSize += length;
                lastmods.put(raf, file);
                lengths.put(raf, length);
            }

            int succeeded = 0;
            int fileCount = rafs.size();
            int left = fileCount + 1;
            SocketChannel socketChan = stream.getSocketChannel();
            socketChan.configureBlocking(true);
            InputStream socketIn = Channels.newInputStream(socketChan);
            OutputStream socketOut = Channels.newOutputStream(socketChan);
            Boolean redirectedBool = transfer.getTransferProperty(KEY_REDIRECTED);
            boolean redirected = redirectedBool != null && redirectedBool == true;
            for (RandomAccessFile raf : rafs) {
                left--;
                File file = lastmods.get(raf);
                FileChannel fileChannel = raf.getChannel();

                long len = lengths.get(raf);
                pauseTimeout();
                long fileChecksum = otransfer.getChecksumManager().getChecksum(file);
                resumeTimeout();

                FileTransferHeader sendheader = new FileTransferHeader();
                sendheader.setDefaults();
                sendheader.setFilename(new SegmentedFilename(getName(names, file)));
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
                RvSession rvSession = rvTransfer.getRvSession();
                long rvSessionId = rvSession.getRvSessionId();
                if (redirected) {
                    sendheader.setIcbmMessageId(rvSessionId);
                    rvSession.sendRv(new FileSendAcceptRvCmd());
                }
                sendheader.write(socketOut);

                FileTransferHeader ack = FileTransferHeader.readHeader(socketIn);
                if (ack == null) break;

                if (!redirected) {
                    if (ack.getIcbmMessageId() != rvSessionId) {
                        break;
                    }
                }
                setConnected();

                long startAt;

                int headerType = ack.getHeaderType();
                if (headerType == HEADERTYPE_RESUME) {
                    long attemptStartAt = ack.getBytesReceived();
                    final long attemptChecksum = ack.getReceivedChecksum();
                    Checksummer rsummer = new Checksummer(fileChannel, attemptStartAt);
                    eventPost.fireEvent(new ChecksummingEvent(file, rsummer));
                    long oursum = rsummer.compute();
                    long respondStartAt;
                    if (oursum != attemptChecksum) {
                        eventPost.fireEvent(new ResumeChecksumFailedEvent(file));
                        respondStartAt = 0;

                    } else {
                        respondStartAt = attemptStartAt;
                    }

                    FileTransferHeader resumeSend = new FileTransferHeader(sendheader);
                    resumeSend.setBytesReceived(respondStartAt);
                    resumeSend.setChecksum(fileChecksum);
                    resumeSend.setHeaderType(HEADERTYPE_RESUME_SENDHEADER);
                    resumeSend.write(socketOut);

                    FileTransferHeader resumeAck = FileTransferHeader.readHeader(socketIn);
                    if (resumeAck.getHeaderType() != HEADERTYPE_RESUME_ACK) {
                        break;
                    }
                    startAt = resumeAck.getBytesReceived();

                } else if (headerType == HEADERTYPE_ACK) {
                    startAt = 0;

                } else {
                    break;
                }

                long toSend = len - startAt;
                Sender sender = new Sender(fileChannel, startAt, len);
                TransferredFileInfo info = new TransferredFileInfo(file, len, startAt);
                eventPost.fireEvent(new TransferringFileEvent(info, sender));
                int sent = sender.sendFile(socketChan);

                fileChannel.close();
                raf.close();
                if (sent != toSend) break;

                FileTransferHeader receivedHeader = FileTransferHeader.readHeader(socketIn);
                if (receivedHeader == null) break;
                if (receivedHeader.getBytesReceived() != len
                        || receivedHeader.getChecksum() != fileChecksum) {
                    fireFailed(new CorruptTransferEvent(info));
                    break;
                }
                eventPost.fireEvent(new FileCompleteEvent(info));
                succeeded++;
            }
            if (succeeded == rafs.size()) {
                fireSucceeded(new TransferSucceededInfo(files));
            } else {
                fireFailed(new UnknownErrorEvent());
            }
        } finally {
            for (RandomAccessFile file : rafs) {
                try {
                    file.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static String getName(Map<File, String> names, File file) {
        String name = names.get(file);
        if (name == null) return file.getName();
        else return name;
    }

    private class Sender implements ProgressStatusProvider {
        private final FileChannel fileChannel;
        private final long offset;
        private final long length;
        private volatile long position = -1;

        public Sender(FileChannel fileChannel, long offset, long length) {
            this.fileChannel = fileChannel;
            this.offset = offset;
            this.length = length;
        }

        public int sendFile(SocketChannel out) throws IOException {
            Selector selector = Selector.open();
            boolean wasBlocking = out.isBlocking();
            try {
                out.register(selector, SelectionKey.OP_WRITE);
                if (wasBlocking) out.configureBlocking(false);

                int uploaded = 0;
                while (uploaded < length) {
                    //TODO: fire paused event
                    if (waitUntilUnpause()) continue;

                    long remaining = length - uploaded;
                    selector.select(50);
                    long transferred = fileChannel.transferTo(offset + uploaded,
                            Math.min(1024, remaining), out);

                    if (transferred == 0 || transferred == -1) break;

                    uploaded += transferred;
                    setPosition(offset + uploaded);
                    if (shouldStop()) break;
                }
                return uploaded;

            } finally {
                if (wasBlocking) out.configureBlocking(true);
                selector.close();
            }
        }

        public long getStartPosition() {
            return offset;
        }

        public long getPosition() {
            return position;
        }

        public long getLength() {
            return length;
        }

        public void setPosition(long position) {
            this.position = position;
        }
    }
}
