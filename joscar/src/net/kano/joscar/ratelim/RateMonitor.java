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
 *  File created by keith @ Jun 4, 2003
 *
 */

package net.kano.joscar.ratelim;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.*;
import net.kano.joscar.snaccmd.conn.RateChange;
import net.kano.joscar.snaccmd.conn.RateClassInfo;
import net.kano.joscar.snaccmd.conn.RateInfoCmd;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

public class RateMonitor {
    public static final int ERRORMARGIN_DEFAULT = 100;

    private int errorMargin = ERRORMARGIN_DEFAULT;

    private static final Logger logger
            = Logger.getLogger("net.kano.joscar.ratelim");

    private SnacProcessor snacProcessor = null;

    private boolean gotRateClasses = false;

    private Map classToQueue = new HashMap();
    private Map typeToQueue = new HashMap(500);
    private RateClassMonitor defaultQueue = null;

    private OutgoingSnacRequestListener requestListener
            = new OutgoingSnacRequestListener() {
        public void handleSent(SnacRequestSentEvent e) {
            updateRate(e);
        }

        public void handleTimeout(SnacRequestTimeoutEvent event) { }
    };
    private SnacResponseListener responseListener = new SnacResponseListener() {
        public void handleResponse(SnacResponseEvent e) {
            SnacCommand cmd = e.getSnacCommand();

            if (cmd instanceof RateInfoCmd) {
                RateInfoCmd ric = (RateInfoCmd) cmd;

                setRateClasses(ric.getRateClassInfos());
            }
        }
    };
    private SnacPacketListener packetListener = new SnacPacketListener() {
        public void handleSnacPacket(SnacPacketEvent e) {
            SnacCommand cmd = e.getSnacCommand();

            if (cmd instanceof RateChange) {
                RateChange rc = (RateChange) cmd;

                RateClassInfo rateInfo = rc.getRateInfo();
                if (rateInfo != null) {
                    int code = rc.getChangeCode();
                    updateRateClass(code, rateInfo);
                }
            }
        }
    };

    public synchronized final void attach(SnacProcessor processor) {
        DefensiveTools.checkNull(processor, "processor");

        detach();

        processor.addGlobalRequestListener(requestListener);
        processor.addPacketListener(packetListener);
        processor.addGlobalResponseListener(responseListener);
    }

    public synchronized final void detach() {
        if (snacProcessor == null) return;

        snacProcessor.removeGlobalRequestListener(requestListener);
        snacProcessor.removePacketListener(packetListener);
        snacProcessor.removeGlobalResponseListener(responseListener);

        snacProcessor = null;
    }

    public void setRateClasses(RateClassInfo[] rateInfos) {
        DefensiveTools.checkNull(rateInfos, "rateInfos");

        synchronized(this) {
            if (gotRateClasses) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Already got rate classes for monitor " + this
                            + "; ignoring new list of classes");
                }
                return;
            }
            gotRateClasses = true;
        }

        rateInfos = (RateClassInfo[]) rateInfos.clone();

        DefensiveTools.checkNullElements(rateInfos, "rateInfos");

        for (int i = 0; i < rateInfos.length; i++) {
            RateClassInfo rateInfo = rateInfos[i];

            setRateClass(rateInfo);
        }

        //TODO: update listeners to tell them we got rate infoz
    }

    private synchronized void setRateClass(RateClassInfo rateInfo) {
        DefensiveTools.checkNull(rateInfo, "rateInfo");

        RateClassMonitor queue = createRateMonitor(rateInfo);

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
    }

    public void updateRateClass(int changeCode, RateClassInfo rateInfo) {
        DefensiveTools.checkRange(changeCode, "changeCode", 0);
        DefensiveTools.checkNull(rateInfo, "rateInfo");

        RateClassMonitor queue = getRateMonitor(rateInfo);

        queue.updateRateInfo(changeCode, rateInfo);
    }

    private void updateRate(SnacRequestSentEvent e) {
        CmdType cmdType = CmdType.ofCmd(e.getRequest().getCommand());

        RateClassMonitor monitor = getMonitor(cmdType);

        if (monitor == null) return;

        monitor.updateRate(e.getSentTime());
    }


    public synchronized final void setErrorMargin(int errorMargin) {
        DefensiveTools.checkRange(errorMargin, "errorMargin", 0);

        this.errorMargin = errorMargin;
    }

    public final synchronized int getErrorMargin() { return errorMargin; }

    private synchronized RateClassMonitor getMonitor(CmdType type) {
        DefensiveTools.checkNull(type, "type");

        RateClassMonitor queue = (RateClassMonitor) typeToQueue.get(type);

        if (queue == null) queue = defaultQueue;

        return queue;
    }

    private synchronized RateClassMonitor getRateMonitor(RateClassInfo info) {
        return getRateMonitor(info.getRateClass());
    }

    private synchronized RateClassMonitor getRateMonitor(int rateClass) {
        Integer key = new Integer(rateClass);
        return (RateClassMonitor) classToQueue.get(key);
    }

    private synchronized RateClassMonitor createRateMonitor(
            RateClassInfo rateInfo) {
        Integer key = new Integer(rateInfo.getRateClass());

        if (classToQueue.containsKey(key)) {
            throw new IllegalArgumentException("rate monitor already exists for " +
                    "rate class '" + rateInfo + "'");
        }

        RateClassMonitor monitor = new RateClassMonitor(this, rateInfo);
        classToQueue.put(key, monitor);

        return monitor;
    }

}
