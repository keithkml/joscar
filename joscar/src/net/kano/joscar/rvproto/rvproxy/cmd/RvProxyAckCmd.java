/*
 *  Copyright (c) 2002, The Joust Project
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
 *  File created by keith @ Mar 6, 2003
 *
 */

package net.kano.joscar.rvproto.rvproxy.cmd;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.rvproto.rvproxy.cmd.AbstractRvProxyCmd;
import net.kano.joscar.rvproto.rvproxy.cmd.RvProxyHeader;

import java.io.OutputStream;
import java.io.IOException;
import java.net.InetAddress;

public class RvProxyAckCmd extends AbstractRvProxyCmd {
    private final InetAddress ip;
    private final int port;

    protected RvProxyAckCmd(RvProxyHeader header) {
        super(header);

        ByteBlock data = header.getData();

        port = BinaryTools.getUShort(data, 0);

        ip = BinaryTools.getIPFromBytes(data, 2);
    }

    public RvProxyAckCmd(InetAddress ip, int port) {
        super(RvProxyHeader.HEADERTYPE_ACK);

        DefensiveTools.checkNull(ip, "ip");
        DefensiveTools.checkRange(port, "port", -1);

        this.ip = ip;
        this.port = port;
    }

    public final InetAddress getProxyIp() { return ip; }

    public final int getProxyPort() { return port; }

    public void writeData(OutputStream out) throws IOException {
        if (port != -1) {
            BinaryTools.writeUShort(out, port);

            if (ip != null) {
                out.write(ip.getAddress());
            }
        }
    }

    public String toString() {
        return "RvProxyAckCmd: ip=" + ip + ", port=" + port;
    }
}