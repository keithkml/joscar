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

import net.kano.joustsim.oscar.oscar.service.icbm.ft.FailureEventException;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.TransferPropertyHolder;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransferTools;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectedEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectionTimedOutEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

//TODO: allow pause, limit bandwidth
//TODO: allow setting whole-transfer pre-connection timeout
public abstract class TransferController extends StateController {
    private volatile boolean stop = false;
    private boolean connected = false;
    private Thread receiveThread;
    private boolean suppressErrors = false;
    private FileTransfer transfer;
    private long timeoutPaused = -1;
    private long threadStarted = -1;
    private long timeIgnored = 0;
    private TimerTask latestTask = null;

    protected synchronized void pauseTimeout() {
        if (timeoutPaused != -1) return;
        timeoutPaused = System.currentTimeMillis();
        latestTask = null;
    }

    protected synchronized void resumeTimeout() {
        long current = System.currentTimeMillis();
        long pausedAt = timeoutPaused;
        if (pausedAt == -1) return;
        timeIgnored += (current - pausedAt);
        timeoutPaused = -1;
        makeTimerTask();
    }

    private synchronized void makeTimerTask() {
        final long timeout = getTransferTimeoutMillis();
        final long timeoutAt = threadStarted + timeout + timeIgnored;
        Timer timer = FileTransferTools.getTimer(transfer);
        TimerTask task = new TimerTask() {
            public void run() {
                boolean timedout = false;
                synchronized (TransferController.this) {
                    if (this != latestTask) return;
                    if (!isConnected()) {
                        stop = true;
                        suppressErrors = true;
                        timedout = true;
                    }
                }
                if (timedout) {
                    interruptThread();
                    fireFailed(new ConnectionTimedOutEvent(timeout));
                }
            }
        };
        latestTask = task;
        long currentTime = System.currentTimeMillis();
        long delay = Math.max(0, timeoutAt - currentTime);
        timer.schedule(task, delay);
    }

    protected synchronized boolean shouldSuppressErrors() {
        return suppressErrors;
    }

    public void start(final FileTransfer transfer, StateController last) {
        this.transfer = transfer;
        StateInfo endState = last.getEndState();
        if (endState instanceof StreamInfo) {
            final StreamInfo stream = (StreamInfo) endState;
            receiveThread = new Thread(new Runnable() {
                public void run() {
                    synchronized(TransferController.this) {
                        threadStarted = System.currentTimeMillis();
                    }
                    try {
                        makeTimerTask();
                        TransferPropertyHolder itransfer = (TransferPropertyHolder) transfer;
                        transferInThread(stream, itransfer);

                    } catch (Exception e) {
                        if (!shouldSuppressErrors()) {
                            fireFailed(e);
                        }
                        return;
                    }
                }

            });

            receiveThread.start();
        } else {
            throw new IllegalArgumentException("I don't know how to deal with "
                    + "previous end state " + endState);
        }
    }
    private void interruptThread() {
        receiveThread.interrupt();
    }

    protected long getTransferTimeoutMillis() {
        return transfer.getDefaultPerConnectionTimeout();
    }

    public void stop() {
        stop = true;
        interruptThread();
    }

    protected boolean shouldStop() {
        return stop;
    }

    protected void setConnected() {
        synchronized (this) {
            if (connected) return;
            connected = true;
        }
        transfer.getEventPost().fireEvent(new ConnectedEvent());
    }

    protected synchronized boolean isConnected() {
        return connected;
    }

    protected abstract void transferInThread(StreamInfo stream,
            TransferPropertyHolder transfer)
            throws IOException, FailureEventException;

}
