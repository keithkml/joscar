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

import net.kano.joscar.rv.RvSession;
import static net.kano.joscar.rvcmd.AbstractRejectRvCmd.REJECTCODE_CANCELLED;
import net.kano.joscar.rvcmd.ConnectionRequestRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendRejectRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendAcceptRvCmd;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ReceiveFileController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectionCompleteEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;

import java.util.logging.Logger;

public class IncomingFileTransferImpl
    extends IncomingRvConnectionImpl implements IncomingFileTransfer {
  //TODO: send rejection RV when we fail, if we haven't gotten one
  //TODO: don't re-use connection controllers
  private static final Logger LOGGER = Logger
      .getLogger(IncomingFileTransferImpl.class.getName());

  private FileMapper fileMapper;

  IncomingFileTransferImpl(RvConnectionManager rvConnectionManager,
      RvSession session) {
    super(rvConnectionManager, session);

    fileMapper = new DefaultFileMapper(getBuddyScreenname());
  }

  protected FtRvSessionHandler createSessionHandler() {
    return new IncomingFtRvSessionHandler();
  }

  public synchronized void setFileMapper(FileMapper mapper) {
    fileMapper = mapper;
  }

  public synchronized FileMapper getFileMapper() { return fileMapper; }

  protected void sendAcceptRv() {
    getRvSession().sendRv(new FileSendAcceptRvCmd());
  }

  protected void sendRejectRv() {
    getRvSession().sendRv(new FileSendRejectRvCmd(REJECTCODE_CANCELLED));
  }

  protected StateController getNextStateControllerFromSuccessState(
      StateController oldController, StateInfo oldStateInfo) {
    if (oldController instanceof ReceiveFileController) {
      LOGGER.fine("Changing from success of receive controller to "
          + "completed");
      queueStateChange(RvConnectionState.FINISHED,
          new ConnectionCompleteEvent());
      return null;

    } else {
      if (oldStateInfo instanceof StreamInfo) {
        LOGGER.fine("Changing from success of conn controller "
            + oldController + " to receive");
        return new ReceiveFileController();

      } else {
        throw new IllegalStateException("Unknown last controller "
            + oldController);
      }
    }
  }

  protected StateController getNextStateFromErrorWithUnknownController(
      StateController oldController, StateInfo oldState,
      RvConnectionEvent event) {
    if (oldController instanceof ReceiveFileController) {
      queueStateChange(RvConnectionState.FAILED, event);
      return null;

    } else {
      throw new IllegalStateException("Unknown controller " + oldController);
    }
  }

  private class IncomingFtRvSessionHandler extends IncomingRvSessionHandler {
    protected void handleFirstRequest(ConnectionRequestRvCmd reqCmd) {
      FileSendReqRvCmd ftcmd = (FileSendReqRvCmd) reqCmd;
      setFileInfo(ftcmd.getFileSendBlock());
      setInvitationMessage(ftcmd.getMessage());
    }
  }
}
