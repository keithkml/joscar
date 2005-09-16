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

package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.ConnectionType;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FailureEventException;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransferImpl;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectionTimedOutEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.LocallyCancelledInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;

import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public abstract class AbstractConnectionController extends StateController {
    private static final Logger LOGGER = Logger
            .getLogger(AbstractConnectionController.class.getName());

    private RvConnectionInfo connectionInfo;
    private StreamInfo stream;
    private FileTransferImpl fileTransfer;
    private Socket socket;
    private Thread thread;
    private boolean timerStarted = false;
    private boolean connected = false;

    protected abstract ConnectionType getConnectionType();

    private long getConnectionTimeoutMillis() {
        return fileTransfer.getPerConnectionTimeout(getConnectionType());
    }

    public RvConnectionInfo getConnectionInfo() { return connectionInfo; }

    protected StreamInfo getStream() { return stream; }

    public FileTransferImpl getFileTransfer() {
        return fileTransfer;
    }

    protected synchronized void stopConnectionTimer() {
        connected = true;
    }

    public void start(FileTransfer transfer, StateController last) {
        DefensiveTools.checkNull(transfer, "transfer");
        StateInfo endState = getEndStateInfo();
        if (endState != null) {
            throw new IllegalStateException("state is alreaday " + endState);
        }

        this.fileTransfer = (FileTransferImpl) transfer;
        connectionInfo = fileTransfer.getTransferProperty(
                FileTransferImpl.KEY_CONN_INFO);

        try {
            checkConnectionInfo();
            initializeBeforeStarting();
        } catch (Exception e) {
            fireFailed(e);
            return;
        }

        thread = new Thread(new Runnable() {
            public void run() {
                try {
                    openConnectionInThread();
                } catch (Exception e) {
                    fireFailed(e);
                }
            }
        });

        if (shouldStartTimerAutomatically()) startTimer();
        thread.start();
    }

    protected void checkConnectionInfo() throws IllegalStateException {

    }

    protected boolean shouldStartTimerAutomatically() {
        return true;
    }

    protected void startTimer() {
        synchronized(this) {
            if (timerStarted) return;
            timerStarted = true;
        }
        Timer timer = fileTransfer.getTimer();
        final long timeout = getConnectionTimeoutMillis();
        TimerTask task = new TimerTask() {

            public void run() {
                boolean connected = isConnected();
                if (!connected) {
                    thread.interrupt();
                    fireFailed(new ConnectionTimedOutEvent(timeout));
                }
            }

        };
        timer.schedule(task, timeout);
    }

    public synchronized boolean isConnected() {
        return connected;
    }

    public void stop() {
        fireFailed(new LocallyCancelledInfo());
        if (thread != null) thread.interrupt();
    }

    public Socket getSocket() { return socket; }

    protected void initializeBeforeStarting() throws IOException {

    }

    protected void openConnectionInThread() {
        try {
            LOGGER.fine(this + " opening socket");
            socket = createSocket();
            stream = new StreamInfo(socket.getChannel());
            LOGGER.fine(this + " initializing connection in thread");
            initializeConnectionInThread();
        } catch (Exception e) {
            fireFailed(e);
        }
    }

    protected abstract void setConnectingState();
    protected abstract void setResolvingState();

    protected abstract Socket createSocket() throws IOException;

    protected void fireConnected() {
        fireSucceeded(stream);
    }

    protected void initializeConnectionInThread()
            throws IOException, FailureEventException {
        fireConnected();
    }

}
