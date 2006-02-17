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

import net.kano.joscar.DefensiveTools;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FailureEventException;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvSessionConnectionInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectionTimedOutEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailedStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.LocallyCancelledInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.SuccessfulStateInfo;

import java.io.IOException;
import java.util.logging.Logger;

public abstract class AbstractConnectionController
    extends AbstractStateController implements StreamInfoProvider, TimeoutableController {

  private static final Logger LOGGER = Logger
      .getLogger(AbstractConnectionController.class.getName());

  private StreamInfo stream;
  private RvConnection rvConnection;
  private Thread thread = null;
  private boolean timerStarted = false;
  private boolean connected = false;
  private Connector connector = null;

  {
    addControllerListener(new ControllerListener() {
      public void handleControllerSucceeded(StateController controller,
          SuccessfulStateInfo info) {
      }

      public void handleControllerFailed(StateController controller,
          FailedStateInfo info) {
        if (thread != null) thread.interrupt();
      }
    });
  }

  private long getConnectionTimeoutMillis() {
    return rvConnection.getSettings()
        .getPerConnectionTimeout(getTimeoutType());
  }

  public StreamInfo getStreamInfo() { return stream; }

  public RvConnection getRvConnection() {
    return rvConnection;
  }

  public RvSessionConnectionInfo getRvSessionInfo() {
    return rvConnection.getRvSessionInfo();
  }

  protected synchronized void stopConnectionTimer() {
    connected = true;
  }

  public Connector getConnector() {
    return connector;
  }

  public void setConnector(Connector connector) {
    this.connector = connector;
  }

  public void start(RvConnection transfer, StateController last) {
    DefensiveTools.checkNull(transfer, "transfer");
    StateInfo endState = getEndStateInfo();
    if (endState != null) {
      throw new IllegalStateException("state is alreaday " + endState);
    }

    this.rvConnection = transfer;

    try {
      connector.checkConnectionInfo();
      initializeBeforeStarting();
    } catch (Exception e) {
      fireFailed(e);
      return;
    }

    thread = new Thread(new Runnable() {
      public void run() {
        try {
          openConnectionInThread();
        } catch (Exception e) {
          fireFailed(e);
        }
      }
    });

    if (shouldStartTimerAutomatically()) startTimer();
    thread.start();
  }

  protected boolean shouldStartTimerAutomatically() {
    return true;
  }

  protected void startTimer() {
    synchronized (this) {
      if (timerStarted) return;
      timerStarted = true;
    }
    rvConnection.getTimeoutHandler().startTimeout(this);
  }

  public void cancelIfNotFruitful(long timeout) {
    if (!isConnected()) {
      fireFailed(new ConnectionTimedOutEvent(timeout));
    }
  }

  public synchronized boolean isConnected() { return connected; }

  public void stop() {
    fireFailed(new LocallyCancelledInfo());
  }

  protected void initializeBeforeStarting() throws IOException { }

  protected void openConnectionInThread() {
    try {
      LOGGER.fine(this + " opening socket");
      prepareStream();
      stream = createStream();
      LOGGER.fine(this + " initializing connection in thread");
      initializeConnectionInThread();

    } catch (Exception e) {
      fireFailed(e);
    }
  }

  protected void prepareStream() throws IOException {
    connector.prepareStream();
  }

  protected StreamInfo createStream() throws IOException {
    return connector.createStream();
  }

  protected abstract void handleConnectingState();

  protected abstract void handleResolvingState();

  protected void fireConnected() {
    fireSucceeded(stream);
  }

  protected void initializeConnectionInThread()
      throws IOException, FailureEventException {
    fireConnected();
  }
}