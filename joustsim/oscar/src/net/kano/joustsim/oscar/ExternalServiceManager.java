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

package net.kano.joustsim.oscar;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.net.ClientConn;
import net.kano.joscar.snaccmd.conn.ConnCommand;
import net.kano.joscar.snaccmd.icon.IconCommand;
import net.kano.joscar.snaccmd.mailcheck.MailCheckCmd;
import net.kano.joscar.snaccmd.rooms.RoomCommand;
import net.kano.joustsim.JavaTools;
import net.kano.joustsim.oscar.oscar.BasicConnection;
import net.kano.joustsim.oscar.oscar.ExternalConnection;
import net.kano.joustsim.oscar.oscar.OscarConnListener;
import net.kano.joustsim.oscar.oscar.OscarConnStateEvent;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.DefaultServiceArbiterFactory;
import net.kano.joustsim.oscar.oscar.service.MutableService;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.ServiceArbiter;
import net.kano.joustsim.oscar.oscar.service.ServiceArbiterFactory;
import net.kano.joustsim.oscar.oscar.service.ServiceArbitrationManager;
import net.kano.joustsim.oscar.oscar.service.ServiceFactory;
import net.kano.joustsim.oscar.oscar.service.ServiceListener;
import net.kano.joustsim.oscar.oscar.service.bos.ExternalBosServiceImpl;
import net.kano.joustsim.oscar.oscar.service.bos.OpenedExternalServiceListener;
import net.kano.joustsim.oscar.oscar.service.bos.MainBosService;
import net.kano.joustsim.oscar.oscar.service.chatrooms.RoomFinderServiceArbiter;
import net.kano.joustsim.oscar.oscar.service.icon.IconServiceArbiter;
import net.kano.joustsim.oscar.oscar.service.mailcheck.MailCheckServiceArbiter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExternalServiceManager {
  private static final Logger LOGGER = Logger
      .getLogger(ExternalServiceManager.class.getName());
  private static final int DEFAULT_SERVICE_TIMEOUT = 10 * 1000;
  private Set<ServiceRequestInfo<? extends MutableService>> desiredServiceRequests
      = new HashSet<ServiceRequestInfo<? extends MutableService>>();
  private static final int SERVICE_REQUEST_INTERVAL = 15*1000;

  public static int fixPort(int port) {
    if (port > 0 && port <= 65535) return port;
    else return 5190;
  }

  private final AimConnection aimConnection;

  private final Object externalServicesLock = new Object();
  private final Map<Integer, ServiceArbiter<? extends MutableService>> externalServices
      = new HashMap<Integer, ServiceArbiter<? extends MutableService>>();
  private final Map<ServiceArbiter<? extends MutableService>, OscarConnection> externalConnections
      = new HashMap<ServiceArbiter<? extends MutableService>, OscarConnection>();
  private final Map<Integer, ServiceRequestInfo<? extends MutableService>> pendingServiceRequests
      = new HashMap<Integer, ServiceRequestInfo<? extends MutableService>>();
  private Map<Integer,Long> serviceRequestTimes = new HashMap<Integer, Long>();

  private final ServiceArbitrationManager arbitrationManager
      = new ServiceArbitrationManager() {
    public void openService(ServiceArbiter<? extends MutableService> arbiter) {
      int family = arbiter.getSnacFamily();
      if (getServiceArbiter(family) == arbiter) {
        requestService(family, arbiter);
      }
    }
  };
  private final Timer serviceTimer = createServiceTimer();

  private ServiceArbiterFactory arbiterFactory
      = new DefaultServiceArbiterFactory();
  @SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized"})
  private volatile int serviceConnectionTimeout = DEFAULT_SERVICE_TIMEOUT;

  public ExternalServiceManager(AimConnection aimConnection) {
    this.aimConnection = aimConnection;
    aimConnection.addStateListener(new StateListener() {
      public void handleStateChange(StateEvent event) {
        State newState = event.getNewState();
        if (newState.isFinished()) {
          serviceTimer.cancel();

          // we copy these
          List<OscarConnection> vals;
          synchronized (ExternalServiceManager.this) {
            vals = new ArrayList<OscarConnection>(externalConnections.values());
          }
          for (OscarConnection conn : vals) conn.disconnect();
        }
      }
    });
    aimConnection.addOpenedServiceListener(new OpenedServiceListener() {
      public void openedServices(AimConnection conn,
          Collection<? extends Service> services) {
        MainBosService bos = conn.getBosService();
        if (bos == null) return;

        // when the BOS service is ready, we dequeue pending service requests
        bos.addServiceListener(new ServiceListener() {
          public void handleServiceReady(Service service) {
            Collection<ServiceRequestInfo<? extends MutableService>> infos;
            synchronized (ExternalServiceManager.this) {
              infos = new ArrayList<ServiceRequestInfo<? extends MutableService>>(pendingServiceRequests.values());
            }
            for (ServiceRequestInfo<? extends MutableService> req : infos) {
              makeServiceRequest(req);
            }
          }

          public void handleServiceFinished(Service service) {
          }
        });
      }

      public void closedServices(AimConnection conn,
          Collection<? extends Service> services) {
      }
    });
    //TODO(klea): we need to come up with a better way to initialize arbiters
//    assert getMailCheckServiceArbiter() != null;
  }

  public int getServiceConnectionTimeout() {
    return serviceConnectionTimeout;
  }

  public void setServiceConnectionTimeout(int serviceConnectionTimeout) {
    this.serviceConnectionTimeout = serviceConnectionTimeout;
  }

  private Timer createServiceTimer() {
    Timer serviceTimer = new Timer(true);
    serviceTimer.scheduleAtFixedRate(new TimerTask() {
      public void run() {
        Set<Integer> retry = new HashSet<Integer>();
        Set<ServiceRequestInfo<? extends MutableService>> torequest
            = new HashSet<ServiceRequestInfo<? extends MutableService>>();
        synchronized (this) {
          long time = System.currentTimeMillis();
          Set<Map.Entry<Integer,ServiceRequestInfo<? extends MutableService>>> entries
              = pendingServiceRequests.entrySet();
          boolean changed = true;
          while (changed) {
            changed = false;
            for (Iterator<Map.Entry<Integer,ServiceRequestInfo<? extends MutableService>>> it = entries.iterator(); it.hasNext();) {
              Map.Entry<Integer,ServiceRequestInfo<? extends MutableService>> entry = it.next();

              ServiceRequestInfo<?> request = entry.getValue();
              long diff = time - request.startTime;
              if (diff > serviceConnectionTimeout) {
                LOGGER.info("External service for arbiter "
                    + request.arbiter + "(0x"
                    + Integer.toHexString(request.family)
                    + ") timed out after "
                    + (diff / 1000.0) + "s; retrying");
                retry.add(entry.getKey());
                request.cancel();
                it.remove();
                changed = true;
                break;
              }
            }
          }
          for (Iterator<ServiceRequestInfo<? extends MutableService>> it
              = desiredServiceRequests.iterator(); it.hasNext();) {
            ServiceRequestInfo<? extends MutableService> s = it.next();
            if (System.currentTimeMillis() - s.startTime
                > SERVICE_REQUEST_INTERVAL) {
              it.remove();
              torequest.add(s);
            }
          }
        }

        for (ServiceRequestInfo<? extends MutableService> s : torequest) {
          requestService(s.family, s.arbiter, true);
        }
        for (int service : retry) {
          requestService(service, getMutableServiceArbiter(service));
        }
      }
    }, 5000, 5000);
    return serviceTimer;
  }

  public @Nullable ServiceArbiter<? extends Service> getServiceArbiter(
      int service) {
    return getMutableServiceArbiter(service);
  }

  private ServiceArbiter<? extends MutableService> getMutableServiceArbiter(
      int service) {
    ServiceArbiter<? extends MutableService> arbiter;
    synchronized (externalServicesLock) {
      arbiter = externalServices.get(service);
      if (arbiter != null) return arbiter;

      // NOTE: this calls an external method from within a lock!
      LOGGER.finer("Creating arbiter for service " + service);
      arbiter = arbiterFactory.getInstance(arbitrationManager, service);
      LOGGER.fine("Created arbiter for service " + service + ": " + arbiter);
      if (arbiter == null) return null;
      externalServices.put(service, arbiter);
    }
    requestService(service, arbiter);
    return arbiter;
  }

  public IconServiceArbiter getIconServiceArbiter() {
    return getArbiter(IconCommand.FAMILY_ICON, IconServiceArbiter.class);
  }

  public RoomFinderServiceArbiter getChatRoomFinderServiceArbiter() {
    return getArbiter(RoomCommand.FAMILY_ROOM, RoomFinderServiceArbiter.class);
  }

  public MailCheckServiceArbiter getMailCheckServiceArbiter() {
    return getArbiter(MailCheckCmd.FAMILY_MAILCHECK,
        MailCheckServiceArbiter.class);
  }

  private <A> A getArbiter(int family, Class<A> arbiterClass) {
    ServiceArbiter<?> arbiter = getServiceArbiter(family);
    if (arbiterClass.isInstance(arbiter)) {
      return JavaTools.cast(arbiterClass, arbiter);
    } else {
      return null;
    }
  }

  private void refreshServiceIfNecessary(int family) {
    // queue it up and let the timer deal with it (wriet that code)
    ServiceArbiter<? extends MutableService> arbiter;
    synchronized (externalServicesLock) {
      arbiter = externalServices.get(family);
    }
    if (arbiter == null) {
      LOGGER.warning("Someone requested refresh of 0x"
          + Integer.toHexString(family) + " but there's no arbiter");
      return;
    }

    if (!arbiter.shouldKeepAlive()) {
      LOGGER.log(Level.INFO, "Someone requested a refresh of 0x"
          + Integer.toHexString(family) + " but the arbiter "
          + arbiter + " keepalive = false");
      return;
    }

    requestService(family, arbiter);
  }

  private synchronized <S extends MutableService> void queueServiceRequest(
      int family, ServiceArbiter<S> arbiter) {
    desiredServiceRequests.add(new ServiceRequestInfo<S>(family, arbiter));
  }

  private synchronized boolean requestedRecently(int family) {
    Long num = serviceRequestTimes.get(family);
    return num == null
        || System.currentTimeMillis() - num < SERVICE_REQUEST_INTERVAL;
  }

  private <S extends MutableService> void requestService(int family,
      ServiceArbiter<S> arbiter) {
    requestService(family, arbiter, false);
  }

  /**
   * If {@code definitely} is {@code false}, the request may not be made
   * immediately, but instead it will be queued, for rate limiting purposes.
   */
  private <S extends MutableService> void requestService(int family,
      ServiceArbiter<S> arbiter, boolean definitely) {
    ServiceRequestInfo<S> request;
    synchronized (this) {
      if (pendingServiceRequests.containsKey(family)) return;
      if (externalConnections.containsKey(arbiter)) {
        LOGGER.finer("Someone requested 0x" + Integer.toHexString(family)
            + " but there's already an external connection: "
            + externalConnections.get(arbiter));
        return;
      }

      if (!definitely && requestedRecently(family)) {
        queueServiceRequest(family, arbiter);
        return;
      }
      request = new ServiceRequestInfo<S>(family, arbiter);
      pendingServiceRequests.put(family, request);

      serviceRequestTimes.put(family, System.currentTimeMillis());
    }
    makeServiceRequest(request);
  }

  private <S extends MutableService> void makeServiceRequest(
      ServiceRequestInfo<S> request) {
    int family = request.family;
    LOGGER.fine("Requesting external service " + family + " for "
        + request.arbiter);
    MainBosService bosService = aimConnection.getBosService();
    if (bosService == null) return;
    bosService.requestService(family,
        new ArbitratedExternalServiceListener<S>(request));
  }

  private synchronized <S extends MutableService> boolean clearRequest(
      ServiceRequestInfo<S> request) {
    boolean removed = pendingServiceRequests.values().remove(request);
    if (removed) {
      LOGGER.fine("External connection request " + request + " cleared");
    } else {
      LOGGER.fine("External connection request " + request + " was not cleared "
          + "because it is obsolete");
    }
    return removed;
  }

  private synchronized void clearExternalConnection(OscarConnection conn,
      ServiceArbiter<? extends MutableService> arbiter) {
    if (getExternalConnection(arbiter) == conn) {
      externalConnections.remove(arbiter);
    }
  }

  private synchronized OscarConnection getExternalConnection(
      ServiceArbiter<? extends MutableService> arbiter) {
    return externalConnections.get(arbiter);
  }

  private synchronized boolean storeExternalConnection(BasicConnection conn,
      ServiceRequestInfo<? extends MutableService> request) {
    // we don't clear the request anymore until it actually connects
//    if (!clearRequest(request)) return false;

    externalConnections.put(request.arbiter, conn);
    return true;
  }

  private class ExternalServiceFactory<S extends MutableService>
      implements ServiceFactory {
    private final int serviceFamily;
    private final ServiceArbiter<S> arbiter;

    public ExternalServiceFactory(int serviceFamily,
        ServiceArbiter<S> arbiter) {
      this.serviceFamily = serviceFamily;
      this.arbiter = arbiter;
    }

    public MutableService getService(OscarConnection conn, int family) {
      if (family == ConnCommand.FAMILY_CONN) {
        return new ExternalBosServiceImpl(aimConnection, conn);

      } else if (family == serviceFamily) {
        return arbiter.createService(aimConnection, conn);

      } else {
        LOGGER.warning("External service " + serviceFamily
            + " wants to open service " + family);
        return null;
      }
    }
  }

  private class ExternalServiceConnListener<S extends MutableService>
      implements OscarConnListener {
    private final int serviceFamily;
    private final ServiceRequestInfo<S> request;

    public ExternalServiceConnListener(int serviceFamily,
        ServiceRequestInfo<S> arbiter) {
      this.request = arbiter;
      this.serviceFamily = serviceFamily;
    }

    public void registeredSnacFamilies(OscarConnection conn) {
    }

    public void connStateChanged(OscarConnection conn,
        OscarConnStateEvent event) {
      ClientConn.State state = event.getClientConnEvent().getNewState();
      if (state == ClientConn.STATE_FAILED
          || state == ClientConn.STATE_NOT_CONNECTED) {
        LOGGER.info("External service connection died for service "
            + serviceFamily + " ( " + request + ")");
        conn.removeOscarListener(this);
        clearExternalConnection(conn, request.arbiter);
        refreshServiceIfNecessary(serviceFamily);
      }
    }

    public void allFamiliesReady(OscarConnection conn) {
      LOGGER.fine("External service connection for " + request.arbiter
          + " is connected and ready");
      clearRequest(request);
    }
  }

  private class ArbitratedExternalServiceListener<S extends MutableService>
      implements OpenedExternalServiceListener {
    private final ServiceRequestInfo<S> request;

    public ArbitratedExternalServiceListener(ServiceRequestInfo<S> request) {
      this.request = request;
    }

    public void handleServiceRedirect(MainBosService service,
        int serviceFamily, String host, int port,
        ByteBlock flapCookie) {
      LOGGER.fine("Connecting to " + host + ":" + port + " for external "
          + "service " + serviceFamily);
      BasicConnection conn = new ExternalConnection(host, fixPort(port),
          serviceFamily);
      conn.getClientFlapConn().setSocketFactory(
          aimConnection.getProxy().getSocketFactory());
      conn.setCookie(flapCookie);
      conn.setServiceFactory(new ExternalServiceFactory<S>(
          serviceFamily, request.arbiter));
      conn.addOscarListener(new ExternalServiceConnListener<S>(
          serviceFamily, request));
      boolean isnew = storeExternalConnection(conn, request);
      if (isnew) conn.connect();
    }
  }
}
