/*
 *  Copyright (c) 2003, The Joust Project
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
 *  File created by Keith @ 4:47:43 PM
 *
 */

package net.kano.joscar.net;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.logging.Logger;
import net.kano.joscar.logging.LoggingSystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Iterator;


/**
 * A simple interface for an object that has the ability to be attached and/or
 * detached from a stream or socket.
 */
public abstract class ConnProcessorImpl implements ConnProcessor {
    /** A logger for logging connection processor related events. */
    private static final Logger logger
            = LoggingSystem.getLogger("net.kano.joscar.net");

    /**
     * Represents whether this connection processor is attached to any input or
     * output streams.
     */
    private boolean attached = false;
    /**
     * The input stream from which packets are currently being read, or
     * <code>null</code> if there is currently no such stream.
     */
    private InputStream in = null;
    /**
     * The output stream to which packets are currently being written,
     * or <code>null</code> if there is currently no such stream.
     */
    private OutputStream out = null;
    /** A list of handlers for connection-related errors and exceptions. */
    private final CopyOnWriteArrayList<ConnProcessorExceptionHandler> errorHandlers
            = new CopyOnWriteArrayList<ConnProcessorExceptionHandler>();

    public synchronized final void attachToSocket(Socket socket)
            throws IOException {
        DefensiveTools.checkNull(socket, "socket");

        attachToInput(socket.getInputStream());
        attachToOutput(socket.getOutputStream());
    }

    public synchronized final void attachToInput(InputStream in) {
        DefensiveTools.checkNull(in, "in");

        this.in = in;

        attached = true;
    }

    public synchronized final void attachToOutput(OutputStream out) {
        DefensiveTools.checkNull(out, "out");

        this.out = out;

        attached = true;
    }

    /**
     * Returns whether this connection processor is currently attached to a
     * stream (input, output, or both).
     *
     * @return whether this connection processor is currently "attached"
     */
    protected synchronized final boolean isAttached() { return attached; }

    /**
     * Returns the input stream to which this processor is currently attached,
     * or <code>null</code> if none is currently set.
     *
     * @return this processor's input stream
     */
    protected synchronized final InputStream getInputStream() { return in; }

    /**
     * Returns the output stream to which this processor is currently attached,
     * or <code>null</code> if none is currently set.
     *
     * @return this processor's output stream
     */
    protected synchronized final OutputStream getOutputStream() { return out; }

    public synchronized final void detach() {
        in = null;
        out = null;
        attached = false;
    }

    public final void addExceptionHandler(
            ConnProcessorExceptionHandler handler) {
        DefensiveTools.checkNull(handler, "handler");

        errorHandlers.addIfAbsent(handler);
    }

    public final void removeExceptionHandler(
            ConnProcessorExceptionHandler handler) {
        DefensiveTools.checkNull(handler, "handler");

        errorHandlers.remove(handler);
    }

    public final void handleException(ErrorType type, Throwable t) {
        handleException(type, t, null);
    }

    public final void handleException(ErrorType type, Throwable t,
            Object info) {
        DefensiveTools.checkNull(type, "type");
        DefensiveTools.checkNull(t, "t");

        boolean logFine = logger.logFineEnabled();
        boolean logFiner = logger.logFinerEnabled();

        if (logFine) {
            logger.logFine("Processing connection error (" + type + "): "
                    + t.getMessage() + ": " + info);
        }

        if (type == ConnProcessorExceptionEvent.ERRTYPE_CONNECTION_ERROR
                && !isAttached()) {
            // if we're not attached to anything, the listeners don't want to
            // be hearing about any connection errors.
            if (logFiner) {
                logger.logFine("Ignoring " + type + " connection error because " +
                        "processor is not attached");
            }
            return;
        }

        // get the iterator now, so we can use the same one later and be working
        // with the same copy of the handler list (this method is not
        // synchronized)
        Iterator<ConnProcessorExceptionHandler> iterator = errorHandlers.iterator();
        if (!iterator.hasNext()) {
            if (logger.logWarningEnabled()) {
                logger.logException(
                        "CONNPROCESSOR HAS NO ERROR HANDLERS, DUMPING:\n"
                        + "ERROR TYPE: " + type + "\n"
                        + "ERROR INFO: " + info,
                        t);
            }

            return;
        }

        ConnProcessorExceptionEvent event
                = new ConnProcessorExceptionEvent(this, type, t, info);

        while (iterator.hasNext()) {
            ConnProcessorExceptionHandler handler
                    = iterator.next();

            if (logFiner) {
                logger.logFiner("Running ConnProcessor error handler " + handler);
            }

            try {
                handler.handleException(event);
            } catch (Throwable t2) {
                if (logger.logWarningEnabled()) {
                    logger.logWarning("Exception handler " + handler
                            + "threw exception: " + t2);
                }
            }
        }
    }

}