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
 *  File created by keith @ Feb 7, 2004
 *
 */

package net.kano.joustsim.oscar;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.CapabilityBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CapabilityManager {
    private final AimConnection conn;
    private Map<CapabilityBlock,CapabilityHandler> handlers
            = new HashMap<CapabilityBlock, CapabilityHandler>();
    private CopyOnWriteArrayList<CapabilityManagerListener> listeners
            = new CopyOnWriteArrayList<CapabilityManagerListener>();

    public CapabilityManager(AimConnection conn) {
        DefensiveTools.checkNull(conn, "conn");

        this.conn = conn;
    }

    public AimConnection getAimConnection() { return conn; }

    public void addCapabilityListener(CapabilityManagerListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeCapabilityListener(CapabilityManagerListener l) {
        listeners.remove(l);
    }

    public boolean setCapabilityHandler(CapabilityBlock block,
            CapabilityHandler handler) {
        DefensiveTools.checkNull(block, "block");
        DefensiveTools.checkNull(handler, "handler");

        boolean isnew;
        CapabilityHandler old;
        synchronized(this) {
            old = handlers.put(block, handler);
            if (old == handler) return false;
            isnew = old == null;
        }
        if (isnew) fireCapabilityHandlerAdded(block, handler);
        else fireCapabilityHandlerChanged(block, old, handler);
        return true;
    }

    public synchronized CapabilityHandler getCapabilityHandler(
            CapabilityBlock block) {
        return handlers.get(block);
    }

    public boolean removeCapabilityHandler(CapabilityBlock block,
            CapabilityHandler handler) {
        DefensiveTools.checkNull(block, "block");
        DefensiveTools.checkNull(handler, "handler");

        synchronized(this) {
            if (handler != handlers.get(block)) {
                return false;
            }
            handlers.remove(block);
        }
        fireCapabilityHandlerRemoved(block, handler);
        return true;
    }

    public synchronized List<CapabilityBlock> getEnabledCapabilities() {
        List<CapabilityBlock> actual = new ArrayList<CapabilityBlock>(handlers.size());
        for (Map.Entry<CapabilityBlock, CapabilityHandler> pair : handlers.entrySet()) {
            CapabilityHandler handler = pair.getValue();
            if (handler.isEnabled()) {
                CapabilityBlock block = pair.getKey();
                actual.add(block);
            }
        }
        return DefensiveTools.getUnmodifiable(actual);
    }

    private void fireCapabilityHandlerAdded(CapabilityBlock block,
            CapabilityHandler handler) {
        assert !Thread.holdsLock(this);

        DefensiveTools.checkNull(block, "block");
        DefensiveTools.checkNull(handler, "handler");

        handler.handleAdded(this);

        for (CapabilityManagerListener listener : listeners) {
            listener.capabilityHandlerAdded(this, block, handler);
        }
    }

    private void fireCapabilityHandlerChanged(CapabilityBlock block,
            CapabilityHandler old, CapabilityHandler handler) {
        assert !Thread.holdsLock(this);

        DefensiveTools.checkNull(block, "block");
        DefensiveTools.checkNull(handler, "handler");

        if (old != null) fireCapabilityHandlerRemoved(block, old);

        fireCapabilityHandlerAdded(block, handler);
    }

    private void fireCapabilityHandlerRemoved(CapabilityBlock block,
            CapabilityHandler handler) {
        assert !Thread.holdsLock(this);

        DefensiveTools.checkNull(block, "block");
        DefensiveTools.checkNull(handler, "handler");

        handler.handleRemoved(this);

        for (CapabilityManagerListener listener : listeners) {
            listener.capabilityHandlerRemoved(this, block, handler);
        }
    }


}
