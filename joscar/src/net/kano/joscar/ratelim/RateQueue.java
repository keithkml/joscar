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
 *  File created by keith @ May 25, 2003
 *
 */

package net.kano.joscar.ratelim;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snaccmd.conn.RateClassInfo;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RateQueue {
    private static final Logger logger
            = Logger.getLogger("net.kano.joscar.ratelim");

    private final ConnectionQueueMgr parentMgr;

    private final LinkedList queue = new LinkedList();

    private final RateClassMonitor rateMonitor;

    RateQueue(ConnectionQueueMgr parentMgr, RateClassMonitor monitor) {
        DefensiveTools.checkNull(parentMgr, "parentMgr");
        DefensiveTools.checkNull(monitor, "monitor");

        this.parentMgr = parentMgr;
        this.rateMonitor = monitor;
    }

    public RateClassMonitor getRateMonitor() { return rateMonitor; }

    public ConnectionQueueMgr getParentMgr() { return parentMgr; }

    public synchronized int getQueueSize() { return queue.size(); }

    public synchronized void enqueue(SnacRequest req) {
        DefensiveTools.checkNull(req, "req");

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Enqueuing " + req.getCommand() + " within ratequeue " +
                    "(class " + rateMonitor.getRateInfo().getRateClass()
                    + ")...");
        }

        queue.add(req);
    }

    public synchronized boolean hasRequests() {
        return !queue.isEmpty();
    }

    public synchronized SnacRequest dequeue() {
        if (queue.isEmpty()) return null;

        SnacRequest request = (SnacRequest) queue.removeFirst();

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Dequeueing " + request.getCommand()
                    + " from ratequeue (class "
                    + rateMonitor.getRateInfo().getRateClass() + ")...");
        }

        return request;
    }

    public synchronized void clear() {
        queue.clear();
    }
}
