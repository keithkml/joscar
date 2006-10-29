/*
 *  Copyright (c) 2006, The Joust Project
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

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.logging.Logger;
import net.kano.joscar.logging.LoggingSystem;
import net.kano.joscar.snaccmd.conn.RateChange;
import net.kano.joscar.snaccmd.conn.RateClassInfo;


/**
 * Monitors rate information for a single rate class.
 */
public class RateClassMonitorImpl implements RateClassMonitor {
    /** A logger for rate-related logging. */
    private static final Logger logger
            = LoggingSystem.getLogger("net.kano.joscar.ratelim");

    /** The rate monitor that acts as this monitor's parent. */
    private final RateMonitor rateMonitor;
    /** Rate information for the rate class that this monitor is monitoring. */
    private RateClassInfo rateInfo;
    private RateClassListener listener;
    /** The time at which the last command was sent in this class. */
    private long last = -1;
    /** The current "running average" for this class. */
    private long runningAvg;
    /** Whether or not this rate class is limited. */
    private boolean limited = false;
    /**
     * This class's error margin, or <code>-1</code> to fall through to the
     * parent rate monitor's error margin.
     */
    private int errorMargin = -1;
  
    private TimeProvider timeProvider;

    /**
     * Creates a new rate class monitor with the given parent rate monitor and
     * rate class information.
     *
     * @param rateMonitor this rate class monitor's "parent" rate monitor
     * @param rateInfo information about the rate class that this monitor should
     *        monitor
     */
    RateClassMonitorImpl(RateMonitor rateMonitor, RateClassInfo rateInfo,
            RateClassListener listener) {
        this(rateMonitor, rateInfo, listener, new TimeProvider() {
            public long getCurrentTime() {
                return System.currentTimeMillis();
            }
        });
    }

    RateClassMonitorImpl(RateMonitor rateMonitor, RateClassInfo rateInfo,
                      RateClassListener listener, TimeProvider provider) {
        this.rateMonitor = rateMonitor;
        this.rateInfo = rateInfo;
        this.listener = listener;
        this.runningAvg = rateInfo.getCurrentAvg();
        this.timeProvider = provider;
        long sinceLast = rateInfo.getTimeSinceLastCommand();
        if (sinceLast == 0) {
            last = -1;
        } else {
            last = timeProvider.getCurrentTime() - sinceLast;
        }
        this.setLimited(rateInfo.getCurrentState() == RateChange.CODE_LIMITED);
    }

    /**
     * Updates the rate information for this monitor's associated rate class.
     *
     * @param changeCode the rate change code sent by the server with the given
     *        rate information, or <code>-1</code> if none was sent
     * @param rateInfo the rate information that was sent
     */
    synchronized void updateRateInfo(int changeCode, RateClassInfo rateInfo) {
        DefensiveTools.checkNull(rateInfo, "rateInfo");

        if (rateInfo.getRateClass() != this.rateInfo.getRateClass()) {
            throw new IllegalArgumentException("updated rate information is " +
                    "not the same class as the previous rate information for " +
                    "this rate class monitor");
        }

        if (logger.logFinerEnabled()) {
            logger.logFiner("Rate monitor for class " + rateInfo.getRateClass()
                    + " thinks rate average is " + runningAvg + "ms; server "
                    + "thinks it is " + rateInfo.getCurrentAvg() + "ms");
        }

        this.rateInfo = rateInfo;
        // I'm not sure if this min call is necessary, or correct, but I know
        // sometimes the server will give you really crazy values (in the range
        // of several minutes) for an average. but that is only on the initial
        // rate class packet. either way I think your average can never
        // correctly be greater than the max.
        runningAvg = Math.min(rateInfo.getMax(), runningAvg);

        if (changeCode == RateChange.CODE_LIMITED) {
            if (logger.logWarningEnabled()) {
                logger.logWarning("Rate class " + this.rateInfo.getRateClass()
                        + " is now rate-limited!");
            }
            setLimited(true);

        } else if (changeCode == RateChange.CODE_LIMIT_CLEARED) {
            if (logger.logWarningEnabled()) {
                logger.logWarning("Rate class " + this.rateInfo.getRateClass()
                        + " is no longer rate-limited, according to server");
            }
            setLimited(false);
        }
    }

    /**
     * Updates this monitor's associated rate class's current rate with the
     * given send time.
     *
     * @param sentTime the time at which a command in the associated rate class
     *        was sent, in milliseconds since the unix epoch
     */
    synchronized void updateRate(long sentTime) {
        if (last != -1) {
            assert sentTime >= last;
            runningAvg = computeCurrentAvg(sentTime);
        }
        last = sentTime;
    }

    public synchronized final RateClassInfo getRateInfo() { return rateInfo; }

    /**
     * Ensures that this monitor's limited status ({@link #limited}) is
     * accurate.
     */
    private synchronized void updateLimitedStatus() {
        if (limited) {
            long avg = computeCurrentAvg();
            if (avg >= rateInfo.getClearAvg() + getErrorMargin()) {
                if (logger.logFineEnabled()) {
                    logger.logFine("We think that rate class "
                            + rateInfo.getRateClass() + " is not limited "
                            + "anymore (avg is " + avg + ")");
                }
                setLimited(false);
            }
        }
    }

    public synchronized final int getErrorMargin() {
      if (errorMargin != -1) {
        return errorMargin;
      }
      return rateMonitor.getErrorMargin();
    }

    public synchronized final int getLocalErrorMargin() { return errorMargin; }

    public synchronized final void setErrorMargin(int errorMargin) {
        DefensiveTools.checkRange(errorMargin, "errorMargin", -1);

        this.errorMargin = errorMargin;
    }

    /**
     * Computes a new rate average given the time at which a command was sent.
     * Note that this method does <i>not</i> modify the current running average;
     * it merely computes a new one and returns it.
     *
     * @param sentTime the time at which the command was sent
     * @return a new average computed from the given send time and the current
     *         running average
     */
    private synchronized long computeCurrentAvg(long sentTime) {
        long diff = sentTime - last;
        long winSize = rateInfo.getWindowSize();
        long max = rateInfo.getMax();
        return Math.min(max, (runningAvg * (winSize - 1) + diff) / winSize);
    }

    /**
     * Computes "the current rate average," what the average would be if a
     * command were sent at the time this method was invoked.
     *
     * @return the "current rate average"
     *
     * @see #computeCurrentAvg(long)
     */
    private synchronized long computeCurrentAvg() {
        return computeCurrentAvg(getCurrentTime());
    }

    /**
     * Sets whether this rate monitor's associated rate class is currently
     * rate-limited. This method will notify any listeners if the value has
     * changed.
     *
     * @param limited whether or not this monitor's rate class is currently rate
     *        limited
     */
    private synchronized void setLimited(boolean limited) {
        if (limited == this.limited) return;

        this.limited = limited;

        listener.handleLimitedEvent(this, limited);
    }

    public synchronized final boolean isLimited() {
        updateLimitedStatus();

        return limited;
    }

/*
    //TOLATER: implement, document getCurrentAvg
    public final long getCurrentAvg() {
        return computeCurrentAvg();
    }
*/

    public synchronized final long getLastRateAvg() {
        return runningAvg;
    }

    public final long getPotentialAvg() {
        return getPotentialAvg(getCurrentTime());
    }

    public final long getPotentialAvg(long time) {
        return computeCurrentAvg(time);
    }

    public synchronized final long getOptimalWaitTime() {
        return getTimeUntil(getMinSafeAvg() + getErrorMargin());
    }

    /**
     * Returns the average above which this monitor's associated rate class's
     * average must stay to avoid being rate-limited. This method ignores the
     * {@linkplain #getErrorMargin error margin}.
     *
     * @return the minimum average that this monitor's associated rate class
     *         must stay above to avoid rate limiting
     */
    private synchronized long getMinSafeAvg() {
        if (isLimited()) return rateInfo.getClearAvg();
        else return rateInfo.getLimitedAvg();
    }

    public synchronized final long getTimeUntil(long minAvg) {
        if (last == -1) return 0;

        long winSize = rateInfo.getWindowSize();
        long sinceLast = getCurrentTime() - last;

        long minLastDiff = (winSize * minAvg) - (runningAvg  * (winSize - 1));
        long toWait = minLastDiff - sinceLast + 1;

        if (logger.logFineEnabled()) {
            logger.logFine("Class " + rateInfo.getRateClass()
                    + " should be waiting " + toWait + "ms (avg is "
                    + computeCurrentAvg() + "ms)");
        }

        return Math.max(toWait, 0);
    }

    private long getCurrentTime() {
        return timeProvider.getCurrentTime();
    }

    public synchronized final int getPossibleCmdCount() {
        return getPossibleCmdCount(runningAvg);
    }

    public synchronized final int getMaxCmdCount() {
        return getPossibleCmdCount(rateInfo.getMax());
    }

    /**
     * Returns the number of commands that could be sent in this monitor's
     * associated rate class without being rate-limited.
     *
     * @param currentAvg the starting average (normally the current average)
     * @return the number of commands that could be sent in this monitor's
     *         associated rate class without being rate-limited
     */
    private synchronized int getPossibleCmdCount(long currentAvg) {
        long diff = getCurrentTime() - last;
        long winSize = rateInfo.getWindowSize();
        long limited = getMinSafeAvg() + getErrorMargin();
        long avg = currentAvg;
        int count = 0;

        while (avg > limited) {
            avg = (diff + avg * (winSize - 1)) / winSize;

            // after the first iteration we set diff to 0, since the difference
            // will be zero
            diff = 0;

            count++;
        }

        // this means the loop never iterated, so no commands can be sent
        if (count == 0) return 0;

        // the loop iterates once past the maximum, so we decrement the counter
        // to get the number of commands that can be sent
        return count - 1;
    }

    public String toString() {
        return "RateClassMonitor: "
                + "rateInfo=" + rateInfo
                + ", last=" + last
                + ", runningAvg=" + runningAvg
                + ", limited=" + limited
                + ", errorMargin=" + errorMargin;
    }
}
