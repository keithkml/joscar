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

class ConnectionQueueMgr {
    private final RateLimitingQueueMgr queueMgr;
    private final SnacProcessor snacProcessor;
    private Map classToQueue = new HashMap();
    private Map typeToQueue = new HashMap(500);
    private RateQueue defaultQueue = null;
    private boolean paused = false;

    public ConnectionQueueMgr(RateLimitingQueueMgr queueMgr,
            SnacProcessor snacProcessor) {
        DefensiveTools.checkNull(queueMgr, "queueMgr");
        DefensiveTools.checkNull(snacProcessor, "snacProcessor");

        this.queueMgr = queueMgr;
        this.snacProcessor = snacProcessor;
    }

    public RateLimitingQueueMgr getQueueMgr() { return queueMgr; }

    public SnacProcessor getSnacProcessor() { return snacProcessor; }

    public void queueSnac(SnacRequest request) {
        DefensiveTools.checkNull(request, "request");

        CmdType type = CmdType.ofCmd(request.getCommand());

        RateQueue queue;
        synchronized(this) {
            queue = (RateQueue) typeToQueue.get(type);

            if (queue == null) queue = defaultQueue;
        }

        if (queue == null) {
            // so there's no queue. let's send it right out!
            queueMgr.sendSnac(snacProcessor, request);
            return;
        }

        queue.enqueue(request);
        queueMgr.getRunner().update(queue);
    }

    public synchronized void setRateClass(RateClassInfo rateInfo) {
        DefensiveTools.checkNull(rateInfo, "rateInfo");

        RateQueue queue = updateRateQueue(rateInfo);

        CmdType[] cmdTypes = rateInfo.getCommands();
        if (cmdTypes != null) {
            if (cmdTypes.length == 0) {
                // if there aren't any member SNAC commands for this rate
                // class, this is the "fallback" rate class, or the
                // "default queue"
                if (defaultQueue == null) defaultQueue = queue;
            } else {
                // there are command types associated with this rate class,
                // so, for speed, we put them into a map
                for (int i = 0; i < cmdTypes.length; i++) {
                    typeToQueue.put(cmdTypes[i], queue);
                }
            }
        }

        // something most likely changed.
        queueMgr.getRunner().update(queue);
    }

    public void updateRateClass(int changeCode, RateClassInfo rateInfo) {
        DefensiveTools.checkNull(rateInfo, "rateInfo");

        RateQueue queue = updateRateQueue(rateInfo);

        if (changeCode != -1) {
            queue.setChangeCode(changeCode);
        }
    }
    private synchronized RateQueue updateRateQueue(RateClassInfo rateInfo) {
        Integer key = new Integer(rateInfo.getRateClass());
        RateQueue queue = (RateQueue) classToQueue.get(key);

        if (queue == null) {
            queue = new RateQueue(this, rateInfo);
            classToQueue.put(key, queue);
            queueMgr.getRunner().addQueue(queue);
        } else {
            queue.setRateInfo(rateInfo);
        }

        return queue;
    }

    public synchronized void clearQueue() {
        for (Iterator it = this.classToQueue.values().iterator();
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
        queueMgr.getRunner().update();
    }

    public synchronized boolean isPaused() { return paused; }
}
