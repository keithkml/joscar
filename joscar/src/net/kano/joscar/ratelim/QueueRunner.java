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

import net.kano.joscar.snac.SnacProcessor;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.DefensiveTools;

import java.util.*;

class QueueRunner implements Runnable {
    private boolean started = false;

    private final Object lock = new Object();

    private boolean updated = false;
    private Set queues = new HashSet();
    private RateLimitingQueueMgr queueMgr;

    public QueueRunner(RateLimitingQueueMgr queueMgr) {
        this.queueMgr = queueMgr;
    }

    private void forceStart() {
        started = true;

        // I think this is correct
        updated = true;

        Thread thread = new Thread(this, "SNAC queue manager");
        thread.start();
    }

    private synchronized void ensureStarted() {
        if (!started) forceStart();
    }

    public void run() {
        long minWait = 0;
        for (;;) {
            RateQueue[] queueArray;
            synchronized(lock) {
                if (!updated) {
                    // if we haven't been updated, we need to wait until a
                    // call to update() or until we need to send the next
                    // command (this time is specified in minWait, if non-
                    // zero)
                    try {
                        if (minWait == 0) lock.wait();
                        else lock.wait(minWait);
                    } catch (InterruptedException ignored) { }
                }

                // cache the list of queues so we don't need to lock while
                // iterating
                queueArray = (RateQueue[])
                        queues.toArray(new RateQueue[queues.size()]);

                // and set this flag back to off while we're in the lock
                updated = false;
            }

            // now we go through the queues to send any "ready" commands and
            // figure out when the next command should be sent (so we can
            // wait that long in the next iteration of the outer loop)
            minWait = 0;
            for (int i = 0; i < queueArray.length; i++) {
                RateQueue queue = queueArray[i];

                // a case for why the following line is safe outside a lock:
                // it is possible that the queue manager changes its rate-
                // limiting avoidance status between this line and the
                // synchronizatoin block, but it doesn't matter, because if
                // this happens, update() will be called, and this loop will
                // simply be immediately run again once this iteration is
                // over. I don't think there's any benefit to putting it in
                // the synchronized block and I feel like it might cause
                // deadlocks because I don't really know what's going on in
                // that rate manager as I am a simple queue thread.
                if (!queue.getParentMgr().isAvoidingLimiting()) continue;

                boolean finished;
                long wait = 0;
                List reqs = null;
                synchronized(queue) {
                    // if the queue is paused or there aren't any requests,
                    // we can skip it
                    if (queue.getParentMgr().isPaused()
                            || !queue.hasRequests()) {
                        continue;
                    }

                    // if there are one or more commands that can be sent
                    // right now, dequeue them
                    if (isReady(queue)) reqs = dequeueAll(queue);

                    // see whether the queue needs to be waited upon (if it
                    // doesn't have any queued commands, there's nothing to
                    // wait for -- we'll be notified with a call to
                    // update() if any are added)
                    finished = !queue.hasRequests();

                    // and, if necessary, compute how long we need to wait
                    // for this queue (the time until the next command can
                    // be sent)
                    if (!finished) wait = getWaitTime(queue);
                }

                // if there are any commands that were dequeued above, we
                // send them now, outside of the lock on the queue
                if (reqs != null && !reqs.isEmpty()) {
                    sendRequests(queue, reqs);
                }

                // and if there's nothing more to wait for, move on to the
                // next queue
                if (finished) continue;

                // we make sure wait isn't zero, because that would cause us
                // to wait forever, which we don't want to do unless we
                // explicitly decide to do so.
                if (wait < 1) wait = 1;

                // now change the minimum waiting time if necessary
                if (minWait == 0 || wait < minWait)  minWait = wait;
            }
        }
    }

    private long getWaitTime(RateQueue queue) {
        return queue.getRateMonitor().getOptimalWaitTime();
    }

    private boolean isReady(RateQueue queue) {
        return getWaitTime(queue) <= 0;
    }

    private List dequeueAll(RateQueue queue) {
        List reqs = null;
        synchronized(queue) {
            for (;;) {
                if (!queue.hasRequests() || !isReady(queue)) break;

                if (reqs == null) reqs = new ArrayList(5);
                reqs.add(queue.dequeue());
            }
        }

        return reqs;
    }

    private void sendRequests(RateQueue queue, List reqs) {
        SnacProcessor processor = queue.getParentMgr().getSnacProcessor();

        for (Iterator it = reqs.iterator(); it.hasNext();) {
            SnacRequest req = (SnacRequest) it.next();

            queueMgr.sendSnac(processor, req);
        }
    }

    public void update(ConnectionQueueMgr updated) {
        if (updated.isAvoidingLimiting()) {
            forceUpdate();
        } else {
            List queues = getQueuesCopy();

            for (Iterator it = queues.iterator(); it.hasNext();) {
                RateQueue queue = (RateQueue) it.next();

                if (queue.getParentMgr() == updated) {
                    flushQueue(queue);
                }
            }
        }
    }

    public void update(RateQueue updated) {
        if (updated.getParentMgr().isAvoidingLimiting()) {
            forceUpdate();
        } else {
            sendRequests(updated, dequeueAll(updated));
        }
    }

    public void update() {
        List queues = getQueuesCopy();

        boolean avoiding = false;
        for (Iterator it = queues.iterator(); it.hasNext();) {
            RateQueue queue = (RateQueue) it.next();

            if (queue.getParentMgr().isAvoidingLimiting()) {
                avoiding = true;
                break;
            }
        }

        if (avoiding) forceUpdate();
        else flushAllQueues();
    }

    private List getQueuesCopy() {
        synchronized(lock) {
            return new ArrayList(this.queues);
        }
    }

    private void forceUpdate() {
        ensureStarted();

        synchronized(lock) {
            updated = true;
            lock.notifyAll();
        }
    }

    private void flushAllQueues() {
        // we make a copy of the queue list since we'd need to make a copy
        // of something anyway (SNAC requests can't be sent inside a lock)
        // and there's no reason to not just copy the queue list itself
        List queues = getQueuesCopy();

        for (Iterator it = queues.iterator(); it.hasNext();) {
            RateQueue queue = (RateQueue) it.next();

            flushQueue(queue);
        }
    }

    private void flushQueue(RateQueue queue) {
        sendRequests(queue, dequeueAll(queue));
    }

    public void addQueue(RateQueue queue) {
        DefensiveTools.checkNull(queue, "queue");

        synchronized(lock) {
            queues.add(queue);
        }
        update(queue);
    }
}
