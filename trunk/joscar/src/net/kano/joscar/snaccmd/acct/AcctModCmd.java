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
 *  File created by keith @ Feb 24, 2003
 *
 */

package net.kano.joscar.snaccmd.acct;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A SNAC command used to modify the formatting of one's screenname or to change
 * one's registered email address.
 *
 * @snac.src client
 * @snac.cmd 0x07 0x04
 *
 * @see AcctModAck
 */
public class AcctModCmd extends AcctCommand {
    /** A command type indicating that the screen name is being reformatted. */
    private static final int TYPE_SN = 0x0001;
    /**
     * A command type indicating that the registered email address is being
     * changed.
     */
    private static final int TYPE_EMAIL = 0x0011;

    /** The new screenname. */
    private final String sn;
    /** The new email address. */
    private final String email;

    /**
     * Generates an account modification command object from the given incoming
     * SNAC packet.
     *
     * @param packet the account modification command packet
     */
    protected AcctModCmd(SnacPacket packet) {
        super(CMD_ACCT_MOD);

        ByteBlock snacData = packet.getData();

        TlvChain chain = TlvChain.readChain(snacData);

        sn = chain.getString(TYPE_SN);
        email = chain.getString(TYPE_EMAIL);
    }

    /**
     * Creates an outgoing account modification command that sets the given
     * screenname and/or registered email address.
     *
     * @param sn a newly formatted screenname
     * @param email a new registered email address for this screenname
     */
    public AcctModCmd(String sn, String email) {
        super(CMD_ACCT_MOD);

        this.sn = sn;
        this.email = email;
    }

    /**
     * Returns the new screen name format requested in this command, or
     * <code>null</code> if that field was not sent.
     *
     * @return this command's requested screen name format field
     */
    public final String getScreenname() {
        return sn;
    }

    /**
     * Returns the new registered email address requested by this command, or
     * <code>null</code> if that field was not sent.
     *
     * @return this command's requested new registered email address field
     */
    public final String getEmail() {
        return email;
    }

    public void writeData(OutputStream out) throws IOException {
        if (sn != null) Tlv.getStringInstance(TYPE_SN, sn).write(out);
        if (email != null) Tlv.getStringInstance(TYPE_EMAIL, email).write(out);
    }

    public String toString() {
        return "AccountModCmd: sn=" + sn + ", email=" + email;
    }
}
