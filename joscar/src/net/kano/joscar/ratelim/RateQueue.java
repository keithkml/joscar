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

import net.kano.joscar.snaccmd.conn.RateClassInfo;
import net.kano.joscar.snaccmd.conn.RateChange;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snac.SnacRequest;

import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.logging.Level;

class RateQueue {
    private static final Logger logger
            = Logger.getLogger("net.kano.joscar.ratelim");

    private final ConnectionQueueMgr parentMgr;

    private RateClassInfo rateInfo;
    private LinkedList queue = new LinkedList();
    private long last = -1;
    private long runningAvg;
    private boolean limited = false;

    public RateQueue(ConnectionQueueMgr parentMgr, RateClassInfo rateInfo) {
        DefensiveTools.checkNull(parentMgr, "parentMgr");
        DefensiveTools.checkNull(rateInfo, "rateInfo");

        this.parentMgr = parentMgr;
        this.rateInfo = rateInfo;
        this.runningAvg = rateInfo.getMax();
    }

    public ConnectionQueueMgr getParentMgr() { return parentMgr; }

    public synchronized RateClassInfo getRateInfo() { return rateInfo; }

    public synchronized void setRateInfo(RateClassInfo rateInfo) {
        DefensiveTools.checkNull(rateInfo, "rateInfo");

        this.rateInfo = rateInfo;
        // I'm not sure if this max call is necessary, or correct, but I know
        // sometimes the server will give you really crazy values (in the range
        // of several minutes) for an average. but that is only on the initial
        // rate class packet. either way I think your average can never
        // correctly be greater than the max.
        runningAvg = Math.min(rateInfo.getCurrentAvg(), rateInfo.getMax());
    }

    public synchronized int getQueueSize() { return queue.size(); }

    public synchronized long getRunningAvg() { return runningAvg; }

    public synchronized boolean isLimited() {
        updateLimitedStatus();

        return limited;
    }

    private synchronized void updateLimitedStatus() {
        if (limited) {
            long avg = computeCurrentAvg();
            if (avg > rateInfo.getClearAvg() + getErrorMargin()) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("We think that rate class "
                            + rateInfo.getRateClass() + " is not limited "
                            + "anymore (avg is " + avg + ")");
                }
                limited = false;
            }
        }
    }

    private int getErrorMargin() {
        return parentMgr.getParentQueueMgr().getErrorMargin();
    }

    public synchronized long getOptimalWaitTime() {
        return getOptimalWaitTime(getErrorMargin());
    }

    public synchronized long getOptimalWaitTime(int errorMargin) {
        long minAvg;
        if (isLimited()) minAvg = rateInfo.getClearAvg();
        else minAvg = rateInfo.getLimitedAvg();

        return getWaitTime(minAvg + errorMargin);
    }

    public synchronized long getWaitTime(long minAvg) {
        if (last == -1) return 0;

        long winSize = rateInfo.getWindowSize();
        long sinceLast = System.currentTimeMillis() - last;

        long minLastDiff = (winSize * minAvg) - (runningAvg  * (winSize - 1));
        long toWait = minLastDiff - sinceLast;

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Class " + rateInfo.getRateClass()
                    + " should be waiting " + toWait + "ms (avg is "
                    + computeCurrentAvg() + "ms)");
        }

        return Math.max(toWait, 0);
    }

    public synchronized void enqueue(SnacRequest req) {
        DefensiveTools.checkNull(req, "req");

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Enqueuing " + req.getCommand() + " within ratequeue " +
                    "(class " + rateInfo.getRateClass() + ")...");
        }

        queue.add(req);
    }

    public synchronized boolean hasRequests() {
        return !queue.isEmpty();
    }

    public synchronized SnacRequest dequeue() {
        if (queue.isEmpty()) return null;

        long cur = System.currentTimeMillis();
        if (last != -1) {
            runningAvg = computeCurrentAvg(cur);
        }
        last = cur;

        SnacRequest request = (SnacRequest) queue.removeFirst();

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Dequeueing " + request.getCommand()
                    + " from ratequeue (class " + rateInfo.getRateClass()
                    + ")...");
        }

        return request;
    }

    private synchronized long computeCurrentAvg(long currentTime) {
        long diff = currentTime - last;
        long winSize = rateInfo.getWindowSize();
        long max = rateInfo.getMax();
        return Math.min(max, (runningAvg * (winSize - 1) + diff) / winSize);
    }

    private synchronized long computeCurrentAvg() {
        return computeCurrentAvg(System.currentTimeMillis());
    }

    public synchronized void clear() {
        queue.clear();
    }

    public synchronized void setChangeCode(int changeCode) {
        if (changeCode == RateChange.CODE_LIMITED) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Rate class " + rateInfo.getRateClass()
                        + ") is now rate-limited!");
            }
            limited = true;
        } else if (changeCode == RateChange.CODE_LIMIT_CLEARED) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Rate class " + rateInfo.getRateClass()
                        + ") is no longer rate-limited, according to server");
            }
            limited = false;
        }
    }

    public synchronized long getPotentialAvg(long time) {
        return computeCurrentAvg(time);
    }

    public synchronized int getPossibleCmdCount() {
        return getPossibleCmdCount(runningAvg);
    }

    public synchronized int getMaxCmdCount() {
        return getPossibleCmdCount(rateInfo.getMax());
    }

    private int getPossibleCmdCount(long initialAvg) {
        long diff = System.currentTimeMillis() - last;
        long winSize = rateInfo.getWindowSize();
        long limited = rateInfo.getLimitedAvg() + getErrorMargin();
        long avg = initialAvg;
        int count = 0;

        while (avg > limited) {
            avg = (diff + avg * (winSize - 1)) / winSize;

            // after the first iteration we set diff to 0, since the difference
            // will be zero
            diff = 0;

            count++;
        }

        // the loop iterates once past the maximum
        return count - 1;
    }
}
