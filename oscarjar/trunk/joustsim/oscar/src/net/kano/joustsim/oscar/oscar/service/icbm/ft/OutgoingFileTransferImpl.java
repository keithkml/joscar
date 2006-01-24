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

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.InvitationMessage;
import net.kano.joscar.rvcmd.sendfile.FileSendBlock;
import static net.kano.joscar.rvcmd.sendfile.FileSendBlock.SENDTYPE_DIR;
import static net.kano.joscar.rvcmd.sendfile.FileSendBlock.SENDTYPE_SINGLEFILE;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ChecksumController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendFileController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendOverProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendPassivelyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ChecksummingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectionCompleteEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.ComputedChecksumsInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailedStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutgoingFileTransferImpl
    extends OutgoingRvConnectionImpl implements OutgoingFileTransfer {
  private List<File> files = new ArrayList<File>();
  private String folderName;
  private Map<File, Long> checksums = new HashMap<File, Long>();
  private FileChecksummer fileChecksummer = new FileChecksummer() {
    public long getChecksum(File file) throws IOException {
      Long sum = checksums.get(file);
      if (sum == null) {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        Checksummer summer = new Checksummer(raf.getChannel(), raf.length());
        fireEvent(new ChecksummingEvent(file, summer));
        sum = summer.compute();
      }
      return sum;
    }
  };
  private FileTransferHelper helper = new FileTransferHelper(this);

  public OutgoingFileTransferImpl(RvConnectionManager rvConnectionManager,
      RvSession session) {
    super(rvConnectionManager, session);
  }

  public void sendRequest(InvitationMessage msg) {
    setInvitationMessage(msg);
    startStateController(new ChecksumController());
  }

  private Map<File, String> nameMappings = new HashMap<File, String>();

  public synchronized Map<File, String> getNameMappings() {
    return Collections.unmodifiableMap(new HashMap<File, String>(nameMappings));
  }

  public synchronized void mapName(File file, String name) {
    DefensiveTools.checkNull(file, "file");

    nameMappings.put(file, name);
  }

  public synchronized String getFolderName() { return folderName; }

  public synchronized List<File> getFiles() {
    return DefensiveTools.getUnmodifiableCopy(files);
  }

  public synchronized void setFile(File file) {
    DefensiveTools.checkNull(file, "file");

    this.folderName = null;
    this.files = Collections.singletonList(file);
  }

  public synchronized void setFiles(String folderName, List<File> files) {
    DefensiveTools.checkNull(folderName, "folderName");
    DefensiveTools.checkNullElements(files, "files");

    this.folderName = folderName;
    this.files = DefensiveTools.getUnmodifiableCopy(files);
  }

  public synchronized String getMappedName(File file) {
    String name = nameMappings.get(file);
    if (name == null) {
      return file.getName();
    } else {
      return name;
    }
  }

  public FileChecksummer getChecksummer() { return fileChecksummer; }

  public FileSendBlock getFileInfo() {
    long totalSize = 0;
    List<File> files = getFiles();
    for (File file : files) totalSize += file.length();
    int numFiles = files.size();
    boolean folderMode = numFiles > 1;
    int sendType = folderMode ? SENDTYPE_DIR : SENDTYPE_SINGLEFILE;
    String filename = folderMode ? getFolderName()
        : getMappedName(files.get(0));
    return new FileSendBlock(sendType, filename, numFiles, totalSize);
  }

  protected StateController getNextControllerFromUnknownError(
      StateController oldController, FailedStateInfo failedStateInfo,
      RvConnectionEvent event) {
    if (oldController instanceof SendFileController) {
      //TODO: retry send with other controllers like receiver does
//                if (getState() == FileTransferState.TRANSFERRING) {
      setState(RvConnectionState.FAILED,
          event == null ? new UnknownErrorEvent() : event);
//                } else {
//
//                }
      return null;

    } else {
      throw new IllegalStateException("unknown previous controller "
          + oldController);
    }
  }

  protected StateController getNextControllerFromUnknownSuccess(
      StateController oldController, StateInfo endState) {
    if (oldController instanceof SendFileController) {
      queueStateChange(RvConnectionState.FINISHED,
          new ConnectionCompleteEvent());
      return null;

    } else if (oldController instanceof ChecksumController) {
      if (endState instanceof ComputedChecksumsInfo) {
        ComputedChecksumsInfo info = (ComputedChecksumsInfo) endState;
        checksums.putAll(info.getChecksums());
      }
      if (getSettings().isOnlyUsingProxy()) {
        return new SendOverProxyController();
      } else {
        return new SendPassivelyController();
      }

    } else {
      throw new IllegalStateException("unknown previous controller "
          + oldController);
    }
  }

  protected StateController getConnectedController() {
    return new SendFileController();
  }

  public RvRequestMaker getRvRequestMaker() {
    return helper.getRvRequestMaker();
  }

  public InvitationMessage getInvitationMessage() {
    return helper.getInvitationMessage();
  }

  private void setInvitationMessage(InvitationMessage msg) {
    helper.setInvitationMessage(msg);
  }

  public void sendRequest() {
    sendRequest(null);
  }
}