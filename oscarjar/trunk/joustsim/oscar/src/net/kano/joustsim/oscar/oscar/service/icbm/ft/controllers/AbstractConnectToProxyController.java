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

import net.kano.joscar.MiscTools;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.rvproto.rvproxy.RvProxyAckCmd;
import net.kano.joscar.rvproto.rvproxy.RvProxyCmd;
import net.kano.joscar.rvproto.rvproxy.RvProxyInitRecvCmd;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvSessionConnectionInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectingToProxyEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;

public abstract class AbstractConnectToProxyController
    extends AbstractProxyConnectionController
    implements ManualTimeoutController {

  {
    setConnector(new ConnectToProxyConnector());
  }

  protected void handleConnectingState() {
    int outPort = getConnector().getConnectionPort();
    InetAddress outAddr;
    try {
      outAddr = getConnector().getIpAddress();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    getRvConnection().getEventPost().fireEvent(
        new ConnectingToProxyEvent(outAddr, outPort));
  }

  public ProxyConnector getConnector() {
    return (ProxyConnector) super.getConnector();
  }

  protected void handleResolvingState() {
  }

  protected void handleAck(RvProxyAckCmd ackCmd) throws IOException {
  }

  protected void initializeProxy() throws IOException {
    RvSessionConnectionInfo conn = getRvSessionInfo();
    int port = conn.getConnectionInfo().getPort();
    String mysn = getRvConnection().getMyScreenname().getNormal();
    RvProxyCmd initCmd = new RvProxyInitRecvCmd(mysn,
        conn.getRvSession().getRvSessionId(), port,
        conn.getRequestMaker().getCapabilityBlock());
    getProxyConnection().sendProxyPacket(initCmd);
  }

  public class ConnectToProxyConnector extends DefaultProxyConnector {
    public @NotNull InetAddress getIpAddress() throws IllegalStateException {
      RvConnectionInfo connInfo = getRvSessionInfo().getConnectionInfo();
      if (!connInfo.isProxied()) {
        throw new IllegalStateException("Connection is not proxied: " + connInfo);
      }
      InetAddress proxyIp = connInfo.getProxyIP();
      if (proxyIp == null) {
        throw new IllegalStateException(MiscTools.getClassName(this)
            + " has invalid connection info: " + connInfo);
      }
      return proxyIp;
    }

    public void checkConnectionInfo() throws Exception {
      getIpAddress();
    }
  }
}
