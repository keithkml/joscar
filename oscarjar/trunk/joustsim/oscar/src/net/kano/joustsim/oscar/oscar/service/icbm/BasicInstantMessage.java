/*
 *  Copyright (c) 2004, The Joust Project
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
 *  File created by keith @ Jan 26, 2004
 *
 */

package net.kano.joustsim.oscar.oscar.service.icbm;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.snaccmd.icbm.InstantMessage;
import net.kano.joscar.snaccmd.icbm.RecvImIcbm;

import java.util.Collection;

public class BasicInstantMessage extends SimpleMessage {
    static BasicInstantMessage getInstance(RecvImIcbm icbm) {
        InstantMessage message = icbm.getMessage();
        if (message == null) return null;

        String msg = message.getMessage();
        if (msg == null) return null;

        boolean ar = icbm.isAutoResponse();

        String aimexp = extractAimExpressionName(icbm.getAimExpressionInfo());

        return new BasicInstantMessage(msg, ar, aimexp);
    }

    private static String extractAimExpressionName(Collection<? extends ExtraInfoBlock> aimexpInfo) {
        if (aimexpInfo == null) return null;

        String aimexp = null;

        for (ExtraInfoBlock infoBlock : aimexpInfo) {
            int blockType = infoBlock.getType();
            if (blockType != ExtraInfoBlock.TYPE_AIMEXPINFO_A
                    && blockType != ExtraInfoBlock.TYPE_AIMEXPINFO_B) {
                continue;
            }

            ByteBlock data = infoBlock.getExtraData().getData();
            if (data.getLength() > 1) {
                aimexp = BinaryTools.getAsciiString(data.subBlock(1));
                break;
            }
        }

        return aimexp;
    }

    private final String aimexp;

    public BasicInstantMessage(String messageBody) {
        this(messageBody, false);
    }

    public BasicInstantMessage(String messageBody, boolean autoResponse) {
        this(messageBody, autoResponse, null);
    }

    public BasicInstantMessage(String msg, boolean ar, String aimexp) {
        super(msg, ar);

        this.aimexp = aimexp;
    }

    public final String getAimexp() { return aimexp; }
}
