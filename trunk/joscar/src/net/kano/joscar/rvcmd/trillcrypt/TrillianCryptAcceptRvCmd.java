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
 *  File created by keith @ Apr 27, 2003
 *
 */

package net.kano.joscar.rvcmd.trillcrypt;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TrillianCryptAcceptRvCmd
        extends AbstractTrillianCryptRvCmd {
    public static final int CODE_DEFAULT = 0x05;

    private static final int TYPE_CODE = 0x03eb;

    private final int code;

    public TrillianCryptAcceptRvCmd(RecvRvIcbm icbm) {
        super(icbm);

        TlvChain chain = getExtraTlvs();

        Tlv codeTlv = chain.getLastTlv(TYPE_CODE);

        int codeValue = -1;
        if (codeTlv != null) {
            ByteBlock data = codeTlv.getData();

            // hooray for sscanf.
            ByteBlock stringBlock = data.subBlock(0, data.getLength() - 1);

            String hex = BinaryTools.getAsciiString(stringBlock);
            try {
                codeValue = Integer.parseInt(hex, 16);
            } catch (NumberFormatException doh) { }
        }
        code = codeValue;
    }

    public TrillianCryptAcceptRvCmd(long icbmMessageId) {
        this(icbmMessageId, CODE_DEFAULT);
    }

    public TrillianCryptAcceptRvCmd(long icbmMessageId, int code) {
        super(icbmMessageId, ENCSTATUS_ACCEPT);

        DefensiveTools.checkRange(code, "code", -1);

        this.code = code;
    }

    public final int getCode() { return code; }

    protected void writeExtraTlvs(OutputStream out) throws IOException {
        if (code != -1) {
            // yay for sprintf.
            ByteArrayOutputStream bout = new ByteArrayOutputStream(3);
            byte[] stringData = Integer.toString(code, 16).getBytes("US-ASCII");
            for (int i = stringData.length; i < 2; i++) {
                bout.write('0');
            }
            bout.write(stringData);
            bout.write(0);

            new Tlv(TYPE_CODE, ByteBlock.wrap(bout.toByteArray())).write(out);
        }
    }

    public String toString() {
        return "TrillianEncryptAcceptRvCmd: code=" + code;
    }
}
