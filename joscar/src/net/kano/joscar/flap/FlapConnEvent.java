/*
 *  Copyright (c) 2002, The Joust Project
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
 *  File created by keith @ Mar 6, 2003
 *
 */

package net.kano.joscar.flap;

/**
 * An event fired upon a change in the connection state of a
 * <code>ClientFlapConn</code>.
 */
public class FlapConnEvent {
    /**
     * The FLAP connection that fired this event.
     */
    private final ClientFlapConn flapConn;

    /**
     * The previous state of the associated FLAP connection.
     */
    private final Object oldState;

    /**
     * The new state of the associated FLAP connection.
     */
    private final Object newState;

    /**
     * An object representing some sort of reason, description, or explanation
     * for this event.
     */
    private final Object reason;

    /**
     * Creates a new event representing a state change on the given FLAP
     * connection.
     *
     * @param conn the FLAP connection firing this event
     * @param oldState the previous state of the given FLAP connection
     * @param newState the new state of the given FLAP connection
     * @param reason a reason, description, or explanation for the state change,
     *        or <code>null</code> to indicate an implied reason or no reason
     *        at all
     */
    protected FlapConnEvent(ClientFlapConn conn, Object oldState,
            Object newState, Object reason) {
        this.flapConn = conn;
        this.oldState = oldState;
        this.newState = newState;
        this.reason = reason;
    }

    /**
     * Returns the FLAP connection that fired this event.
     *
     * @return the FLAP connection whose state changed
     */
    public final ClientFlapConn getFlapConn() {
        return flapConn;
    }

    /**
     * Returns the previous state of the FLAP connection that fired this event.
     *
     * @return the previous state of the FLAP connection that fired this event
     */
    public final Object getOldState() {
        return oldState;
    }

    /**
     * Returns the new state of the FLAP connection that fired this event.
     *
     * @return the new state of the FLAP connection that fired this event
     */
    public final Object getNewState() {
        return newState;
    }

    /**
     * Returns an object representing a reason, description, or explanation
     * for why this state change occurred, or <code>null</code> to represent
     * an implied reason or lack of information as to why it happened.
     *
     * @return an object representing a reason, description, or explanation
     *         for why this state change occurred
     */
    public final Object getReason() {
        return reason;
    }
}
