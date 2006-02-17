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
import net.kano.joscar.rvcmd.ConnectionRequestRvCmd;
import net.kano.joscar.rvcmd.RequestRvCmd;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectToProxyForOutgoingController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.OutgoingConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.RedirectToProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendOverProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendPassivelyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailedStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailureEventInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.proxy.AimProxyInfo;

import java.util.logging.Logger;

public abstract class OutgoingRvConnectionImpl extends RvConnectionImpl
    implements OutgoingRvConnection {
  private static final Logger LOGGER = Logger
      .getLogger(OutgoingRvConnectionImpl.class.getName());

  public OutgoingRvConnectionImpl(Screenname myScreenname,
      RvSessionConnectionInfo rvsessioninfo) {
    super(AimProxyInfo.forNoProxy(), myScreenname, rvsessioninfo);
  }

  public OutgoingRvConnectionImpl(AimProxyInfo proxy,
      Screenname myScreenname, RvSessionConnectionInfo rvsessioninfo) {
    super(proxy, myScreenname, rvsessioninfo);
  }

  protected StateController getNextControllerFromError(
      StateController oldController, StateInfo endState) {
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
  }

  protected abstract StateController getNextControllerFromUnknownError(
      StateController oldController, FailedStateInfo failedStateInfo,
      RvConnectionEvent event);

  protected boolean isSomeConnectionController(StateController oldController) {
    return oldController instanceof SendPassivelyController
        || isLanController(oldController)
        || isInternetController(oldController)
        || oldController instanceof RedirectToProxyController
        || oldController instanceof ConnectToProxyForOutgoingController
        || oldController instanceof SendOverProxyController;
  }

  protected AbstractRvSessionHandler createSessionHandler() {
    return new OutgoingRvSessionHandler();
  }

  protected class OutgoingRvSessionHandler extends AbstractRvSessionHandler {
    public OutgoingRvSessionHandler() {super(OutgoingRvConnectionImpl.this);}

    protected void handleIncomingRequest(RecvRvEvent event,
        ConnectionRequestRvCmd reqCmd) {
      int reqType = reqCmd.getRequestIndex();
      if (reqType > RequestRvCmd.REQINDEX_FIRST) {
        HowToConnect how = processRedirect(reqCmd);
        if (how == HowToConnect.PROXY || how == HowToConnect.NORMAL) {
          boolean worked;
          if (how == HowToConnect.PROXY) {
            worked = changeStateController(
                new ConnectToProxyForOutgoingController());

          } else {
            //noinspection ConstantConditions
            assert how == HowToConnect.NORMAL;
            worked = changeStateController(
                new OutgoingConnectionController(ConnectionType.LAN));
          }
          if (worked) {
            getRvSessionInfo().getRequestMaker().sendRvAccept();
          }
        } else if (how == HowToConnect.DONT) {
          changeStateController(new RedirectToProxyController());
        }
      } else {
        LOGGER.warning("got unknown rv connection request type in outgoing "
            + "transfer: " + reqType);
      }
    }
  }
}
