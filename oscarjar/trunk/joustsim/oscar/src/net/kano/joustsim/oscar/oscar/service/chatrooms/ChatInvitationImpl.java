/*
 *  Copyright (c) 2005, The Joust Project
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
 */

package net.kano.joustsim.oscar.oscar.service.chatrooms;

import net.kano.joustsim.Screenname;
import net.kano.joscar.snaccmd.MiniRoomInfo;
import net.kano.joscar.OscarTools;

import javax.crypto.SecretKey;
import java.security.cert.X509Certificate;

class ChatInvitationImpl implements ChatInvitation {
    private Screenname screenname;
    private MiniRoomInfo roomInfo;
    private String message;
    private SecretKey roomKey = null;
    private InvalidInvitationReason invalidReason = null;
    private X509Certificate buddySignature = null;
    private final boolean secure;
    private ChatRoomManager chatRoomManager;

    public ChatInvitationImpl(ChatRoomManager chatRoomManager,
            Screenname screenname, MiniRoomInfo roomInfo,
            String message) {
        this(chatRoomManager, screenname, roomInfo, message, false);
    }

    private ChatInvitationImpl(ChatRoomManager chatRoomManager,
            Screenname screenname, MiniRoomInfo roomInfo,
            String message, boolean secure) {
        this.chatRoomManager = chatRoomManager;
        this.screenname = screenname;
        this.roomInfo = roomInfo;
        this.message = message;
        this.secure = secure;
    }

    public ChatInvitationImpl(ChatRoomManager chatRoomManager,
            Screenname screenname, MiniRoomInfo roomInfo,
            X509Certificate buddySignature, SecretKey roomKey,
            String message) {
        this(chatRoomManager, screenname, roomInfo, message, true);

        this.roomKey = roomKey;
        this.buddySignature = buddySignature;
    }

    public ChatInvitationImpl(ChatRoomManager chatRoomManager,
            Screenname screenname, MiniRoomInfo roomInfo,
            InvalidInvitationReason reason, String message) {
        this(chatRoomManager, screenname, roomInfo, message, true);
        this.invalidReason = reason;
    }

    public Screenname getScreenname() {
        return screenname;
    }

    public MiniRoomInfo getRoomInfo() {
        return roomInfo;
    }

    public int getRoomExchange() {
        return getRoomInfo().getExchange();
    }

    public String getRoomName() {
        return OscarTools.getRoomNameFromCookie(getRoomInfo().getCookie());
    }

    public X509Certificate getBuddySignature() {
        return buddySignature;
    }

    public SecretKey getRoomKey() {
        return roomKey;
    }

    public String getMessage() {
        return message;
    }

    public InvalidInvitationReason getInvalidReason() {
        return invalidReason;
    }

    public ChatRoomManager getChatRoomManager() {
        return chatRoomManager;
    }

    public boolean isValid() {
        return !isForSecureChatRoom() || roomKey != null;
    }

    public boolean isForSecureChatRoom() {
        return secure;
    }
}
