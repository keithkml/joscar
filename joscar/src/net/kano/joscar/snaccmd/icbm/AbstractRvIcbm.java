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

package net.kano.joscar.snaccmd.icbm;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.LiveWritable;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.snaccmd.AbstractIcbm;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.AbstractTlvChain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A base class for the two rendezvous-based ICBM commands provided in this
 * package. The two commands are {@link SendRvIcbm} and {@link RecvRvIcbm}.
 */
public abstract class AbstractRvIcbm extends AbstractIcbm {
    /** A status code indicating that this rendezvous ICBM is a request. */
    public static final int STATUS_REQUEST = 0x0000;
    /** A status code indicating that a rendezvous has been accepted. */
    public static final int STATUS_ACCEPT = 0x0002;
    /** A status code indicating that a rendezvous has been denied. */
    public static final int STATUS_DENY = 0x0001;

    /** A TLV type containing the rendezvous-specific data. */
    private static final int TYPE_RV_DATA = 0x0005;

    /** A status code. */
    private int status;
    /** A rendezvous cookie. */
    private long rvCookie;
    /** The capability block associated with this rendezvous command. */
    private CapabilityBlock cap;
    /** Command-specific data. */
    private ByteBlock rvData;
    /** A writable to write the rendezvous-specific data. */
    private LiveWritable rvDataWriter;

    /**
     * Generates a rendezvous ICBM from the given incoming SNAC packet.
     *
     * @param command the SNAC command subtype of this command
     * @param packet an incoming rendezvous ICBM packet
     */
    AbstractRvIcbm(int command, SnacPacket packet) {
        super(IcbmCommand.FAMILY_ICBM, command, packet);
    }

    /**
     * Extracts rendezvous-specific fields from the given TLV chain.
     *
     * @param chain the chain from which to read.
     */
    final void processRvTlvs(AbstractTlvChain chain) {
        ByteBlock rvBlock = chain.getLastTlv(TYPE_RV_DATA).getData();

        status = BinaryTools.getUShort(rvBlock, 0);
        rvCookie = BinaryTools.getLong(rvBlock, 2);

        ByteBlock capBlock = rvBlock.subBlock(10, 16);
        cap = new CapabilityBlock(capBlock);

        rvData = rvBlock.subBlock(26);

        rvDataWriter = rvData;
    }

    /**
     * Creates a new outgoing rendezvous ICBM command with the given properties.
     *
     * @param command the SNAC command subtype of this command
     * @param icbmCookie an ICBM cookie to attach to this command
     * @param status a status code, like {@link #STATUS_REQUEST}
     * @param rvCookie a rendezvous cookie to attach to this command
     * @param cap the capability block associated with this command
     * @param rvDataWriter an object to write rendezvous-command-specific data
     */
    AbstractRvIcbm(int command, long icbmCookie, int status,
            long rvCookie, CapabilityBlock cap, LiveWritable rvDataWriter) {
        super(IcbmCommand.FAMILY_ICBM, command, icbmCookie, CHANNEL_RV);
        this.status = status;
        this.rvCookie = rvCookie;
        this.cap = cap;
        this.rvData = null;
        this.rvDataWriter = rvDataWriter;
    }

    /**
     * Returns the status code of this rendezvous. Will normally be one of
     * {@link #STATUS_REQUEST}, {@link #STATUS_ACCEPT}, and {@link
     * #STATUS_DENY}.
     *
     * @return this rendezvous's status code
     */
    public final int getRvStatus() {
        return status;
    }

    /**
     * Returns the rendezvous cookie sent in this command.
     *
     * @return this rendezvous's rendezvous cookie
     */
    public final long getRvCookie() {
        return rvCookie;
    }

    /**
     * Returns the capability that this rendezvous is using.
     *
     * @return this rendezvous's associated capability block
     */
    public final CapabilityBlock getCapability() {
        return cap;
    }

    /**
     * Returns the rendezvous-specific data in this rendezvous command. The
     * contents of this block vary from capability to capability.
     *
     * @return the rendezvous-specific data
     */
    public final ByteBlock getRvData() {
        return rvData;
    }

    /**
     * Writes the rendezvous-specific fields of this command to the given
     * stream.
     *
     * @param out the stream to write to
     * @throws IOException if an I/O error occurs
     */
    final void writeRvTlvs(OutputStream out) throws IOException {
        ByteArrayOutputStream rvout = new ByteArrayOutputStream();

        BinaryTools.writeUShort(rvout, status);
        BinaryTools.writeLong(rvout, rvCookie);
        cap.write(rvout);
        rvDataWriter.write(out);

        new Tlv(TYPE_RV_DATA, ByteBlock.wrap(rvout.toByteArray())).write(out);
    }
}
