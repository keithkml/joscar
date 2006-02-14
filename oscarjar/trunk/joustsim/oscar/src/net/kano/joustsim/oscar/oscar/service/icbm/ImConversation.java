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

import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AimConnection;

import java.util.Date;

public class ImConversation extends Conversation implements
    TypingNotificationConversation {
  private final AimConnection conn;

  ImConversation(AimConnection conn, Screenname buddy) {
    super(buddy);
    this.conn = conn;

    setAlwaysOpen();
  }

  public void sendMessage(Message msg) throws ConversationException {
    IcbmService service = conn.getIcbmService();
    if (service == null) {
      throw new ConversationException("no ICBM service to send to", this);
    }
    service.sendIM(getBuddy(), msg.getMessageBody(), msg.isAutoResponse());
    fireOutgoingEvent(ImMessageInfo.getInstance(conn.getScreenname(),
        getBuddy(), msg, new Date()));
  }

  protected void handleIncomingEvent(ConversationEventInfo event) {
    assert !Thread.holdsLock(this);

    super.handleIncomingEvent(event);
  }

  public void setTypingState(TypingState typingState) {
    conn.getIcbmService().sendTypingStatus(getBuddy(), typingState);
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
