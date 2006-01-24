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

import net.kano.joscar.rvcmd.SegmentedFilename;
import net.kano.joscar.rvproto.ft.FileTransferHeader;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_ACK;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME_ACK;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME_SENDHEADER;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_SENDHEADER;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.Checksummer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FailureEventException;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileMapper;
import static net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionPropertyHolder.KEY_REDIRECTED;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.IncomingFileTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvSessionBasedConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionPropertyHolder;
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
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

//TODO: look into resource forks

public class ReceiveFileController extends TransferController {
  private static final Logger LOGGER = Logger
      .getLogger(ReceiveFileController.class.getName());

  protected void transferInThread(StreamInfo stream,
      RvConnectionPropertyHolder transfer)
      throws IOException, FailureEventException {
    SocketChannel socketChan = stream.getSocketChannel();
    socketChan.configureBlocking(true);

    InputStream socketIn = stream.getInputStream();
    OutputStream socketOut = stream.getOutputStream();

    List<File> files = new ArrayList<File>();
    RvSessionBasedConnection rvConnection = (RvSessionBasedConnection) transfer;
    EventPost eventpost = transfer.getEventPost();
    IncomingFileTransfer itransfer = (IncomingFileTransfer) transfer;
    long icbmId = rvConnection.getRvSession().getRvSessionId();
    boolean good = false;
    boolean redirected = transfer.getTransferProperty(KEY_REDIRECTED)
        == Boolean.TRUE;
    boolean stop = false;
    for (; !stop && !shouldStop();) {
      LOGGER.fine("Waiting for next FT packet");
      FileTransferHeader sendheader = FileTransferHeader
          .readHeader(socketIn);

      if (sendheader == null) {
        LOGGER.info("Couldn't read FT header");
        break;
      }
      assert sendheader.getHeaderType() == HEADERTYPE_SENDHEADER;
      long desiredChecksum = sendheader.getChecksum();
      if (redirected) {
        long sentid = sendheader.getIcbmMessageId();
        if (sentid != icbmId) {
          LOGGER.info("Other end sent " + sentid + " but we're looking for "
              + icbmId);
          break;
        }
      }
      setConnected();

      SegmentedFilename segName = sendheader.getFilename();
      List<String> parts = segName.getSegments();
      File destFile;
      FileMapper fileMapper = itransfer.getFileMapper();
      if (parts.size() > 0) {
        destFile = fileMapper.getDestinationFile(segName);
      } else {
        destFile = fileMapper.getUnspecifiedFilename();
      }

      files.add(destFile);
      boolean attemptResume = destFile.exists();
      RandomAccessFile raf = new RandomAccessFile(destFile, "rw");
      FileChannel fileChannel = raf.getChannel();

      long toDownload;
      if (attemptResume) {
        FileTransferHeader outHeader = new FileTransferHeader(
            sendheader);
        outHeader.setHeaderType(HEADERTYPE_RESUME);
        outHeader.setIcbmMessageId(icbmId);
        long len = raf.length();
        outHeader.setBytesReceived(len);
        Checksummer summer = new Checksummer(fileChannel, len);
        eventpost.fireEvent(new ChecksummingEvent(destFile, summer));
        outHeader.setReceivedChecksum(summer.compute());
        outHeader.setCompression(0);
        outHeader.setEncryption(0);
        outHeader.write(socketOut);

        FileTransferHeader resumeResponse = FileTransferHeader
            .readHeader(socketIn);
        if (resumeResponse == null) {
          break;
        }
        assert resumeResponse.getHeaderType()
            == HEADERTYPE_RESUME_SENDHEADER
            : resumeResponse.getHeaderType();
        long bytesReceived = resumeResponse.getBytesReceived();
        assert bytesReceived <= len : "sender is trying to trick us: "
            + bytesReceived + " > " + len;
        if (bytesReceived != len) {
          eventpost.fireEvent(new ResumeChecksumFailedEvent(destFile));
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
      Receiver receiver = new FileReceiver(fileChannel, startedAt, toDownload);
      TransferredFileInfo info = new TransferredFileInfo(destFile,
          startedAt + toDownload, startedAt);
      eventpost.fireEvent(new TransferringFileEvent(info, receiver));
      int downloaded = receiver.receive(socketChan);
      if (downloaded != toDownload) {
        break;
      }

      Checksummer summer = new Checksummer(fileChannel,
          startedAt + downloaded);
      eventpost.fireEvent(new ChecksummingEvent(destFile, summer));
      long calculatedSum = summer.compute();

      fileChannel.close();
      raf.close();


      boolean failed = calculatedSum != desiredChecksum;
      if (!failed) eventpost.fireEvent(new FileCompleteEvent(info));

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
          stop = true;
        }
      }
      int filesLeft = doneHeader.getFilesLeft();
      int partsLeft = doneHeader.getPartsLeft();
      if (filesLeft == 0 && partsLeft == 0) {
        good = true;
        break;
      } else {
        LOGGER.info(
            "Waiting for " + filesLeft + " files and " + partsLeft + " parts");
      }
    }

    if (good) {
      fireSucceeded(new TransferSucceededInfo(files));
    } else {
      fireFailed(new UnknownErrorEvent());
    }
  }

  private class FileReceiver extends Receiver {
    private final FileChannel fileChannel;

    public FileReceiver(FileChannel fileChannel, long offset, long length) {
      super(offset, length);
      this.fileChannel = fileChannel;
    }

    protected boolean isCancelled() {
      return shouldStop();
    }

    protected boolean waitIfPaused() {
      return waitUntilUnpause();
    }

    protected long transfer(SocketChannel socketIn, int downloaded,
        long remaining) throws IOException {
      return fileChannel.transferFrom(socketIn,
          offset + downloaded, Math.min(1024, remaining));
    }
  }
}
