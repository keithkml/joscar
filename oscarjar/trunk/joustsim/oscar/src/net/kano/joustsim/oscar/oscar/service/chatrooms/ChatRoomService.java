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
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.OscarTools;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snaccmd.FullRoomInfo;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.chat.ChatCommand;
import net.kano.joscar.snaccmd.chat.ChatMsg;
import net.kano.joscar.snaccmd.chat.RecvChatMsgIcbm;
import net.kano.joscar.snaccmd.chat.SendChatMsgIcbm;
import net.kano.joscar.snaccmd.chat.UsersJoinedCmd;
import net.kano.joscar.snaccmd.chat.UsersLeftCmd;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joustsim.JavaTools;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.AbstractService;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatRoomService extends AbstractService {
  private Set<ChatRoomUser> users = new HashSet<ChatRoomUser>();
  private CopyOnWriteArrayList<ChatRoomServiceListener> listeners
      = new CopyOnWriteArrayList<ChatRoomServiceListener>();
  private FullRoomInfo roomInfo;
  private ChatRoomMessageFactory messageFactory;
  private String roomName;

  public ChatRoomService(AimConnection aimConnection,
      OscarConnection oscarConnection,
      FullRoomInfo roomInfo) {
    super(aimConnection, oscarConnection,
        ChatCommand.FAMILY_CHAT);
    this.roomInfo = roomInfo;
    roomName = OscarTools.getRoomNameFromCookie(roomInfo.getCookie());
  }

  public void connected() {
    setReady();
  }

  public FullRoomInfo getRoomInfo() {
    return roomInfo;
  }

  public String getRoomName() { return roomName; }

  public SnacFamilyInfo getSnacFamilyInfo() {
    return ChatCommand.FAMILY_INFO;
  }

  public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
    SnacCommand cmd = snacPacketEvent.getSnacCommand();
    if (cmd instanceof UsersJoinedCmd) {
      UsersJoinedCmd joinedCmd = (UsersJoinedCmd) cmd;
      Set<ChatRoomUser> added = addUsers(joinedCmd.getUsers());
      List<Exception> exceptions = new ArrayList<Exception>();
      for (ChatRoomServiceListener listener : listeners) {
        try {
          listener.handleUsersJoined(this, added);
        } catch (Exception e) {
          exceptions.add(e);
        }
      }
      JavaTools.throwExceptions(exceptions, "Error while handling room "
          + "join listeners");

    } else if (cmd instanceof UsersLeftCmd) {
      UsersLeftCmd leftCmd = (UsersLeftCmd) cmd;
      List<FullUserInfo> users = leftCmd.getUsers();
      Set<ChatRoomUser> removed = removeUsers(users);

      List<Exception> exceptions = new ArrayList<Exception>();
      for (ChatRoomServiceListener listener : listeners) {
        try {
          listener.handleUsersLeft(this, removed);
        } catch (Exception e) {
          exceptions.add(e);
        }
      }
      JavaTools.throwExceptions(exceptions, "Error while handling room "
          + "left listeners");

    } else if (cmd instanceof RecvChatMsgIcbm) {
      RecvChatMsgIcbm msgIcbm = (RecvChatMsgIcbm) cmd;
      FullUserInfo senderInfo = msgIcbm.getSenderInfo();
      ChatRoomUser user = findChatRoomUser(senderInfo);
      if (user == null) {
        user = new ChatRoomUser(senderInfo);
      }
      ChatMsg message = msgIcbm.getMessage();
      ChatMessage ourMsg = messageFactory
          .createMessage(this, user, message);

      List<Exception> exceptions = new ArrayList<Exception>();
      for (ChatRoomServiceListener listener : listeners) {
        try {
          listener.handleIncomingMessage(this, user, ourMsg);
        } catch (Exception e) {
          exceptions.add(e);
        }
      }
      JavaTools.throwExceptions(exceptions, "Error while handling room "
          + "left listeners");
    }
  }

  public synchronized Set<ChatRoomUser> getUsers() {
    return DefensiveTools.getUnmodifiableSetCopy(users);
  }

  private synchronized Set<ChatRoomUser> removeUsers(
      List<FullUserInfo> users) {
    Set<ChatRoomUser> removed = new HashSet<ChatRoomUser>();
    for (FullUserInfo userInfo : users) {
      ChatRoomUser user = new ChatRoomUser(userInfo);
      if (this.users.remove(user)) removed.add(user);
    }
    return removed;
  }

  private @Nullable synchronized ChatRoomUser findChatRoomUser(
      FullUserInfo senderInfo) {
    for (ChatRoomUser user : users) {
      if (user.getScreenname().matches(senderInfo.getScreenname())) {
        return user;
      }
    }
    return null;
  }

  private synchronized Set<ChatRoomUser> addUsers(List<FullUserInfo> users) {
    Set<ChatRoomUser> added = new HashSet<ChatRoomUser>();
    for (FullUserInfo userInfo : users) {
      ChatRoomUser user = new ChatRoomUser(userInfo);
      if (this.users.add(user)) added.add(user);
    }
    return added;
  }

  public void addChatRoomListener(ChatRoomServiceListener listener) {
    listeners.add(listener);
  }

  public void removeChatRoomListener(ChatRoomServiceListener listener) {
    listeners.remove(listener);
  }

  void setMessageFactory(ChatRoomMessageFactory factory) {
    this.messageFactory = factory;
  }

  public void sendMessage(String message) throws EncodingException {
    sendSnac(new SendChatMsgIcbm(messageFactory.encodeMessage(message)));
  }
}
