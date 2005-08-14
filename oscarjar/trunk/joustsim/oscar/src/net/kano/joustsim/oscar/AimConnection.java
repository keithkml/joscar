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

package net.kano.joustsim.oscar;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.net.ClientConn;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.auth.AuthCommand;
import net.kano.joscar.snaccmd.buddy.BuddyCommand;
import net.kano.joscar.snaccmd.conn.ConnCommand;
import net.kano.joscar.snaccmd.icbm.IcbmCommand;
import net.kano.joscar.snaccmd.icon.IconCommand;
import net.kano.joscar.snaccmd.loc.LocCommand;
import net.kano.joscar.snaccmd.ssi.SsiCommand;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.BasicConnection;
import net.kano.joustsim.oscar.oscar.LoginConnection;
import net.kano.joustsim.oscar.oscar.LoginServiceListener;
import net.kano.joustsim.oscar.oscar.OscarConnListener;
import net.kano.joustsim.oscar.oscar.OscarConnStateEvent;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.loginstatus.LoginFailureInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.LoginSuccessInfo;
import net.kano.joustsim.oscar.oscar.service.DefaultServiceArbiterFactory;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.ServiceArbiter;
import net.kano.joustsim.oscar.oscar.service.ServiceArbiterFactory;
import net.kano.joustsim.oscar.oscar.service.ServiceFactory;
import net.kano.joustsim.oscar.oscar.service.ServiceListener;
import net.kano.joustsim.oscar.oscar.service.ServiceArbitrationManager;
import net.kano.joustsim.oscar.oscar.service.bos.ExternalBosService;
import net.kano.joustsim.oscar.oscar.service.bos.MainBosService;
import net.kano.joustsim.oscar.oscar.service.bos.OpenedExternalServiceListener;
import net.kano.joustsim.oscar.oscar.service.buddy.BuddyService;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmService;
import net.kano.joustsim.oscar.oscar.service.icon.IconServiceArbiter;
import net.kano.joustsim.oscar.oscar.service.info.BuddyTrustAdapter;
import net.kano.joustsim.oscar.oscar.service.info.BuddyTrustEvent;
import net.kano.joustsim.oscar.oscar.service.info.BuddyTrustManager;
import net.kano.joustsim.oscar.oscar.service.info.CertificateInfoTrustManager;
import net.kano.joustsim.oscar.oscar.service.info.InfoService;
import net.kano.joustsim.oscar.oscar.service.login.LoginService;
import net.kano.joustsim.oscar.oscar.service.ssi.SsiService;
import net.kano.joustsim.trust.CertificateTrustManager;
import net.kano.joustsim.trust.SignerTrustManager;
import net.kano.joustsim.trust.TrustPreferences;
import net.kano.joustsim.trust.TrustedCertificatesTracker;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

//TODO: time out waiting for all services to be ready, to ensure connection comes up eventually
public class AimConnection {
    private static final Logger LOGGER
            = Logger.getLogger(AimConnection.class.getName());

    private final AppSession appSession;
    private final AimSession aimSession;

    private final Screenname screenname;
    private String password;

    private final LoginConnection loginConn;
    private BasicConnection mainConn = null;

    private Map<Integer,List<Service>> snacfamilies
            = new HashMap<Integer, List<Service>>();

    private boolean triedConnecting = false;
    private State state = State.NOTCONNECTED;
    private StateInfo stateInfo = NotConnectedStateInfo.getInstance();
    private boolean wantedDisconnect = false;

    private CopyOnWriteArrayList<StateListener> stateListeners
            = new CopyOnWriteArrayList<StateListener>();
    private CopyOnWriteArrayList<OpenedServiceListener> serviceListeners
            = new CopyOnWriteArrayList<OpenedServiceListener>();

    private final BuddyInfoManager buddyInfoManager;
    private final BuddyInfoTracker buddyInfoTracker;
    private final BuddyIconTracker buddyIconTracker;
    private final CertificateInfoTrustManager certificateInfoTrustManager;
    private final TrustedCertificatesTracker trustedCertificatesTracker;
    private final TrustPreferences localPrefs;
    private final BuddyTrustManager buddyTrustManager;
    private final CapabilityManager capabilityManager;

    private ServiceArbiterFactory arbiterFactory
            = new DefaultServiceArbiterFactory();

    private final Object externalServicesLock = new Object();
    private final Map<Integer, ServiceArbiter<? extends Service>> externalServices
            = new HashMap<Integer, ServiceArbiter<? extends Service>>();
    private Map<ServiceArbiter<? extends Service>,OscarConnection> externalConnections
            = new HashMap<ServiceArbiter<? extends Service>, OscarConnection>();
    private ServiceArbitrationManager arbitrationManager = new ServiceArbitrationManager() {
        public void openService(ServiceArbiter<? extends Service> arbiter) {
            int family = arbiter.getSnacFamily();
            if (getServiceArbiter(family) == arbiter) {
                requestService(family, arbiter);
            }
        }
    };

    public AimConnection(Screenname screenname, String password) {
        this(new AimConnectionProperties(screenname, password));
    }

    public AimConnection(AimConnectionProperties props) {
        this(new DefaultAimSession(props.getScreenname()), props);
    }

    public AimConnection(AimSession aimSession, AimConnectionProperties props) {
        this(aimSession, aimSession.getTrustPreferences(), props);
    }

    public AimConnection(AimSession aimSession,
            TrustPreferences prefs, AimConnectionProperties props)
            throws IllegalArgumentException {
        Screenname sn = aimSession.getScreenname();
        if (!props.getScreenname().equals(sn)) {
            throw new IllegalArgumentException("connection properties object "
                    + "is for screenname " + props.getScreenname() + ", but "
                    + "this connection is for " + sn);
        }
        DefensiveTools.checkNull(aimSession, "aimSession");
        DefensiveTools.checkNull(props, "props");

        if (!props.isComplete()) {
            throw new IllegalArgumentException("connection properties are "
                    + "incomplete (props.isComplete() == false): " + props);
        }

        this.appSession = aimSession.getAppSession();
        this.aimSession = aimSession;

        this.screenname = sn;

        this.buddyInfoManager = new BuddyInfoManager(this);
        this.buddyInfoTracker = new BuddyInfoTracker(this);
        this.buddyIconTracker = new BuddyIconTracker(this);
        this.loginConn = new LoginConnection(props.getLoginHost(),
                props.getLoginPort());
        this.password = props.getPassword();
        this.localPrefs = prefs;

        CertificateTrustManager certMgr;
        SignerTrustManager signerMgr;
        if (prefs != null) {
            certMgr = prefs.getCertificateTrustManager();
            signerMgr = prefs.getSignerTrustManager();

        } else {
            LOGGER.warning("Warning: this AIM connection's certificate "
                    + "and signer managers will not be set because the trust "
                    + "manager is null");
            certMgr = null;
            signerMgr = null;
        }
        trustedCertificatesTracker = new TrustedCertificatesTracker(certMgr,
                signerMgr);
        certificateInfoTrustManager
                = new CertificateInfoTrustManager(trustedCertificatesTracker);
        buddyTrustManager = new BuddyTrustManager(this);
        buddyTrustManager.addBuddyTrustListener(new BuddyTrustAdapter() {
            public void buddyTrusted(BuddyTrustEvent event) {
                LOGGER.fine("* " + event.getBuddy() + " is trusted");
            }

            public void buddyTrustRevoked(BuddyTrustEvent event) {
                LOGGER.fine("* " + event.getBuddy() + " is no longer trusted");
            }

            public void gotTrustedCertificateChange(BuddyTrustEvent event) {
                LOGGER.fine("* " + event.getBuddy() + " has a trusted certificate");
            }

            public void gotUntrustedCertificateChange(BuddyTrustEvent event) {
                LOGGER.fine("* " + event.getBuddy() + " has an untrusted certificate");
            }
        });
        capabilityManager = new CapabilityManager(this);
        capabilityManager.setCapabilityHandler(CapabilityBlock.BLOCK_ENCRYPTION,
                new SecurityEnabledHandler(this));
        capabilityManager.setCapabilityHandler(CapabilityBlock.BLOCK_ICQCOMPATIBLE,
                new DefaultEnabledCapabilityHandler());
        capabilityManager.setCapabilityHandler(CapabilityBlock.BLOCK_SHORTCAPS,
                new DefaultEnabledCapabilityHandler());

        loginConn.addOscarListener(new LoginConnListener());
        loginConn.setServiceFactory(new LoginServiceFactory());
    }

    public final AppSession getAppSession() { return appSession; }

    public final AimSession getAimSession() { return aimSession; }

    public final Screenname getScreenname() { return screenname; }

    public BuddyInfoManager getBuddyInfoManager() { return buddyInfoManager; }

    public BuddyIconTracker getBuddyIconTracker() { return buddyIconTracker; }

    public CertificateInfoTrustManager getCertificateInfoTrustManager() {
        return certificateInfoTrustManager;
    }

    public TrustedCertificatesTracker getTrustedCertificatesTracker() {
        return trustedCertificatesTracker;
    }

    public TrustPreferences getLocalPrefs() {
        return localPrefs;
    }

    public BuddyTrustManager getBuddyTrustManager() {
        return buddyTrustManager;
    }

    public CapabilityManager getCapabilityManager() {
        return capabilityManager;
    }

    public BuddyInfoTracker getBuddyInfoTracker() {
        return buddyInfoTracker;
    }

    public synchronized State getState() { return state; }

    public synchronized StateInfo getStateInfo() { return stateInfo; }

    public void addStateListener(StateListener l) {
        DefensiveTools.checkNull(l, "l");
        stateListeners.addIfAbsent(l);
    }

    public void removeStateListener(StateListener l) {
        DefensiveTools.checkNull(l, "l");
        stateListeners.remove(l);
    }
    
    public void addOpenedServiceListener(OpenedServiceListener l) {
        DefensiveTools.checkNull(l, "l");
        serviceListeners.addIfAbsent(l);
    }

    public void removeOpenedServiceListener(OpenedServiceListener l) {
        DefensiveTools.checkNull(l, "l");
        serviceListeners.remove(l);
    }

    public synchronized boolean getTriedConnecting() {
        return triedConnecting;
    }

    private synchronized boolean setTriedConnecting() {
        if (triedConnecting) return false;
        triedConnecting = true;
        return true;
    }

    public boolean connect() {
        setTriedConnecting();
        loginConn.connect();
        setState(State.NOTCONNECTED, State.CONNECTINGAUTH,
                new AuthorizingStateInfo(loginConn));
        return true;
    }

    public void disconnect() {
        disconnect(true);
    }

    public synchronized void disconnect(boolean onPurpose) {
        wantedDisconnect = onPurpose;
        closeConnections();
    }

    public synchronized boolean wantedDisconnect() { return wantedDisconnect; }

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
            if (arbiter == null) {
                return null;
            }
            externalServices.put(service, arbiter);
        }
        requestService(service, arbiter);
        return arbiter;
    }

    public IconServiceArbiter getIconServiceArbiter() {
        ServiceArbiter<?> arbiter = getServiceArbiter(IconCommand.FAMILY_ICON);
        if (arbiter instanceof IconServiceArbiter) {
            return (IconServiceArbiter) arbiter;
        } else {
            return null;
        }
    }

    private void refreshServiceIfNecessary(int family) {
        ServiceArbiter<? extends Service> arbiter;
        synchronized (externalServicesLock) {
            arbiter = externalServices.get(family);
        }
        if (arbiter == null || !arbiter.shouldKeepAlive()) return;
        requestService(family, arbiter);
    }

    private <S extends Service> void requestService(int service,
            final ServiceArbiter<S> arbiter) {
        LOGGER.info("Requesting external service " + service + " for "
                + arbiter);
        MainBosService bosService = getBosService();
        bosService.requestService(service,
                new ArbitratedExternalServiceListener<S>(arbiter));
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

    private synchronized void storeExternalConnection(BasicConnection conn,
            ServiceArbiter<? extends Service> arbiter) {
        externalConnections.put(arbiter, conn);
    }

    public @Nullable synchronized Service getService(int family) {
        List<Service> list = getServiceListIfExists(family);
        return list == null ? null : list.get(0);
    }

    public LoginService getLoginService() {
        return getServiceOfType(AuthCommand.FAMILY_AUTH, LoginService.class);
    }

    public MainBosService getBosService() {
        return getServiceOfType(ConnCommand.FAMILY_CONN, MainBosService.class);
    }

    public IcbmService getIcbmService() {
        return getServiceOfType(IcbmCommand.FAMILY_ICBM, IcbmService.class);
    }

    public BuddyService getBuddyService() {
        return getServiceOfType(BuddyCommand.FAMILY_BUDDY, BuddyService.class);
    }

    public InfoService getInfoService() {
        return getServiceOfType(LocCommand.FAMILY_LOC, InfoService.class);
    }

    public SsiService getSsiService() {
        return getServiceOfType(SsiCommand.FAMILY_SSI, SsiService.class);
    }

    private @Nullable <E extends Service> E getServiceOfType(int fam, Class<E> cls) {
        Service service = getService(fam);
        if (cls.isInstance(service)) {
            return cls.cast(service);
        } else {
            return null;
        }
    }

    public void sendSnac(SnacCommand snac) {
        int family = snac.getFamily();
        Service service = getService(family);
        if (service == null) {
            ServiceArbiter<?> arbiter = getServiceArbiter(family);
            if (arbiter != null) {

            }
        } else {
            service.sendSnac(snac);
        }
    }

    private boolean setState(State expectedOld, State state, StateInfo info) {
        DefensiveTools.checkNull(state, "state");
        DefensiveTools.checkNull(info, "info");

        LOGGER.fine("New state: " + state + " - " + info);

        State oldState;
        StateInfo oldStateInfo;
        synchronized(this) {
            oldState = this.state;
            oldStateInfo = this.stateInfo;

            if (expectedOld != null && oldState != expectedOld) {
                LOGGER.warning("Tried converting state " + expectedOld + " to "
                        + state + ", but was in " + oldState);
                return false;
            }

            this.state = state;
            this.stateInfo = info;
        }

        StateEvent event = new StateEvent(this, oldState, oldStateInfo, state,
                info);

        for (StateListener listener : stateListeners) {
            listener.handleStateChange(event);
        }
        return true;
    }

    private void connectBos(LoginSuccessInfo info) {
        BasicConnection mainConn = prepareMainConn(info);
        mainConn.connect();
        setState(State.AUTHORIZING, State.CONNECTING,
                new ConnectingStateInfo(info));
    }

    private synchronized BasicConnection prepareMainConn(LoginSuccessInfo info) {
        if (state != State.AUTHORIZING) {
            throw new IllegalStateException("tried to connect to BOS "
                    + "server in state " + state);
        }
        BasicConnection mainConn = new BasicConnection(info.getServer(),
                info.getPort());
        mainConn.setCookie(info.getCookie());
        mainConn.addOscarListener(new MainBosConnListener());
        mainConn.setServiceFactory(new BasicServiceFactory());
        this.mainConn = mainConn;
        return mainConn;
    }

    private void internalDisconnected() {
        setState(null, State.DISCONNECTED,
                new DisconnectedStateInfo(wantedDisconnect()));
        closeConnections();
        List<Service> services = DefensiveTools.getUnmodifiable(
                getLocalServices());
        for (OpenedServiceListener listener : serviceListeners) {
            listener.closedServices(this, services);
        }
    }

    private synchronized void closeConnections() {
        loginConn.disconnect();
        BasicConnection mainConn = this.mainConn;
        for (OscarConnection conn : externalConnections.values()) {
            conn.disconnect();
        }
        if (mainConn != null) mainConn.disconnect();
    }

    private void recordSnacFamilies(OscarConnection conn) {
        List<Service> added = new ArrayList<Service>();
        synchronized(this) {
            for (int family : conn.getSnacFamilies()) {
                List<Service> services = getServiceList(family);
                Service service = conn.getService(family);
                if (service == null) {
                    LOGGER.finer("Could not find service handler for family 0x"
                            + Integer.toHexString(family));
                } else {
                    services.add(service);
                    added.add(service);
                }
            }
        }

        List<Service> addedClone = DefensiveTools.getUnmodifiable(added);

        for (OpenedServiceListener listener : serviceListeners) {
            listener.openedServices(this, addedClone);
        }
    }

    private synchronized List<Service> getServiceList(int family) {
        List<Service> list = snacfamilies.get(family);
        if (list == null) {
            list = new ArrayList<Service>();
            snacfamilies.put(family, list);
        }
        return list;
    }

    private synchronized List<Service> getServiceListIfExists(int family) {
        List<Service> list = snacfamilies.get(family);
        return list == null || list.isEmpty() ? null : list;
    }

    private synchronized List<Service> getLocalServices() {
        List<Service> list = new ArrayList<Service>();
        for (Map.Entry<Integer, List<Service>> entry : snacfamilies
                .entrySet()) {
            list.addAll(entry.getValue());
        }
        return list;
    }

    private class LoginServiceFactory implements ServiceFactory {
        public Service getService(OscarConnection conn, int family) {
            if (family == AuthCommand.FAMILY_AUTH) {
                return new LoginService(AimConnection.this, loginConn,
                        screenname, password);

            } else {
                return null;
            }
        }
    }

    private class BasicServiceFactory implements ServiceFactory {
        public Service getService(OscarConnection conn, int family) {
            if (family == ConnCommand.FAMILY_CONN) {
                return new MainBosService(AimConnection.this, conn);
            } else if (family == IcbmCommand.FAMILY_ICBM) {
                return new IcbmService(AimConnection.this, conn);
            } else if (family == BuddyCommand.FAMILY_BUDDY) {
                return new BuddyService(AimConnection.this, conn);
            } else if (family == LocCommand.FAMILY_LOC) {
                return new InfoService(AimConnection.this, conn);
            } else if (family == SsiCommand.FAMILY_SSI) {
                return new SsiService(AimConnection.this, conn);
            } else {
                System.out.println("no service for family " + family);
                return null;
            }
        }
    }

    private class DefaultConnListener implements OscarConnListener {
        public void registeredSnacFamilies(OscarConnection conn) {
            recordSnacFamilies(conn);
        }

        public void connStateChanged(OscarConnection conn,
                OscarConnStateEvent event) {
        }

        public void allFamiliesReady(OscarConnection conn) {
        }
    }

    private class LoginConnListener extends DefaultConnListener {
        public void registeredSnacFamilies(OscarConnection conn) {
            super.registeredSnacFamilies(conn);

            LoginService ls = loginConn.getLoginService();
            ls.addLoginListener(new LoginProcessListener());
        }

        public void connStateChanged(OscarConnection conn, OscarConnStateEvent event) {
            ClientConn.State state = event.getClientConnEvent().getNewState();
            if (state == ClientConn.STATE_CONNECTED) {
                setState(State.CONNECTINGAUTH, State.AUTHORIZING,
                        new AuthorizingStateInfo(loginConn));
            }
        }
    }

    private class LoginProcessListener implements LoginServiceListener {
        public void loginSucceeded(LoginSuccessInfo info) {
            connectBos(info);
        }

        public void loginFailed(LoginFailureInfo info) {
            setState(null, State.FAILED, new LoginFailureStateInfo(info));
        }
    }

    private class MainBosConnListener extends DefaultConnListener {
        public void allFamiliesReady(OscarConnection conn) {
            super.allFamiliesReady(conn);

            // I don't know why this could happen
            if (getState() == State.CONNECTING) {
                LOGGER.finer("State was CONNECTING, but now we're ONLINE. The "
                        + "state should've been SIGNINGON.");
                setState(State.CONNECTING, State.SIGNINGON,
                        new SigningOnStateInfo());
            }
            setState(State.SIGNINGON, State.ONLINE, new OnlineStateInfo());
        }

        public void connStateChanged(OscarConnection conn,
                OscarConnStateEvent event) {
            ClientConn.State state = event.getClientConnEvent().getNewState();

            if (state == ClientConn.STATE_FAILED) {
                setState(null, State.FAILED, new ConnectionFailedStateInfo(
                        conn.getHost(), conn.getPort()));

            } else if (state == ClientConn.STATE_NOT_CONNECTED) {
                internalDisconnected();

            } else if (state == ClientConn.STATE_CONNECTED) {
                setState(State.CONNECTING, State.SIGNINGON,
                        new SigningOnStateInfo());
            }
        }
    }

    private class ExternalServiceFactory<S extends Service>
            implements ServiceFactory {
        private final int serviceFamily;
        private final ServiceArbiter<S> arbiter;

        public ExternalServiceFactory(int serviceFamily, ServiceArbiter<S> arbiter) {
            this.serviceFamily = serviceFamily;
            this.arbiter = arbiter;
        }

        public Service getService(OscarConnection conn, int family) {
            if (family == ConnCommand.FAMILY_CONN) {
                return new ExternalBosService(AimConnection.this, conn);

            } else if (family == serviceFamily) {
                return arbiter.createService(AimConnection.this, conn);

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
        private final ServiceArbiter<S> arbiter;

        public ExternalServiceConnListener(int serviceFamily,
                ServiceArbiter<S> arbiter) {
            this.arbiter = arbiter;
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
                        + serviceFamily + " ( " + arbiter + ")");
                conn.removeOscarListener(this);
                clearExternalConnection(conn, arbiter);
                refreshServiceIfNecessary(serviceFamily);
            }
        }

        public void allFamiliesReady(OscarConnection conn) {
        }
    }

    private class DispatchingServiceListener implements ServiceListener {
        public void handleServiceReady(Service service) {
            for (OpenedServiceListener listener : serviceListeners) {
                listener.openedServices(AimConnection.this,
                        Collections.singleton(service));
            }
        }

        public void handleServiceFinished(Service service) {
            for (OpenedServiceListener listener : serviceListeners) {
                listener.closedServices(AimConnection.this,
                        Collections.singleton(service));
            }
        }
    }

    private class ArbitratedExternalServiceListener<S extends Service> implements
            OpenedExternalServiceListener {
        private final ServiceArbiter<S> arbiter;

        public ArbitratedExternalServiceListener(ServiceArbiter<S> arbiter) {
            this.arbiter = arbiter;
        }

        public void handleServiceRedirect(final MainBosService service,
                final int serviceFamily, String host, int port,
                ByteBlock flapCookie) {
            //TODO: add timeout for external service to be ready
            //        ^-- should it be a global thing in OscarConnection,
            //            or what?
            int usePort;
            if (port <= 0) usePort = 5190;
            else usePort = port;
            LOGGER.fine("Connecting to " + host + ":" + port + " for external "
                    + "service " + serviceFamily);
            BasicConnection conn = new BasicConnection(host, usePort);
            conn.setServiceFactory(new ExternalServiceFactory<S>(
                    serviceFamily, arbiter));
            conn.setCookie(flapCookie);
            conn.addGlobalServiceListener(new DispatchingServiceListener());
            conn.addOscarListener(new ExternalServiceConnListener<S>(
                    serviceFamily, arbiter));
            storeExternalConnection(conn, arbiter);
            conn.connect();
        }
    }
}