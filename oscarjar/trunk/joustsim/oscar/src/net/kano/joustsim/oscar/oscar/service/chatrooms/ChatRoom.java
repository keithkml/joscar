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

import net.kano.joscar.snaccmd.FullRoomInfo;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.ServiceListener;
import net.kano.joustsim.oscar.oscar.service.Service;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChatRoom {
    private AimConnection aimConnection;
    private FullRoomInfo roomInfo;
    private OscarConnection oscarConnection;
    private ChatRoomService chatRoomService;

    private CopyOnWriteArrayList<ChatRoomListener> listeners
            = new CopyOnWriteArrayList<ChatRoomListener>();
    private ChatRoomServiceListener delegatingRoomListener
            = new DelegatingRoomListener();
    private ChatInvitation invitation = null;

    ChatRoom(AimConnection aimConnection, FullRoomInfo roomInfo,
            OscarConnection oscarConnection) {
        this.aimConnection = aimConnection;
        this.roomInfo = roomInfo;
        this.oscarConnection = oscarConnection;
        oscarConnection.addGlobalServiceListener(new ServiceListener() {
            public void handleServiceReady(Service service) {
                if (service instanceof ChatRoomService) {
                    ChatRoomService chatService = (ChatRoomService) service;
                    chatRoomService = chatService;
                    chatService.addChatRoomListener(
                            delegatingRoomListener);
                }
            }

            public void handleServiceFinished(Service service) {
            }
        });
    }

    public void addChatRoomListener(ChatRoomListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeChatRoomListener(ChatRoomListener listener) {
        listeners.remove(listener);
    }

    public AimConnection getAimConnection() {
        return aimConnection;
    }

    public OscarConnection getOscarConnection() {
        return oscarConnection;
    }

    public String getRoomName() {
        return roomInfo.getRoomName();
    }

    public int getRoomExchange() {
        return roomInfo.getExchange();
    }

    public @Nullable ChatRoomService getChatRoomService() {
        return chatRoomService;
    }

    public void setInvitation(ChatInvitation invitation) {
        this.invitation = invitation;
    }

    public @Nullable ChatInvitation getInvitation() {
        return invitation;
    }

    private class DelegatingRoomListener implements ChatRoomServiceListener {
        public void handleUsersJoined(ChatRoomService service,
                List<ChatRoomUser> added) {
            for (ChatRoomListener listener : listeners) {
                listener.handleUsersJoined(ChatRoom.this, added);
            }
        }

        public void handleUsersLeft(ChatRoomService service,
                List<ChatRoomUser> removed) {
            for (ChatRoomListener listener : listeners) {
                listener.handleUsersLeft(ChatRoom.this,
                        removed);
            }
        }

        public void handleIncomingMessage(ChatRoomService service,
                ChatRoomUser user, ChatMessage message) {
            for (ChatRoomListener listener : listeners) {
                listener.handleIncomingMessage(ChatRoom.this, user, message);
            }
        }
    }
}
