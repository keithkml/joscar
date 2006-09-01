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

import net.kano.joscar.ByteBlock;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacRequestAdapter;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snaccmd.FullRoomInfo;
import net.kano.joscar.snaccmd.MiniRoomInfo;
import net.kano.joscar.snaccmd.chat.ChatCommand;
import net.kano.joscar.snaccmd.conn.ConnCommand;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joscar.snaccmd.rooms.JoinRoomCmd;
import net.kano.joscar.snaccmd.rooms.RoomCommand;
import net.kano.joscar.snaccmd.rooms.RoomResponse;
import net.kano.joscar.snaccmd.rooms.RoomRightsRequest;
import net.kano.joscar.snaccmd.rooms.RoomInfoReq;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.ExternalServiceManager;
import net.kano.joustsim.oscar.oscar.BasicConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.AbstractService;
import net.kano.joustsim.oscar.oscar.service.ServiceFactory;
import net.kano.joustsim.oscar.oscar.service.MutableService;
import net.kano.joustsim.oscar.oscar.service.bos.ExternalBosServiceImpl;
import net.kano.joustsim.oscar.oscar.service.bos.OpenedChatRoomServiceListener;
import net.kano.joustsim.oscar.oscar.service.bos.MainBosService;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public class RoomFinderService extends AbstractService {
  private static final Logger LOGGER = Logger
      .getLogger(RoomFinderService.class.getName());

  private @Nullable Integer maxRoomsAllowed = null;

  public RoomFinderService(AimConnection aimConnection,
      OscarConnection oscarConnection) {
    super(aimConnection, oscarConnection, RoomCommand.FAMILY_ROOM);
  }

  public void connected() {
    sendSnacRequest(new RoomRightsRequest(), new SnacRequestAdapter() {
      public void handleResponse(SnacResponseEvent e) {
        SnacCommand cmd = e.getSnacCommand();
        if (cmd instanceof RoomResponse) {
          RoomResponse response = (RoomResponse) cmd;
          int max = response.getMaxRooms();
          if (max != -1) {
            synchronized (RoomFinderService.this) {
              maxRoomsAllowed = max;
            }
          }
          setReady();
        }
      }
    });
  }

  public synchronized @Nullable Integer getMaxRoomsAllowed() {
    return maxRoomsAllowed;
  }

  public void joinChatRoom(int exchange, String name) {
    FullRoomInfo roomInfo = new FullRoomInfo(exchange, name);
    JoinRoomCmd cmd = new JoinRoomCmd(roomInfo);
    sendSnacRequest(cmd, new SnacRequestAdapter() {
      public void handleResponse(SnacResponseEvent e) {
        SnacCommand cmd = e.getSnacCommand();
        if (cmd instanceof RoomResponse) {
          RoomResponse response = (RoomResponse) cmd;
          FullRoomInfo roominfo = response.getRoomInfo();
          LOGGER.fine("Got join-room response for " + roominfo
              + "; connecting to chat room");
          joinChatRoom(roominfo);

        } else {
          LOGGER.warning("Got unknown response to JoinRoomCmd: " + cmd);
        }
      }
    });
  }

  public void joinChatRoom(FullRoomInfo roomInfo) {
    getAimConnection().getBosService().requestChatService(
        roomInfo, new MyOpenedChatRoomServiceListener(roomInfo));
  }

  private CopyOnWriteArrayList<RoomFinderServiceListener> listeners
      = new CopyOnWriteArrayList<RoomFinderServiceListener>();


  public SnacFamilyInfo getSnacFamilyInfo() { return RoomCommand.FAMILY_INFO; }

  public void addRoomManagerServiceListener(
      RoomFinderServiceListener listener) {
    listeners.add(listener);
  }

  public void removeRoomManagerServiceListener(
      RoomFinderServiceListener listener) {
    listeners.remove(listener);
  }

  public void requestRoomInfo(final MiniRoomInfo roomInfo) {
    sendSnacRequest(new RoomInfoReq(roomInfo), new SnacRequestAdapter() {
      public void handleResponse(SnacResponseEvent e) {
        SnacCommand cmd = e.getSnacCommand();
        if (cmd instanceof RoomResponse) {
          RoomResponse roomResponse = (RoomResponse) cmd;

          LOGGER.finer("Got room info for " + roomResponse.getRoomInfo());

          for (RoomFinderServiceListener listener : listeners) {
            listener.handleRoomInfo(RoomFinderService.this, roomInfo,
                roomResponse.getRoomInfo());
          }
        }
        setReady();
      }
    });
  }

  private class ChatRoomServiceFactory implements ServiceFactory {
    private FullRoomInfo roomInfo;

    public ChatRoomServiceFactory(FullRoomInfo roomInfo) {
      this.roomInfo = roomInfo;
    }

    public MutableService getService(OscarConnection conn, int family) {
      if (family == ConnCommand.FAMILY_CONN) {
        return new ExternalBosServiceImpl(getAimConnection(), conn);
      } else if (family == ChatCommand.FAMILY_CHAT) {
        return new ChatRoomService(getAimConnection(), conn, roomInfo);
      } else {
        return null;
      }
    }
  }

  private class MyOpenedChatRoomServiceListener
      implements OpenedChatRoomServiceListener {
    private final FullRoomInfo roomInfo;

    public MyOpenedChatRoomServiceListener(FullRoomInfo fullRoomInfo) {
      this.roomInfo = fullRoomInfo;
    }

    public void handleChatRoomRedirect(MainBosService service,
        FullRoomInfo roomInfo, String host, int port,
        ByteBlock flapCookie) {
      LOGGER.fine("Got chat room redirect for " + roomInfo.getName() + ": "
          + host + ":" + port);

      BasicConnection conn = new BasicConnection(host, ExternalServiceManager.fixPort(port));
      conn.getClientFlapConn().setSocketFactory(getAimConnection().getProxy().getSocketFactory());
      conn.setCookie(flapCookie);
      conn.setServiceFactory(new ChatRoomServiceFactory(this.roomInfo));
      for (RoomFinderServiceListener listener : listeners) {
        listener.handleNewChatRoom(RoomFinderService.this, this.roomInfo, conn);
      }
      conn.connect();
    }
  }
}