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

import junit.framework.TestCase;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionEventListener;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionState;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.OutgoingConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.StartingControllerEvent;
import net.kano.joscar.rvproto.rvproxy.RvProxyReadyCmd;
import net.kano.joscar.rvproto.rvproxy.RvProxyAckCmd;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.snaccmd.CapabilityBlock;

import java.util.List;
import java.net.UnknownHostException;
import java.net.InetAddress;

public abstract class RvConnectionTestCase extends TestCase {
  public static final CapabilityBlock MOCK_CAPABILITY
      = new CapabilityBlock(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15);

  protected abstract MockRvConnection getConnection();

  protected void generateRequestAndWaitForStream() {
    StateInfo end = generateRequestAndRun(getConnection());

    assertTrue(end instanceof StreamInfo);
  }

  protected StateInfo generateRequestAndRun(MockRvConnection conn) {
    conn.getRvSessionHandler().handleIncomingRequest(null, new GenericRequest());
    return conn.waitForCompletion();
  }

  protected MockProxyConnector getDirectedToProxyConnector() {
    return new MockProxyConnector(
        new MockProxyConnection(new RvProxyReadyCmd()));
  }

  protected void assertSentRvs(int numreqs, int accepts, int rejects) {
    MockRvConnection connection = getConnection();
    MockRvRequestMaker maker = connection.getRvSessionInfo().getRvRequestMaker();
    List<Integer> requests = maker.getSentRequests();
    assertEquals(numreqs, requests.size());
    for (int i = 0; i < numreqs; i++) {
      assertEquals((Object) (i + getBaseOutgoingRequestId()), requests.get(i));
    }
    assertEquals(accepts, maker.getAcceptCount());
    assertEquals(rejects, maker.getRejectCount());
  }

  protected abstract int getBaseOutgoingRequestId();

  protected MockProxyConnector getInitiateProxyConnector() {
    try {
      return new MockProxyConnector(new MockProxyConnection(
          new RvProxyAckCmd(InetAddress.getByName("9.9.9.9"), 1000),
          new RvProxyReadyCmd()));
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  protected StateInfo simulateBuddyRedirectionAndWait(
      MockIncomingRvConnection conn, HangConnector hangConnector,
      RvConnectionInfo conninfo) {
    MockRvSessionHandler handler = conn.getRvSessionHandler();
    handler.handleIncomingRequest(null, new GenericRequest());
    hangConnector.waitForConnectionAttempt();
    handler.handleIncomingRequest(null, new GenericRequest(2,
        conninfo));
    return conn.waitForCompletion();
  }

  protected void addNopConnector(MockRvConnection conn) {
    conn.addEventListener(new RvConnectionEventListener() {
      public void handleEventWithStateChange(RvConnection transfer,
          RvConnectionState state, RvConnectionEvent event) {
      }

      public void handleEvent(RvConnection transfer, RvConnectionEvent event) {
        if (event instanceof StartingControllerEvent) {
          StateController controller = ((StartingControllerEvent) event)
              .getController();
          if (controller instanceof OutgoingConnectionController) {
            OutgoingConnectionController ogc
                = (OutgoingConnectionController) controller;
            ogc.setConnector(new NopConnector());
          }
        }
      }
    });
  }
}
