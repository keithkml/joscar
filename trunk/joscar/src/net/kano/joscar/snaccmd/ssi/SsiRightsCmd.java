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
 *  File created by keith @ Mar 3, 2003
 *
 */

package net.kano.joscar.snaccmd.ssi;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SsiRightsCmd extends SsiCommand {
    private static final int TYPE_MAXIMA = 0x0004;

    private final int[] maxima;

    protected SsiRightsCmd(SnacPacket packet) {
        super(CMD_RIGHTS);

        ByteBlock snacData = packet.getData();

        TlvChain chain = TlvChain.readChain(snacData);

        Tlv maximaTlv = chain.getLastTlv(TYPE_MAXIMA);

        if (maximaTlv != null) {
            ByteBlock block = maximaTlv.getData();

            maxima = new int[block.getLength() / 2];

            for (int i = 0; i < maxima.length; i++) {
                maxima[i] = BinaryTools.getUShort(block, i*2);
            }
        } else {
            maxima = null;
        }
    }

    public SsiRightsCmd(int[] maxima) {
        super(CMD_RIGHTS);

        this.maxima = maxima;
    }

    public final int[] getMaxima() {
        return maxima;
    }

    public void writeData(OutputStream out) throws IOException {
        if (maxima != null) {
            ByteArrayOutputStream maximout = new ByteArrayOutputStream();

            try {
                for (int i = 0; i < maxima.length; i++) {
                    BinaryTools.writeUShort(maximout, maxima[i]);
                }
            } catch (IOException impossible) { }

            ByteBlock maximaBlock = ByteBlock.wrap(maximout.toByteArray());

            new Tlv(TYPE_MAXIMA, maximaBlock).write(out);
        }
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer(maxima.length * 12);

        if (maxima != null) {
            for (int i = 0; i < maxima.length; i++) {
                buffer.append("0x");
                buffer.append(Integer.toHexString(i));
                buffer.append(": ");
                buffer.append(maxima[i]);
                buffer.append(", ");
            }
        }
        return "SsiRightsCmd: " + maxima.length + " maxima: " + buffer;
    }
}
