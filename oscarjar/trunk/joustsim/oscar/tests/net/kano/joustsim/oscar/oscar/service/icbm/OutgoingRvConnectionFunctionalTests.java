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
import net.kano.joscar.rvproto.rvproxy.RvProxyCmd;
import net.kano.joustsim.TestTools;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.ControllerRestartConsultant;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.DefaultRvConnectionEventListener;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionEventListener;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionSettings;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionState;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.TimeoutHandler;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.ConnectionType;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionImpl;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.NextStateControllerInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.AbstractConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.AbstractProxyConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.AbstractStateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectToProxyForIncomingController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectToProxyForOutgoingController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectedController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.OutgoingConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.PassiveConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ProxyConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.RedirectConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.RedirectToProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendOverProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendPassivelyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.TimeoutableController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.StartedControllerEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.StartingControllerEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.AbstractStreamInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailedStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.SuccessfulStateInfo;
import net.kano.joustsim.oscar.proxy.AimProxyInfo;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    sendAcceptWait();
    assertHitOnce(PassiveConnectionController.class);
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
    assertEquals(2, TestTools.findInstances(getConnection().getHitControllers(),
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

          } else if (!(controller instanceof InstantlyConnectedController)) {
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
    sendAcceptWait();
    assertHitOnce(PassiveConnectionController.class);
    assertHitOnce(RedirectToProxyController.class);
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
            ip("1.2.3.4"), ip("2.3.4.5"),
            null, 5000, false, false)));
    StateInfo end = conn.waitForCompletion();
    assertEndWasStream(end);
    assertHitOnce(PassiveConnectionController.class);
    assertHitOnce(OutgoingConnectionController.class);
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
            ip("1.2.3.4"), ip("2.3.4.5"),
            ip("4.5.6.7"), 5000, true, false)));
    StateInfo end = conn.waitForCompletion();
    assertEndWasStream(end);
    assertHitOnce(PassiveConnectionController.class);
    assertHitOnce(ConnectToProxyForOutgoingController.class);
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
    assertHitOnce(PassiveConnectionController.class);
    assertHitOnce(RedirectToProxyController.class);
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
            ip("1.2.3.4"), ip("2.3.4.5"),
            null, 5000, false, false)));
    StateInfo end = conn.waitForCompletion();
    assertEndWasStream(end);
    assertHitOnce(PassiveConnectionController.class);
    assertDidntHit(OutgoingConnectionController.class);
    assertHitOnce(RedirectToProxyController.class);
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
                ip("4.5.6.7"), 5000)));
    StateInfo end = conn.waitForCompletion();
    assertEndWasStream(end);
    assertHitOnce(PassiveConnectionController.class);
    assertHitOnce(ConnectToProxyForOutgoingController.class);
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
            ip("4.5.6.7"), 5000)));
    StateInfo end = conn.waitForCompletion();
    assertEndWasStream(end);
    assertHitOnce(PassiveConnectionController.class);
    assertHitOnce(RedirectToProxyController.class);
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
    sendAcceptWait();
    assertHitOnce(PassiveConnectionController.class);
    assertDidntHit(OutgoingConnectionController.class);
    assertSentRvs(1, 0, 0);
    conn.getRvSessionHandler().handleIncomingRequest(null, new GenericRequest(2,
        new RvConnectionInfo(ip("1.2.3.4"),
            ip("2.3.4.5"), null, 500, false, true)));
    assertDidntHit(OutgoingConnectionController.class);
  }

  private void sendAcceptWait() {
    conn.sendRequest();
    conn.getRvSessionHandler().handleIncomingAccept(null, new GenericAcceptRv());
    assertEndWasStream(conn.waitForCompletion());
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
    assertHitOnce(PassiveConnectionController.class);
    assertSentRvs(1, 0, 0);
    List<StateController> hit = conn.getHitControllers();
    StateController last = hit.get(hit.size() - 1);
    conn.getRvSessionHandler().handleIncomingRequest(null, new GenericRequest(2,
        new RvConnectionInfo(ip("1.2.3.4"),
            ip("2.3.4.5"), null, 500, false, true)));
    assertSame("Redirect while connected should not cause controller change",
        last, hit.get(hit.size() - 1));
    assertSentRvs(1, 0, 0);
  }

  /**
   * This test succeeds when the next connection controller is tried if the
   * connected controller failed. This situation happens in practice when
   * we connect to someone's LAN IP on the specified port, but we're on a
   * different LAN than the buddy, BUT that port is open on that IP on our LAN,
   * so we actually connected to some other service on some other computer. This
   * will trigger the connected controller (for example SendFileController),
   * which will either time out, or fail when it sees an incorrect ICBM ID.
   */
  public void testConnectionControllerRetry() {
    conn.addEventListener(new DefaultRvConnectionEventListener() {
      public void handleEvent(RvConnection transfer, RvConnectionEvent event) {
        if (event instanceof StartingControllerEvent) {
          StateController controller
              = ((StartingControllerEvent) event).getController();
          if (controller instanceof SendPassivelyController) {
            ((SendPassivelyController) controller)
                .setConnector(new PassiveNopConnector());

          } else if (controller instanceof RedirectToProxyController) {
            RedirectToProxyController redirectToProxyController
                = (RedirectToProxyController) controller;
            redirectToProxyController.setConnector(getInitiateProxyConnector());
          }
        }
      }
    });
    conn.setConnectedController(new FailThenSucceedController(conn, false));
    conn.sendRequest();
    assertEndWasStream(conn.waitForCompletion());
    assertHitMultiple(FailThenSucceedController.class, 2);
    assertHitOnce(SendPassivelyController.class);
    assertHitOnce(RedirectToProxyController.class);
  }

  public void testConnectionControllerRetryLast() {
    conn.addEventListener(new DefaultRvConnectionEventListener() {
      private boolean firstRedirect = true;

      public void handleEvent(RvConnection transfer, RvConnectionEvent event) {
        if (event instanceof StartingControllerEvent) {
          StateController controller
              = ((StartingControllerEvent) event).getController();
          if (controller instanceof SendPassivelyController) {
            ((SendPassivelyController) controller)
                .setConnector(new FailConnector());

          } else if (controller instanceof RedirectToProxyController) {
            RedirectToProxyController redirectToProxyController
                = (RedirectToProxyController) controller;
            if (firstRedirect) {
              firstRedirect = false;
              redirectToProxyController.setConnector(new MockProxyConnector(
                  new FailProxyConnection()));

            } else {
              redirectToProxyController.setConnector(
                  getInitiateProxyConnector());
            }
          }
        }
      }
    });
    conn.sendRequest();
    assertEndWasStream(conn.waitForCompletion());
    assertHitOnce(SendPassivelyController.class);
    assertHitMultiple(RedirectToProxyController.class, 2);
  }

  public void testControllersTimeout() throws IllegalAccessException,
      InstantiationException, UnknownHostException {
    conn.getRvSessionInfo().setConnectionInfo(new RvConnectionInfo(null, null,
        ip("1.2.3.4"), 102, true, false));
    RecordingTimeoutHandler timeouter = new RecordingTimeoutHandler();
    conn.setTimeoutHandler(timeouter);
    for (Class<? extends AbstractProxyConnectionController> proxy
        : Arrays.asList(ConnectToProxyForIncomingController.class,
        ConnectToProxyForOutgoingController.class,
        RedirectToProxyController.class, SendOverProxyController.class)) {
      checkForTimeout(proxy.newInstance(), new MockProxyConnector(
          new HangProxyConnection()), timeouter);
    }
    conn.getRvSessionInfo().setConnectionInfo(new RvConnectionInfo(
        ip("1.2.3.4"), ip("5.6.7.8"), null, 500, false, false));
    for (Class<? extends AbstractConnectionController> normal
        : Arrays.asList(RedirectConnectionController.class,
        SendPassivelyController.class)) {
      checkForTimeout(normal.newInstance(), new HangConnector(), timeouter);
    }
    checkForTimeout(new OutgoingConnectionController(ConnectionType.LAN),
        new HangConnector(), timeouter);
    checkForTimeout(new OutgoingConnectionController(ConnectionType.INTERNET),
        new HangConnector(), timeouter);
  }

  /**
   * Ensures that the "old controller" passed to a retried last controller in
   * start() is the previous controller, not the previous instance of the
   * retried controller.
   */
  public void testLastControllerOnRetry() throws InterruptedException {
    RetryLastTestConnection connection = new RetryLastTestConnection();
    final Object lock = new Object();
    class MyDefaultRvConnectionEventListener
        extends DefaultRvConnectionEventListener {
      private volatile boolean done = false;

      public void handleEventWithStateChange(RvConnection transfer,
          RvConnectionState state, RvConnectionEvent event) {
        if (state.isClosed()) {
          synchronized (lock) {
            done = true;
            lock.notifyAll();
          }
        }
      }
    }
    MyDefaultRvConnectionEventListener listener
        = new MyDefaultRvConnectionEventListener();
    connection.addEventListener(listener);
    connection.start();
    synchronized(lock) {
      while (!listener.done) lock.wait();
    }
    StateController last = connection.getSecondLastController();
    assertNotNull(last);
    assertSame(connection.getFirstController(), last);
  }

  private void checkForTimeout(AbstractConnectionController controller,
      MockConnector connector, RecordingTimeoutHandler timeouter)
      throws InstantiationException, IllegalAccessException {
    controller.setConnector(connector);
    controller.start(conn, null);
    connector.waitForConnectionAttempt();
    assertTrue(controller + " never called startTimeout",
        timeouter.started(controller));
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

  private static class FailThenSucceedController
      extends AbstractStateController implements ConnectedController {
    private final MockRvConnection connection;
    private final boolean connected;

    public FailThenSucceedController(MockRvConnection connection,
        boolean connected) {
      this.connection = connection;
      this.connected = connected;
    }

    public void start(RvConnection transfer, StateController last) {
      if (connected) {
        fireSucceeded(new MyStreamInfo());

      } else {
        connection.setConnectedController(new FailThenSucceedController(
            connection, true));
        fireFailed(new FailedStateInfo() {
        });
      }
    }

    public void stop() {
    }

    public boolean isConnected() {
      return connected;
    }

    public boolean didConnect() {
      return connected;
    }

    public boolean equals(Object obj) {
      return obj.getClass().equals(getClass());
    }

    private static class MyStreamInfo
        extends AbstractStreamInfo implements StreamInfo {
      public SelectableChannel getSelectableChannel() {
        return null;
      }

      public WritableByteChannel getWritableChannel() {
        return null;
      }

      public ReadableByteChannel getReadableChannel() {
        return null;
      }
    }
  }

  private static class HangProxyConnection implements ProxyConnection {
    public RvProxyCmd readPacket() throws IOException {
      hang();
      throw new IOException("Hang interrupted");
    }

    private void hang() {
      Object o = new Object();
      synchronized(o) {
        try {
          o.wait();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }

    public void sendProxyPacket(RvProxyCmd initCmd) throws IOException {
      hang();
      throw new IOException("Hang interrupted");
    }
  }

  private static class RecordingTimeoutHandler implements TimeoutHandler {
    private Set<TimeoutableController> started
        = new HashSet<TimeoutableController>();

    public void startTimeout(TimeoutableController controller) {
      started.add(controller);
    }

    public void pauseTimeout(TimeoutableController controller) {
    }

    public void unpauseTimeout(TimeoutableController controller) {
    }

    public boolean started(TimeoutableController controller) {
      return started.contains(controller);
    }
  }

  private static class RetryLastTestConnection extends RvConnectionImpl {
    private StateController first = new AbstractStateController() {
      public void start(RvConnection transfer, StateController last) {
        System.out.println("Doing first");
        fireFailed(new FailedStateInfo() {
        });
      }

      public void stop() {
      }
    };
    private boolean didSecond = false;
    private StateController secondLast = null;

    public RetryLastTestConnection() {
      super(AimProxyInfo.forNoProxy(), new Screenname("me"),
          new MockRvSessionConnectionInfo());
    }

    protected RendezvousSessionHandler createSessionHandler() {
      return null;
    }

    protected NextStateControllerInfo getNextControllerFromError(
        StateController oldController, StateInfo endState) {
      if (oldController == first) {
        return new NextStateControllerInfo(new SecondController());
      }
      return new NextStateControllerInfo(RvConnectionState.FAILED, new RvConnectionEvent() {
      });
    }

    protected NextStateControllerInfo getNextControllerFromSuccess(
        StateController oldController, StateInfo endState) {
      return new NextStateControllerInfo(RvConnectionState.FINISHED, new RvConnectionEvent() {
      });
    }

    protected ConnectedController createConnectedController(StateInfo endState) {
      return new InstantlyConnectedController();
    }

    protected boolean isSomeConnectionController(StateController controller) {
      return controller == first || controller instanceof SecondController;
    }

    protected boolean isConnectedController(StateController controller) {
      return controller instanceof InstantlyConnectedController;
    }

    public void start() {
      startStateController(first);
    }

    public StateController getSecondLastController() {
      return secondLast;
    }

    public StateController getFirstController() {
      return first;
    }

    private class SecondController extends AbstractStateController {
      private boolean started = false;

      public void start(RvConnection transfer, StateController last) {
        if (started) {
          throw new IllegalStateException("started already");
        }
        started = true;
        if (didSecond) {
          if (secondLast != null) {
            System.out.println("Did second 3 times?? secondLast is "
                + secondLast);
            fireFailed(new FailedStateInfo() { });
            return;
          }
          System.out.println("Setting secondLast to " + last);
          secondLast = last;
          fireSucceeded(new SuccessfulStateInfo() {
          });
        } else {
          System.out.println("Doing second for the first time");
          didSecond = true;
          fireFailed(new FailedStateInfo() { });
        }
      }

      public void stop() {
      }
    }
  }


}
