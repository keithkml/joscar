/*
 *  Copyright (c) 2002-2003, The Joust Project
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
 *  File created by keith @ Feb 21, 2003
 *
 */

package net.kano.joscar.snaccmd.conn;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.Writable;
import net.kano.joscar.snac.CmdType;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A data structure containing rate limiting information for a specific "class"
 * of SNAC commands.
 * <br>
 * <br>
 * Here's a quick explanation of what is currently known about rate classes.
 * A rate class identifies a set of SNAC commands and limitations on how fast
 * any sequence of those commands can be sent to the server. For example, one
 * rate class normally contains the outgoing ICBM command and the info request
 * command. You may have noticed that sometimes WinAIM will tell you you can't
 * look at someone's info because your rate is too high; this is why.
 * <br>
 * <br>
 * Now, somehow the AIM hacking community (namely Sean Egan) knows some things
 * about rate class information packets, but no one seems to know how these data
 * correspond to the rate at which you can send packets, exactly.
 * <br>
 * <br>
 * So, here is a description of all of the fields in this object, described as
 * best I can. (These values can be retrieved with the fields' getters, like
 * <code>getWindowSize</code>.)
 * <dl>
 * <dt><code>windowSize</code></dt>
 * <dd>The number of previously sent commands that will be included in
 * the calculation of your current "rate average" (this value varies from
 * rate class to rate class; normally ranges from <code>10</code> to
 * <code>60</code>)</dd>
 * <dt><code>currentAvg</code>
 * <dd>Your current "rate average," which is some sort of average of the times
 * between each of your last <code><i>windowSize</i></code> commands</dd>
 * <dt><code>warnAvg</code></dt>
 * <dd>The "rate average" that will put you into the yellow part of
 * WinAIM's rate limiting bar (normally <code>5000</code> ms)</dd>
 * <dt><code>limitedAvg</code></dt>
 * <dd>The "rate average" under which you will be "rate limited" until your
 * rate average is back above <code>clearAvg</code> (normally <code>4000</code>
 * ms)</dd>
 * <dt><code>clearAvg</code></dt>
 * <dd>The "rate average" above which you will stop being rate limited, if you
 * are currently limited; this is normally equal to <code>warnAvg</code> plus
 * <code>100</code> ms, or <code>5100</code> ms</dd>
 * <dt><code>disconnectAvg</code></dt>
 * <dd>The "rate average" below which you will be disconnected from the server
 * (normally <code>3000</code> ms)</dd>
 * <dt><code>max</code></dt>
 * <dd>May be the maximum time between commands to use in "rate average"
 * computations; for example, if 5 minutes passed between two commands, the
 * <code>300000</code> ms value would be replaced with this value (normally
 * <code>6000</code> ms)
 * </dl>
 *
 * Now this all seems pretty easy to implement, but in fact it is not as simple
 * as it appears. While here I refer to these values as "rate averages" they are
 * in fact not pure arithmetic means. From my research it appears as if the
 * "rate average" value is in fact the sum of each indivudual time difference
 * between two subsequent commands (in milliseconds) raised to the power of
 * <code>0.6</code>. In Maple notation, the sum is
 * <code>sum(differences[k]^0.6, k=1..windowSize)</code>.
 * <br>
 * <br>
 * Upon further inspection, the above equation does not work consistently.
 * <br>
 * <br>
 * This is all I have as far as research on this topic so far.
 */
public class RateClassInfo implements Writable {
    /** The rate class ID of this rate class information block. */
    private final int rateClass;
    /** The "window size." */
    private final long windowSize;

    /** The average below which you are "warned." */
    private final long warnAvg;
    /** The average below which you are rate-limited. */
    private final long limitedAvg;
    /** The average above which you are no longer rate-limited. */
    private final long clearAvg;
    /** The average below which you will be disconnected. */
    private final long disconnectAvg;

    /** Your current average. */
    private final long currentAvg;

    /** The maximum time between messages. We think. */
    private final long max;

    /** The commands in this rate class. */
    private CmdType[] commands = null;

    /**
     * Generates a rate class information block from the given block of data.
     * The total number of bytes read can be accessed by calling the
     * <code>getTotalSize</code> method of the returned
     * <code>RateClassInfo</code>.
     *
     * @param block a block of data containing a rate information block
     * @return a rate class information object read from the given block of
     *         data
     */
    public static RateClassInfo readRateClassInfo(ByteBlock block) {
        if (block.getLength() < 35) return null;

        return new RateClassInfo(block);
    }

    /**
     * Creates a new rate class information block from the data in the given
     * block.
     *
     * @param block the block of data containing rate class information
     */
    private RateClassInfo(ByteBlock block) {
        rateClass     = BinaryTools.getUShort(block,  0);
        windowSize    = BinaryTools.getUInt  (block,  2);
        clearAvg      = BinaryTools.getUInt  (block,  6);
        warnAvg       = BinaryTools.getUInt  (block, 10);
        limitedAvg    = BinaryTools.getUInt  (block, 14);
        disconnectAvg = BinaryTools.getUInt  (block, 18);
        currentAvg    = BinaryTools.getUInt  (block, 22);
        max           = BinaryTools.getUInt  (block, 26);
    }

    /**
     * Sets the commands included in this rate class.
     *
     * @param commands the SNAC commands included in this rate class
     */
    synchronized void setCommands(CmdType[] commands) {
        this.commands = commands;
    }

    /**
     * Creates a new rate class information block with the given properties.
     * See {@linkplain RateClassInfo above} for details on what these mean.
     *
     * @param rateClass the rate class ID that this block describes
     * @param windowSize the "window size"
     * @param clearAvg the "not rate limited anymore" average
     * @param warnAvg the "warned" average
     * @param limitedAvg the "rate limited" average
     * @param disconnectAvg the "disconnected" average
     * @param currentAvg the current average
     * @param max the maximum time between commands
     */
    public RateClassInfo(int rateClass, long windowSize, long clearAvg,
            long warnAvg, long limitedAvg, long disconnectAvg, long currentAvg,
            long max) {
        DefensiveTools.checkRange(rateClass, "rateClass", 0);
        DefensiveTools.checkRange(windowSize, "windowSize", 0);
        DefensiveTools.checkRange(clearAvg, "clearAvg", 0);
        DefensiveTools.checkRange(warnAvg, "warnAvg", 0);
        DefensiveTools.checkRange(limitedAvg, "limitedAvg", 0);
        DefensiveTools.checkRange(disconnectAvg, "disconnectAvg", 0);
        DefensiveTools.checkRange(currentAvg, "currentAvg", 0);
        DefensiveTools.checkRange(max, "max", 0);

        this.rateClass = rateClass;
        this.windowSize = windowSize;
        this.clearAvg = clearAvg;
        this.warnAvg = warnAvg;
        this.limitedAvg = limitedAvg;
        this.disconnectAvg = disconnectAvg;
        this.currentAvg = currentAvg;
        this.max = max;
    }

    /**
     * Returns the ID of the rate class that holds this rate class info.
     *
     * @return this rate class information block's rate class ID
     */
    public final int getRateClass() {
        return rateClass;
    }

    /**
     * Returns the "window size" of this rate class. This is a number indicating
     * how far back in the command history to look when computing rate averages.
     * See {@linkplain RateClassInfo above} for more details.
     *
     * @return the rate class's window size
     */
    public final long getWindowSize() {
        return windowSize;
    }

    /**
     * Returns the rate average below which the user is "warned." See
     * {@linkplain RateClassInfo above} for more details.
     *
     * @return the "warned rate average"
     */
    public final long getWarnAvg() {
        return warnAvg;
    }

    /**
     * Returns the rate average below which the user is rate-limited. No
     * commands should be sent to the server in this rate class until the rate
     * average is above the {@linkplain #getClearAvg clear average}. See
     * {@linkplain RateClassInfo above} for more details.
     *
     * @return the rate-limited rate average
     */
    public final long getLimitedAvg() {
        return limitedAvg;
    }

    /**
     * Returns the rate average above which the user is no longer rate limited.
     * See {@linkplain RateClassInfo above} for more details.
     *
     * @return the rate class's "cleared of rate limiting" average
     */
    public final long getClearAvg() {
        return clearAvg;
    }

    /**
     * Returns the rate average below which the user will be disconnected. See
     * {@linkplain RateClassInfo above} for more details.
     *
     * @return the disconnect rate average
     */
    public final long getDisconnectAvg() {
        return disconnectAvg;
    }

    /**
     * Returns the user's current rate average in this rate class.
     *
     * @return the user's current rate average.
     */
    public final long getCurrentAvg() {
        return currentAvg;
    }

    /**
     * Returns the maximum amount of time between commands to use in rate
     * average computation. I think. See {@linkplain RateClassInfo above} for
     * more details.
     *
     * @return the maximum time between commands in this class
     */
    public final long getMax() {
        return max;
    }

    /**
     * Returns the commands included in this rate class, or <code>null</code>
     * if they were not sent (as in a <code>RateChange</code>).
     *
     * @return the SNAC command types included in this rate class
     */
    public synchronized final CmdType[] getCommands() {
        return (CmdType[]) (commands == null ? null : commands.clone());
    }

    public long getWritableLength() {
        return 35;
    }

    public void write(OutputStream out) throws IOException {
        BinaryTools.writeUShort(out, rateClass);
        BinaryTools.writeUInt(out, windowSize);
        BinaryTools.writeUInt(out, clearAvg);
        BinaryTools.writeUInt(out, warnAvg);
        BinaryTools.writeUInt(out, limitedAvg);
        BinaryTools.writeUInt(out, disconnectAvg);
        BinaryTools.writeUInt(out, currentAvg);
        BinaryTools.writeUInt(out, max);
    }

    public synchronized String toString() {
        return "RateClassInfo for class " + rateClass +
                ", currentAvg=" + currentAvg +
                ", windowSize=" + windowSize +
                ", clearAvg=" + clearAvg +
                ", warnAvg=" + warnAvg +
                ", limitedAvg=" + limitedAvg +
                ", disconnectAvg=" + disconnectAvg +
                ", max=" + max +
                ", families: "
                + (commands == null ? "none" : "" + commands.length);
    }
}
