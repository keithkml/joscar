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
 *  File created by keith @ Apr 24, 2003
 *
 */

package net.kano.joscar.rv;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flap.FlapProcessor;
import net.kano.joscar.snac.*;
import net.kano.joscar.snaccmd.AbstractIcbm;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.OscarTools;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joscar.snaccmd.icbm.RvResponse;
import net.kano.joscar.snaccmd.icbm.SendRvIcbm;

import java.util.*;
import java.util.logging.Logger;

/**
 * Provides an easy interface for creating and manipulating "rendezvous
 * sessions" as well as sending and receiving individual rendezvous commands
 * nested in {@linkplain net.kano.joscar.snaccmd.icbm.AbstractRvIcbm RV ICBM's}.
 * <br>
 * <br>
 * This class manages rendezvous "sessions" as follows. Every rendezvous command
 * contains a unique session ID. For example, if you attempt to send a file to
 * someone, and that person rejects the file transfer, each of those commands
 * will (normally) contain the same session ID, as they pertain to the same
 * "session": the file transfer you initiated. This class recognizes such
 * sessions and creates appropriate <code>RvSession</code> instances for each
 * new rendezvous session created. It is certainly possible to use rendezvous
 * sessions without using a <code>RvProcessor</code>, but this class attempts
 * to make session management and <code>RvCommand</code> generation simpler and
 * somewhat transparent to the developer.
 * <br>
 * <br>
 * This class will generate exceptions in the attached SNAC processor's attached
 * FLAP processor's exception handler should any exceptions occur when running
 * callbacks or RV command factory methods.
 * <table>
 * <tr><th>Exception type</th><th>Meaning</th><th>Reason object</th></tr>
 *
 * <tr><td>{@link #ERRTYPE_RV_CMD_GEN}</td><td>This type indicates that an
 * exception was thrown when attempting to convert an incoming RV ICBM to a
 * <code>RvCommand</code> using a <code>RvCommandFactory</code>'s
 * <code>genRvCommand</code> method.</td><td>The reason object in this case will
 * be the <code>RecvRvIcbm</code> whose attempted processing caused the given
 * exception.</td></tr>
 *
 * <tr><td>{@link #ERRTYPE_RV_LISTENER}</td><td>This type indicates that an
 * exception was thrown when calling a method of a
 * <code>RvProcessorListener</code>.</td> <td>The reason object in this case
 * will be the <code>RvProcessorListener</code> which threw the
 * exception.</td></tr>
 *
 * <tr><td>{@link #ERRTYPE_RV_SESSION_LISTENER}</td><td>This type indicates that
 * an exception was thrown when calling a method of a
 * <code>RvSessionListener</code> attached to a specific
 * <code>RvSession</code>.</td><td>The reason object in this case is the
 * <code>RvSessionListener</code> that threw the exception. </td></tr>
 * </table>
 * <br>
 * When {@linkplain #attachToSnacProcessor attached} to a
 * <code>SnacProcessor</code>, an instance of this class will intercept all
 * <code>RecvRvIcbm</code>s and all <code>RvResponse</code>s. After attaching
 * an RV processor to a given SNAC processor, that SNAC processor's packet
 * listeners will no longer receive packet events for either of these two
 * commands (until detaching the RV processor).
 * <br>
 * <br>
 * Note that if an <code>RvProcessor</code> is not attached to a SNAC processor,
 * all attempts at sending rendezvous commands and responses (via {@link
 * RvSession#sendRv} and {@link RvSession#sendResponse} will be <i>quietly
 * ignored</i>.
 * <br>
 * <br>
 * <code>RvProcessor</code> uses the Java Logging API namespace
 * <code>"net.kano.joscar.rv"</code>, logging various events at the levels
 * <code>Level.FINE</code> and <code>Level.FINER</code>, in order to, hopefully,
 * ease the debugging of rendezvous-related applications. For more information
 * on how to log such events, consult the Java Logging API reference at the <a
 * href="http://java.sun.com/j2se">J2SE website</a>.
 */
public class RvProcessor {
    /**
     * A {@linkplain net.kano.joscar.flap.FlapExceptionHandler FLAP exception
     * handler} error type indicating that an exception was thrown when
     * generating a <code>RvCommand</code> with an attached
     * <code>RvCommandFactory</code>. See {@linkplain RvProcessor above} for
     * details.
      */
    public static final Object ERRTYPE_RV_CMD_GEN = "ERRTYPE_RV_CMD_GEN";
    /**
     * A {@linkplain net.kano.joscar.flap.FlapExceptionHandler FLAP exception
     * handler} error type indicating that an exception was thrown when
     * calling a method of an attached <code>RvProcessorListener</code>. See
     * {@linkplain RvProcessor above} for details.
     */
    public static final Object ERRTYPE_RV_LISTENER = "ERRTYPE_RV_LISTENER";
    /**
     * A {@linkplain net.kano.joscar.flap.FlapExceptionHandler FLAP exception
     * handler} error type indicating that an exception was thrown when
     * calling a method of an <code>RvSessionListener</code> attached to an
     * <code>RvSession</code>. See {@linkplain RvProcessor above} for details.
     */
    public static final Object ERRTYPE_RV_SESSION_LISTENER
            = "ERRTYPE_RV_SESSION_LISTENER";

    /** A logger used to log RV-related events. */
    private static final Logger logger = Logger.getLogger("net.kano.joscar.rv");

    /** The SNAC processor to which this RV processor is attached. */
    private SnacProcessor snacProcessor = null;

    /** The session ID of the last outgoing rendezvous session. */
    private long lastSessionId = new Random().nextLong();

    /** The sessions being managed by this RV processor. */
    private Map sessions = new HashMap();

    /** The "new session listeners" attached to this processor. */
    private List rvListeners = new ArrayList();

    /** The <code>RvCommand</code> factories attached to this processor. */
    private Map rvFactories = new HashMap();

    /**
     * The packet listener that is attached to whichever SNAC processor this RV
     * processor is attached to.
     */
    private VetoableSnacPacketListener packetListener
            = new VetoableSnacPacketListener() {
        public Object handlePacket(SnacPacketEvent event) {
            SnacCommand cmd = event.getSnacCommand();

            if (cmd instanceof RecvRvIcbm) {
                logger.finer("RvProcessor got RecvRvIcbm: " + cmd);

                processRv(event);

                return STOP_PROCESSING_LISTENERS;

            } else if (cmd instanceof RvResponse) {
                logger.finer("RvProcessor got RvResponse: " + cmd);

                processResponse(event);

                return STOP_PROCESSING_LISTENERS;
            }

            return CONTINUE_PROCESSING;
        }
    };

    /**
     * Creates a RV processor which is not attached to any SNAC processor and
     * which contains no <code>RvCommand</code> factories or new session
     * listeners.
     */
    public RvProcessor() { }

    /**
     * Creates a RV processor attached to the given SNAC processor and
     * which contains no <code>RvCommand</code> factories or new session
     * listeners.
     *
     * @param snacProcessor the SNAC processor to which to attach
     *
     * @see #attachToSnacProcessor
     */
    public RvProcessor(SnacProcessor snacProcessor) {
        attachToSnacProcessor(snacProcessor);
    }

    /**
     * Returns the SNAC processor to which this RV processor is currently
     * attached, or <code>null</code> if none is attached.
     *
     * @return the SNAC processor to which this RV processor is currently
     *         attached
     */
    public synchronized final SnacProcessor getSnacProcessor() {
        return snacProcessor;
    }

    /**
     * "Attaches" this RV processor to the given SNAC processor. See {@linkplain
     * RvProcessor above} for details on attaching. Note that calling this
     * method implies a call to {@link #detach}; that is, an RV processor cannot
     * be attached to more than one SNAC processor.
     *
     * @param snacProcessor the SNAC processor to which this RV processor should
     *        attach
     *
     * @see #detach
     */
    public synchronized final void attachToSnacProcessor(
            SnacProcessor snacProcessor) {
        DefensiveTools.checkNull(snacProcessor, "snacProcessor");

        detach();

        this.snacProcessor = snacProcessor;
        snacProcessor.addVetoablePacketListener(packetListener);
    }

    /**
     * "Detaches" this RV processor from the currently attached SNAC processor,
     * if any. This RV processor will stop intercepting packet events from the
     * currently attached processor and sending rendezvous commands will be
     * disabled until a new SNAC processor is attached.
     */
    public synchronized final void detach() {
        if (snacProcessor != null) {
            snacProcessor.removeVetoablePacketListener(packetListener);

            snacProcessor = null;
        }
    }

    /**
     * Adds the given RV processor listener to this processor's listener list.
     *
     * @param l the RV processor listener to add
     */
    public synchronized final void addListener(RvProcessorListener l) {
        DefensiveTools.checkNull(l, "l");

        if (!rvListeners.contains(l)) rvListeners.add(l);
    }

    /**
     * Removes the given RV processor from this processor's listener list, if
     * present.
     *
     * @param l the listener to remove
     */
    public synchronized final void removeListener(RvProcessorListener l) {
        rvListeners.remove(l);
    }

    /**
     * Registers a new <code>RvCommand</code> factory with this
     * <code>RvProcessor</code>. The given factory will be used to generate
     * <code>RvCommand</code>s for incoming rendezvouses of the types specified
     * in its {@linkplain
     * RvCommandFactory#getSupportedCapabilities supported capabilities}.
     *
     * @param factory the RV command factory to register
     */
    public synchronized final void registerRvCmdFactory(
            RvCommandFactory factory) {
        DefensiveTools.checkNull(factory, "factory");

        CapabilityBlock[] blocks = factory.getSupportedCapabilities();

        if (blocks == null) {
            registerRvCmdFactory(null, factory);
        } else {
            for (int i = 0; i < blocks.length; i++) {
                registerRvCmdFactory(blocks[i], factory);
            }
        }
    }

    /**
     * Registers the given <code>RvCommand</code> factory for the given
     * capability (that is, RV type). The given factory will be used to generate
     * <code>RvCommand</code>s from incoming RV packets of the given type
     * (capability). Note that if <code>cap</code> is <code>null</code>, the
     * given factory will be used as a "fallback" factory, being used to
     * generate commands whose types (capabilities) have no associated factory.
     *
     * @param cap the "type" or "capability" for which the given factory should
     *        be used, or <code>null</code> to handle all types for which a
     *        factory is not explicitly defined
     * @param factory the RV command factory to use for the given rendezvous
     *        type
     */
    public synchronized final void registerRvCmdFactory(CapabilityBlock cap,
            RvCommandFactory factory) {
        DefensiveTools.checkNull(factory, "factory");

        rvFactories.put(cap, factory);
    }

    /**
     * Returns the <code>RvCommand</code> factory that is registered for the
     * given capability (rendezvous type). If <code>cap</code> is
     * <code>null</code>, the "fallback factory" is returned. See {@linkplain
     * #registerRvCmdFactory(CapabilityBlock, RvCommandFactory) above} for
     * details.
     * <br>
     * <br>
     * Note that even if a "fallback factory" is set, <code>null</code> will be
     * returned if no factory is explicitly registered for the given type
     * (unless, of course, <code>cap</code> is <code>null</code>, as stated
     * above).
     *
     * @param cap the capability (rendezvous type) whose registered RV command
     *        factory should be returned, or <code>null</code> to return the
     *        "fallback factory"
     * @return the <code>RvCommandFactory</code> registered for the given RV
     *         type, or <code>null</code> if no factory is registered for that
     *         type
     */
    public synchronized final RvCommandFactory getRegisteredRvCmdFactory(
            CapabilityBlock cap) {
        return (RvCommandFactory) rvFactories.get(cap);
    }

    /**
     * Unregisters the given RV command factory for all RV types for which it
     * is currently registered.
     *
     * @param factory the RV command factory to fully unregister
     */
    public synchronized final void unregisterRvCmdFactory(
            RvCommandFactory factory) {
        DefensiveTools.checkNull(factory, "factory");

        rvFactories.values().remove(factory);
    }

    /**
     * Unregisters the given RV command factory for the given RV type
     * (capability). If the given factory is not registered for the given RV
     * type, no change will take place. If <code>cap</code> is
     * <code>null</code>, the given factory is unregistered as the "fallback
     * factory" (if, of course, it is currently the fallback factory). See
     * {@linkplain #registerRvCmdFactory(CapabilityBlock, RvCommandFactory)
     * above} for details.
     *
     * @param cap the RV type for which the given factory should be unregistered
     * @param factory the factory that should be unregistered from the given
     *        RV type
     */
    public synchronized final void unregisterRvCmdFactory(CapabilityBlock cap,
            RvCommandFactory factory) {
        DefensiveTools.checkNull(factory, "factory");

        if (rvFactories.get(cap) == factory) rvFactories.remove(cap);
    }

    /**
     * Generates a <code>RvCommand</code> from the given incoming rendezvous
     * ICBM. This method first looks for a factory assigned to the RV type
     * (capability) of the given rendezvous. If none is found, the "fallback
     * factory" is used (<code>rvFactories.get(null)</code>). If no "fallback
     * factory" is present, <code>null</code> is returned. Otherwise, the found
     * factory is used to generate the returned <code>RvCommand</code>.
     *
     * @param icbm the incoming RV ICBM from which a <code>RvCommand</code>
     *        should be generated
     * @return an <code>RvCommand</code> generated from the given ICBM, or
     *         <code>null</code> if none could be generated
     */
    private synchronized RvCommand genRvCommand(RecvRvIcbm icbm) {
        DefensiveTools.checkNull(icbm, "icbm");

        // find a factory for this capability type
        RvCommandFactory factory
                = (RvCommandFactory) rvFactories.get(icbm.getCapability());

        if (factory == null) {
            // if there's no factory for that type, try the generic factory
            factory = (RvCommandFactory) rvFactories.get(null);
        }

        // if there's no factory, we can't make a rendezvous command
        if (factory == null) return null;

        // tell the factory to make a command
        return factory.genRvCommand(icbm);
    }

    /**
     * Returns the <code>RvSessionImpl</code> associated with the given session
     * ID, or <code>null</code> if none is present.
     *
     * @param sessionId a rendezvous session ID
     * @param sn the screenname with whom the associated session exists
     * @return the RV session object associated with the given session ID and
     *         screenname
     */
    private synchronized RvSessionImpl getSession(long sessionId, String sn) {
        DefensiveTools.checkNull(sn, "sn");

        RvSessionMapKey key = new RvSessionMapKey(sessionId, sn);
        RvSessionImpl session = (RvSessionImpl) sessions.get(key);

        return session;
    }

    /**
     * Returns an <code>RvSessionImpl</code> corresponding to the given RV
     * session ID and with the given user. If none currently exists, a new one
     * is created and a <code>NewRvSessionEvent</code> is fired.
     *
     * @param sessionId the RV session ID whose session object will be returned
     * @param sn the screenname with whom the associated session exists
     * @return an RV session object associated with the given session ID and
     *         with the given screenname
     */
    private synchronized RvSessionImpl getOrCreateIncomingSession(
            long sessionId, String sn) {
        DefensiveTools.checkNull(sn, "sn");

        RvSessionImpl session = getSession(sessionId, sn);

        if (session == null) {
            logger.fine("Creating new incoming RV session for " + sn + ", id=0x"
                    + Long.toHexString(sessionId));
            session = createNewSession(sessionId, sn);

            fireNewSessionEvent(session,
                    NewRvSessionEvent.TYPE_INCOMING);
        }

        return session;
    }

    /**
     * Passes the given parameters to this processor's (grand)parent FLAP
     * processor. If this processor is not currently attached to a SNAC
     * processor, information is printed to <code>System.err</code>.
     *
     * @param type the type of exception event being fired
     * @param t the exception that was thrown
     * @param info an object describing the given exception
     *
     * @see FlapProcessor#handleException(Object, Throwable, Object)
     */
    private synchronized void handleException(Object type, Throwable t,
            Object info) {
        DefensiveTools.checkNull(type, "type");
        DefensiveTools.checkNull(t, "t");

        logger.finer("RV processor got exception type " + type + ": " + t
                + " - " + info);

        if (snacProcessor != null) {
            FlapProcessor fp = snacProcessor.getFlapProcessor();
            if (fp != null) {
                fp.handleException(type, t, info);
                return;
            }
        }

        System.err.println("RV processor got exception; no exception handlers" +
                "present (type=" + type + ", info=" + info + ")");
        t.printStackTrace(System.err);
    }

    /**
     * Calls the <code>handleNewSession</code> method of each attached
     * listener with the given session and type. Note that <code>type</code>
     * should be one of {@link NewRvSessionEvent#TYPE_INCOMING} and
     * {@link NewRvSessionEvent#TYPE_OUTGOING}.
     *
     * @param session the session that was created
     * @param type the type of session, like {@link
     *        NewRvSessionEvent#TYPE_OUTGOING}
     */
    private synchronized void fireNewSessionEvent(RvSessionImpl session,
            Object type) {
        NewRvSessionEvent event = new NewRvSessionEvent(this, session, type);

        for (Iterator it = rvListeners.iterator(); it.hasNext();) {
            RvProcessorListener listener = (RvProcessorListener) it.next();

            try {
                listener.handleNewSession(event);
            } catch (Throwable t) {
                handleException(ERRTYPE_RV_LISTENER, t, listener);
            }
        }
    }

    /**
     * Creates a new session and places it in the RV session map. This method
     * does <i>not</i> fire a new session event.
     *
     * @param sessionId the RV session ID of the session to be created
     * @param sn the screenname of the user with whom the session exists
     * @return a new <code>RvSessionImpl</code> corresponding to the given RV
     *         session ID and the given screenname
     */
    private RvSessionImpl createNewSession(long sessionId, String sn) {
        RvSessionImpl session = new RvSessionImpl(sessionId, sn);

        RvSessionMapKey key = new RvSessionMapKey(sessionId, sn);

        sessions.put(key, session);

        return session;
    }

    /**
     * Processes a new incoming rendezvous packet, generating a
     * <code>RvCommand</code>, creating a <code>RvSession</code> if necessary,
     * and passing it to the given session's listeners.
     *
     * @param e the SNAC packet event containing a <code>RecvRvIcbm</code>
     *
     * @see #getOrCreateIncomingSession
     */
    private synchronized void processRv(SnacPacketEvent e) {
        RecvRvIcbm cmd = (RecvRvIcbm) e.getSnacCommand();

        RvSessionImpl session = getOrCreateIncomingSession(cmd.getRvSessionId(),
                cmd.getSender().getScreenname());

        RvCommand rvCommand = null;
        try {
            rvCommand = genRvCommand(cmd);
        } catch (Throwable t) {
            handleException(ERRTYPE_RV_CMD_GEN, t, cmd);
        }

        logger.fine("Generated RV command: " + rvCommand);

        RecvRvEvent event = new RecvRvEvent(e, this, session, rvCommand);

        session.processRv(event);

        logger.finer("Done processing RV");
    }

    /**
     * Processes an incoming "RV response" packet, creating a new session if
     * necessary and passing an event to the session's listeners.
     *
     * @param e the SNAC packet event containing a <code>RvResponse</code>
     */
    private synchronized void processResponse(SnacPacketEvent e) {
        RvResponse cmd = (RvResponse) e.getSnacCommand();

        RvSessionImpl session = getOrCreateIncomingSession(cmd.getRvSessionId(),
                cmd.getScreenname());

        RecvRvEvent event = new RecvRvEvent(e, this, session,
                cmd.getResultCode());

        session.processRv(event);

        logger.finer("Done processing RV response");
    }

    /**
     * Creates a new "outgoing" rendezvous session with the given user and a
     * new unique session ID. The session is only "outgoing" in that it was
     * created locally, and the first command will probably be outgoing. Note
     * that when this method is called a new <code>NewRvSessionEvent</code> of
     * type {@link NewRvSessionEvent#TYPE_OUTGOING TYPE_OUTGOING}
     * will be fired.
     *
     * @param sn the screenname of the user with whom to create a new session
     * @return a new <code>RvSession</code> with the given user
     */
    public synchronized final RvSession createRvSession(String sn) {
        DefensiveTools.checkNull(sn, "sn");

        lastSessionId++;

        return createRvSession(sn, lastSessionId);
    }

    /**
     * Creates a new "outgoing" rendezvous session with the given user and the
     * given session ID. The session is only "outgoing" in that it was
     * created locally, and the first command will probably be outgoing. Note
     * that when this method is called a new <code>NewRvSessionEvent</code> of
     * type {@link NewRvSessionEvent#TYPE_OUTGOING TYPE_OUTGOING}
     * will be fired.
     *
     * @param sn the screenname of the user with whom to create a new session
     * @param sessionID a session ID number to use for the created session
     * @return a new <code>RvSession</code> with the given user
     */
    public synchronized final RvSession createRvSession(String sn,
            long sessionID) {
        DefensiveTools.checkNull(sn, "sn");

        logger.finer("Creating new outgoing RV session for " + sn);

        RvSessionImpl session = createNewSession(sessionID, sn);

        fireNewSessionEvent(session, NewRvSessionEvent.TYPE_OUTGOING);

        return session;
    }

    /**
     * Sends the given SNAC request to the attached SNAC processor, if any. (If
     * no SNAC processor is attached, no action will take place.)
     *
     * @param req the SNAC request to send
     */
    private synchronized void sendSnac(SnacRequest req) {
        if (snacProcessor == null) return;

        snacProcessor.sendSnac(req);
    }

    /**
     * A simple class holding a session ID and screenname, for use as a map key
     * in {@link RvProcessor#sessions RvProcessor.sessions}.
     */
    private class RvSessionMapKey {
        /** A rendezvous session ID. */
        private final long sessionId;
        /** A screenname. */
        private final String sn;

        /**
         * Creates a new RV session map key with the given properties.
         *
         * @param sessionId a rendezvous session ID
         * @param sn a screenname
         */
        public RvSessionMapKey(long sessionId, String sn) {
            this.sessionId = sessionId;
            this.sn = OscarTools.normalize(sn);
        }

        public int hashCode() {
            return (int) (sessionId >> 32 ^ sessionId ^ sn.hashCode());
        }

        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof RvSessionMapKey)) return false;

            RvSessionMapKey key = (RvSessionMapKey) obj;

            return sessionId == key.sessionId && sn.equals(key.sn);
        }
    }

    /**
     * An implementation of <code>RvSession</code> for use in
     * <code>RvProcessor</code>.
     */
    private class RvSessionImpl implements RvSession {
        /** The rendezvous session ID associated with this session. */
        private final long rvSessionId;
        /** The screenname with whom this session exists. */
        private final String sn;

        /** This session's listeners. */
        private List listeners = new ArrayList();

        /**
         * A SNAC request listener attached to all outgoing SNAC requests sent
         * by <code>RvSessionImpl</code>.
         */
        private SnacRequestListener reqListener = new SnacRequestAdapter() {
            public void handleResponse(SnacResponseEvent e) {
                RvSnacResponseEvent event = new RvSnacResponseEvent(e,
                        RvProcessor.this, RvSessionImpl.this);

                processSnacResponse(event);
            }
        };

        /**
         * Creates a new RV session object with the given session ID and
         * screenname.
         *
         * @param rvSessionId the RV session ID of this session
         * @param sn the screenname with whom this session exists
         */
        public RvSessionImpl(long rvSessionId, String sn) {
            this.rvSessionId = rvSessionId;
            this.sn = sn;
        }


        public RvProcessor getRvProcessor() { return RvProcessor.this; }

        public long getRvSessionId() { return rvSessionId; }

        public String getScreenname() { return sn; }

        public synchronized void addListener(RvSessionListener l) {
            DefensiveTools.checkNull(l, "l");

            if (!listeners.contains(l)) listeners.add(l);
        }

        public synchronized void removeListener(RvSessionListener l) {
            listeners.remove(l);
        }

        /**
         * Handles an incoming rendezvous event, passing the event to this
         * session's listeners.
         *
         * @param event the incoming rendezvous event
         */
        private synchronized void processRv(RecvRvEvent event) {
            for (Iterator it = new LinkedList(listeners).iterator();
                 it.hasNext();) {
                RvSessionListener listener = (RvSessionListener) it.next();

                try {
                    listener.handleRv(event);
                } catch (Throwable t) {
                    handleException(ERRTYPE_RV_SESSION_LISTENER, t, listener);
                }
            }
        }

        /**
         * Handles an incoming SNAC response event, passing the event to this
         * session's listeners.
         *
         * @param event the incoming rendezvous event
         */
        private synchronized void processSnacResponse(
                RvSnacResponseEvent event) {
            for (Iterator it = new LinkedList(listeners).iterator();
                 it.hasNext();) {
                RvSessionListener listener = (RvSessionListener) it.next();

                try {
                    listener.handleSnacResponse(event);
                } catch (Throwable t) {
                    handleException(ERRTYPE_RV_SESSION_LISTENER, t, listener);
                }
            }
        }

        public void sendRv(RvCommand command) {
            sendRv(command, 0);
        }

        public void sendRv(RvCommand command, long icbmMessageId) {
            DefensiveTools.checkNull(command, "command");

            logger.fine("Sending RV to " + sn + ": " + command);

            SnacCommand cmd = new SendRvIcbm(sn, icbmMessageId, rvSessionId,
                    command);

            sendSnac(new SnacRequest(cmd, reqListener));
        }

        public void sendResponse(int code) {
            RvResponse cmd = new RvResponse(rvSessionId,
                    AbstractIcbm.CHANNEL_RV, sn, code);

            logger.fine("Sending RV response to " + sn + ": " + code);

            sendSnac(new SnacRequest(cmd, reqListener));
        }

        public String toString() {
            return "RvSession with " + getScreenname() + " (sessionid=0x"
                    + Long.toHexString(rvSessionId) + ")";
        }
    }
}
