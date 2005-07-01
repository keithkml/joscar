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

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rv.RvSnacResponseEvent;
import net.kano.joscar.rvcmd.InvitationMessage;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.rvcmd.sendfile.FileSendAcceptRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendBlock;
import net.kano.joscar.rvcmd.sendfile.FileSendRejectRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousSessionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

abstract class FileTransferImpl implements FileTransfer {
    static final Key<Boolean> KEY_PROXY_REDIRECTED = new Key<Boolean>("PROXY_REDIRECTED");
    static final Key<Boolean> KEY_NORMAL_REDIRECTED = new Key<Boolean>("NORMAL_REDIRECTED");
    static final Key<RvConnectionInfo> KEY_CONN_INFO = new Key<RvConnectionInfo>("CONN_INFO");

    private FileSendBlock fileInfo;
    private InvitationMessage message;
    private Timer timer = new Timer("File transfer timer");
    private RendezvousSessionHandler rvSessionHandler;
    private RvSession session;
    private StateController controller = null;
    private Map<Key<?>,Object> transferProperties
            = new HashMap<Key<?>, Object>();
    private FileTransferManager fileTransferManager;

    private CopyOnWriteArrayList<FileTransferListener> listeners
            = new CopyOnWriteArrayList<FileTransferListener>();

    protected FileTransferImpl(FileTransferManager fileTransferManager,
            RvSession session) {
        this.fileTransferManager = fileTransferManager;
        this.session = session;
        rvSessionHandler = createSessionHandler();
    }

    protected abstract FtRvSessionHandler createSessionHandler();

    protected synchronized void setInvitationMessage(InvitationMessage message) {
        this.message = message;
    }

    protected synchronized void setFileInfo(FileSendBlock fileInfo) {
        this.fileInfo = fileInfo;
    }

    protected Timer getTimer() { return timer; }

    public synchronized FileSendBlock getFileInfo() { return fileInfo; }

    public synchronized InvitationMessage getInvitationMessage() { return message; }

    protected void startStateController(final StateController controller) {
        StateController oldController = this.controller;
        if (oldController != null) {
            throw new IllegalStateException("Cannot start state controller: "
                    + "controller is already set to " + oldController);
        }
        StateController last = null;
        changeStateController(controller, last);
    }

    protected void changeStateController(final StateController controller) {
        changeStateController(controller, this.controller);
    }

    private void changeStateController(final StateController controller,
            StateController last) {
        if (last != null) last.stop();
        this.controller = controller;
        controller.addControllerListener(new ControllerListener() {
            public void handleControllerSucceeded(StateController c,
                    StateInfo info) {
                goNext();
            }

            public void handleControllerFailed(StateController c,
                    StateInfo info) {
                goNext();
            }

            private void goNext() {
                controller.removeControllerListener(this);
                changeStateController(getNextStateController(), controller);
            }
        });
        controller.start(this, last);
    }

    protected StateController getStateController() { return controller; }

    protected abstract StateController getNextStateController();

    protected RendezvousSessionHandler getRvSessionHandler() {
        return rvSessionHandler;
    }

    protected RvSession getRvSession() {
        return session;
    }

    synchronized <V> void putTransferProperty(Key<V> key, V value) {
        transferProperties.put(key, value);
    }

    synchronized <V> V getTransferProperty(Key<V> key) {
        return (V) transferProperties.get(key);
    }

    public FileTransferManager getFileTransferManager() {
        return fileTransferManager;
    }

    public void addTransferListener(FileTransferListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeTransferListener(FileTransferListener listener) {
        listeners.remove(listener);
    }

    protected void fireEvent(FileTransferEvent event) {
        for (FileTransferListener listener : listeners) {
            listener.handleEvent(this, event);
        }
    }

    protected abstract class FtRvSessionHandler implements RendezvousSessionHandler {
        public final void handleRv(RecvRvEvent event) {
            RvCommand cmd = event.getRvCommand();
            if (cmd instanceof FileSendReqRvCmd) {
                FileSendReqRvCmd reqCmd = (FileSendReqRvCmd) cmd;
                handleIncomingRequest(event, reqCmd);

            } else if (cmd instanceof FileSendAcceptRvCmd) {
                FileSendAcceptRvCmd acceptCmd = (FileSendAcceptRvCmd) cmd;
                handleIncomingAccept(event, acceptCmd);

            } else if (cmd instanceof FileSendRejectRvCmd) {
                FileSendRejectRvCmd rejectCmd = (FileSendRejectRvCmd) cmd;
                handleIncomingReject(event, rejectCmd);
            }
        }

        protected abstract void handleIncomingReject(RecvRvEvent event,
                FileSendRejectRvCmd rejectCmd);

        protected abstract void handleIncomingAccept(RecvRvEvent event,
                FileSendAcceptRvCmd acceptCmd);

        protected abstract void handleIncomingRequest(RecvRvEvent event,
                FileSendReqRvCmd reqCmd);

        public void handleSnacResponse(RvSnacResponseEvent event) {
        }

        protected FileTransfer getFileTransfer() {
            return FileTransferImpl.this;
        }
    }

    static class Key<V> {
        private final String name;

        public Key(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
