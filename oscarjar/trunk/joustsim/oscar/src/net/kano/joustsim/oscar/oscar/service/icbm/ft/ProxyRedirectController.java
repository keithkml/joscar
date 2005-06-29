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

import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joscar.rvproto.rvproxy.RvProxyAckCmd;
import net.kano.joscar.rvproto.rvproxy.RvProxyCmd;
import net.kano.joscar.rvproto.rvproxy.RvProxyInitSendCmd;
import net.kano.joustsim.oscar.AimConnection;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Random;

/**
 * 1. connect to ars.oscar.aol.com:5190
 * 2. find ip of server
 * 3. send rv
 */
class ProxyRedirectController extends AbstractProxyConnectionController {
    private Random random = new Random();

    protected InetAddress getIpAddress() throws IOException {
        return InetAddress.getByName("ars.oscar.aol.com");
    }

    protected void handleAck(RvProxyAckCmd ackCmd) throws IOException {
        Inet4Address addr = ackCmd.getProxyIpAddress();
        int port = ackCmd.getProxyPort();

        long msgId = random.nextInt(5000);
        RvSession rvSession = getFileTransfer().getRvSession();
        rvSession.sendRv(new FileSendReqRvCmd(
                RvConnectionInfo.createForOutgoingProxiedRequest(addr, port)),
                msgId);
        OutputStream out = getStream().getOutputStream();
        FileTransferManager ftManager = getFileTransfer().getFileTransferManager();
        AimConnection connection = ftManager.getIcbmService().getAimConnection();
        RvProxyCmd initCmd = new RvProxyInitSendCmd(
                connection.getScreenname().getNormal(), msgId);
        initCmd.writeCommandData(out);
    }

    protected void initializeProxy() throws IOException {
    }
}
