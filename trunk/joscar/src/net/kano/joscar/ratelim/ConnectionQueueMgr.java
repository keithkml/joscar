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

import net.kano.joscar.snac.SnacProcessor;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snac.CmdType;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.conn.RateClassInfo;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.lang.reflect.Field;

public final class ConnectionQueueMgr {
    private final RateLimitingQueueMgr queueMgr;

    private final SnacProcessor snacProcessor;

    private boolean avoidLimiting = true;
    private boolean paused = false;

    private Map typeToQueue = new HashMap();
    private Map classToQueue = new HashMap();
    private RateQueue defaultQueue = null;

    ConnectionQueueMgr(RateLimitingQueueMgr queueMgr,
            SnacProcessor snacProcessor) {
        DefensiveTools.checkNull(queueMgr, "queueMgr");
        DefensiveTools.checkNull(snacProcessor, "snacProcessor");

        this.queueMgr = queueMgr;
        this.snacProcessor = snacProcessor;
        this.avoidLimiting = queueMgr.getDefaultAvoidLimiting();
    }

    public RateLimitingQueueMgr getParentQueueMgr() { return queueMgr; }

    public SnacProcessor getSnacProcessor() { return snacProcessor; }

    public void queueSnac(SnacRequest request) {
        DefensiveTools.checkNull(request, "request");

        CmdType type = CmdType.ofCmd(request.getCommand());

        RateQueue queue = getRateQueue(type);

        if (queue == null) {
            // so there's no queue. let's send it right out!
            queueMgr.sendSnac(snacProcessor, request);
            return;
        }

        queue.enqueue(request);
        queueMgr.getRunner().update(queue);
    }

    private synchronized RateQueue getRateQueue(CmdType type) {
        DefensiveTools.checkNull(type, "type");

        RateQueue queue = (RateQueue) typeToQueue.get(type);

        if (queue == null) queue = defaultQueue;

        return queue;
    }



    public synchronized void clearQueue() {
        for (Iterator it = classToQueue.values().iterator();
             it.hasNext();) {
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


    public synchronized final boolean isAvoidingLimiting() {
        return avoidLimiting;
    }

    public synchronized final void setAvoidingLimiting(boolean avoidLimiting) {
        if (avoidLimiting = this.avoidLimiting) return;

        this.avoidLimiting = avoidLimiting;

        if (!avoidLimiting) queueMgr.getRunner().update(this);
    }
}
