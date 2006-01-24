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
import net.kano.joscar.rvcmd.RequestRvCmd;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectToProxyForOutgoingController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ManualTimeoutController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.OutgoingConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.RedirectToProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendOverProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendPassivelyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.BuddyCancelledEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailedStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailureEventInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.SuccessfulStateInfo;

import java.util.logging.Logger;

public abstract class OutgoingRvConnectionImpl extends RvConnectionImpl
    implements OutgoingRvConnection {
  private static final Logger LOGGER = Logger
      .getLogger(OutgoingRvConnectionImpl.class.getName());

  public OutgoingRvConnectionImpl(RvConnectionManager rvConnectionManager,
      RvSession session) {
    super(rvConnectionManager, session);
  }

  public synchronized StateController getNextStateController() {
    StateController oldController = getStateController();
    StateInfo endState = oldController.getEndStateInfo();
    if (endState instanceof SuccessfulStateInfo) {
      if (isConnectionController(oldController)) {
        return getConnectedController();

      } else {
        return getNextControllerFromUnknownSuccess(oldController, endState);
      }

    } else if (endState instanceof FailedStateInfo) {
      RvConnectionEvent event = null;
      if (endState instanceof FailureEventInfo) {
        FailureEventInfo failureEventInfo = (FailureEventInfo) endState;

        event = failureEventInfo.getEvent();
      }

      if (isLanController(oldController)) {
        if (event != null) queueEvent(event);
        return new OutgoingConnectionController(ConnectionType.INTERNET);

      } else if (oldController instanceof SendPassivelyController
          || isInternetController(oldController)
          || oldController instanceof ConnectToProxyForOutgoingController) {
        if (event != null) queueEvent(event);
        return new RedirectToProxyController();

      } else if (oldController instanceof RedirectToProxyController) {
        setState(RvConnectionState.FAILED,
            event == null ? new UnknownErrorEvent() : event);
        return null;

      } else {
        return getNextControllerFromUnknownError(oldController,
            (FailedStateInfo) endState, event);
      }
    } else {
      throw new IllegalStateException("Unknown previous state " + endState);
    }
  }

  protected abstract StateController getNextControllerFromUnknownError(
      StateController oldController, FailedStateInfo failedStateInfo,
      RvConnectionEvent event);

  protected abstract StateController getNextControllerFromUnknownSuccess(
      StateController oldController, StateInfo endState);

  protected abstract StateController getConnectedController();

  private static boolean isLanController(StateController oldController) {
    return oldController instanceof OutgoingConnectionController
        && ((OutgoingConnectionController) oldController).getConnectionType()
        == ConnectionType.LAN;
  }

  private static boolean isInternetController(StateController oldController) {
    return oldController instanceof OutgoingConnectionController
        && ((OutgoingConnectionController) oldController).getConnectionType()
        == ConnectionType.INTERNET;
  }

  private boolean isConnectionController(StateController oldController) {
    return oldController instanceof SendPassivelyController
        || isLanController(oldController)
        || isInternetController(oldController)
        || oldController instanceof RedirectToProxyController
        || oldController instanceof ConnectToProxyForOutgoingController
        || oldController instanceof SendOverProxyController;
  }

  protected AbstractRvSessionHandler createSessionHandler() {
    return new AbstractRvSessionHandler(this) {
      protected void handleIncomingReject(RecvRvEvent event,
          RejectRvCmd rejectCmd) {
        setState(RvConnectionState.FAILED,
            new BuddyCancelledEvent(rejectCmd.getRejectCode()));
      }

      protected void handleIncomingAccept(RecvRvEvent event,
          AcceptRvCmd acceptCmd) {
        ManualTimeoutController mtc = getManualTimeoutController();
        if (mtc != null) mtc.startTimeoutTimer();
      }

      private synchronized ManualTimeoutController getManualTimeoutController() {
        StateController controller = getStateController();
        if (controller instanceof ManualTimeoutController) {
          return (ManualTimeoutController) controller;
        }
        return null;
      }

      protected void handleIncomingRequest(RecvRvEvent event,
          ConnectionRequestRvCmd reqCmd) {
        int reqType = reqCmd.getRequestIndex();
        if (reqType > RequestRvCmd.REQINDEX_FIRST) {
          HowToConnect how = processRedirect(reqCmd);
          if (how == HowToConnect.PROXY) {
            changeStateController(
                new OutgoingConnectionController(ConnectionType.LAN));
          } else if (how == HowToConnect.NORMAL) {
            changeStateController(
                new ConnectToProxyForOutgoingController());
          }
        } else {
          LOGGER.warning("got unknown rv connection request type in outgoing "
              + "transfer: " + reqType);
        }
      }
    };
  }
}
