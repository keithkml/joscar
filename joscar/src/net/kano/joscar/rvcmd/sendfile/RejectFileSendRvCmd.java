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
 *  File created by keith @ Apr 24, 2003
 *
 */

package net.kano.joscar.rvcmd.sendfile;

import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.tlv.ImmutableTlvChain;
import net.kano.joscar.tlv.TlvChain;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.DefensiveTools;

import java.io.OutputStream;
import java.io.IOException;

public class RejectFileSendRvCmd extends AbstractFileSendRvCmd {
    public static final int ERRORCODE_CANCELLED = 0x0001;

    private static final int TYPE_ERRORCODE = 0x0b;

    private final int errorCode;

    public RejectFileSendRvCmd(RecvRvIcbm icbm) {
        super(icbm);

        TlvChain chain = ImmutableTlvChain.readChain(icbm.getRvData());

        errorCode = chain.getUShort(TYPE_ERRORCODE);
    }

    public RejectFileSendRvCmd(int errorCode) {
        super(STATUS_DENY);

        DefensiveTools.checkRange(errorCode, "errorCode", -1);

        this.errorCode = errorCode;
    }

    public final int getErrorCode() { return errorCode; }

    public void writeRvData(OutputStream out) throws IOException {
        if (errorCode != -1) {
            Tlv.getUShortInstance(TYPE_ERRORCODE, errorCode).write(out);
        }
    }

    public String toString() {
        return "RejectFileSendRvCmd: errorCode=" + errorCode;
    }
}
