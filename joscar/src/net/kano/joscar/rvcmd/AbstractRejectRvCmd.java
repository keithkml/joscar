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
 *  File created by Keith @ 4:27:21 AM
 *
 */

package net.kano.joscar.rvcmd;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.MiscTools;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.tlv.MutableTlvChain;
import net.kano.joscar.tlv.Tlv;

import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractRejectRvCmd extends AbstractRvCmd {
    public static final int REJECTCODE_CANCELLED = 0x0001;

    private static final int TYPE_REJECTCODE = 0x000b;

    private final int rejectCode;

    public AbstractRejectRvCmd(RecvRvIcbm icbm) {
        super(icbm);

        MutableTlvChain chain = getMutableTlvs();

        this.rejectCode = chain.getUShort(TYPE_REJECTCODE);

        chain.removeTlvs(new int[] {
            TYPE_REJECTCODE,
        });
    }

    public AbstractRejectRvCmd(long icbmMessageId, CapabilityBlock cap,
            int rejectionCode) {
        super(icbmMessageId, RVSTATUS_DENY, cap);

        DefensiveTools.checkRange(rejectionCode, "rejectionCode", -1);

        this.rejectCode = rejectionCode;
    }

    public final int getRejectCode() { return rejectCode; }

    protected final void writeHeaderRvTlvs(OutputStream out)
            throws IOException {
        if (rejectCode != -1) {
            Tlv.getUShortInstance(TYPE_REJECTCODE, rejectCode).write(out);
        }
    }

    protected void writeRvTlvs(OutputStream out) throws IOException { }

    public String toString() {
        return MiscTools.getClassName(this) + ": rejectCode=" + rejectCode;
    }
}