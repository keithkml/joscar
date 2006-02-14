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
import net.kano.joscar.rvproto.ft.FileTransferChecksum;
import net.kano.joscar.rvproto.ft.FileTransferHeader;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_ACK;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME_ACK;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME_SENDHEADER;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.Checksummer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FailureEventException;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.OutgoingFileTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvSessionConnectionInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.Initiator;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ChecksummingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.CorruptTransferEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.FileCompleteEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ResumeChecksumFailedEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.TransferredFileInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.TransferringFileEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.TransferSucceededInfo;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

//TOLATER: reverse lookup proxies and check for *.aol.com

public class SendFileController extends TransferController {
  private static final Logger LOGGER = Logger
      .getLogger(SendFileController.class.getName());
  
  private OutgoingFileTransferPlumber plumber;

  public void setPlumber(OutgoingFileTransferPlumber plumber) {
    this.plumber = plumber;
  }

  protected void transferInThread(RvConnection transfer)
      throws IOException, FailureEventException {
    OutgoingFileTransfer otransfer = (OutgoingFileTransfer) transfer;
    RvSessionConnectionInfo rvConnectionInfo = transfer.getRvSessionInfo();
    EventPost eventPost = otransfer.getEventPost();
    if (plumber == null) {
      plumber = new OutgoingFileTransferPlumberImpl(otransfer, this);
    }
    List<TransferredFile> rafs = plumber.getFilesToTransfer();
    try {
      long totalSize = getTotalSize(rafs);

      int fileCount = rafs.size();
      int left = fileCount + 1;

      int succeeded = 0;
      for (TransferredFile mfile : rafs) {
        left--;

        long len = mfile.getSize();
        pauseTimeout();
        long fileChecksum = otransfer.getChecksummer().getChecksum(mfile);
        resumeTimeout();

        FileTransferHeader sendheader = new FileTransferHeader();
        sendheader.setDefaults();
        sendheader.setFilename(SegmentedFilename.fromNativeFilename(mfile.getTransferredName()));
        sendheader.setHeaderType(FileTransferHeader.HEADERTYPE_SENDHEADER);
        sendheader.setChecksum(fileChecksum);
        sendheader.setReceivedChecksum(FileTransferChecksum.CHECKSUM_EMPTY);
        sendheader.setFileCount(fileCount);
        sendheader.setFileSize(len);
        sendheader.setFilesLeft(left);
        sendheader.setLastmod(mfile.getLastModifiedMillis() / 1000);
        sendheader.setPartCount(1);
        sendheader.setPartsLeft(1);
        sendheader.setResForkChecksum(FileTransferChecksum.CHECKSUM_EMPTY);
        sendheader.setResForkReceivedChecksum(FileTransferChecksum.CHECKSUM_EMPTY);
        sendheader.setListNameOffset(FileTransferHeader.LISTNAMEOFFSET_DEFAULT);
        sendheader.setListSizeOffset(FileTransferHeader.LISTSIZEOFFSET_DEFAULT);
        sendheader.setTotalFileSize(totalSize);

        long rvSessionId = rvConnectionInfo.getRvSession().getRvSessionId();
        if (rvConnectionInfo.getInitiator() == Initiator.BUDDY) {
          sendheader.setIcbmMessageId(rvSessionId);
        }
        plumber.sendHeader(sendheader);

        FileTransferHeader ack = plumber.readHeader();
        if (ack == null) {
          LOGGER.warning("Couldn't read file transfer header, closing");
          break;
        }

        if (rvConnectionInfo.getInitiator() == Initiator.ME
            && ack.getIcbmMessageId() != rvSessionId) {
          LOGGER.warning("Buddy sent wrong ICBM message ID: "
              + ack.getIcbmMessageId() + " should have been " + rvSessionId);
          fireFailed(new UnknownErrorEvent());
          break;
        }
        setConnected();

        long startAt;

        int headerType = ack.getHeaderType();
        if (headerType == HEADERTYPE_RESUME) {
          long attemptStartAt = ack.getBytesReceived();
          long attemptChecksum = ack.getReceivedChecksum();
          Checksummer rsummer = plumber.getChecksummer(mfile, attemptStartAt);
          eventPost.fireEvent(new ChecksummingEvent(mfile, rsummer));
          long oursum = rsummer.compute();
          long respondStartAt;
          if (oursum != attemptChecksum) {
            eventPost.fireEvent(new ResumeChecksumFailedEvent(mfile));
            respondStartAt = 0;

          } else {
            respondStartAt = attemptStartAt;
          }

          FileTransferHeader resumeSend = new FileTransferHeader(sendheader);
          resumeSend.setBytesReceived(respondStartAt);
          resumeSend.setChecksum(fileChecksum);
          resumeSend.setHeaderType(HEADERTYPE_RESUME_SENDHEADER);
          plumber.sendHeader(resumeSend);

          FileTransferHeader resumeAck = plumber.readHeader();
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
        Transferrer sender = plumber.createTransferrer(mfile, startAt, len);
        assert sender != null : plumber;
        TransferredFileInfo info = new TransferredFileInfo(mfile, len, startAt);
        eventPost.fireEvent(new TransferringFileEvent(info, sender));
        long sent = sender.transfer();

        mfile.close();
        if (sent != toSend) break;

        FileTransferHeader receivedHeader = plumber.readHeader();
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
        fireSucceeded(new TransferSucceededInfo(rafs));
      } else {
        fireFailed(new UnknownErrorEvent());
      }
    } finally {
      for (TransferredFile file : rafs) {
        try {
          file.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  private static long getTotalSize(List<TransferredFile> rafs) {
    long totalSize = 0;
    for (TransferredFile file : rafs) totalSize += file.getSize();
    return totalSize;
  }
}
