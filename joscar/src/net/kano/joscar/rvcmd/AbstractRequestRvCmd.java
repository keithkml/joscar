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

package net.kano.joscar.rvcmd;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;

import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractRequestRvCmd extends AbstractRvCmd {
    public static final int REQTYPE_INITIAL_REQUEST = 0x0001;
    public static final int REQTYPE_REDIRECT = 0x0002;

    public static final boolean FPRESENT_DEFAULT = true;

    private static final int TYPE_REQTYPE = 0x000a;
    private static final int TYPE_F = 0x000f;

    private final int reqType;
    private final boolean fPresent;

    public AbstractRequestRvCmd(RecvRvIcbm icbm) {
        super(icbm);

        TlvChain chain = getRvTlvs();

        reqType = chain.getUShort(TYPE_REQTYPE);
        fPresent = chain.hasTlv(TYPE_F);

        getMutableTlvs().removeTlvs(new int[] {
            TYPE_REQTYPE, TYPE_F
        });
    }

    protected AbstractRequestRvCmd(long icbmMessageId, CapabilityBlock cap) {
        this(icbmMessageId, cap, REQTYPE_INITIAL_REQUEST);
    }

    protected AbstractRequestRvCmd(long icbmMessageId, CapabilityBlock cap,
            int requestType) {
        this(icbmMessageId, cap, requestType, FPRESENT_DEFAULT);
    }

    protected AbstractRequestRvCmd(long icbmMessageId, CapabilityBlock cap,
            int reqType, boolean fPresent) {
        super(icbmMessageId, STATUS_REQUEST, cap);

        DefensiveTools.checkRange(reqType, "reqType", -1);

        this.reqType = reqType;
        this.fPresent = fPresent;
    }

    public final int getRequestType() { return reqType; }

    protected final boolean isFPresent() { return fPresent; }

    protected final void writeHeaderRvTlvs(OutputStream out)
            throws IOException {
        if (reqType != -1) {
            Tlv.getUShortInstance(TYPE_REQTYPE, reqType).write(out);
        }
        if (fPresent) {
            new Tlv(TYPE_F).write(out);
        }
    }
}
