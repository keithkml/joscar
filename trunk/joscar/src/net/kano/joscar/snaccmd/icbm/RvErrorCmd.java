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
 *  File created by keith @ Mar 28, 2003
 *
 */

package net.kano.joscar.snaccmd.icbm;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.snaccmd.OscarTools;
import net.kano.joscar.snaccmd.ScreenNameBlock;
import net.kano.joscar.tlv.TlvChain;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A SNAC command indicating that a rendezvous failed.
 *
 * @snac.src client
 * @snac.cmd 0x04 0x0b
 */
public class RvErrorCmd extends IcbmCommand {
    /**
     * A code indicating that a rendezvous type is not supported by the client.
     */
    public static final int CODE_NOT_SUPPORTED = 0x0000;
    /** A code indicating that a rendezvous was declined. */
    public static final int CODE_DECLINED = 0x0001;
    /**
     * A code indicating that a client is not accepting requests of the given
     * type.
     */
    public static final int CODE_NOT_ACCEPTING = 0x0002;

    /** A TLV type containing an error code. */
    private static final int TYPE_ERROR_CODE = 0x0003;

    /** The rendezvous cookie of the rendezvous that failed. */
    private final long cookie;
    /** The channel on which the rendezvous was sent. */
    private final int channel;
    /** The screenname whose rendezvous request is being denied. */
    private final String sn;
    /** An error code. */
    private final int code;

    /**
     * Creates a new rendezvous error command from the given incoming SNAC
     * packet.
     *
     * @param packet an incoming rendezvous error packet
     */
    protected RvErrorCmd(SnacPacket packet) {
        super(CMD_RV_ERROR);

        ByteBlock snacData = packet.getData();

        cookie = BinaryTools.getLong(snacData, 0);
        channel = BinaryTools.getUShort(snacData, 8);

        ByteBlock snData = snacData.subBlock(10);
        ScreenNameBlock snInfo = OscarTools.readScreenname(snData);
        sn = snInfo.getScreenname();

        ByteBlock rest = snData.subBlock(snInfo.getTotalSize());

        TlvChain chain = TlvChain.readChain(rest);

        code = chain.getUShort(TYPE_ERROR_CODE);
    }

    /**
     * Creates a new outgoing rendezvous error command with the given
     * properties.
     *
     * @param cookie the rendezvous cookie of the rendezvous being denied
     * @param channel the ICBM channel on which the denied rendezvous was sent
     * @param sn the screenname whose rendezvous is being denied
     * @param code an error code, like {@link #CODE_NOT_SUPPORTED}
     */
    public RvErrorCmd(long cookie, int channel, String sn, int code) {
        super(CMD_RV_ERROR);
        this.cookie = cookie;
        this.channel = channel;
        this.sn = sn;
        this.code = code;
    }

    /**
     * Returns the rendezvous cookie of the rendezvous that failed.
     *
     * @return the failed rendezvous's "rendezvous cookie"
     */
    public final long getCookie() { return cookie; }

    /**
     * Returns the ICBM channel on which the denied rendezvous was received.
     *
     * @return the failed rendezvous's ICBM channel
     */
    public final int getChannel() { return channel; }

    /**
     * Returns the screenname of the user who sent the denied rendezvous, and
     * to whom this error command is being sent.
     *
     * @return the screenname whose rendezvous was denied
     */
    public final String getScreenname() { return sn; }

    /**
     * Returns the associated error code. Normally one of {@link
     * #CODE_DECLINED}, {@link #CODE_NOT_ACCEPTING}, and {@link
     * #CODE_NOT_SUPPORTED}.
     *
     * @return the error code associated with this rendezvous error
     */
    public final int getErrorCode() { return code; }

    public void writeData(OutputStream out) throws IOException {
        BinaryTools.writeLong(out, cookie);
        BinaryTools.writeUShort(out, channel);
        OscarTools.writeScreenname(out, sn);
        BinaryTools.writeUShort(out, code);
    }

    public String toString() {
        return "RvErrorCmd to " + sn + ": channel=" + channel + ", code=0x"
                + Integer.toHexString(code);
    }
}
