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
 *  File created by keith @ Jan 17, 2004
 *
 */

package net.kano.joustsim.oscar.oscar.service.icbm;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.rv.NewRvSessionEvent;
import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvProcessor;
import net.kano.joscar.rv.RvProcessorListener;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rv.RvSessionListener;
import net.kano.joscar.rv.RvSnacResponseEvent;
import net.kano.joscar.rvcmd.DefaultRvCommandFactory;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snac.SnacRequestListener;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.WarningLevel;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joscar.snaccmd.icbm.IcbmCommand;
import net.kano.joscar.snaccmd.icbm.InstantMessage;
import net.kano.joscar.snaccmd.icbm.MissedMessagesCmd;
import net.kano.joscar.snaccmd.icbm.MissedMsgInfo;
import net.kano.joscar.snaccmd.icbm.ParamInfo;
import net.kano.joscar.snaccmd.icbm.ParamInfoCmd;
import net.kano.joscar.snaccmd.icbm.ParamInfoRequest;
import net.kano.joscar.snaccmd.icbm.RecvImIcbm;
import net.kano.joscar.snaccmd.icbm.RecvTypingNotification;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joscar.snaccmd.icbm.SendImIcbm;
import net.kano.joscar.snaccmd.icbm.SendTypingNotification;
import net.kano.joscar.snaccmd.icbm.SetParamInfoCmd;
import net.kano.joscar.snaccmd.icbm.TypingCmd;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.BuddyInfo;
import net.kano.joustsim.oscar.BuddyInfoManager;
import net.kano.joustsim.oscar.CapabilityHandler;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.AbstractService;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.DirectimConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.IncomingRvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionManager;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionManagerListener;
import net.kano.joustsim.oscar.oscar.service.icbm.secureim.EncryptedAimMessage;
import net.kano.joustsim.oscar.oscar.service.icbm.secureim.EncryptedAimMessageInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.secureim.InternalSecureTools;
import net.kano.joustsim.oscar.oscar.service.icbm.secureim.SecureAimConversation;
import net.kano.joustsim.trust.BuddyCertificateInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IcbmService extends AbstractService {
  private static final Logger LOGGER = Logger
      .getLogger(IcbmService.class.getName());
  //TODO(klea): what is max message size?
  private static final int MAX_MESSAGE_SIZE = 8000;

  private final RvProcessor rvProcessor;
  private final RvConnectionManager rvConnectionManager;

  private final CopyOnWriteArrayList<IcbmListener> listeners
      = new CopyOnWriteArrayList<IcbmListener>();
  private final BuddyInfoManager buddyInfoManager;

  private final Map<Screenname, SecureAimConversation> secureAimConvs
      = new HashMap<Screenname, SecureAimConversation>();

  private final Map<Screenname, Set<DirectimConversation>> directimconvs
      = new HashMap<Screenname, Set<DirectimConversation>>();

  private final Map<Screenname, ImConversation> imconvs
      = new HashMap<Screenname, ImConversation>();
  private Map<Message,Set<DirectimConversation>> sentConversations
      = new WeakHashMap<Message, Set<DirectimConversation>>();
  private Map<Message, Set<DirectimConversation>> failedConversations
      = new WeakHashMap<Message, Set<DirectimConversation>>();

  public IcbmService(AimConnection aimConnection,
      OscarConnection oscarConnection) {
    super(aimConnection, oscarConnection, IcbmCommand.FAMILY_ICBM);
    rvProcessor = new RvProcessor(oscarConnection.getSnacProcessor());
    rvProcessor.registerRvCmdFactory(new DefaultRvCommandFactory());
    rvProcessor.addListener(new DelegatingRvProcessorListener());
    rvConnectionManager = new RvConnectionManager(this);
    buddyInfoManager = getAimConnection().getBuddyInfoManager();

    rvConnectionManager.addConnectionManagerListener(
        new RvConnectionManagerListener() {
      public void handleNewIncomingConnection(RvConnectionManager manager,
          IncomingRvConnection connection) {
        if (connection instanceof DirectimConnection) {
          DirectimConnection dim = (DirectimConnection) connection;
          DirectimConversation conv = new DirectimConversation(
              getAimConnection(), dim);
          initializeDirectimConv(conv);
          synchronized (IcbmService.this) {
            getActualDirectimConversations(dim.getBuddyScreenname()).add(conv);
          }
          fireNewConversationEvent(conv);
        }
      }
    });
  }

  private void initializeDirectimConv(final DirectimConversation conv) {
    conv.initialize();
    conv.addConversationListener(new ConversationAdapter() {
      public void sentOtherEvent(Conversation conversation,
          ConversationEventInfo event) {
        if (event instanceof MessageQueuedEvent) {
          MessageQueuedEvent mqe = (MessageQueuedEvent) event;
          synchronized (IcbmService.this) {
            initializeMap(sentConversations, mqe.getMessage()).add(conv);
          }

        } else if (event instanceof SendFailedEvent) {
          SendFailedEvent event1 = (SendFailedEvent) event;

          Message msg = event1.getMessage();
          boolean fire = false;
          Set<Conversation> convs = null;
          synchronized (IcbmService.this) {
            Set<DirectimConversation> sent = sentConversations.get(msg);
            if (sent != null && sent.contains(conv)) {
              Set<DirectimConversation> failed = initializeMap(
                  failedConversations, msg);
              failed.add(conv);
              if (failed.containsAll(sent)) {
                // sending the message failed everywhere!
                fire = true;
                convs = DefensiveTools.<Conversation>getUnmodifiableSetCopy(
                    failed);
                sentConversations.remove(msg);
                failedConversations.remove(msg);
              }
            }
          }
          if (fire) {
            assert convs != null;
            for (IcbmListener l : listeners) {
              l.sendAutomaticallyFailed(IcbmService.this, msg, convs);
            }
          }
        }
      }
    });
  }

  private Set<DirectimConversation> initializeMap(
      Map<Message, Set<DirectimConversation>> map1, Message msg) {
    Set<DirectimConversation> convs = map1.get(msg);
    if (convs == null) {
      convs = new HashSet<DirectimConversation>();
      map1.put(msg, convs);
    }
    return convs;
  }

  private final Screenname getScreenname() {
    return getAimConnection().getScreenname();
  }

  public RvConnectionManager getRvConnectionManager() {
    return rvConnectionManager;
  }

  public void addIcbmListener(IcbmListener l) {
    listeners.addIfAbsent(l);
  }

  public void removeIcbmListener(IcbmListener l) {
    listeners.remove(l);
  }

  public SnacFamilyInfo getSnacFamilyInfo() {
    return IcbmCommand.FAMILY_INFO;
  }

  public void connected() {
    sendSnac(new ParamInfoRequest());
  }

  public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
    SnacCommand snac = snacPacketEvent.getSnacCommand();

    if (snac instanceof ParamInfoCmd) {
      ParamInfoCmd pic = (ParamInfoCmd) snac;

      handleParamInfo(pic);

    } else if (snac instanceof RecvImIcbm) {
      RecvImIcbm icbm = (RecvImIcbm) snac;
      handleImIcbm(icbm);

    } else if (snac instanceof MissedMessagesCmd) {
      MissedMessagesCmd mc = (MissedMessagesCmd) snac;
      handleMissedMessages(mc);

    } else if (snac instanceof RecvTypingNotification) {
      RecvTypingNotification typnot = (RecvTypingNotification) snac;
      handleTypingNotification(typnot);
    }
  }

  public RvProcessor getRvProcessor() { return rvProcessor; }

  private void handleParamInfo(ParamInfoCmd pic) {
    // we need to change from the default parameter infos to something
    // cooler, so we do it here
    ParamInfo pi = pic.getParamInfo();
    long newflags = pi.getFlags()
        | ParamInfo.FLAG_CHANMSGS_ALLOWED
        | ParamInfo.FLAG_MISSEDCALLS_ALLOWED
        | ParamInfo.FLAG_TYPING_NOTIFICATION;

    ParamInfo newparams = new ParamInfo(newflags, 8000,
        WarningLevel.getInstanceFromX10(999),
        WarningLevel.getInstanceFromX10(999), 0);

    sendSnac(new SetParamInfoCmd(newparams));

    setReady();
  }

  private void handleMissedMessages(MissedMessagesCmd mc) {
    List<MissedMsgInfo> msgs = mc.getMissedMsgInfos();
    for (MissedMsgInfo msg : msgs) {
      Screenname sn = new Screenname(msg.getUserInfo().getScreenname());
      ImConversation conv = getImConversation(sn);

      conv.handleMissedMsg(MissedImInfo.getInstance(getScreenname(),
          msg));
    }
  }

  private void handleImIcbm(RecvImIcbm icbm) {
    FullUserInfo senderInfo = icbm.getSenderInfo();
    if (senderInfo == null) return;
    Screenname sender = new Screenname(senderInfo.getScreenname());

    InstantMessage message = icbm.getMessage();
    if (message == null) return;

    if (message.isEncrypted()) {
      SecureAimConversation conv = getSecureAimConversation(sender);

      //noinspection Deprecation
      EncryptedAimMessage msg = InternalSecureTools
          .getEncryptedAimMessageInstance(icbm);
      if (msg == null) return;

      BuddyInfo info = buddyInfoManager.getBuddyInfo(sender);
      BuddyCertificateInfo certInfo = info.getCertificateInfo();

      EncryptedAimMessageInfo minfo = EncryptedAimMessageInfo.getInstance(
          getScreenname(), icbm, certInfo, new Date());
      if (minfo == null) return;

      //noinspection RedundantCast
      ((Conversation) conv).handleIncomingEvent(minfo);

    } else {
      ImConversation conv = getImConversation(sender);

      ImMessageInfo msg = ImMessageInfo.getInstance(getScreenname(),
          icbm, new Date());

      conv.handleIncomingEvent(msg);
    }
  }

  private void handleTypingNotification(RecvTypingNotification typnot) {
    Screenname sender = new Screenname(typnot.getScreenname());
    Conversation conv = getImConversation(sender);
    TypingState typingState = getTypingState(typnot.getTypingState());
    if (typingState != null) {
      conv.handleIncomingEvent(new TypingInfo(sender, getScreenname(),
          new Date(), typingState));
    }
  }

  private @Nullable static TypingState getTypingState(int typingState) {
    if (typingState == TypingCmd.STATE_TYPING) return TypingState.TYPING;
    if (typingState == TypingCmd.STATE_NO_TEXT) return TypingState.NO_TEXT;
    if (typingState == TypingCmd.STATE_PAUSED) return TypingState.PAUSED;
    LOGGER.log(Level.WARNING, "Unknown typing state " + typingState);
    return null;
  }

  private static int getTypingStateCode(@NotNull TypingState typingState) {
    if (typingState == TypingState.TYPING) return TypingCmd.STATE_TYPING;
    if (typingState == TypingState.PAUSED) return TypingCmd.STATE_PAUSED;
    if (typingState == TypingState.NO_TEXT) return TypingCmd.STATE_NO_TEXT;
    throw new IllegalArgumentException(
        "no code for typing state " + typingState);
  }

  public SecureAimConversation getSecureAimConversation(Screenname sn) {
    boolean isnew = false;
    SecureAimConversation conv;
    synchronized (this) {
      conv = secureAimConvs.get(sn);
      if (conv == null) {
        isnew = true;
        //noinspection Deprecation
        conv = InternalSecureTools.newSecureAimConversation(getAimConnection(), sn);
      }
    }
    // we need to initialize this outside of the lock to prevent deadlocks
    if (isnew) {
      //noinspection RedundantCast
      ((Conversation) conv).initialize();
      synchronized(this) {
        secureAimConvs.put(sn, conv);
      }
      fireNewConversationEvent(conv);
    }

    return conv;
  }

  private synchronized Set<DirectimConversation> getActualDirectimConversations(
      Screenname sn) {
    Set<DirectimConversation> convs = directimconvs.get(sn);
    if (convs == null) {
      convs = new HashSet<DirectimConversation>();
      directimconvs.put(sn, convs);
    }
    return convs;
  }

  public synchronized Set<DirectimConversation> getDirectimConversations(
      Screenname sn) {
    return DefensiveTools.getUnmodifiableSetCopy(getActualDirectimConversations(sn));
  }

  public ImConversation getImConversation(Screenname sn) {
    boolean isnew = false;
    ImConversation conv;
    synchronized (this) {
      conv = imconvs.get(sn);
      if (conv == null) {
        isnew = true;
        conv = new ImConversation(getAimConnection(), sn);
      }
    }
    // we need to initialize this outside of the lock to prevent deadlocks
    if (isnew) {
      conv.initialize();
      synchronized (this) {
        imconvs.put(sn, conv);
      }
      fireNewConversationEvent(conv);
    }

    return conv;
  }

  /**
   * This method sends to whichever conversation is appropriate.
   * <ol>
   * <li> If one or more direct IM conversations are pending or open, the message
   *    is sent to each of them
   * <li> If no DIM conversations are pending or open, but the message is a
   *    {@code DirectMessage}, a new DIM conversation is opened and the message
   *    is sent there
   * <li> If no DIM conversations are pending or open and the message is not a
   *    {@code DirectMessage}, the message is sent through the user's IM
   *    conversation (and one is created if none is not currently open)
   * </ol>
   */
  public void sendAutomatically(Screenname sn, Message message) {
    DirectimConversation newConv = null;
    synchronized (this) {
      boolean sent = false;
      for (DirectimConversation conv : getActualDirectimConversations(sn)) {
        if (!conv.isClosed()) {
          conv.sendMessage(message);
          sent = true;
        }
      }
      if (!sent) {
        if (message instanceof DirectMessage
            || message.getMessageBody().length() > MAX_MESSAGE_SIZE) {
          newConv = new DirectimConversation(getAimConnection(), sn);
          // we send the message later

        } else {
          getImConversation(sn).sendMessage(message);
        }
      }
    }
    if (newConv != null) {
      // we don't want to initialize the conversation within the lock
      initializeDirectimConv(newConv);
      // we don't want to call the new conversation event without having added
      // it to the list of dim conversations
      synchronized(this) {
        getActualDirectimConversations(sn).add(newConv);
      }
      fireNewConversationEvent(newConv);
      newConv.sendMessage(message);
    }
  }

  private void fireNewConversationEvent(Conversation conv) {
    for (IcbmListener listener : listeners) {
      listener.newConversation(this, conv);
    }
  }

  void sendIM(Screenname buddy, String body, boolean autoresponse) {
    sendIM(buddy, body, autoresponse, null);
  }

  void sendIM(Screenname buddy, String body, boolean autoresponse,
      SnacRequestListener listener) {
    sendIM(buddy, new InstantMessage(body), autoresponse, listener);
  }

  void sendIM(Screenname buddy, InstantMessage im, boolean autoresponse) {
    sendIM(buddy, im, autoresponse, null);
  }

  void sendIM(Screenname buddy, InstantMessage im, boolean autoresponse,
      SnacRequestListener listener) {
    // ackRequested must be false if autoresponse is true
    sendSnacRequest(new SendImIcbm(buddy.getFormatted(), im, autoresponse, 0,
        false, null, null, !autoresponse), listener);
  }

  void sendTypingStatus(Screenname buddy, TypingState typingState) {
    DefensiveTools.checkNull(typingState, "typingState");

    sendSnac(new SendTypingNotification(buddy.getFormatted(),
        getTypingStateCode(typingState)));
  }

  private class DelegatingRvProcessorListener implements RvProcessorListener {
    public void handleNewSession(NewRvSessionEvent event) {
      if (event.getSessionType() == NewRvSessionEvent.TYPE_INCOMING) {
        RvSession session = event.getSession();
        session.addListener(new SessionListenerDelegate(session));
      }
    }

  }

  private class SessionListenerDelegate implements RvSessionListener {
    private RendezvousSessionHandler sessionHandler;
    private final RvSession session;

    public SessionListenerDelegate(RvSession session) {
      this.session = session;
      sessionHandler = null;
    }

    public void handleRv(RecvRvEvent event) {
      if (sessionHandler == null) {
        RvCommand rvCommand = event.getRvCommand();
        if (rvCommand == null) {
          LOGGER.warning("unknown rv command: "
              + event.getSnacCommand());
          return;
        }
        CapabilityBlock cap = rvCommand.getCapabilityBlock();
        CapabilityHandler handler = getAimConnection().getCapabilityManager()
            .getCapabilityHandler(cap);
        if (handler instanceof RendezvousCapabilityHandler) {
          RendezvousCapabilityHandler rhandler
              = (RendezvousCapabilityHandler) handler;
          RendezvousSessionHandler sessionHandler = rhandler
              .handleSession(IcbmService.this, session);
          if (sessionHandler == null) {
            throw new IllegalStateException(
                "RendezvousCapabilityHandler " + rhandler
                    + " did not return session handler for " + session);
          }
          this.sessionHandler = sessionHandler;
        } else {
          LOGGER.warning("Rendezvous for " + cap
              + " ignored because the handler, " + handler
              + ", does not support rendezvous");
          session.removeListener(this);
          return;
        }
      }
      this.sessionHandler.handleRv(event);
    }

    public void handleSnacResponse(RvSnacResponseEvent event) {
      if (sessionHandler == null) {
        LOGGER.warning("Got SNAC response in RV processor "
            + "listener, but no handler has been registered "
            + "yet: " + event);
      } else {
        sessionHandler.handleSnacResponse(event);
      }
    }
  }
}
