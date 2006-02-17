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

import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousSessionHandler;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ProxyRedirectDisallowedEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.BuddyCancelledEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ManualTimeoutController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvSnacResponseEvent;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joscar.rvcmd.ConnectionRequestRvCmd;
import net.kano.joscar.rvcmd.AcceptRvCmd;
import net.kano.joscar.rvcmd.RejectRvCmd;
import net.kano.joscar.rvcmd.RvConnectionInfo;

import java.util.logging.Logger;

public abstract class AbstractRvSessionHandler implements RendezvousSessionHandler {
  private static final Logger LOGGER = Logger
      .getLogger(AbstractRvSessionHandler.class.getName());
  
  private final RvConnection connection;

  public AbstractRvSessionHandler(RvConnection transfer) {
    this.connection = transfer;
  }

  protected final RvConnectionEvent getConnectError(RvConnectionInfo connInfo) {
    RvConnectionEvent error = null;
    RvConnectionSettings settings = connection.getSettings();
    if (settings.isOnlyUsingProxy()) {
      if (!(connInfo.isProxied() && settings.isProxyRequestTrusted())) {
        error = new ProxyRedirectDisallowedEvent(
            connInfo.getProxyIP());
      }
    }
    return error;
  }

  private synchronized ManualTimeoutController getManualTimeoutController() {
    if (!(connection instanceof StateBasedRvConnection)) return null;
    StateBasedRvConnection stateBasedRvConnection
        = (StateBasedRvConnection) connection;
    StateController controller = stateBasedRvConnection.getStateController();
    if (!(controller instanceof ManualTimeoutController)) return null;
    return (ManualTimeoutController) controller;
  }

  protected static enum HowToConnect { DONT, PROXY, NORMAL }

  protected HowToConnect processRedirect(ConnectionRequestRvCmd reqCmd) {
    RvSessionConnectionInfo sessionInfo = connection.getRvSessionInfo();
    sessionInfo.setRequestIndex(reqCmd.getRequestIndex());
    RvConnectionInfo connInfo = reqCmd.getConnInfo();
    LOGGER.fine("Received redirect packet: " + reqCmd
        + " - to " + connInfo);
    RvConnectionEvent error = getConnectError(connInfo);
    HowToConnect how;
    if (error == null) {
      sessionInfo.setConnectionInfo(connInfo);
      LOGGER.fine("Storing connection info for redirect: " + connInfo);
      if (connInfo.isProxied()) {
        LOGGER.finer("Deciding to change to proxy connect controller");
        how = HowToConnect.PROXY;
        sessionInfo.setInitiator(Initiator.BUDDY);
      } else {
        LOGGER.finer("Deciding to change to normal connect controller");
        how = HowToConnect.NORMAL;
        sessionInfo.setInitiator(Initiator.BUDDY);
      }
    } else {
      //      we could ignore it
//      connection.close(error);
      how = HowToConnect.DONT;
    }
    return how;
  }

  public final void handleRv(RecvRvEvent event) {
    RvCommand cmd = event.getRvCommand();
    if (cmd instanceof ConnectionRequestRvCmd) {
      handleIncomingRequest(event, (ConnectionRequestRvCmd) cmd);

    } else if (cmd instanceof AcceptRvCmd) {
      handleIncomingAccept(event, (AcceptRvCmd) cmd);

    } else if (cmd instanceof RejectRvCmd) {
      handleIncomingReject(event, (RejectRvCmd) cmd);
    }
  }

  protected void handleIncomingReject(RecvRvEvent event,
      RejectRvCmd rejectCmd) {
    connection.close(new BuddyCancelledEvent(rejectCmd.getRejectCode()));
  }

  protected void handleIncomingAccept(RecvRvEvent event,
      AcceptRvCmd acceptCmd) {
    ManualTimeoutController mtc = getManualTimeoutController();
    if (mtc != null) mtc.startTimeoutTimer();
  }

  protected abstract void handleIncomingRequest(RecvRvEvent event,
                                                ConnectionRequestRvCmd reqCmd);

  public void handleSnacResponse(RvSnacResponseEvent event) {
  }

  protected RvConnection getRvConnection() { return connection; }
}
