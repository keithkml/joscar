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
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.*;
import net.kano.joscar.snaccmd.conn.RateClassInfo;
import net.kano.joscar.snaccmd.conn.RateInfoCmd;
import net.kano.joscar.snaccmd.conn.RateChange;

import java.util.*;

/**
 * A SNAC queue manager which utilizes the "official" rate limiting algorithm to
 * avoid ever becoming rate-limited.
 */
public class RateLimitingQueueMgr extends AbstractSnacQueueMgr {
    public static final int ERRORMARGIN_DEFAULT = 100;

    private int errorMargin = ERRORMARGIN_DEFAULT;
    private boolean defaultAvoidLimiting = true;

    private Map conns = new IdentityHashMap();

    private QueueRunner runner = null;

    public final ConnectionQueueMgr getQueueMgr(SnacProcessor processor) {
        ConnectionQueueMgr rcs;
        synchronized(conns) {
            rcs = (ConnectionQueueMgr) conns.get(processor);
        }
        if (rcs == null) {
            throw new IllegalArgumentException("this rate manager is not " +
                    "currently attached to processor " + processor);
        }
        return rcs;
    }

    public synchronized final void setErrorMargin(int errorMargin) {
        DefensiveTools.checkRange(errorMargin, "errorMargin", 0);

        this.errorMargin = errorMargin;
    }

    public final synchronized int getErrorMargin() { return errorMargin; }

    public synchronized final boolean getDefaultAvoidLimiting() {
        return defaultAvoidLimiting;
    }

    public synchronized final void setDefaultAvoidLimiting(
            boolean defaultAvoidLimiting) {
        this.defaultAvoidLimiting = defaultAvoidLimiting;
    }


    final QueueRunner getRunner() { return runner; }


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

    protected void sendSnac(SnacProcessor processor, SnacRequest request) {
        // sigh
        super.sendSnac(processor, request);
    }
}

