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
 *  File created by keith @ Jun 5, 2003
 *
 */

package net.kano.joscar.ratelim;

import net.kano.joscar.snaccmd.conn.RateClassInfo;
import net.kano.joscar.snaccmd.conn.RateChange;
import net.kano.joscar.DefensiveTools;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RateClassMonitor {
    private static final Logger logger
            = Logger.getLogger("net.kano.joscar.ratelim");

    private final RateMonitor rateMonitor;
    private RateClassInfo rateInfo;
    private long last = -1;
    private long runningAvg;
    private boolean limited = false;
    private int errorMargin = -1;

    RateClassMonitor(RateMonitor rateMonitor, RateClassInfo rateInfo) {
        this.rateMonitor = rateMonitor;
        this.rateInfo = rateInfo;
        this.runningAvg = rateInfo.getMax();
    }

    synchronized void updateRateInfo(int changeCode, RateClassInfo rateInfo) {
        DefensiveTools.checkNull(rateInfo, "rateInfo");

        if (rateInfo.getRateClass() != this.rateInfo.getRateClass()) {
            throw new IllegalArgumentException("updated rate information is " +
                    "not the same class as the previous rate information for " +
                    "this rate class monitor");
        }

        this.rateInfo = rateInfo;
        // I'm not sure if this min call is necessary, or correct, but I know
        // sometimes the server will give you really crazy values (in the range
        // of several minutes) for an average. but that is only on the initial
        // rate class packet. either way I think your average can never
        // correctly be greater than the max.
        runningAvg = Math.min(rateInfo.getCurrentAvg(), rateInfo.getMax());

        if (changeCode == RateChange.CODE_LIMITED) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Rate class " + this.rateInfo.getRateClass()
                        + ") is now rate-limited!");
            }
            setLimited(true);

        } else if (changeCode == RateChange.CODE_LIMIT_CLEARED) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Rate class " + this.rateInfo.getRateClass()
                        + ") is no longer rate-limited, according to server");
            }
            setLimited(false);
        }
    }

    synchronized void updateRate(long cur) {
        if (last != -1) {
            assert cur >= last;
            runningAvg = computeCurrentAvg(cur);
        }
        last = cur;
    }

    public synchronized final RateClassInfo getRateInfo() { return rateInfo; }

    private synchronized void updateLimitedStatus() {
        if (isLimited()) {
            long avg = computeCurrentAvg();
            if (avg > rateInfo.getClearAvg() + getErrorMargin()) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("We think that rate class "
                            + rateInfo.getRateClass() + " is not limited "
                            + "anymore (avg is " + avg + ")");
                }
                setLimited(false);
            }
        }
    }

    public synchronized final int getErrorMargin() {
        if (errorMargin == -1) return rateMonitor.getErrorMargin();
        else return errorMargin;
    }

    public synchronized final void setErrorMargin(int errorMargin) {
        DefensiveTools.checkRange(errorMargin, "errorMargin", -1);

        this.errorMargin = errorMargin;
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

    private synchronized void setLimited(boolean limited) {
        if (limited != this.limited) {
            this.limited = limited;

            // TODO: notify listeners that we are limited or unlimited
        }
    }

    public synchronized final boolean isLimited() {
        updateLimitedStatus();

        return limited;
    }

    public synchronized final long getCurrentAvg() {
        return runningAvg;
    }

    public synchronized final long getPotentialAvg(long time) {
        return computeCurrentAvg(time);
    }

    public synchronized final long getOptimalWaitTime() {
        long minAvg;
        if (isLimited()) minAvg = rateInfo.getClearAvg();
        else minAvg = rateInfo.getLimitedAvg();

        return getTimeUntil(minAvg + getErrorMargin());
    }

    public synchronized final long getTimeUntil(long minAvg) {
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

    public synchronized final int getPossibleCmdCount() {
        return getPossibleCmdCount(runningAvg);
    }

    public synchronized final int getMaxCmdCount() {
        return getPossibleCmdCount(rateInfo.getMax());
    }

    private synchronized int getPossibleCmdCount(long currentAvg) {
        long diff = System.currentTimeMillis() - last;
        long winSize = rateInfo.getWindowSize();
        long limited = rateInfo.getLimitedAvg() + getErrorMargin();
        long avg = currentAvg;
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

    public synchronized final int getTimeUntilPossibleCmdCount(int cmdCount) {
        // TODO: implement getTimeUntilPossibleCmdCount
        return 0;
    }
}
