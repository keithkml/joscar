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
import net.kano.joscar.snac.*;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A SNAC queue manager which utilizes the "official" rate limiting algorithm to
 * avoid ever becoming rate-limited.
 */
public class RateLimitingQueueMgr implements SnacQueueManager {
    public static final int ERRORMARGIN_DEFAULT = 100;

    private Map conns = new IdentityHashMap();

    private int errorMargin = ERRORMARGIN_DEFAULT;

    private QueueRunner runner = new QueueRunner(this);

    private RateListener rateListener = new RateListener() {
        public void attached(RateMonitor monitor, SnacProcessor snacProcessor) {
            attachToSnacProcessor(snacProcessor);
        }

        public void detached(RateMonitor monitor, SnacProcessor snacProcessor) {
            detachFromSnacProcessor(snacProcessor);
        }

        public void gotRateClasses(RateMonitor monitor) {
            resetRateClasses(monitor);
        }

        public void rateClassUpdated(RateMonitor monitor,
                RateClassMonitor classMonitor, RateClassInfo rateInfo) {
            runner.update();
        }

        public void rateClassLimited(RateMonitor rateMonitor,
                RateClassMonitor rateClassMonitor, boolean limited) {
            runner.update();
        }
    };

    public final void attach(RateMonitor monitor) {
        monitor.addListener(rateListener);
        SnacProcessor processor = monitor.getSnacProcessor();

        if (processor != null) {
            attachToSnacProcessor(processor);
        }
    }

    private synchronized void attachToSnacProcessor(SnacProcessor processor) {
        processor.setSnacQueueManager(this);
    }

    private synchronized void detachFromSnacProcessor(SnacProcessor processor) {
        conns.remove(processor);

        processor.unsetSnacQueueManager(this);
    }

    private void resetRateClasses(RateMonitor monitor) {
        
    }

    public final ConnectionQueueMgr getQueueMgr(SnacProcessor processor) {
        DefensiveTools.checkNull(processor, "processor");

        ConnectionQueueMgr mgr;
        synchronized(conns) {
            mgr = (ConnectionQueueMgr) conns.get(processor);
        }
        if (mgr == null) {
            throw new IllegalArgumentException("this rate manager is not " +
                    "currently attached to processor " + processor);
        }
        return mgr;
    }

    public synchronized final void setErrorMargin(int errorMargin) {
        DefensiveTools.checkRange(errorMargin, "errorMargin", 0);

        this.errorMargin = errorMargin;
    }

    public final synchronized int getErrorMargin() { return errorMargin; }


    final QueueRunner getRunner() { return runner; }

    void sendSnac(SnacProcessor processor, SnacRequest request) {
        processor.sendSnacImmediately(request);
    }


    public void queueSnac(SnacProcessor processor, SnacRequest request) {
        DefensiveTools.checkNull(request, "request");

        getQueueMgr(processor).queueSnac(request);
    }

    public void clearQueue(SnacProcessor processor) {
        getQueueMgr(processor).clearQueue();
    }

    public void pause(SnacProcessor processor) {
        getQueueMgr(processor).pause();
    }

    public void unpause(SnacProcessor processor) {
        getQueueMgr(processor).unpause();
    }
}

