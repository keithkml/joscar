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

import net.kano.joscar.rvcmd.AcceptRvCmd;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joustsim.TestHelper;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.ControllerRestartConsultant;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.DefaultRvConnectionEventListener;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionEventListener;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionSettings;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionState;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.AbstractProxyConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectToProxyForOutgoingController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.OutgoingConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.PassiveConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.RedirectToProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.StartedControllerEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.StartingControllerEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class OutgoingRvConnectionFunctionalTests extends RvConnectionTestCase {
  private MockOutgoingRvConnection conn;

  protected void setUp() throws Exception {
    conn = new MockOutgoingRvConnection();
  }

  protected void tearDown() throws Exception {
    conn = null;
  }

  protected MockRvConnection getConnection() {
    return conn;
  }

  protected int getBaseOutgoingRequestId() {
    return 1;
  }

  public void testPassive() {
    conn.addEventListener(new DefaultRvConnectionEventListener() {
      public void handleEvent(RvConnection transfer, RvConnectionEvent event) {
        if (event instanceof StartingControllerEvent) {
          StateController controller = ((StartingControllerEvent) event).getController();
          if (controller instanceof PassiveConnectionController) {
            PassiveConnectionController passive
                = (PassiveConnectionController) controller;
            passive.setConnector(new PassiveNopConnector());
          }
        }
      }
    });
    conn.sendRequest();
    conn.getRvSessionHandler().handleIncomingAccept(null, new GenericAcceptRv());
    assertEndWasStream(conn.waitForCompletion());
    assertHit(PassiveConnectionController.class);
    assertSentRvs(1, 0, 0);
  }

  public void testPassiveNotAcceptedRestarted() {
    final SingleRestartConsultant restarter = new SingleRestartConsultant();
    conn.setRestartConsultant(restarter);
    conn.addEventListener(new DefaultRvConnectionEventListener() {
      public void handleEvent(RvConnection transfer, RvConnectionEvent event) {
        if (event instanceof StartingControllerEvent) {
          StateController controller = ((StartingControllerEvent) event).getController();
          if (controller instanceof PassiveConnectionController) {
            PassiveConnectionController passive
                = (PassiveConnectionController) controller;
            if (restarter.restarted()) {
              passive.setConnector(new PassiveNopConnector());
            } else {
              passive.setConnector(new FailConnector());
            }
          }
        }
      }
    });
    conn.sendRequest();
    assertEndWasStream(conn.waitForCompletion());
    assertTrue(restarter.restarted());
    assertEquals(2, TestHelper.findInstances(getConnection().getHitControllers(),
        PassiveConnectionController.class).size());
    assertSentRvs(2, 0, 0);
  }

  public void testPassiveTimesOut() {
    conn.addEventListener(new RvConnectionEventListener() {
      public void handleEventWithStateChange(RvConnection transfer,
          RvConnectionState state, RvConnectionEvent event) {
      }

      public void handleEvent(RvConnection transfer, RvConnectionEvent event) {
        if (event instanceof StartingControllerEvent) {
          StateController controller = ((StartingControllerEvent) event).getController();
          if (controller instanceof PassiveConnectionController) {
            PassiveConnectionController passive
                = (PassiveConnectionController) controller;
            passive.setConnector(new HangConnector());

          } else if (controller instanceof RedirectToProxyController) {
            RedirectToProxyController redirect
                = (RedirectToProxyController) controller;
            redirect.setConnector(getInitiateProxyConnector());

          } else if (!(controller instanceof MockConnectedController)) {
            throw new IllegalStateException("Unknown controller " + controller);
          }
        }
        if (event instanceof StartedControllerEvent) {
          StateController controller
              = ((StartedControllerEvent) event).getController();
          if (controller instanceof PassiveConnectionController) {
            ((PassiveConnectionController) controller).cancelIfNotFruitful(0);
          }
        }
      }
    });
    conn.sendRequest();
    conn.getRvSessionHandler().handleIncomingAccept(null, new GenericAcceptRv());
    assertEndWasStream(conn.waitForCompletion());
    assertHit(PassiveConnectionController.class);
    assertHit(RedirectToProxyController.class);
    assertSentRvs(2, 0, 0);
  }

  public void testBuddyRedirects() throws UnknownHostException {
    conn.addEventListener(new RvConnectionEventListener() {
      public void handleEventWithStateChange(RvConnection transfer,
          RvConnectionState state, RvConnectionEvent event) {
      }

      public void handleEvent(RvConnection transfer, RvConnectionEvent event) {
        if (event instanceof StartingControllerEvent) {
          StartingControllerEvent event1 = (StartingControllerEvent) event;
          StateController controller = event1.getController();
          if (controller instanceof PassiveConnectionController) {
            PassiveConnectionController passive
                = (PassiveConnectionController) controller;
            passive.setConnector(new HangConnector());

          } else if (controller instanceof OutgoingConnectionController) {
            OutgoingConnectionController out
                = (OutgoingConnectionController) controller;
            out.setConnector(new NopConnector());
          }
        }
      }
    });
    conn.sendRequest();
    assertEquals(RvConnectionState.WAITING, conn.getState());
    conn.getRvSessionHandler().handleIncomingAccept(null, new GenericAcceptRv());
    conn.getRvSessionHandler().handleIncomingRequest(null,
        new GenericRequest(2, new RvConnectionInfo(
            InetAddress.getByName("1.2.3.4"), InetAddress.getByName("2.3.4.5"),
            null, 5000, false, false)));
    StateInfo end = conn.waitForCompletion();
    assertEndWasStream(end);
    assertHit(PassiveConnectionController.class);
    assertHit(OutgoingConnectionController.class);
    assertSentRvs(1, 1, 0);
  }

  public void testBuddyRedirectsToProxy() throws UnknownHostException {
    conn.addEventListener(new RvConnectionEventListener() {
      public void handleEventWithStateChange(RvConnection transfer,
          RvConnectionState state, RvConnectionEvent event) {
      }

      public void handleEvent(RvConnection transfer, RvConnectionEvent event) {
        if (event instanceof StartingControllerEvent) {
          StartingControllerEvent event1 = (StartingControllerEvent) event;
          StateController controller = event1.getController();
          if (controller instanceof PassiveConnectionController) {
            PassiveConnectionController passive
                = (PassiveConnectionController) controller;
            passive.setConnector(new HangConnector());

          } else if (controller instanceof ConnectToProxyForOutgoingController) {
            ConnectToProxyForOutgoingController cont
                = (ConnectToProxyForOutgoingController) controller;
            cont.setConnector(getDirectedToProxyConnector());
          }
        }
      }
    });
    conn.sendRequest();
    assertEquals(RvConnectionState.WAITING, conn.getState());
    conn.getRvSessionHandler().handleIncomingAccept(null, new GenericAcceptRv());
    conn.getRvSessionHandler().handleIncomingRequest(null,
        new GenericRequest(2, new RvConnectionInfo(
            InetAddress.getByName("1.2.3.4"), InetAddress.getByName("2.3.4.5"),
            InetAddress.getByName("4.5.6.7"), 5000, true, false)));
    StateInfo end = conn.waitForCompletion();
    assertEndWasStream(end);
    assertHit(PassiveConnectionController.class);
    assertHit(ConnectToProxyForOutgoingController.class);
    assertSentRvs(1, 1, 0);
  }

  public void testPassiveFails() throws UnknownHostException {
    conn.addEventListener(new RvConnectionEventListener() {
      public void handleEventWithStateChange(RvConnection transfer,
          RvConnectionState state, RvConnectionEvent event) {
      }

      public void handleEvent(RvConnection transfer, RvConnectionEvent event) {
        if (event instanceof StartingControllerEvent) {
          StartingControllerEvent event1 = (StartingControllerEvent) event;
          StateController controller = event1.getController();
          if (controller instanceof PassiveConnectionController) {
            PassiveConnectionController passive
                = (PassiveConnectionController) controller;
            passive.setConnector(new FailConnector());

          } else if (controller instanceof RedirectToProxyController) {
            ((RedirectToProxyController) controller)
                .setConnector(getInitiateProxyConnector());
          }
        }
      }
    });
    conn.sendRequest();
    conn.getRvSessionHandler().handleIncomingAccept(null, new GenericAcceptRv());
    StateInfo end = conn.waitForCompletion();
    assertEndWasStream(end);
    assertHit(PassiveConnectionController.class);
    assertHit(RedirectToProxyController.class);
    assertSentRvs(2, 0, 0);
  }

  public void testOnlyUsingProxyRedirected() throws UnknownHostException {
    conn.getSettings().setOnlyUsingProxy(true);
    conn.addEventListener(createOnlyUsingProxyEventListener());
    conn.sendRequest();
    assertEquals(RvConnectionState.WAITING, conn.getState());
    conn.getRvSessionHandler().handleIncomingAccept(null, new GenericAcceptRv());
    conn.getRvSessionHandler().handleIncomingRequest(null,
        new GenericRequest(2, new RvConnectionInfo(
            InetAddress.getByName("1.2.3.4"), InetAddress.getByName("2.3.4.5"),
            null, 5000, false, false)));
    StateInfo end = conn.waitForCompletion();
    assertEndWasStream(end);
    assertHit(PassiveConnectionController.class);
    assertDidntHit(OutgoingConnectionController.class);
    assertHit(RedirectToProxyController.class);
    assertSentRvs(2, 0, 0);
  }

  public void testOnlyUsingProxyRedirectedToProxyTrusted() throws UnknownHostException {
    RvConnectionSettings settings = conn.getSettings();
    settings.setOnlyUsingProxy(true);
    settings.setProxyRequestTrusted(true);
    conn.addEventListener(createOnlyUsingProxyEventListener());
    conn.sendRequest();
    assertEquals(RvConnectionState.WAITING, conn.getState());
    conn.getRvSessionHandler().handleIncomingAccept(null, new GenericAcceptRv());
    conn.getRvSessionHandler().handleIncomingRequest(null,
        new GenericRequest(2,
            RvConnectionInfo.createForOutgoingProxiedRequest(
                InetAddress.getByName("4.5.6.7"), 5000)));
    StateInfo end = conn.waitForCompletion();
    assertEndWasStream(end);
    assertHit(PassiveConnectionController.class);
    assertHit(ConnectToProxyForOutgoingController.class);
    assertDidntHit(RedirectToProxyController.class);
    assertDidntHit(OutgoingConnectionController.class);
    assertSentRvs(1, 1, 0);
  }

  public void testOnlyUsingProxyRedirectedToProxyUntrusted() throws UnknownHostException {
    RvConnectionSettings settings = conn.getSettings();
    settings.setOnlyUsingProxy(true);
    settings.setProxyRequestTrusted(false);
    conn.addEventListener(createOnlyUsingProxyEventListener());
    conn.sendRequest();
    assertEquals(RvConnectionState.WAITING, conn.getState());
    conn.getRvSessionHandler().handleIncomingAccept(null, new GenericAcceptRv());
    conn.getRvSessionHandler().handleIncomingRequest(null,
        new GenericRequest(2, RvConnectionInfo.createForOutgoingProxiedRequest(
            InetAddress.getByName("4.5.6.7"), 5000)));
    StateInfo end = conn.waitForCompletion();
    assertEndWasStream(end);
    assertHit(PassiveConnectionController.class);
    assertHit(RedirectToProxyController.class);
    assertDidntHit(OutgoingConnectionController.class);
    assertSentRvs(2, 0, 0);
  }

  public void testRedirectAfterCompleted()
      throws UnknownHostException {
    conn.addEventListener(new DefaultRvConnectionEventListener() {
      public void handleEvent(RvConnection transfer, RvConnectionEvent event) {
        if (event instanceof StartingControllerEvent) {
          StateController controller = ((StartingControllerEvent) event).getController();
          if (controller instanceof PassiveConnectionController) {
            ((PassiveConnectionController) controller)
                .setConnector(new PassiveNopConnector());
          }
        }
      }
    });
    conn.sendRequest();
    conn.getRvSessionHandler().handleIncomingAccept(null, new GenericAcceptRv());
    assertEndWasStream(conn.waitForCompletion());
    assertHit(PassiveConnectionController.class);
    assertSentRvs(1, 0, 0);
    conn.getRvSessionHandler().handleIncomingRequest(null, new GenericRequest(2,
        new RvConnectionInfo(InetAddress.getByName("1.2.3.4"),
            InetAddress.getByName("2.3.4.5"), null, 500, false, true)));
    assertDidntHit(OutgoingConnectionController.class);
  }

  public void testRedirectDuringTransfer()
      throws UnknownHostException, ExecutionException, InterruptedException {
    MyFutureTask connectedWaiter = setConnectedWaiter();
    conn.addEventListener(new DefaultRvConnectionEventListener() {
      public void handleEvent(RvConnection transfer, RvConnectionEvent event) {
        if (event instanceof StartingControllerEvent) {
          StateController controller = ((StartingControllerEvent) event).getController();
          if (controller instanceof PassiveConnectionController) {
            ((PassiveConnectionController) controller)
                .setConnector(new PassiveNopConnector());
          }
        }
      }
    });
    conn.sendRequest();
    conn.getRvSessionHandler().handleIncomingAccept(null, new GenericAcceptRv());
    connectedWaiter.get();
    assertHit(PassiveConnectionController.class);
    assertSentRvs(1, 0, 0);
    List<StateController> hit = conn.getHitControllers();
    StateController last = hit.get(hit.size() - 1);
    conn.getRvSessionHandler().handleIncomingRequest(null, new GenericRequest(2,
        new RvConnectionInfo(InetAddress.getByName("1.2.3.4"),
            InetAddress.getByName("2.3.4.5"), null, 500, false, true)));
    assertSame("Redirect while connected should not cause controller change",
        last, hit.get(hit.size() - 1));
    assertSentRvs(1, 0, 0);
  }

  private RvConnectionEventListener createOnlyUsingProxyEventListener() {
    return new DefaultRvConnectionEventListener() {
      public void handleEvent(RvConnection transfer, RvConnectionEvent event) {
        if (event instanceof StartingControllerEvent) {
          StartingControllerEvent event1 = (StartingControllerEvent) event;
          StateController controller = event1.getController();
          if (controller instanceof PassiveConnectionController) {
            PassiveConnectionController passive
                = (PassiveConnectionController) controller;
            passive.setConnector(new HangConnector());

          } else if (controller instanceof AbstractProxyConnectionController) {
            ((AbstractProxyConnectionController) controller)
                .setConnector(getInitiateProxyConnector());
          }
        }
      }
    };
  }

  private static class GenericAcceptRv implements AcceptRvCmd {
    public boolean isEncrypted() {
      return false;
    }
  }

  private static class SingleRestartConsultant implements
      ControllerRestartConsultant {
    private boolean restarted = false;

    public void handleRestart() {
      restarted = true;
    }

    public boolean shouldRestart() {
      return !restarted;
    }

    public boolean restarted() {
      return restarted;
    }
  }
}
