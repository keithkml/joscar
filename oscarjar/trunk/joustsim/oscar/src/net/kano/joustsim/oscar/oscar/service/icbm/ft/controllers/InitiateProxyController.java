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

import net.kano.joscar.rvproto.rvproxy.RvProxyCmd;
import net.kano.joscar.rvproto.rvproxy.RvProxyInitSendCmd;
import net.kano.joscar.rvproto.rvproxy.RvProxyPacket;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.MiscTools;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransferImpl;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransferManager;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectingToProxyEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ResolvingProxyEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;

public abstract class InitiateProxyController extends AbstractProxyConnectionController {
    private InetAddress proxyAddr;

    protected synchronized InetAddress getIpAddress() throws IOException {
        if (proxyAddr == null) {
            proxyAddr = InetAddress.getByName(getProxyHost());
        }
        return proxyAddr;
    }


    private String getProxyHost() {
        return "ars.oscar.aol.com";
    }

    protected void initializeProxy() throws IOException {
        OutputStream out = getStream().getOutputStream();
        FileTransferImpl transfer = getFileTransfer();
        FileTransferManager ftManager = transfer.getFileTransferManager();
        AimConnection connection = ftManager.getIcbmService().getAimConnection();
        RvProxyCmd initCmd = new RvProxyInitSendCmd(
                connection.getScreenname().getNormal(),
                transfer.getRvSession().getRvSessionId(),
                CapabilityBlock.BLOCK_FILE_SEND);
        RvProxyPacket packet = new RvProxyPacket(initCmd);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        packet.write(bout);
        bout.writeTo(out);
    }

    protected void setResolvingState() {
        getFileTransfer().getEventPost().fireEvent(new ResolvingProxyEvent(getProxyHost()));
    }

    protected void setConnectingState() {
        try {
            getFileTransfer().getEventPost().fireEvent(new ConnectingToProxyEvent(getIpAddress(), getConnectionPort()));
        } catch (IOException e) {
        }
    }
}
