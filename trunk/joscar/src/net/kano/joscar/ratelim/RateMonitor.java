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

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snac.*;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snaccmd.conn.RateChange;
import net.kano.joscar.snaccmd.conn.RateClassInfo;
import net.kano.joscar.snaccmd.conn.RateInfoCmd;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RateMonitor {
    public static final Object ERRTYPE_RATE_LISTENER = "ERRTYPE_RATE_LISTENER";

    public static final int ERRORMARGIN_DEFAULT = 100;

    private static final Logger logger
            = Logger.getLogger("net.kano.joscar.ratelim");

    private SnacProcessor snacProcessor;

    private final CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();

    private final Object listenerEventLock = new Object();

    private Map classToQueue = new HashMap();
    private Map typeToQueue = new HashMap(500);
    private RateClassMonitor defaultMonitor = null;

    private int errorMargin = ERRORMARGIN_DEFAULT;

    private OutgoingSnacRequestListener requestListener
            = new OutgoingSnacRequestListener() {
        public void handleSent(SnacRequestSentEvent e) {
            updateRate(e);
        }

        public void handleTimeout(SnacRequestTimeoutEvent event) { }
    };
    private SnacResponseListener responseListener
            = new SnacResponseListener() {
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

    public RateMonitor(SnacProcessor processor) {
        DefensiveTools.checkNull(processor, "processor");

        this.snacProcessor = processor;

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

    public final SnacProcessor getSnacProcessor() {
        return snacProcessor;
    }

    public final void addListener(RateListener l) {
        DefensiveTools.checkNull(l, "l");

        listeners.addIfAbsent(l);
    }

    public final void removeListener(RateListener l) {
        DefensiveTools.checkNull(l, "l");

        listeners.remove(l);
    }

    /**
     * Note that this method clears all rate information
     */
    public final void setRateClasses(RateClassInfo[] rateInfos) {
        DefensiveTools.checkNull(rateInfos, "rateInfos");
        rateInfos = (RateClassInfo[]) rateInfos.clone();
        DefensiveTools.checkNullElements(rateInfos, "rateInfos");

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Got rate classes for monitor " + this);
        }

        // I guess this is a new connection now
        synchronized(this) {
            typeToQueue.clear();
            classToQueue.clear();
            defaultMonitor = null;

            for (int i = 0; i < rateInfos.length; i++) {
                RateClassInfo rateInfo = rateInfos[i];

                setRateClass(rateInfo);
            }
        }

        synchronized(listenerEventLock) {
            for (Iterator it = listeners.iterator(); it.hasNext();) {
                RateListener listener = (RateListener) it.next();

                try {
                    listener.gotRateClasses(this);
                } catch (Throwable t) {
                    handleException(ERRTYPE_RATE_LISTENER, t, listener);
                }
            }
        }
    }

    private synchronized void setRateClass(RateClassInfo rateInfo) {
        DefensiveTools.checkNull(rateInfo, "rateInfo");

        RateClassMonitor monitor = new RateClassMonitor(this, rateInfo);
        classToQueue.put(new Integer(rateInfo.getRateClass()), monitor);

        CmdType[] cmdTypes = rateInfo.getCommands();
        if (cmdTypes != null) {
            if (cmdTypes.length == 0) {
                // if there aren't any member SNAC commands for this rate
                // class, this is the "fallback" rate class, or the
                // "default queue"
                if (defaultMonitor == null) defaultMonitor = monitor;

            } else {
                // there are command types associated with this rate class,
                // so, for speed, we put them into a map
                for (int i = 0; i < cmdTypes.length; i++) {
                    typeToQueue.put(cmdTypes[i], monitor);
                }
            }
        }
    }

    public void updateRateClass(int changeCode, RateClassInfo rateInfo) {
        DefensiveTools.checkRange(changeCode, "changeCode", 0);
        DefensiveTools.checkNull(rateInfo, "rateInfo");

        RateClassMonitor monitor = getRateMonitor(rateInfo);

        monitor.updateRateInfo(changeCode, rateInfo);

        synchronized(listenerEventLock) {
            for (Iterator it = listeners.iterator(); it.hasNext();) {
                RateListener listener = (RateListener) it.next();

                try {
                    listener.rateClassUpdated(this, monitor, rateInfo);
                } catch (Throwable t) {
                    handleException(ERRTYPE_RATE_LISTENER, t, listener);
                }
            }
        }
    }

    private void handleException(Object type, Throwable t, Object info) {
        SnacProcessor processor;
        synchronized(this) {
            processor = snacProcessor;
        }

        if (processor != null) {
            processor.getFlapProcessor().handleException(type, t, info);
        } else {
            System.err.println("Rate monitor couldn't process error because "
                    + "not attached to SNAC processor: " + t.getMessage()
                    + " (reason obj: " + info + ")");
            t.printStackTrace();
        }
    }

    private void updateRate(SnacRequestSentEvent e) {
        CmdType cmdType = CmdType.ofCmd(e.getRequest().getCommand());

        RateClassMonitor monitor = getMonitor(cmdType);

        if (monitor == null) return;

        monitor.updateRate(e.getSentTime());
    }

    void fireLimitedEvent(RateClassMonitor monitor, boolean limited) {
        synchronized(listenerEventLock) {
            for (Iterator it = listeners.iterator(); it.hasNext();) {
                RateListener listener = (RateListener) it.next();

                try {
                    listener.rateClassLimited(this, monitor, limited);
                } catch (Throwable t) {
                    handleException(ERRTYPE_RATE_LISTENER, t, listener);
                }
            }
        }
    }


    public synchronized final void setErrorMargin(int errorMargin) {
        DefensiveTools.checkRange(errorMargin, "errorMargin", 0);

        this.errorMargin = errorMargin;
    }

    public final synchronized int getErrorMargin() { return errorMargin; }

    public final synchronized RateClassMonitor[] getMonitors() {
        return (RateClassMonitor[])
                classToQueue.values().toArray(new RateClassMonitor[0]);
    }

    public final synchronized RateClassMonitor getMonitor(CmdType type) {
        DefensiveTools.checkNull(type, "type");

        RateClassMonitor queue = (RateClassMonitor) typeToQueue.get(type);

        if (queue == null) queue = defaultMonitor;

        return queue;
    }

    private synchronized RateClassMonitor getRateMonitor(RateClassInfo info) {
        return getRateMonitor(info.getRateClass());
    }

    private synchronized RateClassMonitor getRateMonitor(int rateClass) {
        Integer key = new Integer(rateClass);
        return (RateClassMonitor) classToQueue.get(key);
    }
}
