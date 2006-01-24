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
import net.kano.joustsim.oscar.oscar.service.icbm.dim.OutgoingDirectimConnectionImpl;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.DirectImController;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.BuddyTypingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.ReceivedMessageEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionEventListener;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionState;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;

import java.util.Date;

public class DirectConversation extends Conversation {
  private AimConnection conn;
  private OutgoingDirectimConnectionImpl directim = null;

  public DirectConversation(AimConnection conn, Screenname buddy) {
    super(buddy);
    this.conn = conn;
  }

  public boolean open() {
    directim = conn.getIcbmService().getRvConnectionManager()
        .openDirectimConnection(getBuddy());
    directim.addTransferListener(new RvConnectionEventListener() {
      public void handleEventWithStateChange(RvConnection transfer,
          RvConnectionState state, RvConnectionEvent event) {
        if (state == RvConnectionState.CONNECTED) {
          DirectConversation.super.open();
        }
      }

      public void handleEvent(RvConnection transfer, RvConnectionEvent event) {
        if (event instanceof BuddyTypingEvent) {
          fireIncomingEvent(new TypingInfo(getBuddy(), getBuddy(), new Date(),
              ((BuddyTypingEvent) event).getState()));

        } else if (event instanceof ReceivedMessageEvent) {
          //TODO: create events for incoming message data?..
          final ReceivedMessageEvent mevent = (ReceivedMessageEvent) event;
          fireIncomingEvent(ImMessageInfo.getInstance(getBuddy(),
              conn.getScreenname(), new Message() {
            public String getMessageBody() {
              return mevent.getMessage();
            }

            public boolean isAutoResponse() {
              return mevent.isAutoResponse();
            }
          }, new Date()));
        }
      }
    });
    return false;
  }

  protected void closed() {
    if (directim != null) {
      directim.close();
      directim = null;
    }
  }

  public void sendMessage(Message msg) throws ConversationException {
    OutgoingDirectimConnectionImpl directim;
    synchronized (this) {
      if (!isOpen()) {
        throw new ConversationNotOpenException(this);
      }

      directim = this.directim;
    }
    StateController controller = directim.getStateController();
    DirectImController dim = (DirectImController) controller;
    dim.sendMessage(msg.getMessageBody(), msg.isAutoResponse());
  }
}
