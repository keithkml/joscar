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
 *  File created by keith @ Apr 28, 2003
 *
 */

package net.kano.joscar.rvcmd;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.LiveWritable;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;

public class RvConnectionInfo implements LiveWritable {
    private static final int TYPE_PROXYIP = 0x0002;
    private static final int TYPE_INTERNALIP = 0x0003;
    private static final int TYPE_EXTERNALIP = 0x0004;
    private static final int TYPE_PORT = 0x0005;
    private static final int TYPE_PROXIED = 0x0010;

    private final boolean proxied;
    private final InetAddress proxyIP;
    private final InetAddress internalIP;
    private final InetAddress externalIP;
    private final int port;

    public static RvConnectionInfo readConnectionInfo(TlvChain chain) {
        DefensiveTools.checkNull(chain, "chain");

        Tlv internalIpTlv = chain.getLastTlv(TYPE_INTERNALIP);
        InetAddress internalIP = null;
        if (internalIpTlv != null) {
            internalIP = BinaryTools.getIPFromBytes(internalIpTlv.getData(), 0);
        }

        Tlv externalIpTlv = chain.getLastTlv(TYPE_EXTERNALIP);
        InetAddress externalIP = null;
        if (externalIpTlv != null) {
            externalIP = BinaryTools.getIPFromBytes(externalIpTlv.getData(), 0);
        }

        Tlv proxyIpTlv = chain.getLastTlv(TYPE_PROXYIP);
        InetAddress proxyIP = null;
        if (proxyIpTlv != null) {
            proxyIP = BinaryTools.getIPFromBytes(proxyIpTlv.getData(), 0);
        }

        int port = chain.getUShort(TYPE_PORT);

        boolean proxied = chain.hasTlv(TYPE_PROXIED);

        return new RvConnectionInfo(internalIP, externalIP, proxyIP, port, proxied);
    }

    public RvConnectionInfo(InetAddress internalIP) {
        this(internalIP, -1);
    }

    public RvConnectionInfo(InetAddress internalIP, int port) {
        this(internalIP, null, null, port, false);
    }

    public RvConnectionInfo(InetAddress internalIP, InetAddress externalIP,
            InetAddress proxyIP, int port, boolean proxied) {

        DefensiveTools.checkRange(port, "port", -1);

        this.internalIP = internalIP;
        this.externalIP = externalIP;
        this.proxyIP = proxyIP;
        this.port = port;
        this.proxied = proxied;
    }

    public final InetAddress getInternalIP() { return internalIP; }

    public final InetAddress getExternalIP() { return externalIP; }

    public final boolean isProxied() { return proxied; }

    public final InetAddress getProxyIP() { return proxyIP; }

    public final int getPort() { return port; }

    private static final void writeIP(OutputStream out, int type,
            InetAddress addr) throws IOException {
        ByteBlock addrBlock = ByteBlock.wrap(addr.getAddress());
        new Tlv(type, addrBlock).write(out);
    }

    public void write(OutputStream out) throws IOException {
        if (internalIP != null) writeIP(out, TYPE_INTERNALIP, internalIP);
        if (externalIP != null) writeIP(out, TYPE_EXTERNALIP, externalIP);
        if (proxyIP != null) writeIP(out, TYPE_PROXYIP, proxyIP);
        if (port != -1) Tlv.getUShortInstance(TYPE_PORT, port).write(out);
        if (proxied) new Tlv(TYPE_PROXIED).write(out);
    }

    public String toString() {
        return "ConnectionInfo: " + (proxied ? "(proxied) " : "") + 
                "internalIP=" + internalIP +
                ", externalIP=" + externalIP +
                ", proxyIP=" + proxyIP +
                ", port=" + port;
    }
}
