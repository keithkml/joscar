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
 *  File created by keith @ Apr 24, 2003
 *
 */

package net.kano.joscar.rv;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snac.*;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.AbstractIcbm;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joscar.snaccmd.icbm.SendRvIcbm;
import net.kano.joscar.snaccmd.icbm.RvResponse;

import java.util.*;

/**
 * Don't forget logging, Keith.
 */
public class RvProcessor {
    private SnacProcessor snacProcessor = null;

    private Map sessions = new HashMap();

    private List rvListeners = new ArrayList();

    private long lastSessionId = 1;

    private VetoableSnacPacketListener packetListener
            = new VetoableSnacPacketListener() {
        public Object handlePacket(SnacPacketEvent event) {
            SnacCommand cmd = event.getSnacCommand();

            if (cmd instanceof RecvRvIcbm) {
                processRv(event);

                return STOP_PROCESSING_LISTENERS;

            } else if (cmd instanceof RvResponse) {
                processResponse(event);

                return STOP_PROCESSING_LISTENERS;
            }

            return CONTINUE_PROCESSING;
        }
    };

    public RvProcessor(SnacProcessor snacProcessor) {
        attachToSnacProcessor(snacProcessor);
    }

    public synchronized final SnacProcessor getSnacProcessor() {
        return snacProcessor;
    }

    public synchronized final void attachToSnacProcessor(
            SnacProcessor snacProcessor) {
        DefensiveTools.checkNull(snacProcessor, "snacProcessor");

        detach();

        this.snacProcessor = snacProcessor;
        snacProcessor.addVetoablePacketListener(packetListener);
    }

    public synchronized final void detach() {
        if (snacProcessor != null) {
            snacProcessor.removeVetoablePacketListener(packetListener);

            snacProcessor = null;
        }
    }

    public synchronized final void addRvListener(RvListener l) {
        DefensiveTools.checkNull(l, "l");

        if (!rvListeners.contains(l)) rvListeners.add(l);
    }

    public synchronized final void removeRvListener(RvListener l) {
        DefensiveTools.checkNull(l, "l");

        rvListeners.remove(l);
    }

    private Map rvFactories = new HashMap();

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

    public synchronized final void registerRvCmdFactory(CapabilityBlock cap,
            RvCommandFactory factory) {
        DefensiveTools.checkNull(factory, "factory");

        if (rvFactories.get(cap) == null) rvFactories.put(cap, factory);
    }

    public synchronized final void unregisterRvCmdFactory(
            RvCommandFactory factory) {
        DefensiveTools.checkNull(factory, "factory");
        rvFactories.values().remove(factory);
    }

    public synchronized final void unregisterRvCmdFactory(CapabilityBlock cap,
            RvCommandFactory factory) {
        DefensiveTools.checkNull(factory, "factory");

        if (rvFactories.get(cap) == factory) rvFactories.remove(cap);
    }

    private synchronized RvCommand genRvCommand(RecvRvIcbm icbm) {
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

    private synchronized RvSessionImpl getSession(long sessionId, String sn) {
        Long key = new Long(sessionId);
        RvSessionImpl session = (RvSessionImpl) sessions.get(key);

        if (session == null) {
            // we get to create a new session!
            session = new RvSessionImpl(sessionId, sn);
            sessions.put(key, session);

            NewRvSessionEvent event = new NewRvSessionEvent(this, session);

            for (Iterator it = rvListeners.iterator(); it.hasNext();) {
                RvListener listener = (RvListener) it.next();

                listener.handleNewIncomingSession(event);
            }
        }

        return session;
    }

    private synchronized void processRv(SnacPacketEvent e) {
        RecvRvIcbm icbm = (RecvRvIcbm) e.getSnacCommand();

        RvCommand rvCommand = genRvCommand(icbm);

        RvSessionImpl session = getSession(icbm.getRvSessionId(),
                icbm.getSender().getScreenname());

        RecvRvEvent event = new RecvRvEvent(e, this, session, rvCommand);

        session.processRv(event);
    }

    private synchronized void processResponse(SnacPacketEvent e) {
        RvResponse cmd = (RvResponse) e.getSnacCommand();

        RvSessionImpl session = getSession(cmd.getRvSessionId(),
                cmd.getScreenname());

        RecvRvEvent event = new RecvRvEvent(e, this, session,
                cmd.getResultCode());

        session.processRv(event);
    }

    public synchronized final RvSession createRvSession(String sn) {
        lastSessionId++;

        return getSession(lastSessionId, sn);
    }

    private synchronized void sendSnac(SnacRequest req) {
        snacProcessor.sendSnac(req);
    }

    private class RvSessionImpl implements RvSession {
        private final long rvSessionId;
        private final String sn;

        private List listeners = new ArrayList();

        private SnacRequestListener reqListener = new SnacRequestAdapter() {
            public void handleResponse(SnacResponseEvent e) {
                RvSnacResponseEvent event = new RvSnacResponseEvent(e,
                        RvProcessor.this, RvSessionImpl.this);

                processSnacResponse(event);
            }
        };

        private RvSessionImpl(long rvSessionId, String sn) {
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
            DefensiveTools.checkNull(l, "l");

            listeners.remove(l);
        }

        private synchronized void processRv(RecvRvEvent event) {
            for (Iterator it = new LinkedList(listeners).iterator();
                 it.hasNext();) {
                RvSessionListener listener = (RvSessionListener) it.next();

                listener.handleRv(event);
            }
        }

        private synchronized void processSnacResponse(
                RvSnacResponseEvent event) {
            for (Iterator it = new LinkedList(listeners).iterator();
                 it.hasNext();) {
                RvSessionListener listener = (RvSessionListener) it.next();

                listener.handleSnacResponse(event);
            }
        }

        public void sendRv(RvCommand command) {
            DefensiveTools.checkNull(command, "command");

            System.out.println("sending RV to " + sn + ": " + command);

            SnacCommand cmd = new SendRvIcbm(sn, rvSessionId, command);

            sendSnac(new SnacRequest(cmd, reqListener));
        }

        public void sendResponse(int code) {
            RvResponse cmd = new RvResponse(rvSessionId,
                    AbstractIcbm.CHANNEL_RV, sn, code);

            sendSnac(new SnacRequest(cmd, reqListener));
        }

        public String toString() {
            return "RvSession with " + getScreenname() + " (sessionid=0x"
                    + Long.toHexString(rvSessionId) + ")";
        }
    }
}
