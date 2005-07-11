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

import net.kano.joscar.rvproto.rvproxy.DefaultRvProxyCmdFactory;
import net.kano.joscar.rvproto.rvproxy.RvProxyAckCmd;
import net.kano.joscar.rvproto.rvproxy.RvProxyCmd;
import net.kano.joscar.rvproto.rvproxy.RvProxyCmdFactory;
import net.kano.joscar.rvproto.rvproxy.RvProxyErrorCmd;
import net.kano.joscar.rvproto.rvproxy.RvProxyPacket;
import net.kano.joscar.rvproto.rvproxy.RvProxyReadyCmd;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FailureEventException;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.ConnectionType;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.AolProxyTimedOutEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownAolProxyErrorEvent;

import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractProxyConnectionController
        extends AbstractOutgoingConnectionController {
    protected ConnectionType getConnectionType() {
        return ConnectionType.PROXY;
    }

    protected void initializeConnectionInThread()
            throws IOException, FailureEventException {
        StreamInfo stream = getStream();
        InputStream in = stream.getInputStream();

        initializeProxy();
        RvProxyCmdFactory factory = new DefaultRvProxyCmdFactory();
        while (true) {
            RvProxyPacket packet = RvProxyPacket.readPacket(in);
            RvProxyCmd cmd = factory.getRvProxyCmd(packet);
            if (cmd instanceof RvProxyAckCmd) {
                RvProxyAckCmd ackCmd = (RvProxyAckCmd) cmd;
                handleAck(ackCmd);

            } else if (cmd instanceof RvProxyErrorCmd) {
                RvProxyErrorCmd proxyErrorCmd = (RvProxyErrorCmd) cmd;
                int code = proxyErrorCmd.getErrorCode();
                if (code == RvProxyErrorCmd.ERRORCODE_TIMEOUT) {
                    fireFailed(new AolProxyTimedOutEvent());
                } else {
                    fireFailed(new UnknownAolProxyErrorEvent(code));
                }
                break;

            } else if (cmd instanceof RvProxyReadyCmd) {
                fireConnected();
                break;
            }
        }
    }

    protected int getConnectionPort() {
        return 5190;
    }

    protected abstract void handleAck(RvProxyAckCmd ackCmd) throws IOException;

    protected abstract void initializeProxy() throws IOException;
}
