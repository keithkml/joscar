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
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectToProxyForIncomingController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ManualTimeoutController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.OutgoingConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ReceiveFileController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.RedirectConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.RedirectToProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.BuddyCancelledEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectionFailedEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.FileTransferEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.TransferCompleteEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailedStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailureEventInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.SuccessfulStateInfo;

import java.util.logging.Logger;

public class IncomingFileTransferImpl extends FileTransferImpl
        implements IncomingFileTransfer {
    //TODO: send rejection RV when we fail, if we haven't gotten one
    //TODO: don't re-use connection controllers
    private static final Logger LOGGER = Logger
            .getLogger(IncomingFileTransferImpl.class.getName());

    private RvConnectionInfo originalRemoteHostInfo;

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
        StateController controller;
        synchronized (this) {
            if (declined) {
                throw new IllegalStateException(
                        "Transfer was already declined");
            }
            if (accepted) return;
            accepted = true;

            boolean onlyUsingProxy = isOnlyUsingProxy();
            if (!isAlwaysRedirect() && !onlyUsingProxy) {
                controller = new OutgoingConnectionController(ConnectionType.LAN);
            } else if (isProxyRequestTrusted()) {
                controller = new ConnectToProxyForIncomingController();
            } else if (!onlyUsingProxy) {
                controller = new RedirectConnectionController();
            } else {
                controller = new RedirectToProxyController();
            }
        }

        startStateController(controller);
        LOGGER.fine("Sending file transfer accept command to "
                + getBuddyScreenname());
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
        StateInfo oldStateInfo = oldController.getEndStateInfo();
        if (oldStateInfo instanceof SuccessfulStateInfo) {
            if (oldController instanceof ReceiveFileController) {
                LOGGER.fine("Changing from success of receive controller to "
                        + "completed");
                queueStateChange(FileTransferState.FINISHED,
                        new TransferCompleteEvent());
                return null;
            } else {
                if (oldStateInfo instanceof StreamInfo) {
                    LOGGER.fine("Changing from success of conn controller "
                            + oldController + " to " + "receive");
                    return new ReceiveFileController();
                } else {
                    throw new IllegalStateException("Unknown last controller "
                            + oldController);
                }
            }
        } else if (oldStateInfo instanceof FailedStateInfo) {
            LOGGER.fine("Changing from failure of last controller");
            return getNextStateFromError(oldController, oldStateInfo);

        } else {
            throw new IllegalStateException("Unknown last state " + oldStateInfo);
        }
    }

    private synchronized StateController getNextStateFromError(
            StateController oldController, StateInfo oldState) {
        FileTransferEvent event;
        if (oldState instanceof FailureEventInfo) {
            FailureEventInfo failureEventInfo = (FailureEventInfo) oldState;
            event = failureEventInfo.getEvent();
        } else {
            event = new UnknownErrorEvent();
        }
        if (oldController instanceof OutgoingConnectionController) {
            OutgoingConnectionController outController
                    = (OutgoingConnectionController) oldController;
            if (outController.getConnectionType() == ConnectionType.LAN) {
                return new OutgoingConnectionController(ConnectionType.INTERNET);

            } else {
                return new ConnectToProxyForIncomingController();
            }

        } else if (oldController instanceof ConnectToProxyForIncomingController) {
            return new RedirectConnectionController();

        } else if (oldController instanceof RedirectConnectionController) {
            return new RedirectToProxyController();

        } else if (oldController instanceof RedirectToProxyController) {
            queueStateChange(FileTransferState.FAILED,
                    new ConnectionFailedEvent(event));
            return null;

        } else if (oldController instanceof ReceiveFileController) {
            queueStateChange(FileTransferState.FAILED, event);
            return null;
        } else {
            throw new IllegalStateException("Unknown controller "
                    + oldController);
        }
    }

    private class IncomingFtpRvSessionHandler extends FtRvSessionHandler {
        protected void handleIncomingReject(RecvRvEvent event,
                FileSendRejectRvCmd rejectCmd) {
            BuddyCancelledEvent evt = new BuddyCancelledEvent(
                    rejectCmd.getRejectCode());
            setState(FileTransferState.FAILED, evt);
        }

        protected void handleIncomingAccept(RecvRvEvent event,
                FileSendAcceptRvCmd acceptCmd) {
            ManualTimeoutController mtc = null;
            synchronized (this) {
                StateController controller = getStateController();
                if (controller instanceof ManualTimeoutController) {
                    mtc = (ManualTimeoutController) controller;
                }
            }
            if (mtc != null) mtc.startTimeoutTimer();
        }

        protected void handleIncomingRequest(RecvRvEvent event,
                FileSendReqRvCmd reqCmd) {
            int index = reqCmd.getRequestIndex();
            if (index == FileSendReqRvCmd.REQINDEX_FIRST) {
                setFileInfo(reqCmd.getFileSendBlock());
                setInvitationMessage(reqCmd.getMessage());
                RvConnectionInfo connInfo = reqCmd.getConnInfo();
                setOriginalRemoteHostInfo(connInfo);
                putTransferProperty(KEY_CONN_INFO, connInfo);
                putTransferProperty(KEY_REDIRECTED, false);

                FileTransferManager ftMgr = getFileTransferManager();
                ftMgr.fireNewIncomingTransfer(IncomingFileTransferImpl.this);

            } else if (index > FileSendReqRvCmd.REQINDEX_FIRST) {
                HowToConnect how = processRedirect(reqCmd);
                if (how == HowToConnect.PROXY) {
                    changeStateController(
                            new ConnectToProxyForIncomingController());
                } else if (how == HowToConnect.NORMAL) {
                    changeStateController(
                            new OutgoingConnectionController(ConnectionType.LAN));
                }
            } else {
                LOGGER.warning("Got unknown request index " + index + " for "
                        + reqCmd);
            }
        }
    }

}
