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
import net.kano.joscar.snaccmd.conn.RateClassInfo;
import net.kano.joscar.snac.CmdType;
import net.kano.joscar.snac.SnacProcessor;
import net.kano.joscar.snac.SnacRequest;

import java.util.*;

public final class ConnectionQueueMgr {
    private final RateLimitingQueueMgr queueMgr;
    private final RateMonitor monitor;
    private final SnacProcessor snacProcessor;

    private boolean paused = false;

    private final Map queues = new IdentityHashMap();

    private RateListener rateListener = new RateListener() {
        public void detached(RateMonitor rateMonitor, SnacProcessor processor) {
            rateMonitor.removeListener(this);
        }

        public void reset(RateMonitor rateMonitor) { }

        public void gotRateClasses(RateMonitor monitor) {
            resetRateClasses();
        }

        public void rateClassUpdated(RateMonitor monitor,
                RateClassMonitor classMonitor, RateClassInfo rateInfo) {
            RateQueue queue = getRateQueue(classMonitor);

            queueMgr.getRunner().update(queue);
        }

        public void rateClassLimited(RateMonitor rateMonitor,
                RateClassMonitor rateClassMonitor, boolean limited) {
            queueMgr.getRunner().update(getRateQueue(rateClassMonitor));
        }
    };

    ConnectionQueueMgr(RateLimitingQueueMgr queueMgr, RateMonitor monitor) {
        DefensiveTools.checkNull(queueMgr, "queueMgr");
        DefensiveTools.checkNull(monitor, "monitor");

        this.queueMgr = queueMgr;
        this.monitor = monitor;
        this.snacProcessor = monitor.getSnacProcessor();
        monitor.addListener(rateListener);
    }

    public RateLimitingQueueMgr getParentQueueMgr() { return queueMgr; }

    public RateMonitor getRateMonitor() { return monitor; }

    public SnacProcessor getSnacProcessor() { return snacProcessor; }

    public void queueSnac(SnacRequest request) {
        DefensiveTools.checkNull(request, "request");

        CmdType type = CmdType.ofCmd(request.getCommand());

        RateQueue queue = getRateQueue(type);

        if (queue == null) {
            // so there's no queue. let's send it right out!
            queueMgr.sendSnac(snacProcessor, request);

        } else {
            queue.enqueue(request);
            queueMgr.getRunner().update(queue);
        }
    }

    private synchronized RateQueue getRateQueue(RateClassMonitor classMonitor) {
        DefensiveTools.checkNull(classMonitor, "classMonitor");

        return (RateQueue) queues.get(classMonitor);
    }

    public synchronized RateQueue getRateQueue(CmdType type) {
        DefensiveTools.checkNull(type, "type");

        RateClassMonitor cm = monitor.getMonitor(type);

        if (cm == null) return null;

        return getRateQueue(cm);
    }

    public synchronized void clearQueue() {
        for (Iterator it = queues.values().iterator(); it.hasNext();) {
            RateQueue queue = (RateQueue) it.next();

            queue.clear();
        }

        paused = false;
    }

    public synchronized void pause() {
        assert !paused;

        // we just set this flag and we should be pretty okay. we don't need
        // to call runner.update() because it will find out that we're
        // paused before it tries to send anything whether or not we tell it
        // to wake up.
        paused = true;
    }

    public synchronized void unpause() {
        assert paused;

        // we turn the paused flag off and tell the thread to wake up, in
        // case there are some commands queued up that can be sent now
        paused = false;
        queueMgr.getRunner().update(this);
    }

    public synchronized boolean isPaused() { return paused; }

    private synchronized void resetRateClasses() {
        RateClassMonitor[] monitors = monitor.getMonitors();

        // clear the list of queues
        RateQueue[] queueArray = (RateQueue[])
                queues.values().toArray(new RateQueue[0]);
        queueMgr.getRunner().removeQueues(queueArray);
        queues.clear();

        List reqs = new LinkedList();

        // gather up all of the pending SNAC requests
        for (int i = 0; i < queueArray.length; i++) {
            RateQueue queue = queueArray[i];

            queue.dequeueAll(reqs);
        }

        // create new rate queues
        for (int i = 0; i < monitors.length; i++) {
            RateQueue queue = new RateQueue(this, monitors[i]);
            queues.put(monitors[i], queue);
        }

        // and re-queue all of the pending SNACs
        for (Iterator it = reqs.iterator(); it.hasNext();) {
            SnacRequest req = (SnacRequest) it.next();

            queueSnac(req);
        }

        RateQueue[] rateQueues
                = (RateQueue[]) queues.values().toArray(new RateQueue[0]);
        queueMgr.getRunner().addQueues(rateQueues);
        queueMgr.getRunner().update(this);
    }

    synchronized void detach() {
        clearQueue();
        monitor.detach();
    }
}
