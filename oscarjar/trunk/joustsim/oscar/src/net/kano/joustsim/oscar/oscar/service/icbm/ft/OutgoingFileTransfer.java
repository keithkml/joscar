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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class OutgoingFileTransfer extends FileTransferImpl {
    private static final Logger LOGGER = Logger
            .getLogger(OutgoingFileTransfer.class.getName());

    private SendOverProxyController sendOverProxyController
            = new SendOverProxyController();
    private SendPassivelyController sendPassivelyController
            = new SendPassivelyController();
    private OutgoingConnectionController outgoingInternalController
            = new OutgoingConnectionController(OutgoingConnectionType.INTERNAL);
    private OutgoingConnectionController outgoingExternalController
            = new OutgoingConnectionController(OutgoingConnectionType.EXTERNAL);
    private RedirectToProxyController redirectToProxyController
            = new RedirectToProxyController();
    private ConnectToProxyController connectToProxyController
            = new ConnectToProxyController(ConnectToProxyController.ConnectionType.ACK);

    private List<File> files = new ArrayList<File>();

    OutgoingFileTransfer(FileTransferManager fileTransferManager,
            RvSession session) {
        super(fileTransferManager, session);
    }

    private boolean onlyUsingProxy = false;

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

    protected StateController getNextStateController() {
        //TODO: don't allow a state twice
        StateController oldController = getStateController();
        StateInfo endState = oldController.getEndState();
        if (endState instanceof Stream) {
            return new SendController();

        } else if (endState instanceof ExceptionStateInfo) {
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
                //TODO: set state to failed
                System.out.println("outgoing rejected");
            }

            protected void handleIncomingAccept(RecvRvEvent event,
                    FileSendAcceptRvCmd acceptCmd) {
                //TODO: fire accepted event
                System.out.println("outgoing accepted");
            }

            protected void handleIncomingRequest(RecvRvEvent event,
                    FileSendReqRvCmd reqCmd) {
                int reqType = reqCmd.getRequestType();
                RvConnectionInfo connInfo = reqCmd.getConnInfo();
                if (reqType == FileSendReqRvCmd.REQTYPE_REDIRECT) {
                    if (isOnlyUsingProxy()) {
                        //TODO: post event about attempted sabotage

                    } else {
                        putTransferProperty(KEY_CONN_INFO, connInfo);
                        if (connInfo.isProxied()) {
                            putTransferProperty(KEY_PROXY_REDIRECTED, true);
                            changeStateController(connectToProxyController);
                        } else {
                            putTransferProperty(FileTransferImpl.KEY_NORMAL_REDIRECTED, true);
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
