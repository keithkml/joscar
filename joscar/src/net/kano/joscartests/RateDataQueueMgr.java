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
 *  File created by keith @ Apr 14, 2003
 *
 */

package net.kano.joscartests;

import net.kano.joscar.snac.*;
import net.kano.joscar.snaccmd.conn.RateClassInfo;
import net.kano.joscar.flapcmd.SnacCommand;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class RateDataQueueMgr extends ImmediateSnacQueueManager {
    private Map classToQueue = new HashMap();
    private Map typeToQueue = new HashMap(500);

    public synchronized void queueSnac(SnacProcessor processor,
            SnacRequest request) {
        SnacCommand cmd = request.getCommand();
        CmdType type = new CmdType(cmd.getFamily(), cmd.getCommand());

        RateQueue queue = (RateQueue) typeToQueue.get(type);

        if (queue != null) queue.enqueue(request);

        super.queueSnac(processor, request);
    }

    public synchronized final void setRateClasses(RateClassInfo[] rateInfos) {
        for (int i = 0; i < rateInfos.length; i++) {
            setRateClass(rateInfos[i]);
        }
    }

    public synchronized final void setRateClass(RateClassInfo rateInfo) {
        Integer key = new Integer(rateInfo.getRateClass());
        RateQueue queue = (RateQueue) classToQueue.get(key);
        if (queue == null) {
            queue = new RateQueue(rateInfo);
            classToQueue.put(key, queue);
        } else {
            queue.setRateInfo(rateInfo);
        }

        CmdType[] cmds = rateInfo.getCommands();
        if (cmds != null) {
            for (int i = 0; i < cmds.length; i++) {
                typeToQueue.put(cmds[i], queue);
            }
        }
    }

    private static class RateQueue {
        private RateClassInfo rateInfo;

        private LinkedList timestamps = new LinkedList();

        public RateQueue(RateClassInfo rateInfo) {
            this.rateInfo = rateInfo;
        }

        public void enqueue(SnacRequest req) {
            timestamps.add(new Long(System.currentTimeMillis()));
        }

        public void setRateInfo(RateClassInfo rateInfo) {
            this.rateInfo = rateInfo;

            printDifferences();
        }

        private void printDifferences() {
            LinkedList diffs = new LinkedList();

            long last = 0;
            for (Iterator it = timestamps.iterator(); it.hasNext();) {
                long cur = ((Long) it.next()).longValue();
                if (last != 0) {
                    diffs.addFirst(new Long(cur - last));
                }
                last = cur;
            }

            System.out.println("last " + diffs.size() + " differences: "
                    + diffs);
        }
    }
}
