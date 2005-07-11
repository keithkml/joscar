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
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ManualTimeoutController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.FileTransferEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.TransferCompleteEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.BuddyCancelledEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailedStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailureEventInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.SuccessfulStateInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

//TODO: send rejection RV when we fail, if we haven't gotten one
public class IncomingFileTransferImpl extends FileTransferImpl
        implements IncomingFileTransfer {
    private static final Logger LOGGER = Logger
            .getLogger(IncomingFileTransferImpl.class.getName());

    private final OutgoingConnectionController externalController
            = new OutgoingConnectionController(ConnectionType.INTERNET);
    private final OutgoingConnectionController internalController
            = new OutgoingConnectionController(ConnectionType.LAN);
    private final RedirectConnectionController redirectConnectionController
            = new RedirectConnectionController();
    private final ConnectToProxyController proxyController
            = new ConnectToProxyController();
    private final RedirectToProxyController proxyReverseController = new RedirectToProxyController();
    private final ReceiveController receiveController = new ReceiveController();

    private RvConnectionInfo originalRemoteHostInfo;

    private List<StateController> connControllers = null;
    private StateController lastConnController = null;
    private boolean accepted = false;
    private boolean declined = false;
    private FileMapper fileMapper;

    IncomingFileTransferImpl(FileTransferManager fileTransferManager,
            RvSession session) {
        super(fileTransferManager, session);

        fileMapper = new DefaultFileMapper(getBuddyScreenname());
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

    public synchronized void setFileMapper(FileMapper mapper) {
        fileMapper = mapper;
    }

    public synchronized FileMapper getFileMapper() {
        return fileMapper;
    }

    private boolean alwaysRedirect = false;

    public synchronized boolean isAlwaysRedirect() {
        return alwaysRedirect;
    }

    public synchronized void setAlwaysRedirect(boolean alwaysRedirect) {
        this.alwaysRedirect = alwaysRedirect;
    }

    public void accept() throws IllegalStateException {
        StateController first;
        synchronized (this) {
            if (declined) {
                throw new IllegalStateException(
                        "transfer was already declined");
            }
            if (accepted) return;
            accepted = true;

            List<StateController> controllers = new ArrayList<StateController>();
            boolean onlyUsingProxy = isOnlyUsingProxy();
            if (!isAlwaysRedirect() && !onlyUsingProxy) {
                controllers.add(internalController);
                controllers.add(externalController);
            }
            if (isProxyRequestTrusted()) {
                controllers.add(proxyController);
            }
            if (!onlyUsingProxy) {
                controllers.add(redirectConnectionController);
            }
            controllers.add(proxyReverseController);
            connControllers = controllers;
            first = controllers.get(0);
            lastConnController = first;
        }

        startStateController(first);
        getRvSession().sendRv(new FileSendAcceptRvCmd());
    }

    public void decline() throws IllegalStateException {
        synchronized(this) {
            if (accepted) {
                throw new IllegalStateException("transfer was already accepted");
            }
            if (declined) return;
            declined = true;
            cancel();
        }
        getRvSession().sendRv(new FileSendRejectRvCmd(REJECTCODE_CANCELLED));
    }

    public synchronized StateController getNextStateController() {
        StateController oldController = getStateController();
        StateInfo oldState = oldController.getEndState();
        if (oldState instanceof SuccessfulStateInfo) {
            if (oldController == receiveController) {
                queueStateChange(FileTransferState.FINISHED, new TransferCompleteEvent());
                return null;
            } else {
                if (connControllers.contains(oldController)) {
                    return receiveController;
                } else {
                    throw new IllegalStateException("what state? " + oldController);
                }
            }
        } else if (oldState instanceof FailedStateInfo) {
            return getNextStateFromError(oldController, oldState);

        } else {
            throw new IllegalStateException("unknown state " + oldState);
        }
    }

    private synchronized StateController getNextStateFromError(
            StateController oldController, StateInfo oldState) {
        int oldIndex = connControllers.indexOf(oldController);
        boolean isFailureEventInfo = oldState instanceof FailureEventInfo;
        if (oldIndex == -1) {
            if (oldController == receiveController) {
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
                    if (getState() == FileTransferState.TRANSFERRING
                            || oldConnIndex == connControllers.size()-1) {
                        FileTransferEvent event;
                        if (isFailureEventInfo) {
                            FailureEventInfo failureEventInfo = (FailureEventInfo) oldState;

                            event = failureEventInfo.getEvent();
                        } else {
                            LOGGER.warning("receiver failed, but its end state "
                                    + "was " + oldState + " which is not a "
                                    + "FailureEventInfo");
                            event = new UnknownErrorEvent();
                        }
                        queueStateChange(FileTransferState.FAILED, event);
                        return null;

                    } else {
                        StateController nextController
                                = connControllers.get(oldConnIndex + 1);
                        lastConnController = nextController;
                        return nextController;
                    }
                }
            } else {
                throw new IllegalStateException("unknown previous controller "
                        + oldController);
            }

        } else {
            FileTransferEvent event;
            if (isFailureEventInfo) {
                FailureEventInfo failureEventInfo = (FailureEventInfo) oldState;

                event = failureEventInfo.getEvent();
            } else {
                event = null;
            }

            // some connection failed
            if (oldIndex == connControllers.size()-1) {
                // it's the last one. all connections failed.
                queueStateChange(FileTransferState.FAILED,
                        isFailureEventInfo ? event : new UnknownErrorEvent());
                return null;
            } else {
                if (isFailureEventInfo) queueEvent(event);
                StateController nextController = connControllers.get(oldIndex + 1);
                lastConnController = nextController;
                return nextController;
            }
        }
    }

    private class IncomingFtpRvSessionHandler extends FtRvSessionHandler {
        protected void handleIncomingReject(RecvRvEvent event,
                FileSendRejectRvCmd rejectCmd) {
            setState(FileTransferState.FAILED,
                    new BuddyCancelledEvent(rejectCmd.getRejectCode()));
        }

        protected void handleIncomingAccept(RecvRvEvent event,
                FileSendAcceptRvCmd acceptCmd) {
            ManualTimeoutController mtc = null;
            synchronized (this) {
                StateController controller = getStateController();
                if (controller == proxyReverseController
                        || controller == redirectConnectionController) {
                    if (controller instanceof ManualTimeoutController) {
                        mtc = (ManualTimeoutController) controller;

                    }
                }
            }
            if (mtc != null) mtc.startTimeoutTimer();
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

                getFileTransferManager().fireNewIncomingTransfer(IncomingFileTransferImpl.this);

            } else {
                LOGGER.info("Got rendezvous of unknown type in incoming "
                        + "request: " + type);
            }
        }
    }

}
