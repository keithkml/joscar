/*
 *  Copyright (c) 2004, The Joust Project
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
 *  File created by keith @ Jan 14, 2004
 *
 */

package net.kano.joustsim.oscar.oscar;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.MiscTools;
import net.kano.joscar.flap.ClientFlapConn;
import net.kano.joscar.flap.FlapCommand;
import net.kano.joscar.flap.FlapPacketEvent;
import net.kano.joscar.flap.FlapPacketListener;
import net.kano.joscar.flap.FlapProcessor;
import net.kano.joscar.flap.VetoableFlapPacketListener;
import net.kano.joscar.flapcmd.CloseFlapCmd;
import net.kano.joscar.flapcmd.DefaultFlapCmdFactory;
import net.kano.joscar.flapcmd.FlapErrorCmd;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.net.ClientConn;
import net.kano.joscar.net.ClientConnEvent;
import net.kano.joscar.net.ClientConnListener;
import net.kano.joscar.net.ConnDescriptor;
import net.kano.joscar.snac.ClientSnacProcessor;
import net.kano.joscar.snac.FamilyVersionPreprocessor;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snac.SnacPacketListener;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snac.SnacRequestListener;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snac.SnacResponseListener;
import net.kano.joscar.snac.VetoableSnacPacketListener;
import net.kano.joscar.snaccmd.DefaultClientFactoryList;
import net.kano.joscar.snaccmd.error.SnacError;
import net.kano.joustsim.JavaTools;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.ServiceEvent;
import net.kano.joustsim.oscar.oscar.service.ServiceFactory;
import net.kano.joustsim.oscar.oscar.service.ServiceListener;
import net.kano.joustsim.oscar.oscar.service.ServiceManager;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.Timer;
import java.util.logging.Logger;

public class OscarConnection {
  //TODO: send ping flaps to keep connection alive
  private static final Logger LOGGER
      = Logger.getLogger(OscarConnection.class.getName());
  private static final int CONNECTION_DEAD_TIMEOUT = 30000;

  private final ClientFlapConn conn;
  private final String host;
  private final int port;

  private boolean triedConnect = false;
  private boolean disconnected = false;

  private final FlapProcessor flapProcessor;
  private final ClientSnacProcessor snacProcessor;

  private int[] snacFamilies = null;
  private final ServiceManager serviceManager = new ServiceManager();
  private ServiceFactory serviceFactory = null;

  private CopyOnWriteArrayList<OscarConnListener> listeners
      = new CopyOnWriteArrayList<OscarConnListener>();
  private int lastCloseCode = -1;
  private List<ServiceEvent> eventLog = new ArrayList<ServiceEvent>();

  private CopyOnWriteArrayList<ServiceListener> globalServiceListeners
      = new CopyOnWriteArrayList<ServiceListener>();

  private Set<Service> unready = new HashSet<Service>();
  private Set<Service> unfinished = new HashSet<Service>();
  private long lastPacketTime = 0;

  public OscarConnection(String host, int port) {
    DefensiveTools.checkNull(host, "host");
    DefensiveTools.checkRange(port, "port", 0);

    this.host = host;
    this.port = port;

    conn = new ClientFlapConn(new ConnDescriptor(host, port));

    flapProcessor = conn.getFlapProcessor();
    flapProcessor.setFlapCmdFactory(new DefaultFlapCmdFactory());

    snacProcessor = new ClientSnacProcessor(flapProcessor);
    snacProcessor.getCmdFactoryMgr().setDefaultFactoryList(new
        DefaultClientFactoryList());
    snacProcessor.addPreprocessor(new FamilyVersionPreprocessor());

    flapProcessor.addPacketListener(new FlapPacketListener() {
      public void handleFlapPacket(FlapPacketEvent flapPacketEvent) {
        FlapCommand flapCommand = flapPacketEvent.getFlapCommand();
        if (flapCommand instanceof FlapErrorCmd) {
          FlapErrorCmd flapErrorCmd = (FlapErrorCmd) flapCommand;
          LOGGER.warning("Received FLAP error packet: " + flapErrorCmd);
        }
        OscarConnection.this.handleFlapPacket(flapPacketEvent);
      }
    });
    snacProcessor.addPacketListener(new SnacPacketListener() {
      public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
        SnacCommand snacCommand = snacPacketEvent.getSnacCommand();
        if (snacCommand instanceof SnacError) {
          SnacError snacError = (SnacError) snacCommand;
          LOGGER.warning("Received SNAC error packet: " + snacError);
        }
        OscarConnection.this.handleSnacPacket(snacPacketEvent);
      }
    });
    snacProcessor.addGlobalResponseListener(new SnacResponseListener() {
      public void handleResponse(SnacResponseEvent snacResponseEvent) {
        OscarConnection.this.handleSnacResponse(snacResponseEvent);
      }
    });
    conn.addConnListener(new ClientConnListener() {
      public void stateChanged(ClientConnEvent clientConnEvent) {
        ClientConn.State state = clientConnEvent.getNewState();
        if (state == ClientConn.STATE_CONNECTED) {
          beforeServicesConnected();
          internalConnected();
          connected();
        } else if (state == ClientConn.STATE_FAILED) {
          connFailed();
        } else if (state == ClientConn.STATE_NOT_CONNECTED) {
          internalDisconnected();
          disconnected();
        }
        OscarConnection.this.stateChanged(clientConnEvent);
      }
    });
  }

  public void addOscarListener(OscarConnListener l) {
    listeners.addIfAbsent(l);
  }

  public void removeOscarListener(OscarConnListener l) {
    listeners.remove(l);
  }

  private void internalConnected() {
    LOGGER.fine("Connected to " + host);

    flapProcessor.addVetoablePacketListener(new VetoableFlapPacketListener() {
      public VetoResult handlePacket(FlapPacketEvent event) {
        if (event.getFlapPacket().getChannel() == 5) {
          LOGGER.finest("Got FLAP keepalive response from server");
        }
        updateLastPacketTime();
        return VetoableFlapPacketListener.VetoResult.CONTINUE_PROCESSING;
      }
    });
    snacProcessor.addVetoablePacketListener(new VetoableSnacPacketListener() {
      public Object handlePacket(SnacPacketEvent event) {
        return null;
      }
    });
    Timer timer = new Timer("Connection timeout checker", true);
    timer.scheduleAtFixedRate(new TimerTask() {
      public void run() {
        if (!isDisconnected()) {
          sendFlap(new KeepaliveFlapCommand());

          long last = lastPacketTime;
          if (last != 0) {
            long sinceLast = System.currentTimeMillis() - last;
            if (sinceLast >= CONNECTION_DEAD_TIMEOUT) {
              LOGGER.warning("Connection to " + getClientFlapConn()
                  + " timed out after " + sinceLast
                  + " ms; disconnecting");
              disconnect();
            }
          }
        }
      }
    }, 0, 10000);

    for (Service service : getServices()) {
      service.connected();
    }
  }

  private void updateLastPacketTime() {
    lastPacketTime = System.currentTimeMillis();
  }

  private void internalDisconnected() {
    LOGGER.fine("Disconnected from " + host);

    for (Service service : getServices()) service.disconnected();
  }

  private void stateChanged(ClientConnEvent clientConnEvent) {
    OscarConnStateEvent evt;
    if (clientConnEvent.getNewState() == ClientConn.STATE_NOT_CONNECTED) {
      evt = new OscarConnDisconnectEvent(clientConnEvent, getLastCloseCode());
    } else {
      evt = new OscarConnStateEvent(clientConnEvent);
    }
    for (OscarConnListener l : listeners) l.connStateChanged(this, evt);
  }

  private void registeredSnacFamilies() {
    for (OscarConnListener l : listeners) l.registeredSnacFamilies(this);
  }


  public synchronized ServiceFactory getServiceFactory() {
    return serviceFactory;
  }

  public synchronized void setServiceFactory(ServiceFactory serviceFactory) {
    checkFieldModify();
    DefensiveTools.checkNull(serviceFactory, "serviceFactory");

    this.serviceFactory = serviceFactory;
  }

  protected synchronized void checkFieldModify() {
    if (triedConnect) {
      throw new IllegalStateException("Property cannot be modified after "
          + "connect() has been called");
    }
  }

  public final ClientFlapConn getClientFlapConn() { return conn; }

  public synchronized void connect() throws IllegalStateException {
    if (triedConnect) {
      throw new IllegalStateException("cannot connect more than once");
    }
    if (serviceFactory == null) {
      throw new IllegalStateException("cannot connect without first "
          + "setting a ServiceFactory");
    }
    beforeConnect();
    triedConnect = true;
    LOGGER.fine("OscarConnection to " + host + " trying to connect...");
    conn.connect();
  }

  public synchronized boolean isDisconnected() { return disconnected; }

  public synchronized boolean disconnect() {
    if (!triedConnect) {
      throw new IllegalStateException("was never connected");
    }
    if (disconnected) return false;
    disconnected = true;
    conn.disconnect();
    return true;
  }

  public String getHost() { return host; }

  public int getPort() { return port; }

  protected FlapProcessor getFlapProcessor() {
    return flapProcessor;
  }

  public ClientSnacProcessor getSnacProcessor() {
    return snacProcessor;
  }

  public ClientConn.State getConnectionState() {
    return conn.getState();
  }

  public void sendFlap(FlapCommand flap) {
    flapProcessor.sendFlap(flap);
  }

  public void sendSnac(SnacCommand snac) {
    DefensiveTools.checkNull(snac, "snac");

    snacProcessor.sendSnac(new SnacRequest(snac, null));
  }

  public void sendSnacRequest(SnacCommand snac, SnacRequestListener listener) {
    DefensiveTools.checkNull(snac, "snac");
    DefensiveTools.checkNull(listener, "listener");

    snacProcessor.sendSnac(new SnacRequest(snac, listener));
  }

  public void sendSnacRequest(SnacRequest snac) {
    DefensiveTools.checkNull(snac, "snac");

    snacProcessor.sendSnac(snac);
  }

  protected void beforeConnect() { }

  protected void connFailed() { }

  protected void beforeServicesConnected() { }

  protected void connected() { }

  protected void disconnected() { }

  protected void handleFlapPacket(FlapPacketEvent flapPacketEvent) {
    FlapCommand flap = flapPacketEvent.getFlapCommand();

    if (flap instanceof CloseFlapCmd) {
      CloseFlapCmd cfc = (CloseFlapCmd) flap;

      setLastCloseCode(cfc.getCode());
    }
  }

  protected void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
    Service service = getService(snacPacketEvent);
    if (service != null) service.handleSnacPacket(snacPacketEvent);
  }

  private Service getService(SnacPacketEvent snacPacketEvent) {
    int family = snacPacketEvent.getSnacPacket().getFamily();
    return getService(family);
  }

  protected void handleSnacResponse(SnacResponseEvent snacResponseEvent) {
    Service service = getService(snacResponseEvent);
    if (service != null) service.handleSnacPacket(snacResponseEvent);
  }


  public final void setSnacFamilies(int... snacFamilies)
      throws IllegalStateException {
    List<Service> services;
    synchronized (this) {
      if (this.snacFamilies != null) {
        throw new IllegalStateException("this connection "
            + MiscTools.getClassName(this) + " already has SNAC "
            + "families set");
      }
      DefensiveTools.checkNull(snacFamilies, "snacFamilies");

      int[] families = snacFamilies.clone();
      Arrays.sort(families);
      this.snacFamilies = families;

      services = new ArrayList<Service>(snacFamilies.length);
      for (int family : families) {
        Service service = serviceFactory.getService(this, family);

        if (service == null) {
          LOGGER.finer("No service for family 0x"
              + Integer.toHexString(family));
          continue;
        }

        int family2 = service.getFamily();
        if (family2 != family) {
          LOGGER.warning("Service returned by ServiceFactory for family "
              + "0x" + family + " is of wrong family (0x"
              + Integer.toHexString(family2) + ")");
          continue;
        }
        serviceManager.setService(family, service);
        services.add(service);
      }

      unready.addAll(services);
      unfinished.addAll(services);
    }

    // services are initialized in the ascending order of their family codes
    ClientConn.State state = conn.getState();
    boolean connected = state == ClientConn.STATE_CONNECTED;
    boolean disconnected = isDisconnected();
    for (Service service : services) {
      service.addServiceListener(new ServiceListener() {
        public void handleServiceReady(Service service) {
          serviceReady(service);
        }

        public void handleServiceFinished(Service service) {
          serviceFinished(service);
        }
      });

      if (connected) {
        service.connected();
      } else if (disconnected) {
        service.disconnected();
      }
    }

    registeredSnacFamilies();
  }

  public void addGlobalServiceListener(ServiceListener l) {
    globalServiceListeners.addIfAbsent(l);
  }

  public void removeGlobalServiceListener(ServiceListener l) {
    globalServiceListeners.remove(l);
  }

  private void serviceReady(Service service) {
    boolean allReady;
    synchronized (this) {
      unready.remove(service);
      LOGGER.finer(service.getClass().getName() + " is ready, waiting for "
          + unready.size() + ": " + unready);
      allReady = unready.isEmpty();
    }
    for (ServiceListener sl : globalServiceListeners) {
      sl.handleServiceReady(service);
    }
    if (allReady) {
      LOGGER.finer("All services are ready");
      for (OscarConnListener l : listeners) {
        LOGGER.finer("Telling " + l.getClass().getName()
            + " that all services are ready");

        l.allFamiliesReady(this);
      }
    }
  }

  private void serviceFinished(Service service) {
    boolean allFinished;
    synchronized (this) {
      unfinished.remove(service);
      allFinished = unfinished.isEmpty();
    }
    for (ServiceListener sl : globalServiceListeners) {
      sl.handleServiceFinished(service);
    }
    if (allFinished) disconnect();
  }

  public synchronized final int[] getSnacFamilies() { return snacFamilies; }

  public Service getService(int family) {
    return serviceManager.getService(family);
  }

  public List<Service> getServices() {
    return serviceManager.getServices();
  }

  public synchronized void setLastCloseCode(int lastCloseCode) {
    this.lastCloseCode = lastCloseCode;
  }

  public synchronized int getLastCloseCode() {
    return lastCloseCode;
  }

  public void postServiceEvent(ServiceEvent event) {
    synchronized (this) {
      eventLog.add(event);
    }
    for (Service service : getServices()) {
      service.handleEvent(event);
    }
  }

  public synchronized <E extends ServiceEvent> List<E> getServiceEvents(
      Class<E> cls) {
    List<E> matches = new ArrayList<E>();
    for (ServiceEvent event : eventLog) {
      if (cls.isInstance(event)) matches.add(JavaTools.cast(cls, event));
    }
    return matches;
  }

  public synchronized List<ServiceEvent> getEventLog() {
    return DefensiveTools.getUnmodifiableCopy(eventLog);
  }

  private static class KeepaliveFlapCommand extends FlapCommand {
    public KeepaliveFlapCommand() {super(5);}

    public void writeData(OutputStream out) {
    }
  }

}
