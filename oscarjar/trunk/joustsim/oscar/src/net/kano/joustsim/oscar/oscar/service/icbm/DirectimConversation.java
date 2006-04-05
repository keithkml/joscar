/*
 * Copyright (c) 2006, The Joust Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 * - Neither the name of the Joust Project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * File created by keithkml
 */

package net.kano.joustsim.oscar.oscar.service.icbm;

import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.AimSession;
import net.kano.joustsim.oscar.AimSessionListener;
import net.kano.joustsim.oscar.BuddyInfoTracker;
import net.kano.joustsim.oscar.BuddyInfoTrackerListener;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.Attachment;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.BuddyTypingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.DirectimConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.DirectimController;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.DoneReceivingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.OutgoingDirectimConnectionImpl;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.ReceivedAttachmentEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.ReceivedMessageEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.SentCompletePacketEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.IncomingRvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionEventListener;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionState;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class DirectimConversation extends Conversation
    implements TypingNotificationConversation {
  private static final Logger LOGGER = Logger
      .getLogger(DirectimConversation.class.getName());

  private final AimConnection conn;
  private DirectimConnection directim = null;
  private @Nullable RvConnectionEvent closingState = null;
  private BuddyInfoTracker oldTracker = null;
  private final BuddyInfoTrackerListener trackListener = new BuddyInfoTrackerListener() {
  };
  private LinkedList<Message> queue = new LinkedList<Message>();

  public DirectimConversation(AimConnection conn, DirectimConnection directim) {
    super(directim.getBuddyScreenname());
    this.conn = conn;
    this.directim = directim;
    registerConnection(directim);
  }

  public DirectimConversation(AimConnection conn, Screenname buddy) {
    super(buddy);
    this.conn = conn;
  }

  protected void opened() {
    conn.getAimSession().addSessionListener(new AimSessionListener() {
      public void handleOpenedConnection(AimSession aimSession,
          AimConnection conn) {
        synchronized (DirectimConversation.this) {
          clearOldTracker();
          oldTracker = conn.getBuddyInfoTracker();
          oldTracker.addTracker(getBuddy(), trackListener);
        }
      }
    });
  }

  private synchronized void clearOldTracker() {
    if (oldTracker != null) {
      oldTracker.removeTracker(getBuddy(), trackListener);
      oldTracker = null;
    }
  }

  public synchronized DirectimConnection getDirectimConnection() {
    return directim;
  }

  public @Nullable synchronized RvConnectionEvent getClosingEvent() {
    return closingState;
  }

  public boolean open() {
    OutgoingDirectimConnectionImpl directim;
    synchronized (this) {
      if (this.directim != null) {
        if (this.directim instanceof IncomingRvConnection) {
          IncomingRvConnection in = (IncomingRvConnection) this.directim;
          try {
            in.accept();
          } catch (IllegalStateException e) {
            LOGGER.warning("Couldn't accept DIM connection: " + e);
          }
        }
        return false;
      }

      LOGGER.fine("Opening dim connection to " + getBuddy());
      directim = conn.getIcbmService().getRvConnectionManager()
          .openDirectimConnection(getBuddy());
      registerConnection(directim);
      this.directim = directim;
    }

    directim.sendRequest();
    return false;
  }

  private synchronized void registerConnection(DirectimConnection directim) {
    directim.addEventListener(new DirectimEventListener());
    updateState(directim.getState(), null);
  }

  private void updateState(RvConnectionState state,
      @Nullable RvConnectionEvent event) {
    if (state == RvConnectionState.CONNECTED) {
      DirectimController controller = getDirectimConnection().getDirectimController();
      assert controller != null;
      synchronized (this) {
        for (Message msg : queue) controller.sendMessage(msg);
        queue.clear();
      }
      super.open();

    } else if (state == RvConnectionState.FAILED
        || state == RvConnectionState.FINISHED) {

      synchronized (this) {
        if (closingState == null) {
          closingState = event;
        }
      }
      super.close();
    }
  }

  protected void closed() {
    List<Message> failed;
    synchronized (this) {
      if (directim != null) {
        directim.close();
        directim = null;
      }
      clearOldTracker();
      failed = new ArrayList<Message>(queue);
      queue.clear();
    }
    for (Message message : failed) {
      fireOutgoingEvent(new SendFailedEvent(conn.getScreenname(), getBuddy(),
          message));
    }
  }

  public void sendMessage(Message msg) throws ConversationException {
    fireOutgoingEvent(new MessageQueuedEvent(msg, conn.getScreenname(),
        DirectimConversation.this.getBuddy()));
    DirectimController controller;
    synchronized (this) {
      controller = directim == null ? null : directim.getDirectimController();
      if (controller == null) {
        LOGGER.info("Queueing message until DIM opens: " + msg);
        queue.add(msg);
        return;
      }
    }
    LOGGER.fine("Sending message over dim: " + msg);
    controller.sendMessage(msg);
    fireOutgoingEvent(ImMessageInfo.getInstance(conn.getScreenname(),
        getBuddy(), msg, new Date()));
  }

  public void setTypingState(TypingState typingState) {
    DirectimConnection directim;
    synchronized (this) {
      checkOpen();
      directim = this.directim;
    }
    DirectimController controller = directim.getDirectimController();
    if (controller == null) {
      throw new ConversationNotOpenException(this);
    }
    controller.setTypingState(typingState);
    fireOutgoingEvent(new TypingInfo(conn.getScreenname(), getBuddy(),
        new Date(), typingState));
  }

  private class DirectimEventListener implements RvConnectionEventListener {
    private ReceivedMessageEvent lastMsg = null;
    private List<ReceivedAttachmentEvent> attachments = null;

    public void handleEventWithStateChange(RvConnection transfer,
        RvConnectionState state, RvConnectionEvent event) {
      LOGGER.fine("Directim for conversation changed to state: " + state);
      updateState(state, event);
    }

    public void handleEvent(RvConnection transfer, RvConnectionEvent event) {
      if (event instanceof BuddyTypingEvent) {
        BuddyTypingEvent tevent = (BuddyTypingEvent) event;
        LOGGER.finer("Got incoming typing state in conversation: " + tevent);
        fireIncomingEvent(new TypingInfo(getBuddy(), getBuddy(), new Date(),
            tevent.getState()));

      } else if (event instanceof ReceivedMessageEvent) {
        ReceivedMessageEvent revent = (ReceivedMessageEvent) event;
        setLastMessage(revent);
        LOGGER.finer("Got incoming DIM message in conversation: " + revent);

      } else if (event instanceof ReceivedAttachmentEvent) {
        ReceivedAttachmentEvent revent = (ReceivedAttachmentEvent) event;
        LOGGER.finer("Got attachment " + revent.getId() + " in conversation");
        synchronized (this) {
          assert attachments != null : event;
          attachments.add(revent);
        }

      } else if (event instanceof DoneReceivingEvent) {
        ReceivedMessageEvent lastMsg;
        List<ReceivedAttachmentEvent> attachments;
        synchronized (this) {
          lastMsg = this.lastMsg;
          attachments = this.attachments;
          this.lastMsg = null;
          this.attachments = null;
        }

        Date date = new Date();
        DirectMessage msg = new DirectMessage(lastMsg.getMessage(),
            lastMsg.isAutoResponse(), buildAttachmentList(attachments));
        fireIncomingEvent(ImMessageInfo.getInstance(getBuddy(),
            conn.getScreenname(), msg, date));

      } else if (event instanceof SentCompletePacketEvent) {
        SentCompletePacketEvent scpe = (SentCompletePacketEvent) event;
        fireMessageSentEvent(scpe.getMessage(), conn.getScreenname());
      }
    }

    private synchronized void setLastMessage(ReceivedMessageEvent revent) {
      assert lastMsg == null;
      assert attachments == null;
      lastMsg = revent;
      attachments = new ArrayList<ReceivedAttachmentEvent>();
    }

    private Set<Attachment> buildAttachmentList(
        List<ReceivedAttachmentEvent> attachments) {
      Set<Attachment> map = new HashSet<Attachment>();
      for (ReceivedAttachmentEvent ev : attachments) {
        map.add(ev.getAttachment());
      }
      return map;
    }
  }

}
