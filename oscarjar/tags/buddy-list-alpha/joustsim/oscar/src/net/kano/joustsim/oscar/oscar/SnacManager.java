/*
 *  Copyright (c) 2004, The Joust Project
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
 *  File created by keith @ Jan 15, 2004
 *
 */

package net.kano.joustsim.oscar.oscar;

import net.kano.joscar.snac.SnacRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SnacManager {
    protected final Map<Integer,List<BasicConnection>> conns = new HashMap<Integer, List<BasicConnection>>();
    protected final PendingSnacMgr pendingSnacs = new PendingSnacMgr();
    protected final List<PendingSnacListener> listeners = new ArrayList<PendingSnacListener>();
    protected final Map<BasicConnection,int[]> supportedFamilies = new IdentityHashMap<BasicConnection, int[]>();

    public SnacManager() { }

    public SnacManager(PendingSnacListener listener) {
        addListener(listener);
    }

    public void register(BasicConnection conn) {
        int[] families = conn.getSnacFamilies();
        supportedFamilies.put(conn, families);

        for (int familyCode : families) {
            Integer family = familyCode;

            List<BasicConnection> handlers = conns.get(family);

            if (handlers == null) {
                handlers = new LinkedList<BasicConnection>();
                conns.put(family, handlers);
            }

            if (!handlers.contains(conn)) handlers.add(conn);
        }
    }

    public void dequeueSnacs(BasicConnection conn) {
        int[] infos = supportedFamilies.get(conn);

        if (infos != null) {
            for (int familyCode : infos) {
                if (pendingSnacs.isPending(familyCode)) {
                    dequeueSnacs(familyCode);
                }
            }
        }
    }

    protected void dequeueSnacs(int familyCode) {
        SnacRequest[] pending = pendingSnacs.getPending(familyCode);

        pendingSnacs.setPending(familyCode, false);

        for (PendingSnacListener listener : listeners) {
            listener.dequeueSnacs(pending);
        }
    }

    public void unregister(BasicConnection conn) {
        for (List<BasicConnection> handlers : conns.values()) {
            handlers.remove(conn);
        }
    }

    public BasicConnection getConn(int familyCode) {
        Integer family = familyCode;

        List<BasicConnection> handlers = conns.get(family);

        if (handlers == null || handlers.size() == 0) return null;

        return handlers.get(0);
    }


    public boolean isPending(int familyCode) {
        return pendingSnacs.isPending(familyCode);
    }

    public void addRequest(SnacRequest request) {
        int family = request.getCommand().getFamily();
        if (!isPending(family)) {
            throw new IllegalArgumentException("Family 0x"
                    + Integer.toHexString(family) + " is not pending");
        }
        pendingSnacs.add(request);
    }

    public void addListener(PendingSnacListener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(PendingSnacListener l) {
        listeners.remove(l);
    }

    public void setPending(int family, boolean pending) {
        pendingSnacs.setPending(family, pending);
    }

}
