/*
 *  Copyright (c) 2002-2003, The Joust Project
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
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.SeqNum;
import net.kano.joscar.logging.Logger;
import net.kano.joscar.logging.LoggingSystem;
import net.kano.joscar.net.ConnProcessorExceptionEvent;
import static net.kano.joscar.net.ConnProcessorExceptionEvent.ERRTYPE_PACKET_LISTENER;
import net.kano.joscar.net.ConnProcessorImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


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
 * <br>
 * <br>
 * Note that a new instance of <code>FlapProcessor</code> does not come with any
 * FLAP command factory installed! You are advised to {@link #setFlapCmdFactory
 * install} an instance of {@link net.kano.joscar.flapcmd.DefaultFlapCmdFactory}
 * (or your own custom factory).
 *
 * @see ClientFlapConn
 */
public abstract class AbstractFlapProcessor extends ConnProcessorImpl
        implements FlapProcessor {
    /**
     * A <code>Logger</code> to facilitate logging and debugging of FLAP-related
     * activities.
     */
    private static final Logger logger
            = LoggingSystem.getLogger("net.kano.joscar.flap");

    /**
     * Represents the maximum value of a FLAP sequence number.
     */
    private static final int SEQNUM_MAX = 0xffff;

    /**
     * A list of listeners for incoming FLAP packets.
     */
    private final CopyOnWriteArrayList<FlapPacketListener> packetListeners
            = new CopyOnWriteArrayList<FlapPacketListener>();

    /**
     * A list of "vetoable listeners" for incoming FLAP packets. Vetoable
     * listeners have the ability to halt the processing of a given packet.
     */
    private final CopyOnWriteArrayList<VetoableFlapPacketListener> vetoablePacketListeners
            = new CopyOnWriteArrayList<VetoableFlapPacketListener>();

    /** A lock for writing to the stream. */
    private final Object writeLock = new Object();

    /** A lock for reading from the stream. */
    private final Object readLock = new Object();

    /**
     * An object used to generate sequential FLAP sequence numbers.
     */
    private SeqNum seqNum = new SeqNum(0, SEQNUM_MAX);

    /**
     * A FLAP command factory to generate <code>FlapCommand</code>s from
     * incoming FLAP packets.
     */
    private FlapCommandFactory commandFactory = null;

    /**
     * Creates a FLAP processor with the default FLAP command factory and not
     * yet attached to an input or output stream.
     */
    public AbstractFlapProcessor() { }

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
    public AbstractFlapProcessor(Socket socket) throws IOException {
        attachToSocket(socket);
    }

    public final void addVetoablePacketListener(
            VetoableFlapPacketListener listener) {
        DefensiveTools.checkNull(listener, "listener");

        vetoablePacketListeners.addIfAbsent(listener);
    }

    public final void removeVetoablePacketListener(
            VetoableFlapPacketListener listener) {
        DefensiveTools.checkNull(listener, "listener");

        vetoablePacketListeners.remove(listener);
    }

    public final void addPacketListener(FlapPacketListener listener) {
        DefensiveTools.checkNull(listener, "listener");

        packetListeners.addIfAbsent(listener);
    }

    public final void removePacketListener(FlapPacketListener listener) {
        DefensiveTools.checkNull(listener, "listener");

        packetListeners.remove(listener);
    }

    public synchronized final void setFlapCmdFactory(
            FlapCommandFactory factory) {
        this.commandFactory = factory;
    }

    protected abstract void handlePacket(FlapPacket packet);

    /**
     * Processes the given packet by generating a <code>FlapCommand</code>,
     * running it through vetoable listeners, then running it through regular
     * listeners. <b>This method must be called while holding a lock on {@link
     * #readLock}.</b>
     *
     * @param packet the packet to process
     */
    protected void processPacketSynchronously(FlapPacket packet) {
        DefensiveTools.checkNull(packet, "packet");

        boolean logFine = logger.logFineEnabled();
        boolean logFiner = logger.logFinerEnabled();

        if (logFine) logger.logFine("FlapProcessor received packet: " + packet);

        FlapCommandFactory factory;
        synchronized(this) {
            factory = commandFactory;
        }

        FlapCommand cmd = null;
        if (factory != null) {
            try {
                cmd = factory.genFlapCommand(packet);
            } catch (Throwable t) {
                handleException(ConnProcessorExceptionEvent.ERRTYPE_CMD_GEN, t,
                        packet);
            }
        }
        if (logFine) logger.logFine("Flap command for " + packet + ": " + cmd);

        FlapPacketEvent event = new FlapPacketEvent(this, packet, cmd);

        for (VetoableFlapPacketListener listener : vetoablePacketListeners) {
            if (logFiner) {
                logger.logFiner("Running vetoable flap packet listener: "
                        + listener);
            }

            Object result;
            try {
                result = listener.handlePacket(event);
            } catch (Throwable t) {
                handleException(ERRTYPE_PACKET_LISTENER, t, listener);
                continue;
            }
            if (result != VetoableFlapPacketListener.VetoResult.CONTINUE_PROCESSING) {
                if (logFiner) {
                    logger.logFiner("Flap packet listener vetoed further " +
                            "processing: " + listener);
                }
                return;
            }
        }

        for (FlapPacketListener listener : packetListeners) {
            if (logFiner) {
                logger.logFiner("Running Flap packet listener " + listener);
            }

            try {
                listener.handleFlapPacket(event);
            } catch (Throwable t) {
                handleException(ERRTYPE_PACKET_LISTENER, t, listener);
            }
        }

        if (logFiner) logger.logFiner("Finished handling Flap packet");
    }

    public final void sendFlap(FlapCommand command) {
        DefensiveTools.checkNull(command, "command");

        boolean logFine = logger.logFineEnabled();
        boolean logFiner = logger.logFinerEnabled();

        OutputStream out = getOutputStream();

        if (out == null) return;

        if (logFiner) logger.logFiner("Sending Flap command " + command);

        synchronized(writeLock) {
            int seq = (int) seqNum.next();

            FlapPacket packet = new FlapPacket(seq, command);

            ByteBlock block;
            try {
                block = ByteBlock.createByteBlock(packet);
            } catch (Throwable t) {
                handleException(ConnProcessorExceptionEvent.ERRTYPE_CMD_WRITE,
                        t, command);
                return;
            }

            if (logFine) {
                logger.logFine("Sending Flap packet " + packet + ": "
                        + block.getLength() + " total bytes");
            }

            // we trust ByteBlock.write so much that we don't even check for
            // Throwable!
            try {
                block.write(out);
            } catch (IOException e) {
                handleException(
                        ConnProcessorExceptionEvent.ERRTYPE_CONNECTION_ERROR,
                        e);
                return;
            }
        }

        if (logFiner) logger.logFiner("Finished sending Flap command");
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
        boolean logFiner = logger.logFinerEnabled();

        InputStream inputStream = getInputStream();
        if (inputStream == null) return false;

        synchronized(readLock) {
            FlapHeader header = FlapHeader.readFLAPHeader(inputStream);

            if (logFiner) logger.logFiner("Read flap header " + header);

            if (header == null) return false;

            FlapPacket packet = FlapPacket.readRestOfFlap(header, inputStream);

            if (logFiner) logger.logFiner("Read flap packet " + packet);

            if (packet == null) return false;

            handlePacket(packet);

            return true;
        }
    }

    public String toString() {
        return "FlapProcessor: "
                + "seqNum=" + seqNum;
    }
}
