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
import net.kano.joscar.rvcmd.sendfile.FileSendAcceptRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendBlock;
import static net.kano.joscar.rvcmd.sendfile.FileSendBlock.SENDTYPE_DIR;
import static net.kano.joscar.rvcmd.sendfile.FileSendBlock.SENDTYPE_SINGLEFILE;
import net.kano.joscar.rvcmd.sendfile.FileSendRejectRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ChecksumController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectToProxyForOutgoingController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ManualTimeoutController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.OutgoingConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.RedirectToProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendFileController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendOverProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendPassivelyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.BuddyCancelledEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ChecksummingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.TransferCompleteEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.ComputedChecksumsInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailedStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailureEventInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.SuccessfulStateInfo;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class OutgoingFileTransferImpl extends FileTransferImpl
        implements OutgoingFileTransfer {
    private static final Logger LOGGER = Logger
            .getLogger(OutgoingFileTransferImpl.class.getName());

    private final ChecksumController checksumController
            = new ChecksumController();
    private final SendOverProxyController sendOverProxyController
            = new SendOverProxyController();
    private final SendPassivelyController sendPassivelyController
            = new SendPassivelyController();
    private final OutgoingConnectionController outgoingInternalController
            = new OutgoingConnectionController(ConnectionType.LAN);
    private final OutgoingConnectionController outgoingExternalController
            = new OutgoingConnectionController(ConnectionType.INTERNET);
    private final RedirectToProxyController redirectToProxyController
            = new RedirectToProxyController();
    private final ConnectToProxyForOutgoingController connectToProxyController
            = new ConnectToProxyForOutgoingController();
    private SendFileController sendController = new SendFileController();

    private List<File> files = new ArrayList<File>();
    private String folderName;
    private Map<File,Long> checksums = new HashMap<File, Long>();
    private ChecksumManager checksumManager = new ChecksumManager() {
        public long getChecksum(File file) throws IOException {
            Long sum = checksums.get(file);
            if (sum == null) {
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                Checksummer summer = new Checksummer(raf.getChannel(), raf.length());
                fireEvent(new ChecksummingEvent(file, summer));
                sum = summer.compute();
            }
            return sum;
        }
    };

    public OutgoingFileTransferImpl(RvConnectionManager rvConnectionManager,
            RvSession session) {
        super(rvConnectionManager, session);
    }

    public void makeRequest(InvitationMessage msg) {
        setInvitationMessage(msg);
        startStateController(checksumController);
    }

    private Map<File,String> nameMappings = new HashMap<File, String>();

    public synchronized Map<File, String> getNameMappings() {
        return Collections.unmodifiableMap(new HashMap<File, String>(nameMappings));
    }

    public synchronized void mapName(File file, String name) {
        DefensiveTools.checkNull(file, "file");

        nameMappings.put(file, name);
    }

    public synchronized String getFolderName() { return folderName; }

    public synchronized List<File> getFiles() {
        return DefensiveTools.getUnmodifiableCopy(files);
    }

    public synchronized void setFile(File file) {
        DefensiveTools.checkNull(file, "file");

        this.folderName = null;
        this.files = Collections.singletonList(file);
    }

    public synchronized void setFiles(String folderName, List<File> files) {
        DefensiveTools.checkNull(folderName, "folderName");
        DefensiveTools.checkNullElements(files, "files");

        this.folderName = folderName;
        this.files = DefensiveTools.getUnmodifiableCopy(files);
    }

    public synchronized String getMappedName(File file) {
        String name = nameMappings.get(file);
        if (name == null) return file.getName();
        else return name;
    }

    public ChecksumManager getChecksumManager() {
        return checksumManager;
    }

    public FileSendBlock getFileInfo() {
        long totalSize = 0;
        List<File> files = getFiles();
        for (File file : files) totalSize += file.length();
        int numFiles = files.size();
        boolean folderMode = numFiles > 1;
        int sendType = folderMode ? SENDTYPE_DIR : SENDTYPE_SINGLEFILE;
        String filename = folderMode ? getFolderName() : getMappedName(files.get(0));
        return new FileSendBlock(sendType, filename, numFiles, totalSize);
    }

    public synchronized StateController getNextStateController() {
        StateController oldController = getStateController();
        StateInfo endState = oldController.getEndStateInfo();
        if (endState instanceof SuccessfulStateInfo) {
            if (isConnectionController(oldController)) {
                return sendController;

            } else if (oldController == sendController) {
                queueStateChange(FileTransferState.FINISHED,
                        new TransferCompleteEvent());
                return null;

            } else if (oldController == checksumController) {
                if (endState instanceof ComputedChecksumsInfo) {
                    ComputedChecksumsInfo info = (ComputedChecksumsInfo) endState;
                    checksums.putAll(info.getChecksums());
                }
                if (isOnlyUsingProxy()) return sendOverProxyController;
                else return sendPassivelyController;

            } else {
                throw new IllegalStateException("unknown previous controller "
                        + oldController);
            }

        } else if (endState instanceof FailedStateInfo) {
            RvConnectionEvent event = null;
            if (endState instanceof FailureEventInfo) {
                FailureEventInfo failureEventInfo = (FailureEventInfo) endState;

                event = failureEventInfo.getEvent();
            }
            if (oldController == sendController) {
                //TODO: retry send with other controllers like receiver does
//                if (getState() == FileTransferState.TRANSFERRING) {
                    setState(FileTransferState.FAILED,
                            event == null ? new UnknownErrorEvent() : event);
//                } else {
//
//                }
                return null;

            } else if (oldController == outgoingInternalController) {
                if (event != null) queueEvent(event);
                return outgoingExternalController;

            } else if (oldController == sendPassivelyController
                    || oldController == outgoingExternalController
                    || oldController == connectToProxyController) {
                if (event != null) queueEvent(event);
                return redirectToProxyController;

            } else if (oldController == redirectToProxyController) {
                setState(FileTransferState.FAILED,
                        event == null ? new UnknownErrorEvent() : event);
                return null;

            } else {
                throw new IllegalStateException("unknown previous controller "
                        + oldController);
            }
        } else {
            throw new IllegalStateException("unknown previous state " + endState);
        }
    }


    private boolean isConnectionController(StateController oldController) {
        return oldController == sendPassivelyController
                || oldController == outgoingInternalController
                || oldController == outgoingExternalController
                || oldController == redirectToProxyController
                || oldController == connectToProxyController
                || oldController == sendOverProxyController;
    }

    protected FtRvSessionHandler createSessionHandler() {
        return new FtRvSessionHandler() {
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
                    if (controller == sendPassivelyController
                            || controller == redirectToProxyController
                            || controller == sendOverProxyController
                            || controller == connectToProxyController) {
                        if (controller instanceof ManualTimeoutController) {
                            mtc = (ManualTimeoutController) controller;
                        }
                    }
                }
                if (mtc != null) mtc.startTimeoutTimer();
            }

            protected void handleIncomingRequest(RecvRvEvent event,
                    FileSendReqRvCmd reqCmd) {
                int reqType = reqCmd.getRequestIndex();
                if (reqType > FileSendReqRvCmd.REQINDEX_FIRST) {
                    HowToConnect how = processRedirect(reqCmd);
                    if (how == HowToConnect.PROXY) {
                        changeStateController(outgoingInternalController);
                    } else if (how == HowToConnect.NORMAL) {
                        changeStateController(connectToProxyController);
                    }
                } else {
                    LOGGER.warning("got unknown file transfer request type in "
                            + "outgoing transfer: " + reqType);
                }
            }
        };
    }

}