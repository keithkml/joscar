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
 *  File created by keith @ Feb 17, 2003
 *
 */

package net.kano.joscar.snac;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flap.FlapPacketEvent;
import net.kano.joscar.flap.FlapProcessor;
import net.kano.joscar.flap.VetoableFlapPacketListener;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.flapcmd.SnacFlapCmd;
import net.kano.joscar.flapcmd.SnacPacket;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides an easy interface to listening for incoming SNAC packets as well as
 * sending SNAC commands over a FLAP connection. A <code>SnacProcessor</code>
 * provides a request-response system (see {@link SnacRequest}, {@link
 * #sendSnac sendSnac}) with automatic request ID generation, a system for
 * "preprocessing" SNAC's before they are formally handled and processed, a
 * system for listening for incoming packets and optionally "vetoing" their
 * further processing.
 * <br>
 * <br>
 * <code>SnacProcessor</code> logs to the Java Logging API namespace
 * <code>"net.kano.joscar.snac"</code> on the levels <code>Level.FINE</code>
 * and <code>Level.FINER</code> in order to, hopefully, ease the debugging
 * SNAC-related applications. For information on how to access these logs,
 * see the Java Logging API reference at the <a
 * href="http://java.sun.com/j2se">J2SE website</a>.
 * <br>
 * <br>
 * <code>SnacProcessor</code> passes many types of exceptions thrown during
 * SNAC processing to its attached <code>FlapProcessor</code>'s error handlers
 * using <code>FlapProcessor</code>'s <code>handleException</code> method, which
 * in turn causes the exceptions to be passed to your own error handlers. The
 * error types used are {@link #ERRTYPE_SNAC_PACKET_PREPROCESSOR}, {@link
 * #ERRTYPE_SNAC_PACKET_LISTENER}, {@link #ERRTYPE_SNAC_REQUEST_LISTENER},
 * and {@link #ERRTYPE_SNAC_RESPONSE_LISTENER}. See individual documentation for
 * each for further detail.
 * <br>
 * <br>
 * It may also be of interest to note that <code>SnacProcessor</code> attaches
 * a <i>vetoable</i> packet listener to any attached <code>FlapProcessor</code>,
 * effectively removing any incoming SNAC packet from the FlapProcessor's event
 * queue. In practice this means that if you attach a <code>SnacProcessor</code>
 * to a <code>FlapProcessor</code> on which you are listening for FLAP packets,
 * your packet listener will not be called when a channel-2 packet (SNAC packet)
 * is received on that <code>FlapProcessor</code>. Instead, it will be processed
 * as a <code>SnacPacket</code> and passed to any listeners on the
 * <code>SnacProcessor</code>.
 * <br>
 * <br>
 * Upon receipt of a SNAC packet, the packet is processed in the following
 * order:
 * <ol>
 * <li> It is passed through all registered SNAC preprocessors </li>
 * <li> A <code>SnacCommand</code> is generated (see <a
 * href="#factories">below</a>) </li>
 * <li> If the request ID matches that of a previous outgoing request,
 * <ul>
 * <li> An event is passed to each of the processor's {@linkplain
 * #addGlobalResponseListener global response listeners} </li>
 * <li> An event is passed to that request's listeners (see {@link
 * SnacRequest}) </li>
 * <li> Processing of the packet within <code>SnacProcessor</code> stops </li>
 * </ul> </li>
 * <li> Otherwise,
 * <ul>
 * <li> An event is passed to each of the registered <i>vetoable</i>
 * packet listeners, halting immediately if a listener says to </li>
 * <li> If no vetoable listener has halted processing, an event is next passed
 * to all registered non-vetoable (that is, normal
 * <code>SnacPacketListener</code>) packet listeners.</li>
 * <li> Processing of the packet within <code>SnacProcessor</code> stops </li>
 * </ul>
 * </ol>
 *
 * <a name="factories"></a>The process of generating a <code>SnacCommand</code>
 * from a <code>SnacPacket</code> is as such:
 * <ol>
 * <li> First, a suitable <code>SnacCmdFactory</code> must be found
 * <ol>
 * <li> If a factory is registered for the exact command type (SNAC family and
 * command ("subtype")) of the received packet, then that factory is used </li>
 * <li> Otherwise, if a factory is registered for the entire SNAC family of the
 * received packet (via <code>registerSnacFactory(new CmdType(family),
 * factory)</code>, for example), that factory is used </li>
 * <li> Otherwise, if a factory is registered for all commands (via
 * <code>registerSnacFactory(CmdType.CMDTYPE_ALL, factory)</code>, for example),
 * then that factory is used </li>
 * <li> Otherwise, if a default SNAC factory list is set and not
 * <code>null</code>, the above three steps are repeated for the factories
 * registered by the default factory list </li>
 * </ol>
 * </li>
 * <li> If a factory has been found, a <code>SnacCommand</code> is generated
 * with a call to the factory's <code>genSnacCommand</code> method </li>
 * </ol>
 *
 * The above system allows one to customize the <code>SnacCommand</code>s passed
 * to your packet listeners, in order to, for example, process an extra field
 * in a certain command that has been added to the protocol since this library's
 * release. This can be done by registering your own SNAC command factories
 * with the appropriate command types (see {@link #registerSnacFactory(CmdType,
 * SnacCmdFactory) registerSnacFactory}).
 * <br>
 * <br>
 * And finally, a bit about SNAC requests. The OSCAR protocol and SNAC data
 * structure are defined such that each individual SNAC packet has its own
 * request ID, a four-byte integer (or any other way you want to represent it,
 * actually). This combined with one other aspect of the protocol allows for
 * interesting things to be done with regard to automated connection handling.
 * That other aspect is that for any command sent to an OSCAR server, all
 * direct responses to that command that are sent back to the client have the
 * <i>same</i> request ID as the original request. This means that, for example,
 * an application can request two user directory searches at once and display
 * the results to each in separate windows based on their request ID. This also
 * allows for more complicated things like determining network lag between an
 * OSCAR server by matching up request ID's with packet send times. See {@link
 * SnacRequest} for more information on how to utilize the request system.
 */
public class SnacProcessor {
    /**
     * An error type indicating that an exception was thrown while calling
     * a {@linkplain #addPreprocessor registered SNAC preprocessor} to
     * process an incoming SNAC packet. In this case, the extra error
     * information (the value returned by {@link
     * net.kano.joscar.flap.FlapExceptionEvent#getReason getReason()}) will be
     * the <code>SnacPreprocessor</code> that threw the exception.
     */
    public static final Object ERRTYPE_SNAC_PACKET_PREPROCESSOR
            = "ERRTYPE_SNAC_PACKET_PREPROCESSOR";

    /**
     * An error type indicating that an exception was thrown while calling
     * a {@linkplain #addPacketListener registered SNAC packet listener} or
     * {@linkplain #addVetoablePacketListener vetoable packet listener} to
     * handle an incoming SNAC packet. In this case, the extra error information
     * (the value returned by {@link
     * net.kano.joscar.flap.FlapExceptionEvent#getReason getReason()}) will be
     * the <code>VetoableFlapPacketListener</code> or
     * <code>FlapPacketListener</code> from whence the exception was thrown.
     */
    public static final Object ERRTYPE_SNAC_PACKET_LISTENER
            = "ERRTYPE_SNAC_PACKET_LISTENER";

    /**
     * An error type indicating that an exception was thrown while calling a
     * {@linkplain SnacRequest SNAC request} {@linkplain SnacRequestListener
     * response listener} to handle a response to a SNAC request or another
     * request-related event. In this case, the extra error information (the
     * value returned by {@link
     * net.kano.joscar.flap.FlapExceptionEvent#getReason getReason()}) will be
     * the <code>SnacRequest</code> whose listener threw the exception.
     */
    public static final Object ERRTYPE_SNAC_REQUEST_LISTENER
            = "ERRTYPE_SNAC_REQUEST_LISTENER";

    /**
     * An error type indicating that an exception was thrown while calling a
     * {@linkplain #addGlobalResponseListener global SNAC response listener} to
     * handle a response to a SNAC request. In this case, the extra error
     * information (the value returned by {@link
     * net.kano.joscar.flap.FlapExceptionEvent#getReason getReason()}) will be
     * the <code>SnacResponseListener</code> that threw the exception.
     */
    public static final Object ERRTYPE_SNAC_RESPONSE_LISTENER
            = "ERRTYPE_SNAC_RESPONSE_LISTENER";

    /**
     * The default SNAC request "time to live," in seconds.
     *
     * @see #setRequestTtl
     */
    public static final int REQUEST_TTL_DEFAULT = 15*60;

    /**
     * A logger for logging SNAC-related events.
     */
    private static final Logger logger
            = Logger.getLogger("net.kano.joscar.snac");

    private static final long REQID_MIN_CLIENT = 1;
    private static final long REQID_MAX_CLIENT = 0x80000000L - 1L;

    private static final long REQID_MIN_SERVER = 0x80000000L;
    private static final long REQID_MAX_SERVER = 0xffffffffL;

    /** The minimum request ID value. */
    private static final long REQID_MIN = REQID_MIN_CLIENT;

    /** The maximum request ID value before it wraps to {@link #REQID_MIN}. */
    private static final long REQID_MAX = REQID_MAX_CLIENT;

    /** The request ID of the last SNAC command sent. */
    private long lastReqid = REQID_MIN;

    /**
     * The FLAP processor to which this SNAC processor is attached.
     */
    private FlapProcessor flapProcessor = null;

    /**
     * This SNAC processor's command factory manager, used for finding an
     * appropriate SNAC factory upon the receipt of a SNAC packet.
     */
    private CmdFactoryMgr factories = new CmdFactoryMgr();

    /**
     * The SNAC preprocessors registered on this SNAC connection.
     */
    private List preprocessors = new ArrayList();

    /**
     * The SNAC request response listeners registered on this SNAC connection.
     */
    private List responseListeners = new ArrayList();

    /**
     * The vetoable packet listeners registered on this SNAC connection.
     */
    private List vetoableListeners = new ArrayList();

    /**
     * The SNAC packet listeners registered on this SNAC connection.
     */
    private List packetListeners = new ArrayList();

    /**
     * The "time to live" of SNAC requests.
     */
    private int requestTtl = REQUEST_TTL_DEFAULT;

    /**
     * A map from request ID's (<code>Integer</code>s) to
     *  <code>RequestInfo</code>s, which contain <code>SnacRequest</code>s.
     */
    private Map requests = new HashMap();
    private final List requestQueue = new LinkedList();

    /** Whether or not this SNAC connection is currently "paused." */
    private boolean paused = false;

    /** A SNAC queue manager for this processor. */
    private SnacQueueManager queueManager = new ImmediateSnacQueueManager();

    /**
     * The FLAP packet listener we add to whichever FLAP processor to which we
     * become attached.
     */
    private VetoableFlapPacketListener flapPacketListener
            = new VetoableFlapPacketListener() {
        public Object handlePacket(FlapPacketEvent e) {
            if (e.getFlapCommand() instanceof SnacFlapCmd) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("SnacProcessor intercepted channel-2 snac "
                            + "command");
                }

                processPacket(e);

                return STOP_PROCESSING_LISTENERS;
            } else {
                return CONTINUE_PROCESSING;
            }
        }
    };

    /**
     * Creates a SNAC processor with no SNAC factories installed and the default
     * request time-to-live, not yet attached to any FLAP processor.
     */
    public SnacProcessor() { }

    /**
     * Creates a SNAC processor with no SNAC factories installed and the default
     * request time-to-live, attached to the given FLAP processor.
     * <br>
     * <br>
     * Using this constructor is equivalent to calling <code>{@linkplain
     * #SnacProcessor() new SnacProcessor()}.{@linkplain #attachToFlapProcessor
     * attachToFlapProcessor(processor)}</code>.
     *
     * @param processor the FLAP processor to attach to
     */
    public SnacProcessor(FlapProcessor processor) {
        attachToFlapProcessor(processor);
    }

    /**
     * Returns the FLAP processor to which this SNAC processor is attached.
     *
     * @return this SNAC processor's FLAP processor
     */
    public synchronized final FlapProcessor getFlapProcessor() {
        return flapProcessor;
    }

    /**
     * Attaches this SNAC processor to the given FLAP processor. Note that this
     * does not start a SNAC processing loop of any kind; rather, SNACs will
     * be processed during the normal processing of FLAP packets that occurs
     * in the <code>FlapProcessor</code>'s event loop (which you may also have
     * to start on your own).
     * <br>
     * <br>
     * Note that invoking this method detaches this SNAC processor from any
     * previously attached FLAP processor.
     *
     * @param processor the FLAP processor to attach to
     */
    public synchronized final void attachToFlapProcessor(
            FlapProcessor processor) {
        DefensiveTools.checkNull(processor, "processor");

        detach();

        attachMinimally(processor);
    }

    /**
     * Attaches to the given FLAP processor without formally detaching from the
     * previous processor.
     *
     * @param processor the processor to which to attach
     */
    private synchronized final void attachMinimally(FlapProcessor processor) {
        this.flapProcessor = processor;
        this.flapProcessor.addVetoablePacketListener(flapPacketListener);
    }

    /**
     * Pauses this SNAC processor. A paused SNAC processor does not send any
     * SNAC commands to the server until a call to {@link #unpause()}. Note that
     * if this method is called while the processor is already paused, no action
     * will be taken. Note that SNAC commands can still be {@linkplain #sendSnac
     * sent} while the processor is paused; however, they will not be sent to
     * the server until unpausing.
     *
     * @see #unpause()
     * @see #isPaused()
     *
     * @see net.kano.joscar.snaccmd.conn.PauseCmd
     */
    public synchronized final void pause() {
        if (paused) return;

        queueManager.pause(this);

        paused = true;
    }

    /**
     * Unpauses this SNAC processor if previously paused with a call to {@link
     * #pause}. SNAC commands sent during the paused period will begin to be
     * sent to the server (depending on the implementation of the {@linkplain
     * #setSnacQueueManager queue manager}).
     *
     * @see #pause()
     * @see #isPaused()
     *
     * @see net.kano.joscar.snaccmd.conn.ResumeCmd
     */
    public synchronized final void unpause() {
        if (!paused) return;

        queueManager.unpause(this);

        paused = false;
    }

    /**
     * Returns whether this SNAC processor is currently paused.
     *
     * @return whether this SNAC processor is currently paused
     *
     * @see #pause
     */
    public synchronized final boolean isPaused() { return paused; }

    /**
     * Attaches to the given FLAP processor without clearing any SNAC queues.
     * Effectively "moves" this SNAC connection transparently to the given
     * processor. Note that if this processor is {@linkplain #pause paused},
     * a call to <code>migrate</code> will <i>not</i> unpause it. Unpausing must
     * be done explicitly with a call to {@link #unpause}.
     *
     * @param processor a new FLAP processor to use for this SNAC connection
     *
     * @see net.kano.joscar.snaccmd.conn.MigrationNotice
     */
    public synchronized final void migrate(FlapProcessor processor) {
        DefensiveTools.checkNull(processor, "processor");

        if (flapProcessor != null) detachMinimally();

        attachMinimally(processor);
    }

    /**
     * Detaches from the currently attached FLAP processor, if any. This method
     * effectively <b>resets this SNAC processor</b>, causing any information
     * about the current connection such as queued SNAC commands or SNAC request
     * ID's to be <b>discarded</b>. This method is thus <b>not</b> useful for
     * {@linkplain net.kano.joscar.snaccmd.conn.MigrationNotice migrating}. Note
     * that this processor will be unpaused if it is currently paused.
     *
     * @see #migrate
     */
    public synchronized final void detach() {
        if (this.flapProcessor != null) {
            detachMinimally();

            paused = false;

            queueManager.clearQueue(this);
        }
    }

    /**
     * Detaches from the current FLAP processor without clearing any queued
     * SNAC requests.
     */
    private synchronized final void detachMinimally() {
        this.flapProcessor.removeVetoablePacketListener(flapPacketListener);

        clearAllRequests();

        this.flapProcessor = null;
    }

    /**
     * Adds a packet listener to listen for incoming SNAC packets.
     *
     * @param l the listener to add
     */
    public synchronized final void addPacketListener(SnacPacketListener l) {
        DefensiveTools.checkNull(l, "l");

        if (!packetListeners.contains(l)) packetListeners.add(l);
    }

    /**
     * Removes a packet listener from the list of listeners.
     *
     * @param l the listener to remove
     */
    public synchronized final void removePacketListener(SnacPacketListener l) {
        DefensiveTools.checkNull(l, "l");

        packetListeners.remove(l);
    }

    /**
     * Adds a "global response listener" to listen for incoming SNAC request
     * responses. The given listener will be notified of any incoming responses
     * to previously sent outgoing SNAC requests. See {@linkplain SnacProcessor
     * above} for details on when global response listeners' event handling
     * methods are called.
     *
     * @param l the listener to add
     */
    public synchronized final void addGlobalResponseListener(
            SnacResponseListener l) {
        DefensiveTools.checkNull(l, "l");

        if (!responseListeners.contains(l)) responseListeners.add(l);
    }

    /**
     * Removes a "global response listener" from the list of listeners.
     *
     * @param l the listener to remove
     */
    public synchronized final void removeGlobalResponseListener(
            SnacResponseListener l) {
        DefensiveTools.checkNull(l, "l");

        responseListeners.remove(l);
    }

    /**
     * Adds a <i>vetoable</i> packet listener to this SNAC processor. A vetoable
     * SNAC packet listener has the ability to halt the processing of a given
     * packet upon its receipt.
     *
     * @param l the listener to add.
     */
    public synchronized final void addVetoablePacketListener(
            VetoableSnacPacketListener l) {
        DefensiveTools.checkNull(l, "l");

        if (!vetoableListeners.contains(l)) vetoableListeners.add(l);
    }

    /**
     * Removes a vetoable packet listener from the list of listeners.
     * @param l the listener to remove
     */
    public synchronized final void removeVetoablePacketListener(
            VetoableSnacPacketListener l) {
        DefensiveTools.checkNull(l, "l");

        vetoableListeners.remove(l);
    }

    /**
     * Adds a SNAC preprocessor to the list of preprocessors. Preprocessors
     * are the first listeners called when a SNAC packet arrives, and are
     * allowed to modify the contents of a packet.
     *
     * @param p the preprocessor to add
     */
    public synchronized final void addPreprocessor(SnacPreprocessor p) {
        DefensiveTools.checkNull(p, "p");

        if (!preprocessors.contains(p)) preprocessors.add(p);
    }

    /**
     * Removes a SNAC preprocessor from the list of SNAC preprocessors.
     * @param p the preprocessor to remove
     */
    public synchronized final void removePreprocessor(SnacPreprocessor p) {
        DefensiveTools.checkNull(p, "p");

        preprocessors.remove(p);
    }

    /**
     * Sets this SNAC processor's SNAC queue manager. A SNAC queue manager
     * has almost complete control over when individual SNAC commands are
     * actually sent to the server. If <code>mgr</code> is <code>null</code>,
     * as is the default value, all SNACs will be sent to the server immediately
     * (the queue manager will be set to an {@link ImmediateSnacQueueManager})
     *
     * @param mgr the new SNAC queue manager, or <code>null</code> to send all
     *        SNACs immediately
     */
    public synchronized final void setSnacQueueManager(SnacQueueManager mgr) {
        // tell the old queue manager to forget about us
        queueManager.clearQueue(this);

        // we allow null for the manager argument, which means we use our own
        // immediate SNAC queue manager
        if (mgr == null) mgr = new ImmediateSnacQueueManager();

        queueManager = mgr;

        // keep everything synchronized
        if (paused) mgr.pause(this);
    }

    /**
     * Returns this SNAC processor's current SNAC queue manager. Note that
     * this value will <i>never</i> be <code>null</code>, even after an explicit
     * call to {@link #setSnacQueueManager setSnacQueueManager(null)}. See that
     * method's documentation for details.
     *
     * @return this SNAC processor's current SNAC queue manager
     */
    public synchronized final SnacQueueManager getSnacQueueManager() {
        return queueManager;
    }

    /**
     * Sets the "time to live" for SNAC requests, in seconds. After roughly this
     * amount of time, SNAC requests will be removed from the request list,
     * and any future responses will be processed as if they were normal
     * <code>SnacPacket</code>s and not responses to requests.
     * <br>
     * <br>
     * Note that this value must be at least zero. A value of zero enables
     * several special cases to use less memory and CPU time involved in sending
     * SNAC requests. A value of zero also means that SNAC requests' listeners
     * will <i>never</i> be called with responses, as request ID's are not
     * stored at all.
     *
     * @param requestTtl the new "time to live" for SNAC requests, in seconds
     */
    public synchronized void setRequestTtl(int requestTtl) {
        DefensiveTools.checkRange(requestTtl, "requestTtl", 0);

        this.requestTtl = requestTtl;
    }

    /**
     * Returns the current "time to live" for SNAC requests, in seconds.
     *
     * @return the current SNAC request "time to live"
     */
    public synchronized int getRequestTtl() { return requestTtl; }

    /**
     * Processes an incoming FLAP packet. The packet is processed through
     * the list of preprocessors, a SnacCommand is generated, vetoable listeners
     * are called, and, finally, packet listeners are called.
     *
     * @param e the FLAP packet event to process
     */
    private synchronized void processPacket(FlapPacketEvent e) {
        boolean logFine = logger.isLoggable(Level.FINE);
        boolean logFiner = logger.isLoggable(Level.FINER);

        SnacFlapCmd flapCmd = ((SnacFlapCmd) e.getFlapCommand());
        SnacPacket snacPacket = flapCmd.getSnacPacket();

        MutableSnacPacket mutablePacket = null;
        for (Iterator it = preprocessors.iterator(); it.hasNext();) {
            SnacPreprocessor preprocessor = (SnacPreprocessor) it.next();

            if (mutablePacket == null) {
                mutablePacket = new MutableSnacPacket(snacPacket);
            }

            if (logFiner) {
                logger.finer("Running snac preprocessor " + preprocessor);
            }

            try {
                preprocessor.process(mutablePacket);
            } catch (Throwable t) {
                if (logFiner) {
                    logger.finer("Preprocessor " + preprocessor + " threw " +
                            "exception " + t);
                }
                flapProcessor.handleException(ERRTYPE_SNAC_PACKET_PREPROCESSOR,
                        t, preprocessor);
                continue;
            }
        }
        if (mutablePacket != null && mutablePacket.isChanged()) {
            snacPacket = mutablePacket.toSnacPacket();
        }

        SnacCommand cmd = generateSnacCommand(snacPacket);

        if (logFine) {
            logger.fine("Converted Snac packet " + snacPacket + " to " + cmd);
        }

        Long key = new Long(snacPacket.getReqid());
        RequestInfo reqInfo = (RequestInfo) requests.get(key);

        SnacPacketEvent event = new SnacPacketEvent(e, this, snacPacket, cmd);

        if (reqInfo != null) {
            SnacRequest request = reqInfo.getRequest();
            if (logFiner) {
                logger.finer("This Snac packet is a response to a request!");
            }

            SnacResponseEvent sre = new SnacResponseEvent(event, request);

            for (Iterator it = responseListeners.iterator(); it.hasNext();) {
                SnacResponseListener l = (SnacResponseListener) it.next();

                try {
                    l.handleResponse(sre);
                } catch (Throwable t) {
                    flapProcessor.handleException(
                            ERRTYPE_SNAC_RESPONSE_LISTENER, t, l);
                }
            }

            try {
                request.gotResponse(sre);

            } catch (Throwable t) {
                flapProcessor.handleException(ERRTYPE_SNAC_REQUEST_LISTENER, t,
                        request);
            }
            return;
        }

        for (Iterator it = vetoableListeners.iterator(); it.hasNext();) {
            VetoableSnacPacketListener listener
                    = (VetoableSnacPacketListener) it.next();

            if (logFiner) {
                logger.finer("Running vetoable Snac packet listener "
                        + listener);
            }

            Object result;
            try {
                result = listener.handlePacket(event);
            } catch (Throwable t) {
                flapProcessor.handleException(ERRTYPE_SNAC_PACKET_LISTENER, t,
                        listener);
                continue;
            }
            if (result != VetoableSnacPacketListener.CONTINUE_PROCESSING) {
                return;
            }
        }

        for (Iterator it = packetListeners.iterator(); it.hasNext();) {
            SnacPacketListener listener = (SnacPacketListener) it.next();

            if (logFiner) {
                logger.finer("Running Snac packet listener " + listener);
            }

            try {
                listener.handleSnacPacket(event);
            } catch (Throwable t) {
                flapProcessor.handleException(ERRTYPE_SNAC_PACKET_LISTENER, t,
                        listener);
            }
        }

        if (logFiner) logger.finer("Finished processing Snac");
    }

    /**
     * Generates a <code>SnacCommand</code> from the given
     * <code>SnacPacket</code> using the user-registered and default factories.
     *
     * @param packet the packet from which the <code>SnacCommand</code> should
     *        be generated
     * @return an appropriate <code>SnacCommand</code> for the given packet
     */
    private synchronized SnacCommand generateSnacCommand(SnacPacket packet) {
        CmdType type = new CmdType(packet.getFamily(), packet.getCommand());

        SnacCmdFactory factory = factories.findFactory(type);

        if (factory == null) return null;

        return factory.genSnacCommand(packet);
    }

    /**
     * Removes the given request from the request list and passes a
     * <code>SnacRequestTimeoutEvent</code> to the request itself.
     *
     * @param reqInfo the request to timeout
     */
    private synchronized final void timeoutRequest(RequestInfo reqInfo) {
        SnacRequest request = reqInfo.getRequest();
        SnacRequestTimeoutEvent event = new SnacRequestTimeoutEvent(
                flapProcessor, this, request, requestTtl);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Snac request timed out: " + request);
        }
        request.timedOut(event);
    }

    /**
     * "Times out" all requests on the request list.
     */
    private synchronized final void clearAllRequests() {
        for (Iterator it = requests.values().iterator(); it.hasNext();) {
            RequestInfo reqInfo = (RequestInfo) it.next();

            timeoutRequest(reqInfo);
        }

        requests.clear();
        requestQueue.clear();
    }

    /**
     * Removes any SNAC requests who were sent long enough ago such that their
     * lifetime has passed the {@link #requestTtl}.
     */
    private synchronized final void cleanRequests() {
        if (requestQueue.isEmpty()) return;

        if (requestTtl == 0) {
            clearAllRequests();
            return;
        }

        long time = System.currentTimeMillis();

        long ttlms = requestTtl * 1000;

        for (Iterator it = requestQueue.iterator(); it.hasNext();) {
            RequestInfo reqInfo = (RequestInfo) it.next();

            long sentTime = reqInfo.getSentTime();
            if (sentTime == -1) continue;

            long diff = time - sentTime;

            // these are in order, so this is okay. it may not be in the future,
            // with rate limiting and such.
            if (diff < ttlms) break;

            // tell the request it's timed out
            timeoutRequest(reqInfo);

            // and remove the request from the queue and the reqid map
            it.remove();
            requests.remove(new Long(reqInfo.getRequest().getReqid()));
        }
    }

    /**
     * Sends the given <code>SnacRequest</code> to the attached FLAP connection.
     * It may not immediately be sent in future releases due to possible
     * features such as rate limiting prevention.
     *
     * @param request the SNAC request to send
     */
    public synchronized final void sendSnac(SnacRequest request) {
        DefensiveTools.checkNull(request, "request");

        SnacCommand command = request.getCommand();

        registerSnacRequest(request);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Queueing Snac request #" + lastReqid + ": "
                    + command);
        }

        queueManager.queueSnac(this, request);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Finished queueing Snac request #" + lastReqid);
        }
    }

    /**
     * Sends the given SNAC request to the server, bypassing the SNAC request
     * queue and any {@linkplain #pause pausing} status that may be present.
     *
     * @param request the request to send
     *
     * @see #setSnacQueueManager
     */
    public synchronized final void sendSnacImmediately(SnacRequest request) {
        DefensiveTools.checkNull(request, "request");

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Sending SNAC request " + request);
        }

        RequestInfo reqInfo = registerSnacRequest(request);

        if (reqInfo.getSentTime() != -1) {
            throw new IllegalArgumentException("SNAC request " + request
                    + " was already sent");
        }

        long reqid = reqInfo.getRequest().getReqid();
        flapProcessor.sendFlap(new SnacFlapCmd(reqid, request.getCommand()));

        long sentTime = System.currentTimeMillis();
        reqInfo.sent(sentTime);
        request.sent(new SnacRequestSentEvent(flapProcessor, this, request,
                sentTime));
        if (requestTtl != 0) {
            requestQueue.add(reqInfo);
        } else {
            requests.remove(new Long(reqid));
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Finished sending SNAC request " + request);
        }
    }

    /**
     * Registers a SNAC request, giving it a request ID and remembering that ID
     * for future reference. If the given request has already been registered,
     * no change takes place, but its corresponding <code>RequestInfo</code> is
     * still returned.
     *
     * @param request the request to register
     * @return a <code>RequestInfo</code> corresponding to the given request
     */
    private synchronized RequestInfo registerSnacRequest(SnacRequest request) {
        if (request.getReqid() != -1) {
            return (RequestInfo) requests.get(new Long(request.getReqid()));
        }

        // this is sent as an unsigned int, so it has to wrap like one. we
        // avoid request ID zero because that seems like a value the server
        // might use to denote a lack of a request ID.
        if (lastReqid == REQID_MAX) lastReqid = REQID_MIN;
        else lastReqid++;

        request.setReqid(lastReqid);

        Long key = new Long(lastReqid);

        cleanRequests();

        RequestInfo reqInfo = new RequestInfo(request);

        requests.put(key, reqInfo);
        return reqInfo;
    }

    /**
     * Sets the default SNAC command factory list. See {@linkplain SnacProcessor
     * above} for details on what exactly this means.
     *
     * @param list the new default SNAC command factory list, or
     *        <code>null</code>
     */
    public synchronized final void setDefaultSnacCmdFactoryList(
            SnacCmdFactoryList list) {
        factories.setDefaultFactoryList(list);
    }

    /**
     * Returns the current default SNAC command factory list. See {@linkplain
     * SnacProcessor above} for details on what this is.
     *
     * @return the current default SNAC command factory list
     */
    public synchronized final SnacCmdFactoryList
            getDefaultSnacCmdFactoryList() {
        return factories.getDefaultFactoryList();
    }

    /**
     * Registers the given SNAC command factory for all command types specified
     * by the result of invoking its <code>getSupportedTypes</code> method.
     *
     * @param factory the SNAC command factory to fully register
     */
    public synchronized final void registerSnacFactory(SnacCmdFactory factory) {
        factories.registerSupported(factory);
    }

    /**
     * Registers the given SNAC command factory for the given command type.
     * If the given factory does not {@linkplain
     * SnacCmdFactory#getSupportedTypes support} the given type, no action is
     * taken.
     *
     * @param cmdType the command type for which the given factory should be
     *        registered
     * @param factory the factory to register for the given command type
     */
    public synchronized final void registerSnacFactory(CmdType cmdType,
            SnacCmdFactory factory) {
        factories.register(cmdType, factory);
    }

    /**
     * Unregisters the given SNAC command factory from the given command type,
     * if it is currently registered for that type.
     *
     * @param type the command type from which the given factory should be
     *        unregistered
     * @param factory the SNAC command factory to unregister from the given
     *        command type
     */
    public synchronized final void unregisterSnacFactory(CmdType type,
            SnacCmdFactory factory) {
        factories.unregister(type, factory);
    }

    /**
     * Unregisters the given SNAC command factory from all command types for
     * which it is registered.
     *
     * @param factory the SNAC command factory to completely unregister
     */
    public synchronized final void unregisterSnacFactory(
            SnacCmdFactory factory) {
        factories.unregisterAll(factory);
    }

    /**
     * A simple class holding a SNAC request and related request-specific
     * information.
     */
    private static class RequestInfo {
        /**
         * The SNAC request with which this object is associated.
         */
        private final SnacRequest request;

        /**
         * The number of milliseconds between the unix epoch and the time at
         * which this <code>RequestInfo</code>'s request was sent.
         */
        private long sent = -1;

        /**
         * Creates a new <code>RequestInfo</code> for the given request.
         *
         * @param request the request to associate with this
         *        <code>RequestInfo</code>
         */
        public RequestInfo(SnacRequest request) {
            this.request = request;
        }

        /**
         * Marks the request associated with this object as sent.
         *
         * @param time the time at which the request was sent
         * @throws IllegalStateException if this object's associated request has
         *         already been sent
         */
        public synchronized final void sent(long time)
                throws IllegalStateException {
            if (sent != -1) {
                throw new IllegalStateException(request + " was already sent " +
                        ((System.currentTimeMillis() - sent)/1000)
                        + " seconds ago");
            }
            sent = time;
        }

        /**
         * Returns the request associated with this <code>RequestInfo</code>.
         *
         * @return this <code>RequestInfo</code>'s associated SNAC request
         */
        public final SnacRequest getRequest() { return request; }

        /**
         * Returns the time, in milliseconds since unix epoch, at which the
         * associated request was sent.
         *
         * @return the time at which the associated SNAC request was sent
         */
        public synchronized final long getSentTime() { return sent; }
    }
}
