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

import net.kano.joscar.DefensiveTools;

/**
 * An event fired when an exception occurs during the FLAP processing process.
 */
public class FlapExceptionEvent {
    /**
     * An exception type indicating that an exception was thrown while
     * attempting to read from the attached input stream. The "reason" (the
     * value returned by {@link #getReason()}) in this case will be
     * <code>null</code>.
     */
    public static final Object TYPE_CONNECTION_ERROR = "TYPE_CONNECTION_ERROR";

    /**
     * An exception type indicating that an exception was thrown while calling
     * a <code>FlapCommand</code>'s <code>write</code> method. In this case, the
     * "reason" (the value returned by {@link #getReason()}) will be the
     * <code>FlapCommand</code> whose <code>writeData</code> method threw an
     * exception.
     */
    public static final Object TYPE_CMD_WRITE = "TYPE_CMD_WRITE";

    /**
     * An exception type indicating that an exception was thrown while
     * converting a raw FLAP packet to a <code>FlapCommand</code> in a
     * <code>FlapCommandFactory</code>. In this case, the "reason" (the value
     * returned by {@link #getReason()} will be the <code>FlapPacket</code>
     * used in the <code>FlapCommand</code> generation attempt (that threw the
     * exception).
     */
    public static final Object TYPE_CMD_GEN = "TYPE_CMD_GEN";

    /**
     * An exception type indicating that an exception was thrown while passing
     * a packet event to a packet listener.
     */
    public static final Object TYPE_PACKET_LISTENER = "TYPE_PACKET_LISTENER";

    /**
     * The type of this exception.
     */
    private final Object type;

    /**
     * The FLAP connection on which this exception was thrown.
     */
    private final FlapProcessor conn;

    /**
     * The exception that was thrown.
     */
    private final Throwable exception;

    /**
     * A "reason" or description of why the exception was thrown.
     */
    private final Object reason;

    /**
     * Creates a new exception event with the given properties.
     *
     * @param type the type or source of the thrown exception
     * @param conn the FLAP connection on which this exception occurred
     * @param exception the exception that was thrown
     * @param reason a reason or description of why or how this exception was
     *        thrown
     */
    protected FlapExceptionEvent(Object type, FlapProcessor conn,
            Throwable exception, Object reason) {
        DefensiveTools.checkNull(type, "type");
        DefensiveTools.checkNull(conn, "conn");

        this.type = type;
        this.conn = conn;
        this.exception = exception;
        this.reason = reason;
    }

    /**
     * Returns the type of this exception, possibly indicating when or why this
     * exception was thrown. May be one of {@link #TYPE_CONNECTION_ERROR},
     * {@link #TYPE_CMD_WRITE}, {@link #TYPE_CMD_GEN}, and {@link
     * #TYPE_PACKET_LISTENER}, but other classes may define their own types as
     * well.
     *
     * @return the type of exception that was thrown
     */
    public final Object getType() {
        return type;
    }

    /**
     * Returns the FLAP connection on which this exception was thrown.
     *
     * @return the FLAP connection associated with this exception
     */
    public final FlapProcessor getFlapProcessor() {
        return conn;
    }

    /**
     * Returns the exception that was thrown.
     *
     * @return the thrown exception
     */
    public final Throwable getException() {
        return exception;
    }

    /**
     * Returns an object describing or providing more detail regarding the
     * exception that was thrown. The type of this object varies with situation,
     * but it may be a <code>FlapPacketListener</code> if it was the source of
     * this exception, for example. The type of this value may change, so code
     * such as the following is suggested for handling it:
     *
     * <pre>
if (e.getReason() instanceof Throwable) {
    ((Throwable) e.getReason()).printStackTrace();
} else {
    System.err.println(e.getReason());
}
     </pre>
     *
     * @return an object providing more information or detail on this exception
     */
    public final Object getReason() {
        return reason;
    }

    public String toString() {
        return "FlapExceptionEvent of type " + type + ": "
                + (exception != null ? exception.getMessage() : null)
                + " (" + reason + ")";
    }
}
