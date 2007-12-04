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

import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacRequestAdapter;
import net.kano.joscar.snac.SnacRequestSentEvent;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snaccmd.error.SnacError;
import net.kano.joscar.snaccmd.icbm.MessageAck;
import net.kano.joscar.snaccmd.icbm.SendImIcbm;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.BuddyInfoTrackerListener;

import java.util.Date;

public class ImConversation
    extends Conversation implements TypingNotificationConversation {
  private final AimConnection conn;
  private final BuddyInfoTrackerListener tracker = new BuddyInfoTrackerListener() {
  };

  ImConversation(AimConnection conn, Screenname buddy) {
    super(buddy);
    this.conn = conn;

    setAlwaysOpen();
  }

  protected void opened() {
    conn.getBuddyInfoTracker().addTracker(getBuddy(), tracker);
  }

  protected void closed() {
    conn.getBuddyInfoTracker().removeTracker(getBuddy(), tracker);
  }

  public void sendMessage(final Message msg) throws ConversationException {
      sendMessage(msg, false);
  }

  public void sendMessage(final Message msg, boolean isOfflineMsg) throws ConversationException {
    IcbmService service = conn.getIcbmService();
    if (service == null) {
      throw new ConversationException("no ICBM service to send to", this);
    }
    ((MutableIcbmService) service).sendIM(getBuddy(), msg.getMessageBody(),
        msg.isAutoResponse(), new SnacRequestAdapter() {
      private boolean waitingForAck = false;
      public void handleSent(SnacRequestSentEvent e) {
        SnacCommand outCmd = e.getRequest().getCommand();
        if (outCmd instanceof SendImIcbm) {
          SendImIcbm imIcbm = (SendImIcbm) outCmd;

          if (imIcbm.isAckRequested()) {
            // we should wait for the ack from the server if an ack was
            // requested
            waitingForAck = true;
            return;
          }
        }
        fireMessageSentEvent(msg, conn.getScreenname());
      }

      public void handleResponse(SnacResponseEvent e) {
        SnacCommand snac = e.getSnacCommand();
        if (snac instanceof MessageAck) {
          if (waitingForAck) {
            fireMessageSentEvent(msg, conn.getScreenname());
            waitingForAck = false;
          }

        } else if (snac instanceof SnacError) {
          SnacError error = (SnacError) snac;
          Screenname mysn = conn.getScreenname();
          SendFailedEvent event = new ImSendFailedEvent(mysn,
              getBuddy(), error, msg);
          for (ConversationListener l : getListeners()) {
            l.gotOtherEvent(ImConversation.this, event);
          }
        }
      }
    }, isOfflineMsg);
    fireOutgoingEvent(ImMessageInfo.getInstance(conn.getScreenname(),
        getBuddy(), msg, new Date()));
  }

  protected void handleIncomingEvent(ConversationEventInfo event) {
    assert !Thread.holdsLock(this);

    super.handleIncomingEvent(event);
  }

  public void setTypingState(TypingState typingState) {
    ((MutableIcbmService) conn.getIcbmService()).sendTypingStatus(getBuddy(),
        typingState);
    fireOutgoingEvent(new TypingInfo(conn.getScreenname(), getBuddy(),
        new Date(), typingState));
  }

  public void handleMissedMsg(MissedImInfo info) {
    for (ConversationListener listener : getListeners()) {
      if (listener instanceof ImConversationListener) {
        ImConversationListener imlistener
            = (ImConversationListener) listener;

        imlistener.missedMessages(this, info);
      }
    }
  }
}
