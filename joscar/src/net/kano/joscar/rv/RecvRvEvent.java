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

package net.kano.joscar.rv;

import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snaccmd.icbm.RvCommand;

public class RecvRvEvent extends SnacPacketEvent {
    public static final Object TYPE_RV = "TYPE_RV";
    public static final Object TYPE_RESPONSE = "TYPE_RESPONSE";

    private final Object type;

    private final RvProcessor rvProcessor;
    private final RvSession rvSession;

    private final RvCommand rvCommand;
    private final int responseCode;

    protected RecvRvEvent(SnacPacketEvent other,
            RvProcessor processor, RvSession session, RvCommand command) {
        this(TYPE_RV, other, processor, session, command, -1);
    }

    protected RecvRvEvent(SnacPacketEvent other,
            RvProcessor processor, RvSession session, int resultCode) {
        this(TYPE_RV, other, processor, session, null, resultCode);
    }

    private RecvRvEvent(Object type, SnacPacketEvent other,
            RvProcessor rvProcessor, RvSession rvSession, RvCommand rvCommand,
            int responseCode) {
        super(other);

        this.type = type;
        this.rvProcessor = rvProcessor;
        this.rvSession = rvSession;
        this.rvCommand = rvCommand;
        this.responseCode = responseCode;
    }

    public final Object getType() { return type; }

    public final RvProcessor getRvProcessor() { return rvProcessor; }

    public final RvSession getRvSession() { return rvSession; }

    public final RvCommand getRvCommand() { return rvCommand; }

    public final int getResponseCode() { return responseCode; }

    public String toString() {
        return "RecvRvEvent: " +
                (type == TYPE_RV
                ? "rvCommand=" + rvCommand
                : "responseCode=" + responseCode);
    }
}
