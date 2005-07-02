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

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.InvitationMessage;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.rvcmd.sendfile.FileSendAcceptRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendBlock;
import net.kano.joscar.rvcmd.sendfile.FileSendRejectRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectToProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.OutgoingConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.RedirectToProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendOverProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendPassivelyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailedStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.SuccessfulStateInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class OutgoingFileTransferImpl extends FileTransferImpl
        implements OutgoingFileTransfer {
    private static final Logger LOGGER = Logger
            .getLogger(OutgoingFileTransferImpl.class.getName());

    private SendOverProxyController sendOverProxyController
            = new SendOverProxyController();
    private SendPassivelyController sendPassivelyController
            = new SendPassivelyController();
    private OutgoingConnectionController outgoingInternalController
            = new OutgoingConnectionController(ConnectionType.LAN);
    private OutgoingConnectionController outgoingExternalController
            = new OutgoingConnectionController(ConnectionType.INTERNET);
    private RedirectToProxyController redirectToProxyController
            = new RedirectToProxyController();
    private ConnectToProxyController connectToProxyController
            = new ConnectToProxyController(ConnectToProxyController.ConnectionType.ACK);

    private List<File> files = new ArrayList<File>();

    OutgoingFileTransferImpl(FileTransferManager fileTransferManager,
            RvSession session) {
        super(fileTransferManager, session);
    }

    private boolean onlyUsingProxy = false;
    private boolean trustingProxyRedirects = false;

    public boolean isTrustingProxyRedirects() {
        return trustingProxyRedirects;
    }

    public void setTrustProxyRedirects(boolean trustingProxyRedirects) {
        this.trustingProxyRedirects = trustingProxyRedirects;
    }

    public synchronized boolean isOnlyUsingProxy() {
        return onlyUsingProxy;
    }

    public synchronized void setOnlyUsingProxy(boolean onlyUsingProxy) {
        this.onlyUsingProxy = onlyUsingProxy;
    }

    public void makeRequest(InvitationMessage msg) {
        setInvitationMessage(msg);
        StateController controller;
        if (isOnlyUsingProxy()) {
            controller = sendOverProxyController;
        } else {
            controller = sendPassivelyController;
        }
        startStateController(controller);
    }

    public synchronized List<File> getFiles() {
        return DefensiveTools.getUnmodifiableCopy(files);
    }

    public synchronized void setFiles(List<File> files) {
        this.files = DefensiveTools.getUnmodifiableCopy(files);
    }

    public FileSendBlock getFileInfo() {
        long totalSize = 0;
        List<File> files = getFiles();
        for (File file : files) totalSize += file.length();
        int numFiles = files.size();
        boolean folderMode = numFiles > 1;
        int sendType = folderMode ? FileSendBlock.SENDTYPE_DIR
                : FileSendBlock.SENDTYPE_SINGLEFILE;
        String filename = folderMode ? "Folder" : files.get(0).getName();
        return new FileSendBlock(sendType, filename, numFiles, totalSize);
    }

    public StateController getNextStateController() {
        StateController oldController = getStateController();
        StateInfo endState = oldController.getEndState();
        if (endState instanceof SuccessfulStateInfo) {
            return new SendController();

        } else if (endState instanceof FailedStateInfo) {
            if (oldController == outgoingInternalController) {
                return outgoingExternalController;
            } else if (oldController == sendPassivelyController
                    || oldController == outgoingExternalController
                    || oldController == connectToProxyController) {
                return redirectToProxyController;
            } else {
                throw new IllegalStateException("unknown previous controller "
                        + oldController);
            }
        } else {
            throw new IllegalStateException("unknown state " + endState);
        }
    }

    protected FtRvSessionHandler createSessionHandler() {
        return new FtRvSessionHandler() {
            protected void handleIncomingReject(RecvRvEvent event,
                    FileSendRejectRvCmd rejectCmd) {
                //TODO: fail
//                getEventPost().fireStateChange(FileTransferState.FAILED,
//                        new BuddyCancelledEvent(rejectCmd.getRejectCode()));
            }

            protected void handleIncomingAccept(RecvRvEvent event,
                    FileSendAcceptRvCmd acceptCmd) {
            }

            protected void handleIncomingRequest(RecvRvEvent event,
                    FileSendReqRvCmd reqCmd) {
                int reqType = reqCmd.getRequestType();
                RvConnectionInfo connInfo = reqCmd.getConnInfo();
                if (reqType == FileSendReqRvCmd.REQTYPE_REDIRECT) {
                    boolean good;
                    boolean proxied = connInfo.isProxied();
                    if (isOnlyUsingProxy()) {
                        if (proxied && !isTrustingProxyRedirects()) {
                            //TODO: fail
//                            getEventPost().fireStateChange(FileTransferState.FAILED,
//                                    new ProxyRedirectDisallowedEvent(connInfo.getProxyIP()));
                            good = false;

                        } else {
                            good = true;
                        }

                    } else {
                        good = true;
                    }
                    if (good) {
                        putTransferProperty(KEY_CONN_INFO, connInfo);
                        putTransferProperty(KEY_REDIRECTED, true);
                        if (proxied) {
                            changeStateController(connectToProxyController);
                        } else {
                            changeStateController(outgoingInternalController);
                        }
                    }
                } else {
                    LOGGER.info("got unknown file transfer request type in "
                            + "outgoing transfer: " + reqType);
                }
            }
        };
    }

}
