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
 *  File created by Keith @ 12:05:56 AM
 *
 */

package net.kano.joscar.rvcmd.addins;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.rvcmd.AbstractRequestRvCmd;
import net.kano.joscar.rvcmd.InvitationMessage;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.StringBlock;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;

import java.io.IOException;
import java.io.OutputStream;

public class AddinsReqRvCmd extends AbstractRequestRvCmd {
    /*
    00 00 01 00 05 07 4c 7f 11 d1 82 22 44 45 53 54 00 00 00 09 00 09 4d 53 48
    65 61 72 74 73 00 4c 45 41 4b 2d 54 33 30 00 00 00 00 00

    "UUID" is 07050001-7F4C-D111-8222-444553540000
    */

    public static final int CODE_DEFAULT = 0;
    public static final long FLAGS_DEFAULT = 0;

    private static final int TYPE_ADDINURI = 0x0007;

    private final int code;
    private final String addinUri;
    private final ByteBlock uuid;
    private final String addinName;
    private final String computerName;
    private final long flags;
    private final InvitationMessage invMessage;

    public AddinsReqRvCmd(RecvRvIcbm icbm) {
        super(icbm);

        DefensiveTools.checkNull(icbm, "icbm");

        TlvChain chain = getRvTlvs();

        addinUri = chain.getString(TYPE_ADDINURI);

        ByteBlock rvData = getServiceData();

        int code = -1;
        ByteBlock uuid = null;
        String addinName = null;
        String computerName = null;
        long flags = -1;

        if (rvData != null && rvData.getLength() >= 2) {
            code = BinaryTools.getUShort(rvData, 0);

            if (rvData.getLength() > 10) {
                // copy the UUID over, since we think it might be used a lot...
                uuid = ByteBlock.wrap(rvData.subBlock(2, 16).toByteArray());

                if (rvData.getLength() >= 22) {
                    int addinNameLen = BinaryTools.getUShort(rvData, 18);
                    int compNameLen = BinaryTools.getUShort(rvData, 20);

                    if (rvData.getLength() >= 22 + addinNameLen + compNameLen) {
                        StringBlock addinNameBlock = BinaryTools.getNullPadded(
                                rvData.subBlock(22));
                        addinName = addinNameBlock.getString();

                        StringBlock compNameBlock
                                = BinaryTools.getNullPadded(rvData.subBlock(
                                        22 + addinNameBlock.getTotalSize()+1));
                        computerName = compNameBlock.getString();

                        flags = BinaryTools.getUInt(rvData,
                                22 + addinNameBlock.getTotalSize()+1
                                + compNameBlock.getTotalSize()+1);
                    }
                }
            }
        }

        this.code = code;
        this.uuid = uuid;
        this.addinName = addinName;
        this.computerName = computerName;
        this.flags = flags;

        invMessage = InvitationMessage.readInvitationMessage(chain);
    }

    public AddinsReqRvCmd(long icbmMessageId, int code, String addinUri,
            ByteBlock uuid, String addinName, String computerName, long flags,
            InvitationMessage invMessage) {
        super(icbmMessageId, CapabilityBlock.BLOCK_ADDINS);

        DefensiveTools.checkRange(code, "code", -1);
        DefensiveTools.checkRange(flags, "flags", -1);

        if (uuid != null && uuid.getLength() != 16) {
            throw new IllegalArgumentException("uuid length ("
                    + uuid.getLength() + ") must be 16 if uuid is non-null");
        }

        this.code = code;
        this.addinUri = addinUri;
        this.uuid = uuid;
        this.addinName = addinName;
        this.computerName = computerName;
        this.flags = flags;
        this.invMessage = invMessage;
    }

    public final int getCode() { return code; }

    public final String getAddinUri() { return addinUri; }

    public final ByteBlock getAddinUuid() { return uuid; }

    public final String getAddinName() { return addinName; }

    public final String getComputerName() { return computerName; }

    public final long getFlags() { return flags; }

    protected void writeRvTlvs(OutputStream out) throws IOException {
        if (addinUri != null) {
            Tlv.getStringInstance(TYPE_ADDINURI, addinUri).write(out);
        }
        if (invMessage != null) {
            invMessage.write(out);
        }
    }

    protected boolean hasServiceData() {
        return true;
    }

    protected void writeServiceData(OutputStream out) throws IOException {
        if (code != -1) {
            BinaryTools.writeUShort(out, code);

            if (uuid != null) {
                uuid.write(out);

                if (addinName != null && computerName != null) {
                    byte[] addinNameBytes
                            = BinaryTools.getAsciiBytes(addinName);
                    byte[] compNameBytes
                            = BinaryTools.getAsciiBytes(computerName);

                    BinaryTools.writeUShort(out, addinNameBytes.length);
                    BinaryTools.writeUShort(out, compNameBytes.length);

                    out.write(addinNameBytes);
                    out.write(0);

                    out.write(compNameBytes);
                    out.write(0);

                    if (flags != -1) {
                        BinaryTools.writeUInt(out, flags);
                    }
                }
            }
        }
    }

    public String toString() {
        return "AddinsReqRvCmd: code=" + code + ", addinName=" + addinName
                + ", computerName=" + computerName + ", flags=0x"
                + Long.toHexString(flags) + ", message=<" + invMessage
                + ">, uri=" + addinUri;
    }
}