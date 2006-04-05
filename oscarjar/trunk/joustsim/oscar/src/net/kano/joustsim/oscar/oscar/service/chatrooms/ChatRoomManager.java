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
import net.kano.joscar.OscarTools;
import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rv.RvSnacResponseEvent;
import net.kano.joscar.rvcmd.chatinvite.ChatInvitationRvCmd;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.FullRoomInfo;
import net.kano.joscar.snaccmd.MiniRoomInfo;
import net.kano.joscar.snaccmd.chat.ChatMsg;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AbstractCapabilityHandler;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.CapabilityManager;
import net.kano.joustsim.oscar.oscar.BasicConnection;
import net.kano.joustsim.oscar.oscar.NoBuddyKeysException;
import net.kano.joustsim.oscar.oscar.service.ServiceListener;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmService;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousCapabilityHandler;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousSessionHandler;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ChatRoomManager {
  private static final Logger LOGGER = Logger
      .getLogger(ChatRoomManager.class.getName());

  private final AimConnection aimConnection;

  private CopyOnWriteArrayList<ChatRoomManagerListener> listeners
      = new CopyOnWriteArrayList<ChatRoomManagerListener>();

  private Map<RoomDescriptor, ChatRoomSession> sessions
      = new HashMap<RoomDescriptor, ChatRoomSession>();
  private static final int DEFAULT_EXCHANGE = 4;

  public ChatRoomManager(AimConnection conn) {
    this.aimConnection = conn;
    conn.getCapabilityManager().setCapabilityHandler(CapabilityBlock.BLOCK_CHAT,
        new ChatInvitationCapabilityHandler());
    RoomFinderServiceArbiter arbiter = conn.getExternalServiceManager()
        .getChatRoomFinderServiceArbiter();
    arbiter.addRoomManagerServiceListener(new MyRoomFinderServiceListener());
  }

  public AimConnection getAimConnection() {
    return aimConnection;
  }

  public void addListener(ChatRoomManagerListener listener) {
    listeners.addIfAbsent(listener);
  }

  public void removeListener(ChatRoomManagerListener listener) {
    listeners.remove(listener);
  }

  void rejectInvitation(ChatInvitationImpl inv) {
    ChatInvitationImpl invitation = ensureGoodInvitation(inv);
    boolean wasRejected = !invitation.setRejected();
    if (wasRejected) return;
    invitation.getSession().sendResponse(RvCommand.RVSTATUS_DENY);
  }

  ChatRoomSession acceptInvitation(ChatInvitationImpl inv)
      throws IllegalArgumentException {
    ChatInvitationImpl chatInvitation = ensureGoodInvitation(inv);

    if (!chatInvitation.isAcceptable()) {
      throw new IllegalArgumentException("Chat invitation is not valid: "
          + inv);
    }

    ChatRoomSession session;
    FullRoomInfo info = chatInvitation.getRoomInfo();
    synchronized (this) {
      RoomDescriptor descriptor = new RoomDescriptor(
          chatInvitation.getRoomInfo());
      session = sessions.get(descriptor);
      if (session != null) return session;
      session = new ChatRoomSession(aimConnection);
      session.setRoomInfo(info);
      session.setInvitation(chatInvitation);
      sessions.put(descriptor, session);
    }

    LOGGER.fine("Attempting to accept invitation to join " + info);
    getChatArbiter().joinChatRoom(info);
    return session;
  }

  private ChatInvitationImpl ensureGoodInvitation(ChatInvitation inv) {
    if (!(inv instanceof ChatInvitationImpl)) {
      throw new IllegalArgumentException("Invitation was "
          + "not received by this chat manager: " + inv);
    }
    ChatInvitationImpl chatInvitation = (ChatInvitationImpl) inv;
    if (chatInvitation.getChatRoomManager() != this) {
      throw new IllegalArgumentException("Invitation was "
          + "not received by this chat manager: " + inv);
    }
    return chatInvitation;
  }

  public ChatRoomSession joinRoom(String name) {
    return joinRoom(DEFAULT_EXCHANGE, name);
  }

  public ChatRoomSession joinRoom(int exchange, String name) {
    ChatRoomSession session;
    synchronized (this) {
      RoomDescriptor descriptor = new RoomDescriptor(exchange,
          FullRoomInfo.INSTANCE_LAST, name);
      session = sessions.get(descriptor);
      if (session != null) return session;
      session = new ChatRoomSession(aimConnection);
      sessions.put(descriptor, session);
    }

    RoomFinderServiceArbiter arbiter = getChatArbiter();
    arbiter.joinChatRoom(exchange, name);

    return session;
  }

  private RoomFinderServiceArbiter getChatArbiter() {
    return aimConnection.getExternalServiceManager()
        .getChatRoomFinderServiceArbiter();
  }

  private class ChatInvitationCapabilityHandler
      extends AbstractCapabilityHandler
      implements RendezvousCapabilityHandler {
    public RendezvousSessionHandler handleSession(IcbmService service,
        RvSession session) {
      return new MyRendezvousSessionHandler(session);
    }

    public void handleAdded(CapabilityManager manager) {
    }

    public void handleRemoved(CapabilityManager manager) {
    }
  }

  private static class InvitationRecord {
    private Screenname inviter;
    private int exchange;
    private String roomName;
    private int instance;

    public InvitationRecord(Screenname screenname, int exchange,
        String roomName,
        int instance) {
      this.inviter = screenname;
      this.exchange = exchange;
      this.roomName = roomName;
      this.instance = instance;
    }

    public Screenname getInviter() {
      return inviter;
    }

    public int getExchange() {
      return exchange;
    }

    public String getRoomName() {
      return roomName;
    }

    public int getInstance() {
      return instance;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final InvitationRecord that = (InvitationRecord) o;

      if (exchange != that.exchange) return false;
      if (instance != that.instance) return false;
      if (!roomName.equals(that.roomName)) return false;
      if (!inviter.equals(that.inviter)) return false;

      return true;
    }

    public int hashCode() {
      int result = inviter.hashCode();
      result = 29 * result + exchange;
      result = 29 * result + roomName.hashCode();
      result = 29 * result + instance;
      return result;
    }
  }

  private Map<InvitationRecord, IncompleteInvitationInfo> pendingInvitations
      = new LinkedHashMap<InvitationRecord, IncompleteInvitationInfo>();
  private final Object pendingInvitationsLock = new Object();


  private InvitationRecord makeInvitationRecord(Screenname sn,
      MiniRoomInfo roomInfo) {
    return new InvitationRecord(sn, roomInfo.getExchange(),
        OscarTools.getRoomNameFromCookie(roomInfo.getCookie()),
        roomInfo.getInstance());
  }

  private void registerInvitation(IncompleteInvitationInfo invitation,
      FullRoomInfo roomInfo) {
    ChatInvitation ourInvitation;
    if (invitation.getSecurityInfo() == null) {
      ourInvitation = new ChatInvitationImpl(ChatRoomManager.this,
          invitation.getSession(), invitation.getSn(), roomInfo,
          invitation.getMsgString());

    } else if (invitation.getRoomKey() == null) {
      ourInvitation = new ChatInvitationImpl(ChatRoomManager.this,
          invitation.getSession(), invitation.getSn(), roomInfo,
          invitation.getReason(), invitation.getMsgString());

    } else {
      assert invitation.getBuddyCert() != null;
      ourInvitation = new ChatInvitationImpl(ChatRoomManager.this,
          invitation.getSession(), invitation.getSn(), roomInfo,
          invitation.getBuddyCert(), invitation.getRoomKey(),
          invitation.getMsgString());
    }

    LOGGER.fine("Firing invitation: " + ourInvitation);

    for (ChatRoomManagerListener listener : listeners) {
      listener.handleInvitation(ChatRoomManager.this, ourInvitation);
    }
  }

  private synchronized ChatRoomSession getSession(FullRoomInfo roomInfo) {
    ChatRoomSession session = sessions.get(new RoomDescriptor(roomInfo));
    if (session == null) {
      session = sessions.get(new RoomDescriptor(roomInfo.getExchange(),
          FullRoomInfo.INSTANCE_LAST, roomInfo.getName()));
    }
    return session;
  }


  private class MyRendezvousSessionHandler implements RendezvousSessionHandler {
    private RvSession session;

    public MyRendezvousSessionHandler(RvSession session) {
      this.session = session;
    }

    public void handleRv(RecvRvEvent event) {
      RvCommand cmd = event.getRvCommand();
      if (cmd instanceof ChatInvitationRvCmd) {
        ChatInvitationRvCmd invitation = (ChatInvitationRvCmd) cmd;
        Screenname sn = new Screenname(
            event.getRvSession().getScreenname());
        SecretKey roomKey = null;
        InvalidInvitationReason reason = null;
        X509Certificate buddyCert = null;
        ByteBlock securityInfo = invitation.getSecurityInfo();
        if (securityInfo != null) {
          try {
            buddyCert = KeyExtractionTools.getBuddySigningCert(sn,
                aimConnection.getBuddyInfoManager());
            //noinspection ConstantConditions
            assert buddyCert != null;
          } catch (NoBuddyKeysException e) {
            reason = InvalidInvitationReason.NO_BUDDY_KEYS;
          }
          try {
            roomKey = KeyExtractionTools.extractChatKey(securityInfo, buddyCert,
                aimConnection.getLocalPrefs());
            //noinspection ConstantConditions
            assert roomKey != null;
          } catch (NoPrivateKeyException e) {
            reason = InvalidInvitationReason.NO_LOCAL_KEYS;
          } catch (CertificateNotYetValidException e) {
            reason = InvalidInvitationReason.CERT_NOT_YET_VALID;
          } catch (CertificateExpiredException e) {
            reason = InvalidInvitationReason.CERT_EXPIRED;
          } catch (BadKeyException e) {
            reason = InvalidInvitationReason.INVALID_SIGNATURE;
          }
        }

        MiniRoomInfo roomInfo = invitation.getRoomInfo();
        String msgString = invitation.getInvMessage().getMessage();
        synchronized (pendingInvitationsLock) {
          pendingInvitations.put(makeInvitationRecord(sn, roomInfo),
              new IncompleteInvitationInfo(session, sn, roomInfo,
                  msgString, roomKey, securityInfo, reason, buddyCert));
        }
        LOGGER.info("Got invitation from " + sn + " to " + roomInfo + ": "
            + msgString + "; requesting more information");
        aimConnection.getExternalServiceManager()
            .getChatRoomFinderServiceArbiter().getRoomInfo(roomInfo);
      }
    }

    public void handleSnacResponse(RvSnacResponseEvent event) {
    }
  }

  private class MyRoomFinderServiceListener implements
      RoomFinderServiceListener {
    public void handleNewChatRoom(RoomFinderService service,
        FullRoomInfo roomInfo, BasicConnection connection) {
      ChatRoomSession session = getSession(roomInfo);
      assert session != null : "No session for " + roomInfo;
      session.setRoomInfo(roomInfo);
      session.setConnection(connection);
      session.setState(ChatSessionState.CONNECTING);

      LOGGER.fine("Opened new chat room connection for " + roomInfo.getName());

      connection.addGlobalServiceListener(new MyServiceListener(session));
    }
    public void handleRoomInfo(RoomFinderService service, MiniRoomInfo mini,
        FullRoomInfo info) {
      LOGGER.fine("Got room info for pending invitation: " + info);
      List<IncompleteInvitationInfo> uses
          = new ArrayList<IncompleteInvitationInfo>();
      synchronized (pendingInvitationsLock) {
        for (Iterator it = pendingInvitations.values().iterator();
            it.hasNext();) {
          IncompleteInvitationInfo invinfo = (IncompleteInvitationInfo) it.next();
          if (invinfo.getRoomInfo().isSameRoom(mini)) {
            it.remove();
            uses.add(invinfo);
          }
        }
      }
      for (IncompleteInvitationInfo use : uses) registerInvitation(use, info);
    }
  }

  private class MyServiceListener implements ServiceListener {
    private ChatRoomSession session;

    public MyServiceListener(ChatRoomSession session) {
      this.session = session;
    }

    public void handleServiceReady(Service service) {
      if (!(service instanceof ChatRoomService)) return;

      ChatRoomService roomService = (ChatRoomService) service;
      LOGGER.fine("Service for " + roomService.getRoomName() + " is ready");
      String contentType = roomService.getRoomInfo().getContentType();
      if (contentType.equals(ChatMsg.CONTENTTYPE_SECURE)) {
        roomService.setMessageFactory(
            new EncryptedChatRoomMessageFactory(aimConnection, roomService,
                session.getInvitationImpl().getRoomKey()));
      } else {
        if (!contentType.equals(ChatMsg.CONTENTTYPE_DEFAULT)) {
          LOGGER.warning("Chat room " + session.getRoomInfo().getName()
              + " has unknown content type: " + contentType);
        }
        roomService.setMessageFactory(new PlainChatRoomMessageFactory());
      }
      session.setService(roomService);
    }

    public void handleServiceFinished(Service service) {
    }
  }

  private static class RoomDescriptor {
    private int exchange;
    private int instance;
    private @NotNull String name;

    public RoomDescriptor(int exchange, int instance, @NotNull String name) {
      this.exchange = exchange;
      this.instance = instance;
      this.name = name;
    }

    public RoomDescriptor(FullRoomInfo roomInfo) {
      this(roomInfo.getExchange(), roomInfo.getInstance(), roomInfo.getName());
    }

    public int getExchange() {
      return exchange;
    }

    public int getInstance() {
      return instance;
    }

    public String getName() {
      return name;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final RoomDescriptor that = (RoomDescriptor) o;

      if (exchange != that.exchange) return false;
      if (instance != that.instance) return false;
      if (name != null ? !name.equals(that.name) : that.name != null) {
        return false;
      }

      return true;
    }

    public int hashCode() {
      int result = exchange;
      result = 29 * result + instance;
      result = 29 * result + (name != null ? name.hashCode() : 0);
      return result;
    }
  }
}
