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

package net.kano.aimcrypto.connection;

import net.kano.aimcrypto.AimSession;
import net.kano.aimcrypto.AppSession;
import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.connection.oscar.BasicConnection;
import net.kano.aimcrypto.connection.oscar.LoginConnection;
import net.kano.aimcrypto.connection.oscar.LoginServiceListener;
import net.kano.aimcrypto.connection.oscar.OscarConnListener;
import net.kano.aimcrypto.connection.oscar.OscarConnection;
import net.kano.aimcrypto.connection.oscar.loginstatus.LoginFailureInfo;
import net.kano.aimcrypto.connection.oscar.loginstatus.LoginSuccessInfo;
import net.kano.aimcrypto.connection.oscar.service.BosService;
import net.kano.aimcrypto.connection.oscar.service.IcbmService;
import net.kano.aimcrypto.connection.oscar.service.LoginService;
import net.kano.aimcrypto.connection.oscar.service.Service;
import net.kano.aimcrypto.connection.oscar.service.ServiceFactory;
import net.kano.aimcrypto.connection.oscar.service.BuddyService;
import net.kano.aimcrypto.connection.oscar.service.InfoService;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.net.ClientConn;
import net.kano.joscar.net.ClientConnEvent;
import net.kano.joscar.snaccmd.auth.AuthCommand;
import net.kano.joscar.snaccmd.conn.ConnCommand;
import net.kano.joscar.snaccmd.icbm.IcbmCommand;
import net.kano.joscar.snaccmd.buddy.BuddyCommand;
import net.kano.joscar.snaccmd.loc.LocCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

    private boolean triedConnecting = false;

    private LoginConnection loginConn = null;
    private BasicConnection mainConn = null;

    private Map snacfamilies = new HashMap();

    private State state = State.NOT_CONNECTED;
    private StateInfo stateInfo = EmptyStateInfo.getInstance();
    private CopyOnWriteArrayList stateListeners = new CopyOnWriteArrayList();

    public AimConnection(AppSession appSession, AimSession aimSession,
            AimConnectionProperties props) throws IllegalArgumentException {
        Screenname sn = aimSession.getScreenname();
        if (!props.getScreenname().equals(sn)) {
            throw new IllegalArgumentException("connection properties object "
                    + "is for screenname " + props.getScreenname() + ", but "
                    + "this connection is for " + sn);
        }
        DefensiveTools.checkNull(appSession, "appSession");
        DefensiveTools.checkNull(aimSession, "aimSession");
        DefensiveTools.checkNull(props, "props");

        if (!props.isComplete()) {
            throw new IllegalArgumentException("connection properties are "
                    + "incomplete (props.isComplete() == false)");
        }

        this.appSession = appSession;
        this.aimSession = aimSession;

        this.screenname = sn;

        loginConn = new LoginConnection(props.getLoginHost(),
                props.getLoginPort());
        password = props.getPass();
        loginConn.addOscarListener(new LoginConnListener());
        loginConn.setServiceFactory(new LoginServiceFactory());
    }

    public Screenname getScreenname() { return screenname; }

    private void setState(State state, StateInfo info) {
        DefensiveTools.checkNull(state, "state");
        DefensiveTools.checkNull(info, "info");

        logger.fine("New state: " + state + " - " + info);

        State oldState;
        StateInfo oldStateInfo;
        synchronized(this) {
            oldState = this.state;
            oldStateInfo = this.stateInfo;

            this.state = state;
            this.stateInfo = info;
        }

        StateEvent event = new StateEvent(this, oldState, oldStateInfo, state,
                info);

        for (Iterator it = stateListeners.iterator(); it.hasNext();) {
            StateListener listener = (StateListener) it.next();

            listener.handleStateChange(event);
        }
    }

    public void addStateListener(StateListener l) {
        stateListeners.addIfAbsent(l);
    }

    public void removeStateListener(StateListener l) {
        stateListeners.remove(l);
    }

    private void connectBos(LoginSuccessInfo info) {
        mainConn = new BasicConnection(info.getServer(), info.getPort());
        mainConn.setCookie(info.getCookie());
        mainConn.addOscarListener(new BosConnListener());
        mainConn.setServiceFactory(new BasicServiceFactory());
        setState(State.CONNECTING, new ConnectingStateInfo(info));
        mainConn.connect();
    }

    private void internalDisconnected() {
        //TODO: close all related OSCAR connections
        if (loginConn != null) loginConn.disconnect();
        if (mainConn != null) mainConn.disconnect();
        setState(State.DISCONNECTED, new DisconnectedStateInfo());
    }

    public synchronized boolean getTriedConnecting() {
        return triedConnecting;
    }

    public synchronized void connect() throws IllegalStateException {
        if (triedConnecting) {
            throw new IllegalStateException("already connected");
        }
        triedConnecting = true;
        loginConn.connect();
        setState(State.CONNECTINGAUTH, new AuthorizingStateInfo(loginConn));
    }

    private synchronized void recordSnacFamilies(OscarConnection conn) {
        int[] families = conn.getSnacFamilies();
        for (int i = 0; i < families.length; i++) {
            int family = families[i];
            List services = getServiceList(family);
            services.add(conn.getService(family));
        }
    }

    private synchronized List getServiceList(int family) {
        Integer key = new Integer(family);
        List list = (List) snacfamilies.get(key);
        if (list == null) {
            list = new ArrayList();
            snacfamilies.put(key, list);
        }
        return list;
    }

    public synchronized Service getService(int family) {
        List list = getServiceListIfExists(family);
        if (list == null || list.isEmpty()) return null;
        return (Service) list.get(0);
    }

    public LoginService getLoginService() {
        Service service = getService(AuthCommand.FAMILY_AUTH);
        if (service instanceof LoginService) return (LoginService) service;
        else return null;
    }

    public BosService getBosService() {
        Service service = getService(ConnCommand.FAMILY_CONN);
        if (service instanceof BosService) return (BosService) service;
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

    private synchronized List getServiceListIfExists(int family) {
        return (List) snacfamilies.get(new Integer(family));
    }

    public void sendSnac(SnacCommand snac) {
        Service service = getService(snac.getFamily());
        service.sendSnac(snac);
    }

    public void disconnect() {
        //TODO: disconnect
        if (loginConn != null) loginConn.disconnect();
        if (mainConn != null) mainConn.disconnect();
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
                return new BosService(AimConnection.this, conn);
            } else if (family == IcbmCommand.FAMILY_ICBM) {
                return new IcbmService(AimConnection.this, conn);
            } else {
                return null;
            }
        }
    }

    private class DefaultConnListener implements OscarConnListener {
        public void registeredSnacFamilies(OscarConnection conn) {
            recordSnacFamilies(conn);
        }

        public void connStateChanged(OscarConnection conn,
                ClientConnEvent event) {
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

        public void connStateChanged(OscarConnection conn, ClientConnEvent event) {
            ClientConn.State state = event.getNewState();
            if (state == ClientConn.STATE_CONNECTED) {
                setState(State.AUTHORIZING, new AuthorizingStateInfo(loginConn));
            }
        }
    }

    private class LoginProcessListener implements LoginServiceListener {
        public void loginSucceeded(LoginSuccessInfo info) {
            connectBos(info);
        }

        public void loginFailed(LoginFailureInfo info) {
            setState(State.FAILED, new LoginFailureStateInfo(info));
        }
    }

    private class BosConnListener extends DefaultConnListener {
        public void allFamiliesReady(OscarConnection conn) {
            super.allFamiliesReady(conn);

            setState(State.ONLINE, new OnlineStateInfo());
        }

        public void connStateChanged(OscarConnection conn, ClientConnEvent event) {
            ClientConn.State state = event.getNewState();
            if (state == ClientConn.STATE_FAILED) {
                setState(State.FAILED, new ConnectionFailedStateInfo(
                        conn.getHost(), conn.getPort()));
            } else if (state == ClientConn.STATE_NOT_CONNECTED) {
                internalDisconnected();
            } else if (state == ClientConn.STATE_CONNECTED) {
                setState(State.SIGNINGON, new SigningOnStateInfo());
            }
        }
    }
}
