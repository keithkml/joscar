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

package net.kano.joustsim.oscar.oscar.service.icbm;

import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rvcmd.AcceptRvCmd;
import net.kano.joscar.rvcmd.ConnectionRequestRvCmd;
import net.kano.joscar.rvcmd.RejectRvCmd;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.AbstractRvSessionHandler;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.OutgoingRvConnectionImpl;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionEventListener;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionState;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendPassivelyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.StartingControllerEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailedStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;

import java.util.ArrayList;
import java.util.List;

public class MockOutgoingRvConnection
    extends OutgoingRvConnectionImpl implements MockRvConnection {
  private final Object completionLock = new Object();
  private StateInfo endStateInfo = null;
  private boolean done = false;
  private List<StateController> hit = new ArrayList<StateController>();

  public MockOutgoingRvConnection() {
    super(new Screenname("me"), new MockRvSessionConnectionInfo());

    addEventListener(new RvConnectionEventListener() {
      public void handleEventWithStateChange(RvConnection transfer,
          RvConnectionState state, RvConnectionEvent event) {
        if (state == RvConnectionState.FAILED
            || state == RvConnectionState.FINISHED) {
          end(null);
        }
      }

      public void handleEvent(RvConnection transfer, RvConnectionEvent event) {
        if (event instanceof StartingControllerEvent) {
          hit.add(((StartingControllerEvent) event).getController());
        }
      }
    });
    setTimeoutHandler(new NullTimeoutHandler());
  }

  public MockRvSessionConnectionInfo getRvSessionInfo() {
    return (MockRvSessionConnectionInfo) super.getRvSessionInfo();
  }

  protected StateController getNextControllerFromUnknownError(
      StateController oldController, FailedStateInfo failedStateInfo,
      RvConnectionEvent event) {
    end(failedStateInfo);
    return null;
  }

  protected StateController getNextControllerFromUnknownSuccess(
      StateController oldController, StateInfo endState) {
    end(endState);
    return null;
  }

  protected StateController createConnectedController(StateInfo endState) {
    end(endState);
    return null;
  }

  private void end(StateInfo oldStateInfo) {
    synchronized(completionLock) {
      if (oldStateInfo != null) endStateInfo = oldStateInfo;
      done = true;
      completionLock.notifyAll();
    }
  }

  public StateInfo waitForCompletion() {
    synchronized(completionLock) {
      if (done) return endStateInfo;
      try {
        completionLock.wait();

        return endStateInfo;
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
    }
  }
  protected AbstractRvSessionHandler createSessionHandler() {
    return new MockOutgoingRvSessionHandler();
  }

  public MockRvSessionHandler getRvSessionHandler() {
    return (MockRvSessionHandler) super.getRvSessionHandler();
  }

  @SuppressWarnings({"ReturnOfCollectionOrArrayField"})
  public List<StateController> getHitControllers() {
    return hit;
  }

  public void sendRequest() {
    startStateController(new SendPassivelyController());
  }

  private class MockOutgoingRvSessionHandler
      extends OutgoingRvSessionHandler implements MockRvSessionHandler {
    public void handleIncomingReject(RecvRvEvent event, RejectRvCmd rejectCmd) {
      super.handleIncomingReject(event, rejectCmd);
    }

    public void handleIncomingAccept(RecvRvEvent event, AcceptRvCmd acceptCmd) {
      super.handleIncomingAccept(event, acceptCmd);
    }

    public void handleIncomingRequest(RecvRvEvent event,
        ConnectionRequestRvCmd reqCmd) {
      super.handleIncomingRequest(event, reqCmd);
    }
  }
}
