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

package net.kano.aimcrypto.connection.oscar;

import net.kano.aimcrypto.connection.oscar.service.Service;
import net.kano.aimcrypto.connection.oscar.service.ServiceFactory;
import net.kano.aimcrypto.connection.oscar.service.ServiceListener;
import net.kano.aimcrypto.connection.oscar.service.ServiceManager;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.MiscTools;
import net.kano.joscar.flap.ClientFlapConn;
import net.kano.joscar.flap.FlapCommand;
import net.kano.joscar.flap.FlapPacketEvent;
import net.kano.joscar.flap.FlapPacketListener;
import net.kano.joscar.flap.FlapProcessor;
import net.kano.joscar.flapcmd.DefaultFlapCmdFactory;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.flapcmd.CloseFlapCmd;
import net.kano.joscar.net.ClientConn;
import net.kano.joscar.net.ClientConnEvent;
import net.kano.joscar.net.ClientConnListener;
import net.kano.joscar.snac.ClientSnacProcessor;
import net.kano.joscar.snac.FamilyVersionPreprocessor;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snac.SnacPacketListener;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snac.SnacResponseListener;
import net.kano.joscar.snaccmd.DefaultClientFactoryList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class OscarConnection {
    private static final Logger logger = Logger.getLogger(OscarConnection.class.getName());

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

    private CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();

    public OscarConnection(String host, int port) {
        DefensiveTools.checkNull(host, "host");
        DefensiveTools.checkRange(port, "port", 0);

        this.host = host;
        this.port = port;

        conn = new ClientFlapConn(host, port);

        flapProcessor = conn.getFlapProcessor();
        flapProcessor.setFlapCmdFactory(new DefaultFlapCmdFactory());

        snacProcessor = new ClientSnacProcessor(flapProcessor);
        snacProcessor.getCmdFactoryMgr().setDefaultFactoryList(new
                DefaultClientFactoryList());
        snacProcessor.addPreprocessor(new FamilyVersionPreprocessor());

        flapProcessor.addPacketListener(new FlapPacketListener() {
            public void handleFlapPacket(FlapPacketEvent flapPacketEvent) {
                OscarConnection.this.handleFlapPacket(flapPacketEvent);
            }
        });
        snacProcessor.addPacketListener(new SnacPacketListener() {
            public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
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
        logger.fine("Connected to " + host);

        Service[] services = getServices();
        for (int i = 0; i < services.length; i++) {
            Service service = services[i];
            service.connected();
        }
    }

    private void internalDisconnected() {
        logger.fine("Disconnected from " + host);

        Service[] services = getServices();
        for (int i = 0; i < services.length; i++) {
            Service service = services[i];
            service.disconnected();
        }
    }

    private void stateChanged(ClientConnEvent clientConnEvent) {
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            OscarConnListener l = (OscarConnListener) it.next();

            l.connStateChanged(this, clientConnEvent);
        }
    }

    private void registeredSnacFamilies() {
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            OscarConnListener l = (OscarConnListener) it.next();

            l.registeredSnacFamilies(this);
        }
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
        logger.fine("OscarConnection to " + host + " trying to connect...");
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
        snacProcessor.sendSnac(new SnacRequest(snac, null));
    }

    protected void beforeConnect() { }

    protected void connFailed() { }

    protected void connected() { }

    protected void disconnected() { }

    protected void handleFlapPacket(FlapPacketEvent flapPacketEvent) {
        FlapCommand flap = flapPacketEvent.getFlapCommand();

        if (flap instanceof CloseFlapCmd) {
            CloseFlapCmd cfc = (CloseFlapCmd) flap;

            if (cfc.getCode() != 0) {
                //TODO: handle connection closed code
            }
        }
    }

    protected void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
        getService(snacPacketEvent).handleSnacPacket(snacPacketEvent);
    }

    private Service getService(SnacPacketEvent snacPacketEvent) {
        int family = snacPacketEvent.getSnacCommand().getFamily();
        Service service = getService(family);
        return service;
    }

    protected void handleSnacResponse(SnacResponseEvent snacResponseEvent) {
        getService(snacResponseEvent).handleSnacPacket(snacResponseEvent);
    }


    public final void setSnacFamilies(int[] snacFamilies)
            throws IllegalStateException {
        List services;
        synchronized(this) {
            if (this.snacFamilies != null) {
                throw new IllegalStateException("this connection "
                        + MiscTools.getClassName(this) + " already has SNAC "
                        + "families set");
            }
            DefensiveTools.checkNull(snacFamilies, "snacFamilies");

            int[] families = (int[]) snacFamilies.clone();
            Arrays.sort(families);
            this.snacFamilies = families;

            services = new ArrayList(snacFamilies.length);
            for (int i = 0; i < snacFamilies.length; i++) {
                int family = snacFamilies[i];

                Service service = serviceFactory.getService(this, family);

                if (service == null) {
                    logger.finer("No service for family 0x"
                            + Integer.toHexString(family));
                    continue;
                }

                int family2 = service.getFamily();
                if (family2 != family) {
                    logger.warning("Service returned by ServiceFactory for family "
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
        for (Iterator it = services.iterator(); it.hasNext();) {
            Service service = (Service) it.next();

            service.addServiceListener(new ServiceListener() {
                public void ready(Service service) {
                    serviceReady(service);
                }

                public void finished(Service service) {
                    serviceFinished(service);
                }
            });

            if (connected) service.connected();
            else if (disconnected) service.disconnected();
        }

        registeredSnacFamilies();
    }

    private CopyOnWriteArrayList globalServiceListeners
            = new CopyOnWriteArrayList();

    public void addGlobalServiceListener(ServiceListener l) {
        globalServiceListeners.addIfAbsent(l);
    }

    public void removeGlobalServiceListener(ServiceListener l) {
        globalServiceListeners.remove(l);
    }

    private Set unready = new HashSet();
    private Set unfinished = new HashSet();

    private void serviceReady(Service service) {
        boolean allReady;
        synchronized(this) {
            unready.remove(service);
            allReady = unready.isEmpty();
        }
        for (Iterator it = globalServiceListeners.iterator(); it.hasNext();) {
            ServiceListener sl = (ServiceListener) it.next();

            sl.ready(service);
        }
        if (allReady) {
            for (Iterator it = listeners.iterator(); it.hasNext();) {
                OscarConnListener l = (OscarConnListener) it.next();

                l.allFamiliesReady(this);
            }
        }
    }

    private void serviceFinished(Service service) {
        boolean allFinished;
        synchronized(this) {
            unfinished.remove(service);
            allFinished = unfinished.isEmpty();
        }
        for (Iterator it = globalServiceListeners.iterator(); it.hasNext();) {
            ServiceListener sl = (ServiceListener) it.next();

            sl.finished(service);
        }
        if (allFinished) {
            disconnect();
        }
    }

    public synchronized final int[] getSnacFamilies() { return snacFamilies; }

    public Service getService(int family) {
        return serviceManager.getService(family);
    }

    public Service[] getServices() {
        return serviceManager.getServices();
    }
}
