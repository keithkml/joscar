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

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.net.ClientConn;
import net.kano.joscar.snaccmd.auth.AuthCommand;
import net.kano.joscar.snaccmd.buddy.BuddyCommand;
import net.kano.joscar.snaccmd.conn.ConnCommand;
import net.kano.joscar.snaccmd.icbm.IcbmCommand;
import net.kano.joscar.snaccmd.loc.LocCommand;
import net.kano.joscar.snaccmd.ssi.SsiCommand;
import net.kano.joustsim.oscar.oscar.BasicConnection;
import net.kano.joustsim.oscar.oscar.LoginConnection;
import net.kano.joustsim.oscar.oscar.LoginServiceListener;
import net.kano.joustsim.oscar.oscar.OscarConnListener;
import net.kano.joustsim.oscar.oscar.OscarConnStateEvent;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.loginstatus.LoginFailureInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.LoginSuccessInfo;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.ServiceFactory;
import net.kano.joustsim.oscar.oscar.service.bos.MainBosService;
import net.kano.joustsim.oscar.oscar.service.buddy.BuddyService;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmService;
import net.kano.joustsim.oscar.oscar.service.info.InfoService;
import net.kano.joustsim.oscar.oscar.service.login.LoginService;
import net.kano.joustsim.oscar.oscar.service.ssi.SsiService;

import java.util.logging.Logger;

public class ConnectionManager {
    private static final Logger LOGGER = Logger
            .getLogger(ConnectionManager.class.getName());

    private boolean triedConnecting = false;
    private State state = State.NOTCONNECTED;
    private StateInfo stateInfo = NotConnectedStateInfo.getInstance();
    private boolean wantedDisconnect = false;
    private CopyOnWriteArrayList<StateListener> stateListeners
            = new CopyOnWriteArrayList<StateListener>();

    private final LoginConnection loginConn;
    private BasicConnection mainConn = null;
    private final AimConnection aimConnection;
    private String password;

    public ConnectionManager(AimConnection aimConnection,
            AimConnectionProperties props) {
        this.aimConnection = aimConnection;
        this.loginConn = new LoginConnection(props.getLoginHost(),
                props.getLoginPort());
        password = props.getPassword();

        loginConn.addOscarListener(new LoginConnListener());
        loginConn.setServiceFactory(new LoginServiceFactory());
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

        StateEvent event = new StateEvent(aimConnection, oldState,
                oldStateInfo, state, info);

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
    }

    private synchronized void closeConnections() {
        loginConn.disconnect();
        BasicConnection mainConn = this.mainConn;
        if (mainConn != null) mainConn.disconnect();
    }


    private class LoginServiceFactory implements ServiceFactory {
        public Service getService(OscarConnection conn, int family) {
            if (family == AuthCommand.FAMILY_AUTH) {
                return new LoginService(aimConnection, loginConn,
                        aimConnection.getScreenname(), password);

            } else {
                return null;
            }
        }
    }

    private class BasicServiceFactory implements ServiceFactory {
        public Service getService(OscarConnection conn, int family) {
            if (family == ConnCommand.FAMILY_CONN) {
                return new MainBosService(aimConnection, conn);
            } else if (family == IcbmCommand.FAMILY_ICBM) {
                return new IcbmService(aimConnection, conn);
            } else if (family == BuddyCommand.FAMILY_BUDDY) {
                return new BuddyService(aimConnection, conn);
            } else if (family == LocCommand.FAMILY_LOC) {
                return new InfoService(aimConnection, conn);
            } else if (family == SsiCommand.FAMILY_SSI) {
                return new SsiService(aimConnection, conn);
            } else {
                LOGGER.warning("No service for family 0x"
                        + Integer.toHexString(family));
                return null;
            }
        }
    }

    private class DefaultConnListener implements OscarConnListener {
        public void registeredSnacFamilies(OscarConnection conn) {
            aimConnection.recordSnacFamilies(conn);
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
}
