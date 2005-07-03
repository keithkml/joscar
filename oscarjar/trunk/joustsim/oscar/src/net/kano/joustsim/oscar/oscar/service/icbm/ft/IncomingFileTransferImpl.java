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

package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvSession;
import static net.kano.joscar.rvcmd.AbstractRejectRvCmd.REJECTCODE_CANCELLED;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.rvcmd.sendfile.FileSendAcceptRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendRejectRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectToProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.OutgoingConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ReceiveController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.RedirectConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.RedirectToProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailedStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.SuccessfulStateInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class IncomingFileTransferImpl extends FileTransferImpl
        implements IncomingFileTransfer {
    private static final Logger LOGGER = Logger
            .getLogger(IncomingFileTransferImpl.class.getName());

    private OutgoingConnectionController externalController
            = new OutgoingConnectionController(ConnectionType.INTERNET);
    private OutgoingConnectionController internalController
            = new OutgoingConnectionController(ConnectionType.LAN);
    private RedirectConnectionController redirectConnectionController
            = new RedirectConnectionController();
    private ConnectToProxyController proxyController
            = new ConnectToProxyController(ConnectToProxyController.ConnectionType.ACK);
    private StateController proxyReverseController = new RedirectToProxyController();
    private ReceiveController transferController = new ReceiveController();

    private RvConnectionInfo originalRemoteHostInfo;

    private List<StateController> connControllers = null;
    private StateController lastConnController = null;
    private boolean accepted = false;
    private boolean declined = false;

    IncomingFileTransferImpl(FileTransferManager fileTransferManager,
            RvSession session) {
        super(fileTransferManager, session);
    }

    public synchronized RvConnectionInfo getOriginalRemoteHostInfo() {
        return originalRemoteHostInfo;
    }

    synchronized void setOriginalRemoteHostInfo(RvConnectionInfo info) {
        this.originalRemoteHostInfo = info;
    }

    protected FtRvSessionHandler createSessionHandler() {
        return new IncomingFtpRvSessionHandler();
    }

    public synchronized boolean isAccepted() {
        return accepted;
    }

    public synchronized boolean isDeclined() {
        return declined;
    }

    private boolean alwaysRedirect = false;

    public synchronized boolean isAlwaysRedirect() {
        return alwaysRedirect;
    }

    public synchronized void setAlwaysRedirect(boolean alwaysRedirect) {
        this.alwaysRedirect = alwaysRedirect;
    }

    public void accept() throws IllegalStateException {
        synchronized(this) {
            if (declined) {
                throw new IllegalStateException("transfer was already declined");
            }
            if (accepted) return;
            accepted = true;

            connControllers = new ArrayList<StateController>();
            boolean onlyUsingProxy = isOnlyUsingProxy();
            if (!isAlwaysRedirect() && !onlyUsingProxy) {
                connControllers.add(internalController);
                connControllers.add(externalController);
            }
            if (isProxyRequestTrusted()) {
                connControllers.add(proxyController);
            }
            if (!onlyUsingProxy) {
                connControllers.add(redirectConnectionController);
            }
            connControllers.add(proxyReverseController);
        }

        startStateController(connControllers.get(0));
        getRvSession().sendRv(new FileSendAcceptRvCmd());
    }

    public void decline() throws IllegalStateException {
        synchronized(this) {
            if (accepted) {
                throw new IllegalStateException("transfer was already accepted");
            }
            if (declined) return;
            declined = true;
        }
        getRvSession().sendRv(new FileSendRejectRvCmd(REJECTCODE_CANCELLED));
    }

    public StateController getNextStateController() {
        StateController oldController = getStateController();
        StateInfo oldState = oldController.getEndState();
        if (oldState instanceof SuccessfulStateInfo) {
            return getReceiverState(oldController);

        } else if (oldState instanceof FailedStateInfo) {
            return getNextStateFromError(oldController, oldState);

        } else {
            throw new IllegalStateException("unknown state " + oldState);
        }
    }

    private StateController getReceiverState(StateController oldController) {
        if (connControllers.contains(oldController)) {
            return new ReceiveController();
        } else {
            throw new IllegalStateException("what state? " + oldController);
        }
    }

    private StateController getNextStateFromError(StateController oldController,
            StateInfo oldState) {
        int oldIndex = connControllers.indexOf(oldController);
        if (oldIndex == -1) {
            if (oldController == transferController) {
                if (lastConnController == null) {
                    throw new IllegalArgumentException("receiver must "
                            + "have been called before connection was "
                            + "attempted");
                }
                int oldConnIndex = connControllers.indexOf(lastConnController);
                if (oldConnIndex == -1) {
                    throw new IllegalStateException("last connection "
                            + "controller is not in connControllers: "
                            + lastConnController);
                } else {
                    if (oldConnIndex == connControllers.size()-1) {
                        System.out.println("FAILED 2");
                        return null;
                    } else {
                        StateController nextController
                                = connControllers.get(oldConnIndex + 1);
                        lastConnController = nextController;
                        return nextController;
                    }
                }
            } else {
                throw new IllegalStateException("unknown old controller "
                        + oldController);
            }

        } else {
            if (oldIndex == connControllers.size()-1) {
                // it's the last one
                return null;
            } else {
                StateController nextController = connControllers.get(oldIndex
                        + 1);
                lastConnController = nextController;
                return nextController;
            }
        }
    }

    private class IncomingFtpRvSessionHandler extends FtRvSessionHandler {
        protected void handleIncomingReject(RecvRvEvent event,
                FileSendRejectRvCmd rejectCmd) {
            System.out.println("incoming rejected");
        }

        protected void handleIncomingAccept(RecvRvEvent event,
                FileSendAcceptRvCmd acceptCmd) {
            System.out.println("incoming accepted??");
        }

        protected void handleIncomingRequest(RecvRvEvent event,
                FileSendReqRvCmd reqCmd) {
            int type = reqCmd.getRequestType();
            if (type == FileSendReqRvCmd.REQTYPE_INITIALREQUEST) {
                setFileInfo(reqCmd.getFileSendBlock());
                setInvitationMessage(reqCmd.getMessage());
                RvConnectionInfo connInfo = reqCmd.getConnInfo();
                setOriginalRemoteHostInfo(connInfo);
                putTransferProperty(KEY_CONN_INFO, connInfo);

            } else {
                LOGGER.info("Got rendezvous of unknown type in incoming "
                        + "request: " + type);
            }
        }
    }
}
