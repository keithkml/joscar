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

import net.kano.joustsim.oscar.oscar.service.icbm.ft.ConnectionType;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FailureEventException;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectedEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectionTimedOutEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;

import java.io.IOException;
import java.util.logging.Logger;

//TOLATER: allow limiting bandwidth
public abstract class TransferController extends AbstractStateController
    implements PausableController, TimeoutableController, ConnectedController {
  private static final Logger LOGGER = Logger
      .getLogger(TransferController.class.getName());

  private volatile boolean cancelled = false;
  private boolean connected = false;
  private Thread transferThread;
  private boolean suppressErrors = false;
  @SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized"})
  private RvConnection transfer;

  private final PauseHelper pauseHelper = new PauseHelperImpl();
  private StreamInfo stream;

  protected synchronized void pauseTimeout() {
    LOGGER.info("File transfer timeout paused");
    transfer.getTimeoutHandler().pauseTimeout(this);
  }

  protected synchronized void resumeTimeout() {
    LOGGER.info("File transfer timeout resumed");
    transfer.getTimeoutHandler().unpauseTimeout(this);
  }

  public ConnectionType getTimeoutType() {
    return null;
  }

  public void cancelIfNotFruitful(long timeout) {
    boolean timedout = false;
    synchronized (TransferController.this) {
      if (!isConnected()) {
        cancelled = true;
        suppressErrors = true;
        timedout = true;
      }
    }
    if (timedout) {
      transferThread.interrupt();
      fireFailed(new ConnectionTimedOutEvent(timeout));
    }
  }

  protected synchronized boolean shouldSuppressErrors() {
    return suppressErrors;
  }

  public void start(final RvConnection transfer, StateController last) {
    this.transfer = transfer;
    stream = (StreamInfo) last.getEndStateInfo();
    transferThread = new Thread(new Runnable() {
      public void run() {
        try {
          synchronized (TransferController.this) {
            transfer.getTimeoutHandler().startTimeout(TransferController.this);
          }
          transferInThread(transfer);

        } catch (Exception e) {
          if (!shouldSuppressErrors()) {
            fireFailed(e);
          }
        }
      }

    }, "File transfer thread");

    transferThread.start();
  }

  protected long getTransferTimeoutMillis() {
    return transfer.getSettings().getDefaultPerConnectionTimeout(
        transfer.getRvSessionInfo().getInitiator());
  }

  public void stop() {
    LOGGER.info("Stopping transfer controller");
    cancelled = true;
    transferThread.interrupt();
  }

  protected boolean shouldStop() {
    return cancelled;
  }

  protected void setConnected() {
    synchronized (this) {
      if (connected) return;
      connected = true;
    }
    LOGGER.info("File transfer is now connected");
    transfer.getEventPost().fireEvent(new ConnectedEvent());
  }

  public synchronized boolean isConnected() {
    return connected;
  }

  public boolean didConnect() {
    return isConnected();
  }

  protected abstract void transferInThread(
      RvConnection transfer)
      throws IOException, FailureEventException;

  public void pauseTransfer() {
    pauseHelper.setPaused(true);
  }

  public void unpauseTransfer() {
    pauseHelper.setPaused(false);
  }

  /**
   * Returns true if it waited, false if not. If this method returns true it
   * should be called again. If this method returns it does not necessarily mean
   * the controller has been unpaused.
   */
  protected boolean waitUntilUnpause() {
    return pauseHelper.waitUntilUnpause();
  }

  public StreamInfo getStream() {
    return stream;
  }
}
