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
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.nio.channels.ServerSocketChannel;

public abstract class PassiveConnectionController
    extends AbstractConnectionController
    implements ManualTimeoutController {

  {
    setConnector(new MyConnector());
  }

  public PassiveConnector getConnector() {
    return (PassiveConnector) super.getConnector();
  }

  protected void initializeBeforeStarting() throws IOException {
  }

  protected abstract void sendRequest() throws IOException;

  protected void handleResolvingState() {
  }

  protected void handleConnectingState() {
    EventPost post = getRvConnection().getEventPost();
    RvConnectionInfo connInfo = getRvSessionInfo().getConnectionInfo();
    post.fireEvent(new WaitingForConnectionEvent(connInfo.getInternalIP(),
        connInfo.getPort()));
  }

  protected boolean shouldStartTimerAutomatically() {
    return false;
  }

  public void startTimeoutTimer() {
    super.startTimer();
  }

  protected void prepareStream() throws IOException {
    super.prepareStream();
    sendRequest();
  }

  private class MyConnector implements PassiveConnector {
    private ServerSocket serverSocket;
    private int localPort = -1;

    public int getLocalPort() {
      return localPort;
    }

    public InetAddress getLocalHost() {
      return serverSocket.getInetAddress();
    }

    public StreamInfo createStream() throws IOException {
      handleConnectingState();
      return new StreamInfo(serverSocket.accept().getChannel());
    }

    public void prepareStream() throws IOException {
      ServerSocketFactory ssf = getRvConnection().getSettings().getProxyInfo()
          .getServerSocketFactory();
      if (ssf == null) {
        serverSocket = ServerSocketChannel.open().socket();
      } else {
        serverSocket = ssf.createServerSocket();
      }
      serverSocket.bind(null);
      localPort = serverSocket.getLocalPort();
    }

    public void checkConnectionInfo() throws Exception {
    }
  }
}
