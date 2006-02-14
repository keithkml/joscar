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
import net.kano.joscar.MiscTools;
import net.kano.joustsim.Screenname;

import java.util.logging.Logger;

public abstract class Conversation {
  private static final Logger LOGGER = Logger
      .getLogger(Conversation.class.getName());

  private final Screenname buddy;

  private CopyOnWriteArrayList<ConversationListener> listeners
      = new CopyOnWriteArrayList<ConversationListener>();
  private boolean open = false;
  private boolean closed = false;

  public Conversation(Screenname buddy) {
    this.buddy = buddy;
  }

  public void addConversationListener(ConversationListener l) {
    DefensiveTools.checkNull(l, "l");
    listeners.addIfAbsent(l);
  }

  public void removeConversationListener(ConversationListener l) {
    listeners.remove(l);
  }

  public final Screenname getBuddy() { return buddy; }

  protected void initialize() { }

  protected synchronized boolean setAlwaysOpen() {
    if (open || closed) return false;
    open = true;
    return true;
  }

  /**
   * Returns whether the conversation was opened
   */
  public boolean open() {
    synchronized (this) {
      if (open || closed) return false;
      open = true;
    }

    fireOpenEvent();
    opened();

    return true;
  }

  private void fireOpenEvent() {
    assert !Thread.holdsLock(this);

    for (ConversationListener l : listeners) {
      l.conversationOpened(this);
    }
  }

  public boolean close() {
    if (!closeWithoutEvents()) return false;

    finishClosing();

    return true;
  }

  private void finishClosing() {
    fireClosedEvent();
    closed();
  }

  private synchronized boolean closeWithoutEvents() {
    if (closed) return false;
    closed = true;
    return true;
  }

  private void fireClosedEvent() {
    assert !Thread.holdsLock(this);

    for (ConversationListener l : listeners) {
      l.conversationClosed(this);
    }
  }

  protected void opened() {
  }

  protected void closed() {
  }

  public synchronized boolean isOpen() { return open; }

  public synchronized boolean isClosed() { return closed; }

  protected void fireIncomingEvent(ConversationEventInfo event) {
    assert !Thread.holdsLock(this);

    DefensiveTools.checkNull(event, "event");

    LOGGER.fine(MiscTools.getClassName(this) + ": firing incoming "
        + "event: " + event);
    if (event instanceof MessageInfo) {
      MessageInfo messageInfo = (MessageInfo) event;
      for (ConversationListener l : listeners) {
        l.gotMessage(this, messageInfo);
      }
    } else {
      for (ConversationListener l : listeners) {
        l.gotOtherEvent(this, event);
      }
    }
  }

  protected void fireOutgoingEvent(ConversationEventInfo event) {
    assert !Thread.holdsLock(this);

    DefensiveTools.checkNull(event, "event");

    LOGGER.fine(MiscTools.getClassName(this) + ": firing outgoing "
        + "event: " + event);
    if (event instanceof MessageInfo) {
      MessageInfo messageInfo = (MessageInfo) event;
      for (ConversationListener l : listeners) {
        l.sentMessage(this, messageInfo);
      }
    } else {
      for (ConversationListener l : listeners) {
        l.sentOtherEvent(this, event);
      }
    }
  }

  protected void fireCanSendChangedEvent(boolean canSend) {
    assert !Thread.holdsLock(this);

    for (ConversationListener listener : listeners) {
      listener.canSendMessageChanged(this, canSend);
    }
  }

  protected synchronized void checkOpen() throws ConversationNotOpenException {
    if (!open) throw new ConversationNotOpenException(this);
  }

  public boolean canSendMessage() {
    return true;
  }

  public abstract void sendMessage(Message msg)
      throws ConversationException;

  @SuppressWarnings({"ReturnOfCollectionOrArrayField"})
  protected CopyOnWriteArrayList<ConversationListener> getListeners() {
    return listeners;
  }

  protected void handleIncomingEvent(ConversationEventInfo event) {
    fireIncomingEvent(event);

    if (event instanceof TypingInfo) {
      TypingInfo typingInfo = (TypingInfo) event;
      for (ConversationListener listener : getListeners()) {
        if (listener instanceof TypingListener) {
          ((TypingListener) listener).gotTypingState(this, typingInfo);
        }
      }
    }
  }
}
