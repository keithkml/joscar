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
import net.kano.joscar.rvcmd.ConnectionRequestRvCmd;
import net.kano.joscar.rvcmd.InvitationMessage;
import net.kano.joscar.rvcmd.sendfile.FileSendBlock;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousSessionHandler;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.MutableSessionConnectionInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ReceiveFileController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectedController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectionCompleteEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;
import net.kano.joustsim.oscar.proxy.AimProxyInfo;

import java.util.logging.Logger;

public class IncomingFileTransferImpl
    extends IncomingRvConnectionImpl implements IncomingFileTransfer {
  private static final Logger LOGGER = Logger
      .getLogger(IncomingFileTransferImpl.class.getName());

  private FileMapper fileMapper;
  private FileTransferHelper helper = new FileTransferHelper(this);

  IncomingFileTransferImpl(AimProxyInfo proxy,
      Screenname screenname, RvSessionConnectionInfo rvsessioninfo) {
    super(proxy, screenname, rvsessioninfo);

    fileMapper = new DefaultFileMapper(getBuddyScreenname(), System.getProperty("user.dir"));
  }

  public IncomingFileTransferImpl(AimProxyInfo proxy, Screenname screenname,
      RvSession session) {
    this(proxy, screenname, new MutableSessionConnectionInfo(session));
    ((MutableSessionConnectionInfo) getRvSessionInfo())
        .setMaker(new FileTransferRequestMaker(this));
  }

  protected RendezvousSessionHandler createSessionHandler() {
    return new IncomingFtRvSessionHandler();
  }

  public synchronized void setFileMapper(FileMapper mapper) {
    fileMapper = mapper;
  }

  public synchronized FileMapper getFileMapper() { return fileMapper; }

  protected ConnectedController createConnectedController(StateInfo endState) {
    return new ReceiveFileController();
  }

  protected boolean isConnectedController(StateController controller) {
    return controller instanceof ReceiveFileController;
  }

  protected StateController getNextControllerFromSuccess(
      StateController oldController, StateInfo oldStateInfo) {
    if (oldController instanceof ReceiveFileController) {
      LOGGER.fine("Changing from success of receive controller to "
          + "completed");
      queueStateChange(RvConnectionState.FINISHED,
          new ConnectionCompleteEvent());
      return null;

    } else if (oldStateInfo instanceof StreamInfo) {
      throw new IllegalStateException("stream info here??");

    } else {
      throw new IllegalStateException("Unknown last controller "
          + oldController);
    }
  }

  protected StateController getNextControllerFromUnknownError(
      StateController oldController, StateInfo oldState,
      RvConnectionEvent event) {
    if (oldController instanceof ReceiveFileController) {
      queueStateChange(RvConnectionState.FAILED, event);
      return null;

    } else {
      throw new IllegalStateException("Unknown controller " + oldController);
    }
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

  public FileSendBlock getFileInfo() {
    return helper.getFileInfo();
  }

  private void setFileInfo(FileSendBlock block) {
    helper.setFileInfo(block);
  }

  private class IncomingFtRvSessionHandler extends AbstractIncomingRvSessionHandler {
    public IncomingFtRvSessionHandler() {
      super(IncomingFileTransferImpl.this);
    }

    protected void handleFirstRequest(ConnectionRequestRvCmd reqCmd) {
      FileSendReqRvCmd ftcmd = (FileSendReqRvCmd) reqCmd;
      setFileInfo(ftcmd.getFileSendBlock());
      setInvitationMessage(ftcmd.getMessage());
    }
  }
}
