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

import net.kano.joscar.MiscTools;
import net.kano.joscar.rvcmd.SegmentedFilename;
import net.kano.joscar.rvproto.ft.FileTransferChecksum;
import net.kano.joscar.rvproto.ft.FileTransferHeader;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_ACK;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME_ACK;
import static net.kano.joscar.rvproto.ft.FileTransferHeader.HEADERTYPE_RESUME_SENDHEADER;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.Checksummer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FailureEventException;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.Initiator;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.OutgoingFileTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvSessionConnectionInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ChecksummingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.CorruptTransferEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.FileCompleteEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ResumeChecksumFailedEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.TransferringFileEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.TransferringFileInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.TransferSucceededInfo;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SendFileController extends TransferController {
  private static final Logger LOGGER = Logger
      .getLogger(SendFileController.class.getName());
  
  private OutgoingFileTransferPlumber plumber;
  private EventPost eventPost;
  private RvSessionConnectionInfo rvConnectionInfo;
  private OutgoingFileTransfer transfer;

  public void setPlumber(OutgoingFileTransferPlumber plumber) {
    this.plumber = plumber;
  }

  protected void transferInThread(RvConnection connection)
      throws IOException, FailureEventException {
    transfer = (OutgoingFileTransfer) connection;
    rvConnectionInfo = connection.getRvSessionInfo();
    eventPost = transfer.getEventPost();
    initializePlumber();
    List<TransferredFile> fileHandles = plumber.getFilesToTransfer();
    try {
      long totalSize = getTotalSize(fileHandles);

      int fileCount = fileHandles.size();
      int filesRemaining = fileCount;

      try {
        for (TransferredFile currentFile : fileHandles) {
          sendFile(currentFile, filesRemaining, fileCount, totalSize);

          filesRemaining--;
        }
        fireSucceeded(new TransferSucceededInfo(fileHandles));

      } catch (TransferProblemException e) {
        fireFailed(new UnknownErrorEvent());
      }
    } finally {
      close(fileHandles);
    }
  }

  private void sendFile(TransferredFile currentFile, int filesRemaining,
                        int fileCount, long totalSize)
      throws IOException, TransferProblemException {

    long fileChecksum = computeChecksum(currentFile);

    long fileSize = currentFile.getSize();
    FileTransferHeader initialHeader = createInitialHeaderForFile(currentFile,
        fileChecksum, filesRemaining, fileCount, fileSize, totalSize);

    plumber.sendHeader(initialHeader);

    long resumePosition = readInitialResponse(currentFile, initialHeader,
        fileChecksum);

    TransferringFileInfo info = new TransferringFileInfo(currentFile,
        resumePosition);
    long transferred = transferFile(info);

    currentFile.close();

    checkSentCorrectData(resumePosition, fileSize, transferred);

    readReceivedResponse(info, fileChecksum);
  }

  private long readInitialResponse(TransferredFile currentFile,
                                   FileTransferHeader ourInitialHeader,
                                   long fileChecksum)
      throws IOException, TransferProblemException {

    FileTransferHeader ack = plumber.readHeader();

    checkAcknowledgement(ack);

    setConnected();

    return getResumePosition(currentFile, ourInitialHeader, ack, fileChecksum);
  }

  private void readReceivedResponse(TransferringFileInfo info,
                                    long fileChecksum)
      throws IOException, TransferProblemException {
    FileTransferHeader receivedHeader = plumber.readHeader();
    if (receivedHeader == null) throw new TransferProblemException();
    if (receivedHeader.getBytesReceived() != info.getFileSize()
        || receivedHeader.getChecksum() != fileChecksum) {
      fireFailed(new CorruptTransferEvent(info));
      throw new TransferProblemException();
    }
    eventPost.fireEvent(new FileCompleteEvent(info));
  }

  private void checkSentCorrectData(long resumePosition, long fileSize,
                                    long transferred)
      throws TransferProblemException {
    long toSend = fileSize - resumePosition;
    if (transferred != toSend) {
      LOGGER.warning("Expected to send " + toSend + ", but only sent "
          + transferred + ": I don't know why");
      throw new TransferProblemException();
    }
  }

  private long getResumePosition(TransferredFile currentFile,
                                 FileTransferHeader ourInitialHeader,
                                 FileTransferHeader response, long fileChecksum)
      throws IOException, TransferProblemException {
    long resumePosition;
    int headerType = response.getHeaderType();
    if (headerType == HEADERTYPE_RESUME) {
      resumePosition = getResumePositionFromResumePacket(currentFile,
          ourInitialHeader, response, fileChecksum);

    } else if (headerType == HEADERTYPE_ACK) {
      resumePosition = 0;

    } else {
      return throwUnknownHeaderException(currentFile, headerType,
          "in response to initial header");
    }
    return resumePosition;
  }

  private long throwUnknownHeaderException(TransferredFile currentFile,
                                           int headerType, String note)
      throws TransferProblemException {
    String headerTypeName = MiscTools.findEqualField(
        FileTransferHeader.class, headerType, "HEADERTYPE_.*");
    LOGGER.warning("Got unknown header type (" + note + ") for "
        + currentFile.getTransferredName() + ": "
        + headerTypeName);
    throw new TransferProblemException();
  }

  private long transferFile(TransferringFileInfo info) throws IOException {
    Transferrer sender = plumber.createTransferrer(info.getFile(),
        info.getResumePosition(), info.getFileSize());
    assert sender != null : plumber;
    eventPost.fireEvent(new TransferringFileEvent(info, sender));
    //noinspection UnnecessaryLocalVariable
    long sent = sender.transfer();
    return sent;
  }

  private long getResumePositionFromResumePacket(
      TransferredFile currentFile, FileTransferHeader initialHeader,
      FileTransferHeader ack, long fileChecksum)
      throws IOException, TransferProblemException {

    long startAt;
    long respondStartAt = getActualResumePosition(ack, currentFile);

    sendResumeHeader(initialHeader, respondStartAt, fileChecksum);

    FileTransferHeader resumeAck = plumber.readHeader();
    if (resumeAck.getHeaderType() != HEADERTYPE_RESUME_ACK) {
      throwUnknownHeaderException(currentFile, resumeAck.getHeaderType(),
          "in response to resume header");
    }
    startAt = resumeAck.getBytesReceived();
    return startAt;
  }

  private void initializePlumber() {
    if (plumber == null) {
      plumber = new OutgoingFileTransferPlumberImpl(transfer, this);
    }
  }

  private void close(List<TransferredFile> rafs) {
    for (TransferredFile file : rafs) {
      try {
        file.close();
      } catch (IOException e) {
        LOGGER.log(Level.FINE, "Couldn't close " + file.getTransferredName()
            + ": " + e.getMessage(), e);
      }
    }
  }

  private void sendResumeHeader(FileTransferHeader sendheader,
                                long resumePosition,
                                long fileChecksum) throws IOException {
    FileTransferHeader resumeSend = new FileTransferHeader(sendheader);
    resumeSend.setBytesReceived(resumePosition);
    resumeSend.setChecksum(fileChecksum);
    resumeSend.setHeaderType(HEADERTYPE_RESUME_SENDHEADER);
    plumber.sendHeader(resumeSend);
  }

  /**
   * Determines the actual position at which the transfer should be resumed.
   * This method checks the checksum that the receiver sent with our checksum.
   * If they match, this method returns the requested resume position. If they
   * don't match, it returns zero.
   *
   * @param acknowledgement an initial acknowledgement packet
   */
  private long getActualResumePosition(FileTransferHeader acknowledgement,
                                       TransferredFile currentFile)
      throws IOException {
    long attemptStartAt = acknowledgement.getBytesReceived();
    long attemptChecksum = acknowledgement.getReceivedChecksum();
    long oursum = checksumFilePart(currentFile, attemptStartAt);
    long respondStartAt;
    if (oursum != attemptChecksum) {
      eventPost.fireEvent(new ResumeChecksumFailedEvent(currentFile));
      respondStartAt = 0;

    } else {
      respondStartAt = attemptStartAt;
    }
    return respondStartAt;
  }

  private long checksumFilePart(TransferredFile currentFile,
                                long partLength) throws IOException {
    Checksummer rsummer = plumber.getChecksummer(currentFile, partLength);
    eventPost.fireEvent(new ChecksummingEvent(currentFile, rsummer));
    return rsummer.compute();
  }

  private long computeChecksum(TransferredFile mfile) throws IOException {
    pauseTimeout();
    long fileChecksum = transfer.getChecksummer().getChecksum(mfile);
    resumeTimeout();
    return fileChecksum;
  }

  private FileTransferHeader createInitialHeaderForFile(
      TransferredFile mfile, long fileChecksum, int filesLeft, int fileCount,
      long fileSize, long totalSize) {

    FileTransferHeader sendheader = new FileTransferHeader();
    sendheader.setDefaults();
    sendheader.setFilename(
        SegmentedFilename.fromNativeFilename(mfile.getTransferredName()));
    sendheader.setHeaderType(FileTransferHeader.HEADERTYPE_SENDHEADER);
    sendheader.setChecksum(fileChecksum);
    sendheader.setReceivedChecksum(FileTransferChecksum.CHECKSUM_EMPTY);
    sendheader.setFileCount(fileCount);
    sendheader.setFileSize(fileSize);
    sendheader.setFilesLeft(filesLeft);
    sendheader.setLastmod(mfile.getLastModifiedMillis() / 1000);
    sendheader.setPartCount(1);
    sendheader.setPartsLeft(1);
    sendheader.setMacFileInfo(mfile.getMacFileInfo());
    sendheader.setResForkChecksum(FileTransferChecksum.CHECKSUM_EMPTY);
    sendheader.setResForkReceivedChecksum(FileTransferChecksum.CHECKSUM_EMPTY);
    sendheader.setListNameOffset(FileTransferHeader.LISTNAMEOFFSET_DEFAULT);
    sendheader.setListSizeOffset(FileTransferHeader.LISTSIZEOFFSET_DEFAULT);
    sendheader.setTotalFileSize(totalSize);

    long rvSessionId = rvConnectionInfo.getRvSession().getRvSessionId();
    if (rvConnectionInfo.getInitiator() == Initiator.BUDDY) {
      sendheader.setIcbmMessageId(rvSessionId);
    }
    return sendheader;
  }

  private void checkAcknowledgement(FileTransferHeader ack)
      throws TransferProblemException {
    long rvSessionId = rvConnectionInfo.getRvSession().getRvSessionId();
    if (ack == null) {
      LOGGER.warning("Couldn't read file transfer header, closing");
      throw new TransferProblemException();
    }

    if (rvConnectionInfo.getInitiator() == Initiator.ME
        && ack.getIcbmMessageId() != rvSessionId) {
      LOGGER.warning("Buddy sent wrong ICBM message ID: "
          + ack.getIcbmMessageId() + " should have been " + rvSessionId);
      throw new TransferProblemException();
    }
  }

  private static long getTotalSize(Collection<TransferredFile> files) {
    long totalSize = 0;
    for (TransferredFile file : files) totalSize += file.getSize();
    return totalSize;
  }
}
