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

package net.kano.joscar.rvcmd.chatinvite;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.rvcmd.AbstractRequestRvCmd;
import net.kano.joscar.rvcmd.InvitationMessage;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.MiniRoomInfo;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.tlv.TlvChain;

import java.io.IOException;
import java.io.OutputStream;

public class ChatInvitationRvCmd extends AbstractRequestRvCmd {
    private final InvitationMessage invMessage;
    private final MiniRoomInfo roomInfo;

    public ChatInvitationRvCmd(RecvRvIcbm icbm) {
        super(icbm);

        TlvChain chain = getRvTlvs();

        invMessage = InvitationMessage.readInvitationMessage(chain);

        ByteBlock serviceData = getServiceData();
        if (serviceData == null) roomInfo = null;
        else roomInfo = MiniRoomInfo.readMiniRoomInfo(serviceData);
    }

    public ChatInvitationRvCmd(long icbmMessageId, MiniRoomInfo roomInfo,
            InvitationMessage message) {
        super(icbmMessageId, CapabilityBlock.BLOCK_CHAT);

        DefensiveTools.checkNull(roomInfo, "roomInfo");

        this.roomInfo = roomInfo;
        this.invMessage = message;
    }

    public final InvitationMessage getInvMessage() {
        return invMessage;
    }

    public final MiniRoomInfo getRoomInfo() { return roomInfo; }

    protected void writeRvTlvs(OutputStream out) throws IOException {
        if (invMessage != null) invMessage.write(out);
    }

    protected void writeServiceData(OutputStream out) throws IOException {
        roomInfo.write(out);
    }
}
