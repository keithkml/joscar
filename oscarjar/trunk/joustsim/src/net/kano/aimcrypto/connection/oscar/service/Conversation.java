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

package net.kano.aimcrypto.connection.oscar.service;

import net.kano.aimcrypto.Screenname;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;

import java.util.Iterator;

public abstract class Conversation {
    private final Screenname buddy;

    private CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();
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

    /**
     * This is where all non-trivial initialization of the conversation should
     * be done. {@link badlink}
     */
    protected void initialize() { }

    protected boolean open() {
        synchronized(this) {
            if (open || closed) return false;
            open = true;
        }
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            ConversationListener l = (ConversationListener) it.next();

            l.conversationOpened(this);
        }
        return true;
    }

    protected boolean close() {
        synchronized(this) {
            if (closed) return false;
            closed = true;
        }
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            ConversationListener l = (ConversationListener) it.next();

            l.conversationClosed(this);
        }
        return true;
    }

    public synchronized boolean isOpen() { return open; }

    public synchronized boolean isClosed() { return closed; }

    protected void fireIncomingEvent(MessageInfo msg) {
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            ConversationListener l = (ConversationListener) it.next();
            l.gotMessage(this, msg);
        }
    }

    protected void fireOutgoingEvent(MessageInfo msg) {
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            ConversationListener l = (ConversationListener) it.next();
            l.sentMessage(this, msg);
        }
    }

    protected void ensureOpen() throws ConversationNotOpenException {
        synchronized(this) {
            if (!open) throw new ConversationNotOpenException(this);
        }
    }

    public abstract void sendMessage(Message msg)
            throws ConversationNotOpenException;
}
