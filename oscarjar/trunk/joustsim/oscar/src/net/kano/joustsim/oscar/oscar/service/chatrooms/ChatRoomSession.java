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

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.net.ClientConn;
import net.kano.joscar.rv.RvProcessor;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.InvitationMessage;
import net.kano.joscar.rvcmd.chatinvite.ChatInvitationRvCmd;
import net.kano.joscar.snaccmd.FullRoomInfo;
import net.kano.joscar.snaccmd.MiniRoomInfo;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.BasicConnection;
import net.kano.joustsim.oscar.oscar.OscarConnListener;
import net.kano.joustsim.oscar.oscar.OscarConnStateEvent;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.ServiceListener;

import java.util.Collections;
import java.util.Set;

public class ChatRoomSession {
  private AimConnection aimConnection;
  private BasicConnection connection;
  private ChatRoomService service;
  private FullRoomInfo roomInfo;
  private ChatInvitationImpl invitation;
  private ChatSessionState state = ChatSessionState.INITIALIZING;
  private CopyOnWriteArrayList<ChatRoomSessionListener> listeners
      = new CopyOnWriteArrayList<ChatRoomSessionListener>();

  public ChatRoomSession(AimConnection aimConnection) {
    this.aimConnection = aimConnection;
  }

  public AimConnection getAimConnection() {
    return aimConnection;
  }

  void setRoomInfo(FullRoomInfo roomInfo) {
    this.roomInfo = roomInfo;
  }

  public BasicConnection getConnection() {
    return connection;
  }

  public void addListener(ChatRoomSessionListener listener) {
    listeners.add(listener);
  }

  public void removeListener(ChatRoomSessionListener listener) {
    listeners.remove(listener);
  }

  void setConnection(BasicConnection connection) {
    connection.addGlobalServiceListener(new ServiceListener() {
      public void handleServiceReady(Service service) {
        if (service instanceof ChatRoomService) {
          ChatRoomService chatService = (ChatRoomService) service;
          chatService.addChatRoomListener(new ChatRoomServiceListener() {
            public void handleUsersJoined(ChatRoomService service,
                Set<ChatRoomUser> added) {
              for (ChatRoomSessionListener l : listeners) {
                l.handleUsersJoined(ChatRoomSession.this, added);
              }
            }

            public void handleUsersLeft(ChatRoomService service,
                Set<ChatRoomUser> removed) {
              for (ChatRoomSessionListener l : listeners) {
                l.handleUsersLeft(ChatRoomSession.this, removed);
              }
            }

            public void handleIncomingMessage(ChatRoomService service,
                ChatRoomUser user, ChatMessage message) {
              for (ChatRoomSessionListener l : listeners) {
                l.handleIncomingMessage(ChatRoomSession.this, user, message);
              }
            }
          });
          setState(ChatSessionState.INROOM);
        }
      }

      public void handleServiceFinished(Service service) {
      }
    });
    connection.addOscarListener(new OscarConnListener() {
      public void registeredSnacFamilies(OscarConnection conn) {
      }

      public void connStateChanged(OscarConnection conn,
          OscarConnStateEvent event) {
        ClientConn.State state = event.getClientConnEvent().getNewState();
        if (state == ClientConn.STATE_FAILED) {
          setState(ChatSessionState.FAILED);
        } else if (state == ClientConn.STATE_NOT_CONNECTED) {
          setState(ChatSessionState.CLOSED);
        }
      }

      public void allFamiliesReady(OscarConnection conn) {
      }
    });
    this.connection = connection;
  }

  public void close() {
    connection.disconnect();
  }

  public void sendMessage(String msg) throws EncodingException {
    service.sendMessage(msg);
  }

  void setInvitation(ChatInvitationImpl invitation) {
    this.invitation = invitation;
  }

  public ChatInvitation getInvitation() {
    return invitation;
  }

  ChatInvitationImpl getInvitationImpl() { return invitation; }

  public FullRoomInfo getRoomInfo() {
    return roomInfo;
  }

  void setService(ChatRoomService service) {
    this.service = service;
  }

  public synchronized ChatSessionState getState() {
    return state;
  }

  void setState(ChatSessionState state) {
    ChatSessionState oldState;
    synchronized (this) {
      oldState = this.state;
      if (state == oldState) return;
      this.state = state;
    }
    for (ChatRoomSessionListener listener : listeners) {
      listener.handleStateChange(this, oldState, state);
    }
  }

  @SuppressWarnings({"unchecked"})
  public Set<ChatRoomUser> getUsers() {
    if (service == null) return Collections.EMPTY_SET;
    return service.getUsers();
  }

  public void invite(Screenname screenname, String message) {
    RvProcessor rvProcessor = aimConnection.getIcbmService().getRvProcessor();
    RvSession session = rvProcessor.createRvSession(screenname.getNormal());
    session.sendRv(new ChatInvitationRvCmd(new MiniRoomInfo(roomInfo),
        message == null ? null: new InvitationMessage(message)));
  }
}
