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

import net.kano.joscar.MiscTools;
import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rv.RvSnacResponseEvent;
import net.kano.joscar.rvcmd.AcceptRvCmd;
import net.kano.joscar.rvcmd.ConnectionRequestRvCmd;
import net.kano.joscar.rvcmd.InvitationMessage;
import net.kano.joscar.rvcmd.RejectRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendAcceptRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendBlock;
import net.kano.joscar.rvcmd.sendfile.FileSendRejectRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousSessionHandler;

public abstract class FileTransferImpl
    extends RvConnectionImpl implements FileTransfer {

  private FileSendBlock fileInfo;
  private InvitationMessage message;

  protected FileTransferImpl(RvConnectionManager rvConnectionManager,
                             RvSession session) {
    super(rvConnectionManager,  session);
    setPerConnectionTimeout(ConnectionType.LAN, 2L);
    proxyInfo = getAimConnection().getProxy();
  }

  protected synchronized void setInvitationMessage(InvitationMessage message) {
    this.message = message;
  }

  protected synchronized void setFileInfo(FileSendBlock fileInfo) {
    this.fileInfo = fileInfo;
  }

  public synchronized FileSendBlock getFileInfo() { return fileInfo; }

  public synchronized InvitationMessage getInvitationMessage() {
    return message;
  }

  public String toString() {
    return MiscTools.getClassName(this) + " with "
        + getBuddyScreenname() + " of " + getFileInfo().getFilename();
  }

  protected abstract class FtRvSessionHandler
      implements RendezvousSessionHandler {
    public final void handleRv(RecvRvEvent event) {
      RvCommand cmd = event.getRvCommand();
      if (cmd instanceof FileSendReqRvCmd) {
        FileSendReqRvCmd reqCmd = (FileSendReqRvCmd) cmd;
        handleIncomingRequest(event, reqCmd);

      } else if (cmd instanceof FileSendAcceptRvCmd) {
        FileSendAcceptRvCmd acceptCmd = (FileSendAcceptRvCmd) cmd;
        handleIncomingAccept(event, acceptCmd);

      } else if (cmd instanceof FileSendRejectRvCmd) {
        FileSendRejectRvCmd rejectCmd = (FileSendRejectRvCmd) cmd;
        handleIncomingReject(event, rejectCmd);
      }
    }

    protected abstract void handleIncomingReject(RecvRvEvent event,
                                                 RejectRvCmd rejectCmd);

    protected abstract void handleIncomingAccept(RecvRvEvent event,
                                                 AcceptRvCmd acceptCmd);

    protected abstract void handleIncomingRequest(RecvRvEvent event,
                                                  ConnectionRequestRvCmd reqCmd);

    public void handleSnacResponse(RvSnacResponseEvent event) {
    }

    protected RvConnection getFileTransfer() {
      return FileTransferImpl.this;
    }
  }

}
