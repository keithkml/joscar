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
 *  File created by keith @ Apr 26, 2003
 *
 */

package net.kano.joscar.rvcmd.trillcrypt;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

public class TrillianCryptReqRvCmd extends AbstractTrillianCryptRvCmd {
    private static final int TYPE_P = 0x3e9;
    private static final int TYPE_G = 0x3ea;

    private final BigInteger p;
    private final BigInteger g;

    public TrillianCryptReqRvCmd(RecvRvIcbm icbm) {
        super(icbm);

        TlvChain chain = getExtraTlvs();

        Tlv pTlv = chain.getLastTlv(TYPE_P);

        if (pTlv != null) p = getBigIntFromHexBlock(pTlv.getData());
        else p = null;

        Tlv gTlv = chain.getLastTlv(TYPE_G);

        if (gTlv != null) g = getBigIntFromHexBlock(gTlv.getData());
        else g = null;
    }

    public TrillianCryptReqRvCmd(long icbmMessageId, BigInteger p,
            BigInteger g) {
        super(icbmMessageId, ENCSTATUS_REQUEST);

        this.p = p;
        this.g = g;
    }

    public final BigInteger getP() { return p; }

    public final BigInteger getG() { return g; }

    protected void writeExtraTlvs(OutputStream out) throws IOException {
        if (p != null) {
            new Tlv(TYPE_P, ByteBlock.wrap(getBigIntHexBlock(p))).write(out);
        }
        if (g != null) {
            new Tlv(TYPE_G, ByteBlock.wrap(getBigIntHexBlock(g))).write(out);
        }
    }

    public String toString() {
        return "TrillianEncryptReqRvCmd: p=" + p + ", g=" + g;
    }
}
