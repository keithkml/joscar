/*
 * Copyright (c) 2006, The Joust Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 * - Neither the name of the Joust Project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * File created by keithkml
 */

package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.AcceptRvCmd;
import net.kano.joscar.rvcmd.ConnectionRequestRvCmd;
import net.kano.joscar.rvcmd.RejectRvCmd;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectToProxyForIncomingController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ManualTimeoutController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.OutgoingConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.RedirectConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.RedirectToProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.BuddyCancelledEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectionFailedEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailedStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailureEventInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.SuccessfulStateInfo;

import java.util.logging.Logger;

public abstract class IncomingRvConnectionImpl
    extends RvConnectionImpl implements IncomingRvConnection {
  private static final Logger LOGGER = Logger
      .getLogger(IncomingRvConnectionImpl.class.getName());

  private RvConnectionInfo originalRemoteHostInfo;
  private boolean accepted = false;
  private boolean rejected = false;
  private boolean alwaysRedirect = false;

  public IncomingRvConnectionImpl(RvConnectionManager rvConnectionManager,
      RvSession session) {
    super(rvConnectionManager, session);
  }

  public synchronized RvConnectionInfo getOriginalRemoteHostInfo() {
    return originalRemoteHostInfo;
  }

  synchronized void setOriginalRemoteHostInfo(RvConnectionInfo info) {
    this.originalRemoteHostInfo = info;
  }

  public synchronized boolean isAccepted() { return accepted; }

  public synchronized boolean isRejected() { return rejected; }

  public synchronized boolean isAlwaysRedirectEnabled() {
    return alwaysRedirect;
  }

  public synchronized void setAlwaysRedirect(boolean alwaysRedirect) {
    this.alwaysRedirect = alwaysRedirect;
  }

  public void accept() throws IllegalStateException {
    StateController controller;
    synchronized (this) {
      if (rejected) {
        throw new IllegalStateException("Transfer was already rejected");
      }
      if (accepted) return;
      accepted = true;

      boolean onlyUsingProxy = getSettings().isOnlyUsingProxy();
      if (!isAlwaysRedirectEnabled() && !onlyUsingProxy) {
        controller = new OutgoingConnectionController(ConnectionType.LAN);
      } else if (getSettings().isProxyRequestTrusted()) {
        controller = new ConnectToProxyForIncomingController();
      } else if (!onlyUsingProxy) {
        controller = new RedirectConnectionController();
      } else {
        controller = new RedirectToProxyController();
      }
    }

    startStateController(controller);
    LOGGER.fine("Sending accept command to " + getBuddyScreenname());
    getRvRequestMaker().sendRvAccept();
  }

  public void reject() throws IllegalStateException {
    synchronized (this) {
      if (accepted) {
        //TODO: cancel the transfer when rejecting?
        throw new IllegalStateException("transfer was already accepted");
      }
      if (rejected) return;
      rejected = true;
      close();
    }
    getRvRequestMaker().sendRvReject();
  }

  public synchronized StateController getNextStateController() {
    StateController oldController = getStateController();
    StateInfo oldStateInfo = oldController.getEndStateInfo();
    if (oldStateInfo instanceof SuccessfulStateInfo) {
      return getNextStateControllerFromSuccessState(oldController,
          oldStateInfo);

    } else if (oldStateInfo instanceof FailedStateInfo) {
      LOGGER.fine("Changing from failure of last controller");
      return getNextStateFromError(oldController, oldStateInfo);

    } else {
      throw new IllegalStateException("Unknown last state " + oldStateInfo);
    }
  }

  protected abstract StateController getNextStateControllerFromSuccessState(
      StateController oldController, StateInfo oldStateInfo);

  private synchronized StateController getNextStateFromError(
      StateController oldController, StateInfo oldState) {
    RvConnectionEvent event;
    if (oldState instanceof FailureEventInfo) {
      FailureEventInfo failureEventInfo = (FailureEventInfo) oldState;
      event = failureEventInfo.getEvent();
    } else {
      event = new UnknownErrorEvent();
    }
    if (oldController instanceof OutgoingConnectionController) {
      OutgoingConnectionController outController
          = (OutgoingConnectionController) oldController;
      if (outController.getConnectionType() == ConnectionType.LAN) {
        return new OutgoingConnectionController(ConnectionType.INTERNET);

      } else {
        return new ConnectToProxyForIncomingController();
      }

    } else if (oldController instanceof ConnectToProxyForIncomingController) {
      return new RedirectConnectionController();

    } else if (oldController instanceof RedirectConnectionController) {
      return new RedirectToProxyController();

    } else if (oldController instanceof RedirectToProxyController) {
      queueStateChange(RvConnectionState.FAILED,
          new ConnectionFailedEvent(event));
      return null;

    } else {
      return getNextStateFromErrorWithUnknownController(oldController, oldState,
          event);
    }
  }

  protected abstract StateController getNextStateFromErrorWithUnknownController(
      StateController oldController, StateInfo oldState,
      RvConnectionEvent event);


  public abstract class IncomingRvSessionHandler extends AbstractRvSessionHandler {
    public IncomingRvSessionHandler() {
      super(IncomingRvConnectionImpl.this);
    }

    protected void handleIncomingReject(RecvRvEvent event,
        RejectRvCmd rejectCmd) {
      BuddyCancelledEvent evt = new BuddyCancelledEvent(
          rejectCmd.getRejectCode());
      setState(RvConnectionState.FAILED, evt);
    }

    protected void handleIncomingAccept(RecvRvEvent event,
        AcceptRvCmd acceptCmd) {
      ManualTimeoutController mtc = null;
      synchronized (this) {
        StateController controller = getStateController();
        if (controller instanceof ManualTimeoutController) {
          mtc = (ManualTimeoutController) controller;
        }
      }
      if (mtc != null) mtc.startTimeoutTimer();
    }

    protected void handleIncomingRequest(RecvRvEvent event,
        ConnectionRequestRvCmd reqCmd) {
      int index = reqCmd.getRequestIndex();
      if (index == FileSendReqRvCmd.REQINDEX_FIRST) {
        handleFirstRequest(reqCmd);

        RvConnectionInfo connInfo = reqCmd.getConnInfo();
        setOriginalRemoteHostInfo(connInfo);
        setConnectionInfo(connInfo);
        putTransferProperty(RvConnectionPropertyHolder.KEY_REDIRECTED, false);

        RvConnectionManager rvmgr = getRvConnectionManager();
        rvmgr.fireNewIncomingConnection(IncomingRvConnectionImpl.this);

      } else if (index > FileSendReqRvCmd.REQINDEX_FIRST) {
        RvConnectionImpl.HowToConnect how = processRedirect(reqCmd);
        if (how == RvConnectionImpl.HowToConnect.PROXY) {
          changeStateController(new ConnectToProxyForIncomingController());
        } else if (how == RvConnectionImpl.HowToConnect.NORMAL) {
          changeStateController(
              new OutgoingConnectionController(ConnectionType.LAN));
        } else {
          throw new IllegalStateException("How to connect was " + how);
        }
      } else {
        LOGGER.warning("Got unknown request index " + index + " for "
            + reqCmd);
      }
    }

    protected abstract void handleFirstRequest(ConnectionRequestRvCmd reqCmd);
  }
}

//TODO: file this bug
abstract class Tester {
  abstract void ttt();
}