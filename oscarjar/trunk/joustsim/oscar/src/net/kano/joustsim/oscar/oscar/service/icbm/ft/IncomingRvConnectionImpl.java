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

import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectToProxyForIncomingController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.OutgoingConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.RedirectConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.RedirectToProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectionFailedEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailureEventInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.proxy.AimProxyInfo;

import java.util.logging.Logger;

public abstract class IncomingRvConnectionImpl
    extends RvConnectionImpl implements IncomingRvConnection {
  private static final Logger LOGGER = Logger
      .getLogger(IncomingRvConnectionImpl.class.getName());

  private boolean accepted = false;
  private boolean rejected = false;
  private boolean alwaysRedirect = false;

  public IncomingRvConnectionImpl(Screenname screenname,
      RvSessionConnectionInfo rvsessioninfo) {
    this(AimProxyInfo.forNoProxy(), screenname, rvsessioninfo);
  }

  public IncomingRvConnectionImpl(AimProxyInfo proxy,
      Screenname screenname, RvSessionConnectionInfo rvsessioninfo) {
    super(proxy, screenname, rvsessioninfo);
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
      boolean proxied = getRvSessionInfo().getConnectionInfo().isProxied();
      if (!proxied && !isAlwaysRedirectEnabled() && !onlyUsingProxy) {
        controller = new OutgoingConnectionController(ConnectionType.LAN);
      } else if (proxied && getSettings().isProxyRequestTrusted()) {
        controller = new ConnectToProxyForIncomingController();
      } else if (!onlyUsingProxy) {
        controller = new RedirectConnectionController();
      } else {
        controller = new RedirectToProxyController();
      }
    }
    if (startStateController(controller)) {
      LOGGER.fine("Sending accept command to " + getBuddyScreenname());
      getRvSessionInfo().getRequestMaker().sendRvAccept();
    }
  }

  public void reject() throws IllegalStateException {
    synchronized (this) {
      if (accepted) {
        throw new IllegalStateException("transfer was already accepted");
      }
      if (rejected) return;
      rejected = true;
    }
    close();
  }

  protected boolean isSomeConnectionController(StateController oldController) {
//    return oldController instanceof SendPassivelyController
//    || isLanController(oldController)
//      || isInternetController(oldController)
//      || oldController instanceof RedirectToProxyController
//      || oldController instanceof ConnectToProxyForOutgoingController
//      || oldController instanceof SendOverProxyController;

    return isLanController(oldController)
        || isInternetController(oldController)
        || oldController instanceof RedirectToProxyController
        || oldController instanceof ConnectToProxyForIncomingController;
  }

  protected abstract StateController getNextControllerFromSuccess(
      StateController oldController, StateInfo oldStateInfo);

  protected synchronized StateController getNextControllerFromError(
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
      if (outController.getTimeoutType() == ConnectionType.LAN) {
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
      return getNextControllerFromUnknownError(oldController, oldState, event);
    }
  }

  protected abstract StateController getNextControllerFromUnknownError(
      StateController oldController, StateInfo oldState,
      RvConnectionEvent event);

  public boolean isOpen() {
    return isAccepted() && super.isOpen();
  }
}