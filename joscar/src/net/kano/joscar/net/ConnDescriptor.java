/*
 *  Copyright (c) 2003, The Joust Project
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
 *  File created by keith @ Dec 28, 2003
 *
 */

package net.kano.joscar.net;

import net.kano.joscar.DefensiveTools;

import java.net.InetAddress;

/**
 * A class describing a destination host and port, for making an outgoing TCP
 * connection. An instance of this class contains either a host or an IP address
 * along with a port. No instance of this class will contain both a host and an
 * IP address.
 */
public class ConnDescriptor {
    /** The host to connect to, if any. */
    private final String host;
    /** The IP address to connect to, if any. */
    private final InetAddress address;
    /** The port to connect to. */
    private final int port;

    /**
     * Creates a new connection descriptor instance for the given hostname and
     * port.
     *
     * @param host a hostname
     * @param port a port number
     */
    public ConnDescriptor(String host, int port) {
        DefensiveTools.checkNull(host, "host");
        DefensiveTools.checkRange(port, "port", 0);

        this.host = host;
        this.address = null;
        this.port = port;
    }

    /**
     * Creates a new connection descriptor instance for the given IP address and
     * port.
     *
     * @param address an IP address
     * @param port a port number
     */
    public ConnDescriptor(InetAddress address, int port) {
        DefensiveTools.checkNull(address, "address");
        DefensiveTools.checkRange(port, "port", 0);

        this.host = null;
        this.address = address;
        this.port = port;
    }

    /**
     * Returns this connection descriptor's hostname, if any. If this method
     * returns <code>null</code>, {@link #getAddress()} will not return
     * <code>null</code>.
     *
     * @return this connection descriptor's hostname, or <code>null</code>
     */
    public final String getHost() { return host; }

    /**
     * Returns this connection descriptor's IP address, if any. If this method
     * returns <code>null</code>, {@link #getHost()} will not return
     * <code>null</code>.
     *
     * @return this connection descriptor's hostname, or <code>null</code>
     */
    public final InetAddress getAddress() { return address; }

    /**
     * Returns this connection descriptor's destination port. The returned value
     * will never be negative.
     *
     * @return this connection descriptor's destination port
     */
    public final int getPort() { return port; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConnDescriptor)) return false;

        final ConnDescriptor cd = (ConnDescriptor) o;

        if (port != cd.port) return false;
        if (address != null ? !address.equals(cd.address)
                : cd.address != null) {
            return false;
        }
        if (host != null ? !host.equals(cd.host)
                : cd.host != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (host != null ? host.hashCode() : 0);
        result = 29 * result + (address != null ? address.hashCode() : 0);
        result = 29 * result + port;
        return result;
    }

    public String toString() {
        Object first = (host == null ? (Object) address : (Object) host);
        return "ConnDescriptor: " + first + ":" + port;
    }
}
