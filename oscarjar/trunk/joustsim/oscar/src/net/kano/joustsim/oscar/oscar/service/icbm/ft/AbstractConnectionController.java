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

import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.DefensiveTools;

import java.util.Timer;
import java.util.TimerTask;
import java.net.Socket;
import java.io.IOException;

abstract class AbstractConnectionController extends StateController {
    private RvConnectionInfo connectionInfo;
    private Stream stream;
    private FileTransferImpl fileTransfer;
    private Socket socket;
    private Thread thread;

    private long getConnectionTimeoutMillis() {
        return 10000;
    }

    public RvConnectionInfo getConnectionInfo() { return connectionInfo; }

    protected Stream getStream() { return stream; }

    public FileTransferImpl getFileTransfer() {
        return fileTransfer;
    }

    public void start(final FileTransfer transfer,
            StateController last) {
        DefensiveTools.checkNull(transfer, "transfer");

        this.fileTransfer = (FileTransferImpl) transfer;
        connectionInfo = fileTransfer.getTransferProperty(FileTransferImpl.KEY_CONN_INFO);

        try {
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

        Timer timer = fileTransfer.getTimer();
        TimerTask task = new TimerTask() {
            public void run() {
                thread.interrupt();
            }
        };
        timer.schedule(task, getConnectionTimeoutMillis());
        thread.start();
    }

    public void stop() {
        thread.interrupt();
    }

    public Socket getSocket() { return socket; }

    protected void initializeBeforeStarting() throws IOException {

    }

    protected void openConnectionInThread() {
        try {
            socket = createSocket();
            stream = new Stream(socket.getInputStream(), socket.getOutputStream());
        } catch (IOException e) {
            fireFailed(e);
            return;
        }
        try {
            initializeConnectionInThread();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected abstract Socket createSocket() throws IOException;

    protected void fireConnected() {
        fireSucceeded(stream);
    }

    protected void initializeConnectionInThread() throws IOException {
        fireConnected();
    }
}
