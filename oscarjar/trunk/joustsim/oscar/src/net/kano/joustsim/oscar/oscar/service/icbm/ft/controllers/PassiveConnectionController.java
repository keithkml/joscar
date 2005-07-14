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

import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.WaitingForConnectionEvent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;

public abstract class PassiveConnectionController
        extends AbstractConnectionController
        implements ManualTimeoutController {
    private ServerSocketChannel serverSocket;
    private RvConnectionInfo connInfo;

    protected void initializeBeforeStarting() throws IOException {
        ServerSocketChannel chan = ServerSocketChannel.open();
        serverSocket = chan;
        chan.socket().bind(null);
        sendRequest();
    }

    protected ServerSocket getServerSocket() {
        return serverSocket.socket();
    }

    protected abstract void sendRequest() throws IOException;

    protected Socket createSocket() throws IOException {
        setConnectingState();
        return serverSocket.accept().socket();
    }

    protected void setResolvingState() {
    }

    protected void setConnectingState() {
        EventPost post = getFileTransfer().getEventPost();
        post.fireEvent(new WaitingForConnectionEvent(connInfo.getInternalIP(),
                        connInfo.getPort()));
    }

    protected RvConnectionInfo getConnInfo() {
        return connInfo;
    }

    protected void setConnInfo(RvConnectionInfo connInfo) {
        this.connInfo = connInfo;
    }

    protected boolean shouldStartTimerAutomatically() {
        return false;
    }

    public void startTimeoutTimer() {
        super.startTimer();
    }
}
