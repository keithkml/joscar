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
 *  File created by keith @ Feb 17, 2003
 *
 */

package net.kano.joscar.snac;

import net.kano.joscar.flap.FlapPacketEvent;
import net.kano.joscar.flap.FlapProcessor;
import net.kano.joscar.flap.VetoableFlapPacketListener;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.flapcmd.SnacFlapCmd;
import net.kano.joscar.flapcmd.MutableSnacPacket;
import net.kano.joscar.DefensiveTools;

import java.util.*;
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
 * error types used are {@link #ERROR_SNAC_PACKET_PREPROCESSOR}, {@link
 * #ERROR_SNAC_PACKET_LISTENER}, and {@link #ERROR_SNAC_REQUEST_LISTENER}. See
 * individual documentation for each for further detail.
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
 * <li> If the request ID matches that of a previous outgoing request, an event
 * is passed to that request's listeners (see {@link SnacRequest}), and
 * processing within <code>SnacProcessor</code> stops </li>
 * <li> Otherwise, an event is passed to each of the registered <i>vetoable</i>
 * packet listeners, halting immediately if a listener says to </li>
 * <li> If no vetoable listener has halted processing, an event is next passed
 * to all registered non-vetoable (that is, normal
 * <code>SnacPacketListener</code>) packet listeners.</li>
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
    public static final Object ERROR_SNAC_PACKET_PREPROCESSOR
            = "ERROR_SNAC_PACKET_PREPROCESSOR";

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
    public static final Object ERROR_SNAC_PACKET_LISTENER
            = "ERROR_SNAC_PACKET_LISTENER";

    /**
     * An error type indicating that an exception was thrown while calling a
     * {@linkplain SnacRequest SNAC request} {@linkplain SnacRequestListener
     * response listener} to handling a response to a SNAC request. In this
     * case, the extra error information (the value returned by {@link
     * net.kano.joscar.flap.FlapExceptionEvent#getReason getReason()}) will be
     * the <code>SnacRequest</code> whose listener threw the exception.
     */
    public static final Object ERROR_SNAC_REQUEST_LISTENER
            = "ERROR_SNAC_REQUEST_LISTENER";

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

    /**
     * The maximum request ID value before it wraps to zero.
     */
    private static final long REQID_MAX = 0xffffffffL - 1L;

    /**
     * The request ID of the last SNAC command sent.
     */
    private long lastReqid = 1;

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
    private LinkedHashMap requests = new LinkedHashMap();

    /** A SNAC queue manager for this processor. */
    private SnacQueueManager queueManager = null;

    /**
     * The FLAP packet listener we add to whichever FLAP processor to which we
     * become attached.
     */
    private VetoableFlapPacketListener flapPacketListener
            = new VetoableFlapPacketListener() {
        public Object handlePacket(FlapPacketEvent e) {
            if (e.getFlapCommand() instanceof SnacFlapCmd) {
                logger.finer("SnacProcessor intercepted channel-2 snac " +
                        "command");

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

        this.flapProcessor = processor;
        this.flapProcessor.addVetoablePacketListener(flapPacketListener);
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
     * Detaches from the currently attached FLAP processor, if any. The request
     * list is cleared, to prevent responses with the same ID's on a future
     * connection from being passed to unrelated request listeners.
     */
    public synchronized final void detach() {
        if (this.flapProcessor != null) {
            this.flapProcessor.removeVetoablePacketListener(flapPacketListener);

            if (queueManager != null) queueManager.clearQueue(this);

            clearAllRequests();

            this.flapProcessor = null;
        }
    }

    /**
     * Adds a packet listener to listen for incoming SNAC packets.
     *
     * @param l the listener to add
     */
    public synchronized final void addPacketListener(SnacPacketListener l) {
        if (!packetListeners.contains(l)) packetListeners.add(l);
    }

    /**
     * Removes a packet listener from the list of listeners.
     *
     * @param l the listener to remove
     */
    public synchronized final void removePacketListener(SnacPacketListener l) {
        packetListeners.remove(l);
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
        if (!vetoableListeners.contains(l)) vetoableListeners.add(l);
    }

    /**
     * Removes a vetoable packet listener from the list of listeners.
     * @param l the listener to remove
     */
    public synchronized final void removeVetoablePacketListener(
            VetoableSnacPacketListener l) {
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
        if (!preprocessors.contains(p)) preprocessors.add(p);
    }

    /**
     * Removes a SNAC preprocessor from the list of SNAC preprocessors.
     * @param p the preprocessor to remove
     */
    public synchronized final void removePreprocessor(SnacPreprocessor p) {
        preprocessors.remove(p);
    }

    /**
     * Sets this SNAC processor's SNAC queue manager. A SNAC queue manager
     * has almost complete control over when individual SNAC commands are
     * actually sent to the server. If <code>mgr</code> is <code>null</code>,
     * as is the default value, all SNACs are sent to the server immediately.
     *
     * @param mgr the new SNAC queue manager, or <code>null</code> to send all
     *        SNACs immediately
     */
    public synchronized final void setSnacQueueManager(SnacQueueManager mgr) {
        queueManager = mgr;
    }

    /**
     * Sets the "time to live" for SNAC requests, in seconds. After roughly this
     * amount of time, SNAC requests will be removed from the request list,
     * and any future responses will be processed as if they were normal
     * <code>SnacPacket</code>s and not responses to requests.
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
        SnacCommand cmd = null;

        SnacFlapCmd flapCmd = ((SnacFlapCmd) e.getFlapCommand());
        SnacPacket snacPacket = flapCmd.getSnacPacket();

        MutableSnacPacket mutablePacket = null;
        for (Iterator it = preprocessors.iterator(); it.hasNext();) {
            SnacPreprocessor preprocessor = (SnacPreprocessor) it.next();

            if (mutablePacket == null) {
                mutablePacket = new MutableSnacPacket(snacPacket);
            }

            logger.finer("Running snac preprocessor " + preprocessor);

            try {
                preprocessor.process(mutablePacket);
            } catch (Throwable t) {
                logger.finer("Preprocessor " + preprocessor + " threw " +
                        "exception " + t);
                flapProcessor.handleException(ERROR_SNAC_PACKET_PREPROCESSOR, t,
                        preprocessor);
                continue;
            }
        }
        if (mutablePacket != null && mutablePacket.isChanged()) {
            snacPacket = mutablePacket.toSnacPacket();
        }

        cmd = generateSnacCommand(snacPacket);

        logger.fine("Converted Snac packet " + snacPacket + " to " + cmd);

        Long key = new Long(snacPacket.getReqid());
        RequestInfo reqInfo = (RequestInfo) requests.get(key);

        SnacPacketEvent event = new SnacPacketEvent(e, this, snacPacket, cmd);

        if (reqInfo != null) {
            SnacRequest request = reqInfo.getRequest();
            logger.finer("This Snac packet is a response to a request; " +
                    "processing");
            try {
                processResponse(event, request);
            } catch (Throwable t) {
                flapProcessor.handleException(ERROR_SNAC_REQUEST_LISTENER, t,
                        request);
            }
            return;
        }

        for (Iterator it = vetoableListeners.iterator(); it.hasNext();) {
            VetoableSnacPacketListener listener
                    = (VetoableSnacPacketListener) it.next();

            logger.finer("Running vetoable Snac packet listener " + listener);

            Object result;
            try {
                result = listener.handlePacket(event);
            } catch (Throwable t) {
                flapProcessor.handleException(ERROR_SNAC_PACKET_LISTENER, t,
                        listener);
                continue;
            }
            if (result != VetoableSnacPacketListener.CONTINUE_PROCESSING) {
                return;
            }
        }

        for (Iterator it = packetListeners.iterator(); it.hasNext();) {
            SnacPacketListener listener = (SnacPacketListener) it.next();

            logger.finer("Running Snac packet listener " + listener);

            try {
                listener.handlePacket(event);
            } catch (Throwable t) {
                flapProcessor.handleException(ERROR_SNAC_PACKET_LISTENER, t,
                        listener);
            }
        }

        logger.finer("Finished processing Snac");
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
     * Processes a response to a <code>SnacRequest</code> received on this
     * SNAC connection.
     *
     * @param event the associated SNAC packet event
     * @param request the SNAC request to which the given packet event is a
     *        response
     */
    private synchronized final void processResponse(SnacPacketEvent event,
            SnacRequest request) {
        SnacResponseEvent sre = new SnacResponseEvent(event, request);

        request.gotResponse(sre);
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

        logger.finer("Snac request timed out: " + request);
        request.timedOut(event);
    }

    /**
     * "Times out" all requests on the request list.
     */
    private synchronized final void clearAllRequests() {
        for (final Iterator it = requests.values().iterator(); it.hasNext();) {
            final RequestInfo reqInfo = (RequestInfo) it.next();

            timeoutRequest(reqInfo);
        }

        requests.clear();
    }

    /**
     * Removes any SNAC requests who were sent long enough ago such that their
     * lifetime has passed the {@link #requestTtl}.
     */
    private synchronized final void cleanRequests() {
        final long time = System.currentTimeMillis();
        for (final Iterator it = requests.values().iterator(); it.hasNext();) {
            final RequestInfo reqInfo = (RequestInfo) it.next();

            final long sentTime = reqInfo.getSentTime();
            if (sentTime == -1) continue;

            final long diff = time - sentTime;

            // these are in order, so this is okay. it may not be in the future,
            // with rate limiting and such.
            if (diff / 1000 < requestTtl) break;

            timeoutRequest(reqInfo);

            it.remove();
        }
    }

    /**
     * Sends the given <code>SnacRequest</code> to the attached FLAP connection.
     * It may not immediately be sent in future releases due to possible
     * features such as rate limiting prevention.
     *
     * @param request the SNAC request to send
     * @throws NullPointerException if this SNAC connection is not currently
     *         attached to a FLAP connection
     */
    public synchronized final void sendSnac(SnacRequest request)
            throws NullPointerException {
        DefensiveTools.checkNull(request, "request");

        // this is sent as an unsigned int, so it has to wrap like one. we
        // avoid request ID zero because that seems like a value the server
        // might use to denote a lack of a request ID.
        if (lastReqid == REQID_MAX) lastReqid = 1;
        else lastReqid++;

        request.setReqid(lastReqid);

        Long key = new Long(lastReqid);

        cleanRequests();

        RequestInfo reqInfo = new RequestInfo(request);

        requests.put(key, reqInfo);

        SnacCommand command = request.getCommand();

        logger.fine("Queueing Snac request #" + lastReqid + ": "
                + command);

        if (queueManager == null) {
            reallySendSnac(request);
        } else {
            queueManager.queueSnac(this, request);
        }

        logger.finer("Finished queueing Snac request #" + lastReqid);
    }

    /**
     * Sends the given SNAC request that was previously sent to the SNAC queue
     * manager to the server.
     *
     * @param request the request to send
     * @throws NullPointerException if the given request is <code>null</code> or
     *         if
     */
    synchronized final void reallySendSnac(SnacRequest request)
            throws NullPointerException, IllegalArgumentException {
        logger.fine("Sending SNAC request " + request);
        
        long reqid = request.getReqid();
        Long key = new Long(reqid);
        RequestInfo reqInfo = (RequestInfo) requests.get(key);

        if (reqInfo == null) throw new NullPointerException();
        if (reqInfo.getSentTime() != -1) {
            throw new IllegalArgumentException("SNAC request " + request
                    + " was already sent");
        }

        flapProcessor.send(new SnacFlapCmd(reqid, request.getCommand()));

        long sentTime = System.currentTimeMillis();
        reqInfo.sent(sentTime);
        request.sent(new SnacRequestSentEvent(flapProcessor, this, request,
                sentTime));
        logger.finer("Finished sending SNAC request " + request);
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
    private class RequestInfo {
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
