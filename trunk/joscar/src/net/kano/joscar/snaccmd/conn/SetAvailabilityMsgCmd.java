
package net.kano.joscar.snaccmd.conn;

import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.BinaryTools;
import net.kano.joscar.tlv.TlvChain;
import net.kano.joscar.tlv.AbstractTlvChain;
import net.kano.joscar.tlv.ImmutableTlvChain;
import net.kano.joscar.tlv.Tlv;

import java.io.OutputStream;
import java.io.IOException;

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
 *  File created by keith @ Aug 1, 2003
 *
 */

public class SetAvailabilityMsgCmd extends ConnCommand {
    public static final int TYPE_DEFAULT = 0x0002;
    public static final int FLAG_DEFAULT = 0x04;

    private static final int TYPE_DATA = 0x001d;

    private final int type;
    private final int flags;

    protected SetAvailabilityMsgCmd(SnacPacket packet) {
        super(CMD_SETAVAILABILITY);

        ByteBlock data = packet.getData();

        TlvChain chain = ImmutableTlvChain.readChain(data);

        Tlv dataTlv = chain.getLastTlv(TYPE_DATA);

        if (dataTlv != null) {
            ByteBlock availData = dataTlv.getData();

            type = BinaryTools.getUShort(availData, 0);
            flags = BinaryTools.getUByte(availData, 2);

            int len = BinaryTools.getUByte(availData, 3);
            ByteBlock messageData = availData.subBlock(4, len);

            int msgLen = BinaryTools.getUShort(messageData, 0);
            ByteBlock messageBlock = messageData.subBlock(2, msgLen);
        } else {
            type = -1;
            flags = -1;
        }
    }

    public void writeData(OutputStream out) throws IOException {
        
    }
}
