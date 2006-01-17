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
import net.kano.joscar.snaccmd.rooms.RoomCommand;
import net.kano.joustsim.oscar.oscar.BasicConnection;
import net.kano.joustsim.oscar.oscar.OscarConnListener;
import net.kano.joustsim.oscar.oscar.OscarConnStateEvent;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.ExternalConnection;
import net.kano.joustsim.oscar.oscar.service.DefaultServiceArbiterFactory;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.ServiceArbiter;
import net.kano.joustsim.oscar.oscar.service.ServiceArbiterFactory;
import net.kano.joustsim.oscar.oscar.service.ServiceArbitrationManager;
import net.kano.joustsim.oscar.oscar.service.ServiceFactory;
import net.kano.joustsim.oscar.oscar.service.ServiceListener;
import net.kano.joustsim.oscar.oscar.service.bos.ExternalBosService;
import net.kano.joustsim.oscar.oscar.service.bos.MainBosService;
import net.kano.joustsim.oscar.oscar.service.bos.OpenedExternalServiceListener;
import net.kano.joustsim.oscar.oscar.service.icon.IconServiceArbiter;
import net.kano.joustsim.oscar.oscar.service.chatrooms.RoomFinderServiceArbiter;
import net.kano.joustsim.JavaTools;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ExternalServiceManager {
  //TODO: make opened services fire the AimConnection's OpenedServiiceListnener

  private static final Logger LOGGER = Logger
      .getLogger(ExternalServiceManager.class.getName());
  private static final int DEFAULT_SERVICE_TIMEOUT = 10000;

  public static int fixPort(int port) {
    int usePort;
    if (port <= 0) {
      usePort = 5190;
    } else {
      usePort = port;
    }
    return usePort;
  }

  private AimConnection aimConnection;

  private ServiceArbiterFactory arbiterFactory
      = new DefaultServiceArbiterFactory();

  private final Object externalServicesLock = new Object();
  private final Map<Integer, ServiceArbiter<? extends Service>> externalServices
      = new HashMap<Integer, ServiceArbiter<? extends Service>>();
  private Map<ServiceArbiter<? extends Service>, OscarConnection> externalConnections
      = new HashMap<ServiceArbiter<? extends Service>, OscarConnection>();
  private ServiceArbitrationManager arbitrationManager
      = new ServiceArbitrationManager() {
    public void openService(ServiceArbiter<? extends Service> arbiter) {
      int family = arbiter.getSnacFamily();
      if (getServiceArbiter(family) == arbiter) {
        requestService(family, arbiter);
      }
    }
  };
  private final Timer serviceTimer = initializeServiceTimer();
  private int serviceConnectionTimeout = DEFAULT_SERVICE_TIMEOUT;

  public ExternalServiceManager(AimConnection aimConnection) {
    this.aimConnection = aimConnection;
    aimConnection.addStateListener(new StateListener() {
      public void handleStateChange(StateEvent event) {
        State newState = event.getNewState();
        if (newState.isFinished()) {
          serviceTimer.cancel();
          synchronized (ExternalServiceManager.this) {
            //TODO: this can cause ConcurrentModificationException
            for (OscarConnection conn : externalConnections.values()) {
              conn.disconnect();
            }
          }
        }
      }
    });
    aimConnection.addOpenedServiceListener(new OpenedServiceListener() {
      public void openedServices(AimConnection conn,
          Collection<? extends Service> services) {
        MainBosService bos = conn.getBosService();
        if (bos == null) return;

        bos.addServiceListener(new ServiceListener() {
          public void handleServiceReady(Service service) {
            for (ServiceRequestInfo<? extends Service> req : pendingServiceRequests.values()) {
              updateServiceRequest(req);
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
  }

  private Timer initializeServiceTimer() {
    Timer serviceTimer = new Timer("Service connection timeout watcher", true);
    serviceTimer.scheduleAtFixedRate(new TimerTask() {
      public void run() {
        //TODO: use configurable connection timeout
        Set<Integer> retry = new HashSet<Integer>();
        synchronized (this) {
          long time = System.currentTimeMillis();
          Set<Map.Entry<Integer, ServiceRequestInfo<? extends Service>>> entries
              = pendingServiceRequests.entrySet();
          if (entries.isEmpty()) {
            LOGGER.finer("No pending service requests!");
          }
          for (Iterator<Map.Entry<Integer, ServiceRequestInfo<? extends Service>>> it
              = entries.iterator();
              it.hasNext();) {
            Map.Entry<Integer, ServiceRequestInfo<? extends Service>> entry = it.next();

            ServiceRequestInfo request = entry.getValue();
            long diff = time - request.getStartTime();
            if (diff > serviceConnectionTimeout) {
              LOGGER.info("External service for arbiter "
                  + request.getArbiter() + "(0x"
                  + Integer.toHexString(request.getFamily())
                  + ") timed out after "
                  + ((int) diff / 1000.0) + "s; retrying");
              retry.add(entry.getKey());
              request.cancel();
              it.remove();
            } else {
              LOGGER.fine("Service request for "
                  + request.getArbiter() + " still has "
                  + (serviceConnectionTimeout - diff) + "ms before timing out");
            }
          }
        }
        for (int service : retry) {
          requestService(service, getServiceArbiter(service));
        }
      }
    }, 5000, 5000);
    return serviceTimer;
  }

  public @Nullable ServiceArbiter<? extends Service> getServiceArbiter(
      int service) {
    ServiceArbiter<? extends Service> arbiter;
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

  private <A> A getArbiter(int family, Class<A> arbiterClass) {
    ServiceArbiter<?> arbiter = getServiceArbiter(family);
    if (arbiterClass.isInstance(arbiter)) {
      //noinspection unchecked
      return JavaTools.cast(arbiterClass, arbiter);
    } else {
      return null;
    }
  }

  private void refreshServiceIfNecessary(int family) {
    ServiceArbiter<? extends Service> arbiter;
    synchronized (externalServicesLock) {
      arbiter = externalServices.get(family);
    }
    if (arbiter == null) {
      LOGGER.warning("Someone requested refresh of 0x"
          + Integer.toHexString(family) + " but there's no arbiter");
      return;
    } else if (!arbiter.shouldKeepAlive()) {
      LOGGER.log(Level.INFO, "Someone requested a refresh of 0x"
          + Integer.toHexString(family) + " but the arbiter "
          + arbiter + " says it doesn't want to live", new Throwable());
      return;
    }
    requestService(family, arbiter);
  }

  private Map<Integer, ServiceRequestInfo<? extends Service>> pendingServiceRequests
      = new HashMap<Integer, ServiceRequestInfo<? extends Service>>();

  private static class ServiceRequestInfo<S extends Service> {
    private long startTime = System.currentTimeMillis();
    private int family;
    private ServiceArbiter<S> arbiter;
    private BasicConnection connection = null;
    private boolean canceled = false;

    public ServiceRequestInfo(int family, ServiceArbiter<S> arbiter) {
      this.family = family;
      this.arbiter = arbiter;
    }

    public long getStartTime() {
      return startTime;
    }

    public ServiceArbiter<S> getArbiter() {
      return arbiter;
    }

    public synchronized BasicConnection getConnection() {
      return connection;
    }

    public synchronized void setConnection(BasicConnection connection) {
      this.connection = connection;
    }

    public void cancel() {
      canceled = true;
      BasicConnection connection = getConnection();
      if (connection != null) connection.disconnect();
    }

    public int getFamily() {
      return family;
    }

    public boolean isCanceled() {
      return canceled;
    }

    public String toString() {
      return "<" + family + "> " + arbiter;
    }
//
//        public boolean isChatRequest() {
//            return chatRequest;
//        }
//
//        public @Nullable ChatInfo getChatInfo() {
//            return chatInfo;
//        }
  }

  private <S extends Service> void requestService(int service,
      ServiceArbiter<S> arbiter) {
    ServiceRequestInfo<S> request;
    synchronized (this) {
      if (pendingServiceRequests.containsKey(service)) return;
      if (externalConnections.containsKey(arbiter)) {
        LOGGER.finer("Someone requested 0x"
            + Integer.toHexString(service) + " but there's "
            + "already an external connection: "
            + externalConnections.get(arbiter));
        return;
      }
      request = new ServiceRequestInfo<S>(service, arbiter);
      pendingServiceRequests.put(service, request);
    }
    updateServiceRequest(request);
  }

  private <S extends Service>void updateServiceRequest(
      ServiceRequestInfo<S> request) {
    int family = request.getFamily();
    LOGGER.info("Requesting external service " + family + " for " + request.getArbiter());
    MainBosService bosService = aimConnection.getBosService();
    if (bosService == null) return;
    bosService.requestService(family,
        new ArbitratedExternalServiceListener<S>(request));
  }

  private synchronized <S extends Service> boolean clearRequest(
      ServiceRequestInfo<S> request) {
    boolean removed = pendingServiceRequests.values().remove(request);
    if (removed) {
      LOGGER.info("External connection request " + request + " cleared");
    } else {
      LOGGER.info("External connection request " + request + " was not cleared because it is "
          + "obsolete");
    }
    return removed;
  }

  private synchronized void clearExternalConnection(OscarConnection conn,
      ServiceArbiter<? extends Service> arbiter) {
    if (getExternalConnection(arbiter) == conn) {
      externalConnections.remove(arbiter);
    }
  }

  private synchronized OscarConnection getExternalConnection(
      ServiceArbiter<? extends Service> arbiter) {
    return externalConnections.get(arbiter);
  }

//    private synchronized OscarConnection getExternalConnection(
//            int family) {
//        ServiceArbiter<? extends Service> arbiter = getServiceArbiter(family);
//        if (arbiter == null) return null;
//        return externalConnections.get(arbiter);
//    }

  private synchronized boolean storeExternalConnection(BasicConnection conn,
      ServiceRequestInfo<? extends Service> request) {
    if (!clearRequest(request)) return false;
//        if (request.isChatRequest()) {
//            chatConnections.put(request.getChatInfo())
//        }
    externalConnections.put(request.getArbiter(), conn);
    return true;
  }

  private class ExternalServiceFactory<S extends Service>
      implements ServiceFactory {
    private final int serviceFamily;
    private final ServiceArbiter<S> arbiter;

    public ExternalServiceFactory(int serviceFamily,
        ServiceArbiter<S> arbiter) {
      this.serviceFamily = serviceFamily;
      this.arbiter = arbiter;
    }

    public Service getService(OscarConnection conn, int family) {
      if (family == ConnCommand.FAMILY_CONN) {
        return new ExternalBosService(aimConnection, conn);

      } else if (family == serviceFamily) {
        return arbiter.createService(aimConnection, conn);

      } else {
        LOGGER.warning("External service " + serviceFamily
            + " wants to open service " + family);
        return null;
      }
    }
  }

  private class ExternalServiceConnListener<S extends Service>
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
        clearExternalConnection(conn, request.getArbiter());
        refreshServiceIfNecessary(serviceFamily);
      }
    }

    public void allFamiliesReady(OscarConnection conn) {
    }
  }

  private class ArbitratedExternalServiceListener<S extends Service> implements
      OpenedExternalServiceListener {
    private final ServiceRequestInfo<S> request;

    public ArbitratedExternalServiceListener(ServiceRequestInfo<S> request) {
      this.request = request;
    }

    public void handleServiceRedirect(MainBosService service,
        int serviceFamily, String host, int port,
        ByteBlock flapCookie) {
      LOGGER.fine("Connecting to " + host + ":" + port + " for external "
          + "service " + serviceFamily);
      BasicConnection conn = new ExternalConnection(host, fixPort(port), serviceFamily);
      conn.getClientFlapConn().setSocketFactory(aimConnection.getProxy().getSocketFactory());
      conn.setCookie(flapCookie);
      conn.setServiceFactory(new ExternalServiceFactory<S>(
          serviceFamily, request.getArbiter()));
      conn.addOscarListener(new ExternalServiceConnListener<S>(
          serviceFamily, request));
      boolean isnew = storeExternalConnection(conn, request);
      if (isnew) conn.connect();
    }
  }
}
