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
 *  File created by keith @ Feb 14, 2003
 *
 */

package net.kano.joscar.flap;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.DefaultFlapCmdFactory;
import net.kano.joscar.net.ConnProcessor;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a FLAP connection that manages an outgoing FLAP queue as well
 * as parsing and processing incoming FLAP packets. A <code>FlapProcessor</code>
 * can be attached to any pair of input and output streams, allowing one to
 * read and write FLAP commands to almost any source and destination. For an
 * easier way to use the most common source and destination, a TCP socket, see
 * {@link ClientFlapConn}.
 * <br>
 * <br>
 * <code>FlapProcessor</code> uses the Java Logging API namespace
 * <code>"net.kano.joscar.flap"</code>, logging various events at the levels
 * <code>Level.CONFIG</code>, <code>Level.FINE</code>, and
 * <code>Level.FINER</code>, in order to, hopefully, ease the debugging of
 * FLAP-related applications. For more information on how to log such events,
 * consult the Java Logging API reference at the <a 
 * href="http://java.sun.com/j2se">J2SE website</a>.
 * <br>
 * <br>
 * Note that upon receipt of a FLAP packet, an event is passed to each of the
 * registered <i>vetoable</i> listeners first, halting immediately if a listener
 * says to halt processing. If a vetoable listener has not halted processing, an
 * event is next passed to each of the registered <i>non-vetoable</i> (that is,
 * normal <code>FlapPacketListener</code>) listeners.
 *
 * @see ClientFlapConn
 */
public class FlapProcessor extends ConnProcessor {
    /**
     * A <code>Logger</code> to facilitate logging and debugging of FLAP-related
     * activities.
     */
    private static final Logger logger
            = Logger.getLogger("net.kano.joscar.flap");

    /**
     * Represents the maximum value of a FLAP sequence number.
     */
    private static final int SEQNUM_MAX = 0xff - 1;

    /**
     * A list of handlers for FLAP-related errors and exceptions.
     */
    private List errorHandlers = new ArrayList();

    /**
     * A list of listeners for incoming FLAP packets.
     */
    private List packetListeners = new ArrayList();

    /**
     * A list of "vetoable listeners" for incoming FLAP packets. Vetoable
     * listeners have the ability to halt the processing of a given packet.
     */
    private List vetoablePacketListeners = new ArrayList();

    /** A lock for reading from the stream. */
    private final Object readLock = new Object();

    /**
     * The sequence number of the next FLAP packet; increased on each packet
     * send.
     */
    private int seqnum = 1;

    /**
     * A FLAP command factory to generate <code>FlapCommand</code>s from
     * incoming FLAP packets.
     */
    private FlapCommandFactory commandFactory = new DefaultFlapCmdFactory();

    /**
     * Creates a FLAP processor with the default FLAP command factory and not
     * yet attached to an input or output stream.
     */
    public FlapProcessor() { }

    /**
     * Creates a FLAP processor with the default FLAP command factory and
     * attaches it to the given socket.
     * <br>
     * <br>
     * Note that this does not begin any sort of loop or FLAP connection; you
     * still need to do this yourself (see {@link #readNextFlap} and {@link
     * #runFlapLoop}.
     *
     * @param socket the <i>connected</i> socket to which this FLAP processor
     *        should be attached
     * @throws IOException if an I/O exception occurs while attaching to the
     *         socket
     *
     * @see #attachToSocket
     */
    public FlapProcessor(Socket socket) throws IOException {
        attachToSocket(socket);
    }

    /**
     * Adds a "vetoable packet listener." A vetoable packet listener has the
     * ability to halt the processing of a given FLAP.
     *
     * @param listener the listener to add
     */
    public synchronized final void addVetoablePacketListener(
            VetoableFlapPacketListener listener) {
        if (!vetoablePacketListeners.contains(listener)) {
            vetoablePacketListeners.add(listener);
        }
    }

    /**
     * Removes the given vetoable packet listener from this FLAP processor's
     * list of vetoable packet listeners.
     *
     * @param listener the listener to remove
     */
    public synchronized final void removeVetoablePacketListener(
            VetoableFlapPacketListener listener) {
        vetoablePacketListeners.remove(listener);
    }

    /**
     * Adds a FLAP packet listener to this FLAP processor.
     *
     * @param listener the listener to add
     */
    public synchronized final void addPacketListener(
            FlapPacketListener listener) {
        if (!packetListeners.contains(listener)) packetListeners.add(listener);
    }

    /**
     * Removes a FLAP packet listener from this FLAP processor.
     *
     * @param listener the listener to remove
     */
    public synchronized final void removePacketListener(
            FlapPacketListener listener) {
        packetListeners.remove(listener);
    }

    /**
     * Adds an exception handler for FLAP-related exceptions.
     *
     * @param handler the handler to add
     */
    public synchronized final void addExceptionHandler(
            FlapExceptionHandler handler) {
        if (!errorHandlers.contains(handler)) errorHandlers.add(handler);
    }

    /**
     * Removes an exception handler from this FLAP processor.
     *
     * @param handler the handler to remove
     */
    public synchronized final void removeExceptionHandler(
            FlapExceptionHandler handler) {
        errorHandlers.remove(handler);
    }

    /**
     * Sets the FLAP command factory to use for generating
     * <code>FlapCommand</code>s from FLAP packets. This can be
     * <code>null</code>, disabling the generation of <code>FlapCommand</code>s.
     *
     * @param factory the new factory to use, or <code>null</code> to disable
     *        the generation of <code>FlapCommand</code>s on this connection
     */
    public synchronized final void setFlapCmdFactory(
            FlapCommandFactory factory) {
        this.commandFactory = factory;
    }

    /**
     * Processes the given packet by generating a <code>FlapCommand</code>,
     * running it through vetoable listeners, then running it through regular
     * listeners.
     *
     * @param packet the packet to process
     */
    private synchronized final void handlePacket(FlapPacket packet) {
        DefensiveTools.checkNull(packet, "packet");

        logger.fine("FlapProcessor received packet: " + packet);

        FlapCommand cmd = null;
        if (commandFactory != null) {
            try {
                cmd = commandFactory.genFlapCommand(packet);
            } catch (Throwable t) {
                handleException(FlapExceptionEvent.ERRTYPE_CMD_GEN, t, packet);
            }
            logger.fine("Flap command for " + packet + ": " + cmd);
        }

        FlapPacketEvent event = new FlapPacketEvent(this, packet, cmd);

        for (Iterator it = vetoablePacketListeners.iterator(); it.hasNext();) {
            VetoableFlapPacketListener listener
                    = (VetoableFlapPacketListener) it.next();

            logger.finer("Running vetoable flap packet listener: " + listener);

            Object result;
            try {
                result = listener.handlePacket(event);
            } catch (Throwable t) {
                handleException(FlapExceptionEvent.ERRTYPE_PACKET_LISTENER, t,
                        listener);
                continue;
            }
            if (result != VetoableFlapPacketListener.CONTINUE_PROCESSING) {
                logger.finer("Flap packet listener vetoed further processing: "
                        + listener);
                return;
            }
        }

        for (Iterator it = packetListeners.iterator(); it.hasNext();) {
            FlapPacketListener listener = (FlapPacketListener) it.next();

            logger.finer("Running Flap packet listener " + listener);

            try {
                listener.handleFlapPacket(event);
            } catch (Throwable t) {
                handleException(FlapExceptionEvent.ERRTYPE_PACKET_LISTENER, t,
                        listener);
            }
        }

        logger.finer("Finished handling Flap packet");
    }

    /**
     * Processes the given exception with the given error type. This exception
     * will be passed to all registered exception handlers. Calling this method
     * is equivalent to calling {@link
     * #handleException(Object, Throwable, Object) handleException(type,
     * t, null)}.
     *
     * @param type an object representing the type or source of the given
     *        exception
     * @param t the exception that was thrown
     *
     * @see #addExceptionHandler
     */
    public synchronized final void handleException(Object type, Throwable t) {
        handleException(type, t, null);
    }

    /**
     * Processes the given exception with the given error type and error detail
     * info. This exception will be passed to all registered exception handlers.
     *
     * @param type an object representing the type or source of the given
     *        exception
     * @param t the exception that was thrown
     * @param info an object containing extra information or details on this
     *        exception and/or what caused it
     */
    public synchronized final void handleException(Object type, Throwable t,
            Object info) {
        DefensiveTools.checkNull(type, "type");
        DefensiveTools.checkNull(t, "t");

        logger.fine("Processing flap error (" + type + "): " + t.getMessage()
                + ": " + info);

        if (type == FlapExceptionEvent.ERRTYPE_CONNECTION_ERROR
                && !isAttached()) {
            // if we're not attached to anything, the listeners don't want to
            // be hearing about any connection errors.
            logger.finer("Ignoring " + type + " flap error because processor " +
                    "is not attached");
            return;
        }

        FlapExceptionEvent event = new FlapExceptionEvent(type, this, t, info);

        for (Iterator it = errorHandlers.iterator(); it.hasNext();) {
            FlapExceptionHandler handler = (FlapExceptionHandler) it.next();

            logger.finer("Running flap error handler " + handler);

            try {
                handler.handleException(event);
            } catch (Throwable t2) {
                t2.printStackTrace();
            }
        }
    }

    /**
     * Sends the given FLAP command on this FLAP processor's attached output
     * stream.
     *
     * @param command the command to send
     * @throws NullPointerException if this FLAP processor is not attached to
     *         an output stream
     */
    public synchronized final void send(FlapCommand command)
            throws NullPointerException {
        DefensiveTools.checkNull(command, "command");

        OutputStream out = getOutputStream();

        DefensiveTools.checkNull(out, "out");

        logger.finer("Sending Flap command " + command);
        FlapPacket packet = new FlapPacket(seqnum, command);

        ByteBlock block;
        try {
            block = ByteBlock.createByteBlock(packet);
        } catch (Throwable t) {
            handleException(FlapExceptionEvent.ERRTYPE_CMD_WRITE, t, command);
            return;
        }

        logger.fine("Sending Flap packet " + packet + ": " + block.getLength()
                + " total bytes");

        // we trust ByteBlock.write so much that we don't even check for
        // Throwable!
        try {
            block.write(out);
        } catch (IOException e) {
            handleException(FlapExceptionEvent.ERRTYPE_CONNECTION_ERROR, e);
            return;
        }

        // this is sent as an unsigned short, so it needs to wrap like an
        // unsigned short.
        if (seqnum == SEQNUM_MAX) seqnum = 0;
        else seqnum++;

        logger.finer("Finished sending Flap command");
    }

    /**
     * A utility method to read FLAP packets indefinitely (that is, until the
     * end of the stream is reached or an I/O error occurs).
     *
     * @throws IOException if an I/O error occurs
     */
    public final void runFlapLoop() throws IOException {
        // grab packets
        while (readNextFlap());

        // the connection is dead.
    }

    /**
     * Reads and processes a single FLAP packet from the attached input stream.
     * If this method returns <code>false</code>, it is safe to assume that the
     * connection died.
     *
     * @return <code>true</code> if a packet was successfully read;
     *         <code>false</code> otherwise
     *
     * @throws IOException if an I/O error occurs
     */
    public final boolean readNextFlap() throws IOException {
        synchronized(readLock) {
            if (getInputStream() == null) return false;

            FlapHeader header = FlapHeader.readFLAPHeader(getInputStream());

            logger.finer("Read flap header " + header);

            if (header == null) return false;

            FlapPacket packet = FlapPacket.readRestOfFlap(header, getInputStream());

            logger.finer("Read flap packet " + packet);
            if (packet == null) return false;

            handlePacket(packet);

            return true;
        }
    }

}
