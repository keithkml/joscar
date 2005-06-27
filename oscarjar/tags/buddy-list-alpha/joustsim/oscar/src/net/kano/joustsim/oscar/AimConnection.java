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

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.net.ClientConn;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.auth.AuthCommand;
import net.kano.joscar.snaccmd.buddy.BuddyCommand;
import net.kano.joscar.snaccmd.conn.ConnCommand;
import net.kano.joscar.snaccmd.icbm.IcbmCommand;
import net.kano.joscar.snaccmd.loc.LocCommand;
import net.kano.joscar.snaccmd.ssi.SsiCommand;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.BasicConnection;
import net.kano.joustsim.oscar.oscar.LoginConnection;
import net.kano.joustsim.oscar.oscar.LoginServiceListener;
import net.kano.joustsim.oscar.oscar.OscarConnListener;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.OscarConnStateEvent;
import net.kano.joustsim.oscar.oscar.loginstatus.LoginFailureInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.LoginSuccessInfo;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.ServiceFactory;
import net.kano.joustsim.oscar.oscar.service.bos.MainBosService;
import net.kano.joustsim.oscar.oscar.service.buddy.BuddyService;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AimConnection {
    private static final Logger logger
            = Logger.getLogger(AimConnection.class.getName());

    private final AppSession appSession;
    private final AimSession aimSession;

    private final Screenname screenname;
    private String password;

    private final LoginConnection loginConn;
    private BasicConnection mainConn = null;

    private Map<Integer,List<Service>> snacfamilies = new HashMap<Integer, List<Service>>();

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
    private final CertificateInfoTrustManager certificateInfoTrustManager;
    private final TrustedCertificatesTracker trustedCertificatesTracker;
    private final TrustPreferences localPrefs;
    private final BuddyTrustManager buddyTrustManager;
    private final CapabilityManager capabilityManager;

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
                    + "incomplete (props.isComplete() == false)");
        }

        this.appSession = aimSession.getAppSession();
        this.aimSession = aimSession;

        this.screenname = sn;

        this.buddyInfoManager = new BuddyInfoManager(this);
        this.buddyInfoTracker = new BuddyInfoTracker(this);
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
            logger.fine("Warning: this AIM connection's certificate and signer "
                    + "managers will not be set because the trust manager is "
                    + "null");
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
                System.out.println("* " + event.getBuddy() + " is trusted");
            }

            public void buddyTrustRevoked(BuddyTrustEvent event) {
                System.out.println("* " + event.getBuddy() + " is no longer trusted");
            }

            public void gotTrustedCertificateChange(BuddyTrustEvent event) {
                System.out.println("* " + event.getBuddy() + " has a trusted certificate");
            }

            public void gotUntrustedCertificateChange(BuddyTrustEvent event) {
                System.out.println("* " + event.getBuddy() + " has an untrusted certificate");
            }
        });
        capabilityManager = new CapabilityManager(this);
        capabilityManager.setCapabilityHandler(CapabilityBlock.BLOCK_ENCRYPTION,
                new SecurityEnabledHandler(this));
        capabilityManager.setCapabilityHandler(CapabilityBlock.BLOCK_ICQCOMPATIBLE,
                new EmptyCapabilityHandler());
        capabilityManager.setCapabilityHandler(CapabilityBlock.BLOCK_SHORTCAPS,
                new EmptyCapabilityHandler());

        loginConn.addOscarListener(new LoginConnListener());
        loginConn.setServiceFactory(new LoginServiceFactory());
    }

    public final AppSession getAppSession() { return appSession; }

    public final AimSession getAimSession() { return aimSession; }

    public final Screenname getScreenname() { return screenname; }

    public BuddyInfoManager getBuddyInfoManager() { return buddyInfoManager; }

    public synchronized State getState() { return state; }

    public synchronized StateInfo getStateInfo() { return stateInfo; }

    public void addStateListener(StateListener l) {
        stateListeners.addIfAbsent(l);
    }

    public void removeStateListener(StateListener l) {
        stateListeners.remove(l);
    }
    
    public void addOpenedServiceListener(OpenedServiceListener l) {
        serviceListeners.addIfAbsent(l);
    }

    public void removeNewServiceListener(OpenedServiceListener l) {
        serviceListeners.remove(l);
    }

    public synchronized boolean getTriedConnecting() {
        return triedConnecting;
    }

    public void connect() throws IllegalStateException {
        synchronized(this) {
            if (triedConnecting) {
                throw new IllegalStateException("already connected");
            }
            triedConnecting = true;
        }
        loginConn.connect();
        setState(State.NOTCONNECTED, State.CONNECTINGAUTH,
                new AuthorizingStateInfo(loginConn));
    }

    public void disconnect() {
        disconnect(true);
    }

    public void disconnect(boolean onPurpose) {
        wantedDisconnect = onPurpose;
        closeConnections();
    }

    public synchronized Service getService(int family) {
        List<Service> list = getServiceListIfExists(family);
        return list == null ? null : list.get(0);
    }

    public LoginService getLoginService() {
        Service service = getService(AuthCommand.FAMILY_AUTH);
        if (service instanceof LoginService) return (LoginService) service;
        else return null;
    }

    public MainBosService getBosService() {
        Service service = getService(ConnCommand.FAMILY_CONN);
        if (service instanceof MainBosService) return (MainBosService) service;
        else return null;
    }

    public IcbmService getIcbmService() {
        Service service = getService(IcbmCommand.FAMILY_ICBM);
        if (service instanceof IcbmService) return (IcbmService) service;
        else return null;
    }

    public BuddyService getBuddyService() {
        Service service = getService(BuddyCommand.FAMILY_BUDDY);
        if (service instanceof BuddyService) return (BuddyService) service;
        else return null;
    }

    public InfoService getInfoService() {
        Service service = getService(LocCommand.FAMILY_LOC);
        if (service instanceof InfoService) return (InfoService) service;
        else return null;
    }

    public SsiService getSsiService() {
        Service service = getService(SsiCommand.FAMILY_SSI);
        if (service instanceof SsiService) return (SsiService) service;
        else return null;
    }

    public void sendSnac(SnacCommand snac) {
        Service service = getService(snac.getFamily());
        service.sendSnac(snac);
    }

    private boolean setState(State expectedOld, State state, StateInfo info) {
        DefensiveTools.checkNull(state, "state");
        DefensiveTools.checkNull(info, "info");

        logger.fine("New state: " + state + " - " + info);

        State oldState;
        StateInfo oldStateInfo;
        synchronized(this) {
            oldState = this.state;
            oldStateInfo = this.stateInfo;

            if (expectedOld != null && oldState != expectedOld) {
                logger.warning("Tried converting state " + expectedOld + " to "
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
        synchronized(this) {
            if (state != State.AUTHORIZING) {
                throw new IllegalStateException("tried to connect to BOS "
                        + "server in state " + state);
            }
            mainConn = new BasicConnection(info.getServer(), info.getPort());
            mainConn.setCookie(info.getCookie());
            mainConn.addOscarListener(new MainBosConnListener());
            mainConn.setServiceFactory(new BasicServiceFactory());
            mainConn.connect();
        }
        setState(State.AUTHORIZING, State.CONNECTING,
                new ConnectingStateInfo(info));
    }

    private void internalDisconnected() {
        setState(null, State.DISCONNECTED, new DisconnectedStateInfo(wantedDisconnect));
        closeConnections();
    }

    private synchronized void closeConnections() {
        assert Thread.holdsLock(this);

        //TODO: close all related OSCAR connections here
        loginConn.disconnect();
        BasicConnection mainConn = this.mainConn;
        if (mainConn != null) mainConn.disconnect();
    }

    private void recordSnacFamilies(OscarConnection conn) {
        List<Service> added = new ArrayList<Service>();
        synchronized(this) {
            for (int family : conn.getSnacFamilies()) {
                List<Service> services = getServiceList(family);
                Service service = conn.getService(family);
                if (service != null) {
                    services.add(service);
                    added.add(service);
                } else {
                    logger.finer("Could not find service handler for family 0x"
                            + Integer.toHexString(family));
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

    public boolean wantedDisconnect() { return wantedDisconnect; }

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
                logger.finer("State was CONNECTING, but now we're ONLINE. The "
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

}
