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
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joscar.snaccmd.icbm.SendRvIcbm;

import java.util.*;

/**
 * Don't forget logging, Keith.
 */
public class RvProcessor {
    private SnacProcessor snacProcessor = null;

    private Map sessions = new HashMap();

    private List rvListeners = new ArrayList();

    private long lastRvCookie = 1;

    private VetoableSnacPacketListener packetListener
            = new VetoableSnacPacketListener() {
        public Object handlePacket(SnacPacketEvent event) {
            SnacCommand cmd = event.getSnacCommand();

            if (cmd instanceof RecvRvIcbm) {
                processRv(event);

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

    private synchronized RvSessionInfo getSession(long rvCookie, String sn) {
        Long key = new Long(rvCookie);
        RvSessionInfo info = (RvSessionInfo) sessions.get(key);

        if (info == null) {
            // we get to create a new session!
            RvSession session = new RvSession(this, rvCookie, sn);
            info = new RvSessionInfo(session);
            sessions.put(key, info);

            NewRvSessionEvent event = new NewRvSessionEvent(this, session);

            for (Iterator it = rvListeners.iterator(); it.hasNext();) {
                RvListener listener = (RvListener) it.next();

                listener.handleNewIncomingSession(event);
            }
        }

        return info;
    }

    private synchronized void processRv(SnacPacketEvent e) {
        RecvRvIcbm icbm = (RecvRvIcbm) e.getSnacCommand();

        RvCommand rvCommand = genRvCommand(icbm);

        RvSessionInfo sessionInfo = getSession(icbm.getRvCookie(),
                icbm.getSender().getScreenname());

        RecvRvEvent event = new RecvRvEvent(e, this, sessionInfo.session,
                rvCommand);

        sessionInfo.session.processRv(event);
    }

    public synchronized final RvSession createRvSession(String sn) {
        lastRvCookie++;

        return getSession(lastRvCookie, sn).session;
    }

    private synchronized final RvSessionInfo getSessionInfo(RvSession session) {
        Long key = new Long(session.getSessionId());

        return (RvSessionInfo) sessions.get(key);
    }

    synchronized final void sendRv(RvSession rvSession, RvCommand command) {
        SnacCommand cmd = new SendRvIcbm(rvSession.getScreenname(), 0,
                rvSession.getSessionId(), command);

        snacProcessor.sendSnac(new SnacRequest(cmd,
                new RvResponseListener(getSessionInfo(rvSession), command)));
    }

    private static class RvSessionInfo {
        public final RvSession session;

        public RvSessionInfo(RvSession session) {
            DefensiveTools.checkNull(session, "session");

            this.session = session;
        }
    }

    private class RvResponseListener extends SnacRequestAdapter {
        public final RvSessionInfo sessionInfo;
        public final RvCommand rvCommand;

        public RvResponseListener(RvSessionInfo sessionInfo,
                RvCommand rvCommand) {
            this.sessionInfo = sessionInfo;
            this.rvCommand = rvCommand;
        }

        public void handleResponse(SnacResponseEvent e) {
            RvSession session = sessionInfo.session;

            RvSnacResponseEvent event
                    = new RvSnacResponseEvent(e, RvProcessor.this, session);

            session.processSnacResponse(event);
        }
    }
}
