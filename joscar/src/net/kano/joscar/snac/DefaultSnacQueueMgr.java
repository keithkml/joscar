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

package net.kano.joscar.snac;

import net.kano.joscar.snaccmd.conn.RateClassInfo;
import net.kano.joscar.DefensiveTools;

import java.util.*;

public class DefaultSnacQueueMgr extends AbstractSnacQueueManager {
    public static final int ERRORMARGIN_DEFAULT = 100;

    private int errorMargin = ERRORMARGIN_DEFAULT;

    private Map sets = new IdentityHashMap();

    private QueueRunner runner = new QueueRunner();
    { // initialization
        runner.start();
    }

    public final void setRateClasses(SnacProcessor processor,
            RateClassInfo[] rateInfos) {
        DefensiveTools.checkNull(processor, "processor");
        DefensiveTools.checkNull(rateInfos, "rateInfos");

        rateInfos = (RateClassInfo[]) rateInfos.clone();

        DefensiveTools.checkNullElements(rateInfos, "rateInfos");

        for (int i = 0; i < rateInfos.length; i++) {
            setRateClass(processor, rateInfos[i]);
        }
    }

    public final void setRateClass(SnacProcessor processor,
            RateClassInfo rateInfo) {
        DefensiveTools.checkNull(processor, "processor");
        DefensiveTools.checkNull(rateInfo, "rateInfo");

        getSet(processor).setRateClass(rateInfo);
    }

    public void setErrorMargin(int errorMargin) {
        this.errorMargin = errorMargin;
    }

    public int getErrorMargin() { return errorMargin; }

    private synchronized RateClassSet getSet(SnacProcessor processor) {
        RateClassSet set = (RateClassSet) sets.get(processor);

        if (set == null) {
            set = new RateClassSet(processor);
            sets.put(processor, set);
        }

        return set;
    }

    public void queueSnac(SnacProcessor processor, SnacRequest request) {
        getSet(processor).queueSnac(request);
    }

    public void clearQueue(SnacProcessor processor) {
        getSet(processor).clearQueue();
    }

    public void pause(SnacProcessor processor) {
        getSet(processor).pause();
    }

    public void unpause(SnacProcessor processor) {
        getSet(processor).unpause();
    }

    class RateClassSet {
        private final SnacProcessor snacProcessor;
        private Map classToQueue = new HashMap();
        private Map typeToQueue = new HashMap(500);
        private RateQueue defaultQueue = null;
        private boolean paused = false;

        public RateClassSet(SnacProcessor snacProcessor) {
            this.snacProcessor = snacProcessor;
        }

        public SnacProcessor getSnacProcessor() { return snacProcessor; }

        public void queueSnac(SnacRequest request) {
            CmdType type = CmdType.ofCmd(request.getCommand());
            RateQueue queue;
            synchronized(this) {
                queue = (RateQueue) typeToQueue.get(type);
            }

            if (queue == null) queue = defaultQueue;

            if (queue == null) {
                // so there's no queue. let's send it right out!
                sendSnac(snacProcessor, request);
                return;
            }

            queue.enqueue(request);
            runner.update();
        }

        public synchronized void setRateClass(RateClassInfo rateInfo) {
            Integer key = new Integer(rateInfo.getRateClass());
            RateQueue queue = (RateQueue) classToQueue.get(key);
            if (queue == null) {
                queue = new RateQueue(this, rateInfo);
                classToQueue.put(key, queue);
                runner.addQueue(queue);
            } else {
                queue.setRateInfo(rateInfo);
            }

            CmdType[] cmds = rateInfo.getCommands();
            if (cmds != null) {
                if (cmds.length == 0) {
                    if (defaultQueue == null) defaultQueue = queue;
                } else {
                    for (int i = 0; i < cmds.length; i++) {
                        typeToQueue.put(cmds[i], queue);
                    }
                }
            }
        }

        public synchronized void clearQueue() {
            for (Iterator it = this.classToQueue.values().iterator();
                 it.hasNext();) {
                RateQueue queue = (RateQueue) it.next();

                queue.clear();
            }
        }

        public synchronized void pause() {
            paused = true;
        }

        public synchronized void unpause() {
            paused = false;
            runner.update();
        }

        public synchronized boolean isPaused() { return paused; }
    }

    private class QueueRunner extends Thread {
        private final Object lock = new Object();

        private boolean updated = false;
        private Set queues = new HashSet(10);

        public QueueRunner() {
            super("SNAC queue manager");
        }

        public void run() {
            long minWait = 0;
            for (;;) {
                RateQueue[] queueArray;
                synchronized(lock) {
                    if (!updated) {
                        try {
                            System.out.println("waiting for "
                                    + (minWait == 0 ? "ever"
                                    : Long.toString(minWait)));

                            // note that if there's no specific time to wait
                            // for, minWait will be zero (see for loop below)
                            lock.wait(minWait);
                        } catch (InterruptedException ignored) { }
                    }

                    queueArray = (RateQueue[])
                            queues.toArray(new RateQueue[queues.size()]);
                    updated = false;
                }

                minWait = 0;
                long wait;
                for (int i = 0; i < queueArray.length; i++) {
                    RateQueue queue = queueArray[i];

                    boolean finished;
                    long origWait;
                    List reqs = null;
                    synchronized(queue) {
                        if (queue.getParentSet().isPaused()) continue;

                        if (!queue.hasRequests()) continue;

                        if (isReady(queue)) reqs = flush(queue);

                        finished = !queue.hasRequests();

                        origWait = getWaitTime(queue);
                    }

                    if (reqs != null && !reqs.isEmpty()) send(queue, reqs);

                    if (finished) continue;

                    System.out.println("origWait: " + origWait);
                    wait = Math.max(1, origWait);

                    if (minWait == 0 || wait < minWait) {
                        minWait = wait;
                    }
                }
            }
        }

        private long getWaitTime(RateQueue queue) {
            return queue.getWaitTime(queue.getRateInfo().getLimitedAvg()

                    + errorMargin);
        }

        private boolean isReady(RateQueue queue) {
            return getWaitTime(queue) <= 0;
        }

        private List flush(RateQueue queue) {
            List reqs = null;
            for (;;) {
                if (!queue.hasRequests() || !isReady(queue)) break;

                System.out.println("flushing Snac from rate queue..");

                if (reqs == null) reqs = new ArrayList(5);
                reqs.add(queue.dequeue());
            }

            return reqs;
        }

        private void send(RateQueue queue, List snacs) {
            SnacProcessor processor = queue.getParentSet().getSnacProcessor();

            for (Iterator it = snacs.iterator(); it.hasNext();) {
                SnacRequest req = (SnacRequest) it.next();

                sendSnac(processor, req);
            }
        }

        public void update() {
            synchronized(lock) {
                updated = true;
                lock.notifyAll();
            }
        }

        public void addQueue(RateQueue queue) {
            synchronized(lock) {
                queues.add(queue);
                updated = true;
                lock.notifyAll();
            }
        }
    }
}

class RateQueue {
    private final DefaultSnacQueueMgr.RateClassSet parentSet;

    private RateClassInfo rateInfo;
    private LinkedList queue = new LinkedList();
    private long last = -1;
    private long runningAvg;

    public RateQueue(DefaultSnacQueueMgr.RateClassSet parentSet, RateClassInfo rateInfo) {
        this.parentSet = parentSet;
        this.rateInfo = rateInfo;
        this.runningAvg = rateInfo.getMax();
    }

    public DefaultSnacQueueMgr.RateClassSet getParentSet() { return parentSet; }

    public synchronized RateClassInfo getRateInfo() { return rateInfo; }

    public synchronized void setRateInfo(RateClassInfo rateInfo) {
        System.out.println("I think the rate average is " + runningAvg);
        System.out.println("The server thinks that it's "
                + rateInfo.getCurrentAvg());

        this.rateInfo = rateInfo;
        runningAvg = rateInfo.getCurrentAvg();
    }

    public synchronized LinkedList getQueue() { return queue; }

    public synchronized long getRunningAvg() { return runningAvg; }

    public synchronized long getWaitTime() {
        return getWaitTime(rateInfo.getLimitedAvg());
    }

    public synchronized long getWaitTime(long minAvg) {
        if (last == -1) return 0;

        long winSize = rateInfo.getWindowSize();
        long sinceLast = System.currentTimeMillis() - last;
        long toWait = ((winSize * minAvg) - (runningAvg  * (winSize - 1)))
                - sinceLast;
        return Math.max(toWait, 0);
    }

    public synchronized void enqueue(SnacRequest req) {
        queue.add(req);
    }

    public synchronized boolean hasRequests() {
        return !queue.isEmpty();
    }

    public synchronized SnacRequest dequeue() {
        if (queue.isEmpty()) return null;

        long cur = System.currentTimeMillis();
        if (last != -1) {
            long diff = cur - last;
            long winSize = rateInfo.getWindowSize();
            runningAvg = Math.min(rateInfo.getMax(),
                    (runningAvg * (winSize - 1) + diff) / winSize);
        }
        last = cur;

        return (SnacRequest) queue.removeFirst();
    }

    public synchronized void clear() {
        queue.clear();
    }
}
