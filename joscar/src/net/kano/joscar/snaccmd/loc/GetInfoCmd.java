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
 *  File created by keith @ Aug 17, 2003
 *
 */

package net.kano.joscar.snaccmd.loc;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.OscarTools;
import net.kano.joscar.StringBlock;
import net.kano.joscar.flapcmd.SnacPacket;

import java.io.IOException;
import java.io.OutputStream;

public class GetInfoCmd extends LocCommand {
    public static final long FLAG_DEFAULT = 0x00000000;
    public static final long FLAG_INFO = 0x00000001;
    public static final long FLAG_AWAYMSG = 0x00000002;
    public static final long FLAG_CERT = 0x00000008;

    private final long flags;
    private final String sn;

    protected GetInfoCmd(SnacPacket packet) {
        super(CMD_NEW_GET_INFO);

        DefensiveTools.checkNull(packet, "packet");

        ByteBlock data = packet.getData();

        flags = BinaryTools.getUInt(data, 0);

        ByteBlock snData = data.subBlock(4);
        StringBlock snBlock = OscarTools.readScreenname(snData);

        if (snBlock == null) {
            sn = null;
        } else {
            sn = snBlock.getString();
        }
    }

    public GetInfoCmd(long flags, String sn) {
        super(CMD_NEW_GET_INFO);

        DefensiveTools.checkRange(flags, "flags", -1);
        DefensiveTools.checkNull(sn, "sn");

        this.flags = flags;
        this.sn = sn;
    }

    public final long getFlags() { return flags; }

    public final String getScreenname() { return sn; }

    public void writeData(OutputStream out) throws IOException {
        if (flags != -1) {
            BinaryTools.writeUInt(out, flags);
            if (sn != null) {
                OscarTools.writeScreenname(out, sn);
            }
        }
    }

    public String toString() {
        return "GetInfoCmd: flags=" + flags + ", sn=" + sn;
    }
}
