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
import net.kano.joscar.MiscTools;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.ConnectionType;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;

import java.net.InetAddress;

public class OutgoingConnectionController extends AbstractOutgoingConnectionController {
    private ConnectionType type;

    public OutgoingConnectionController(ConnectionType type)
            throws IllegalArgumentException {
        if (type != ConnectionType.INTERNET && type != ConnectionType.LAN) {
            throw new IllegalArgumentException("invalid type for "
                    + MiscTools.getClassName(this) + ": " + type);
        }
        this.type = type;
    }

    protected InetAddress getIpAddress() {
        RvConnectionInfo connectionInfo = getConnectionInfo();
//        assert !connectionInfo.isProxied();

        InetAddress ip;
        if (type == ConnectionType.INTERNET) {
            ip = connectionInfo.getExternalIP();
        } else if (type == ConnectionType.LAN) {
            ip = connectionInfo.getInternalIP();
        } else {
            throw new IllegalStateException(
                    "invalid OutgoingConnectionType " + type);
        }
        return ip;
    }

    protected void checkConnectionInfo() throws IllegalStateException {
        if (getIpAddress() == null) {
            throw new IllegalStateException(MiscTools.getClassName(this) + " ("
                    + type + ") has invalid connection info: "
                    + getConnectionInfo());
        }
    }

    protected int getConnectionPort() {
        return getConnectionInfo().getPort();
    }

    protected void setResolvingState() {
    }

    protected ConnectionType getConnectionType() {
        return type;
    }

    protected void setConnectingState() {
        InetAddress ipAddress = getIpAddress();
        int outPort = getConnectionInfo().getPort();
        ConnectionType type = OutgoingConnectionController.this.type;
        EventPost eventPost = getFileTransfer().getEventPost();
        eventPost.fireEvent(new ConnectingEvent(type, ipAddress, outPort));
    }
}
