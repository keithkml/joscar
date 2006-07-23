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

import net.kano.joscar.CopyOnWriteArraySet;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.logging.Logger;
import net.kano.joscar.logging.LoggingSystem;
import net.kano.joscar.snac.ClientSnacProcessor;
import net.kano.joscar.snac.SnacRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * "Runs" a set of <code>RateQueue</code>s, dequeuing SNACs at appropriate
 * times. This class will create and destroy threads on its own based on
 * activity.
 * TODO: document QueueRunner thread management
 */
public final class QueueRunner {
    public static final long TIMEOUT_DEFAULT = 5*60*1000;

    private static final Logger logger
            = LoggingSystem.getLogger("net.kano.joscar.ratelim.QueueRunner");

    private long timeout = TIMEOUT_DEFAULT;

    /** A lock used in synchronizing updates with the running thread. */
    private final Object lock = new Object();

    /** Whether or not this queue has been updated. */
    private boolean updated = true;

    /** The list of queues to "run." */
    private final Set<RateQueue> queues = new CopyOnWriteArraySet<RateQueue>();

    private boolean stop = false;

    private boolean running = false;

    class QueueRunnerT extends Thread {
        public QueueRunnerT() {
            super("Queue Runner");
        }

        long  actualWait;

        public String getString() {
            return "Queue Runner@" + hashCode() + " actualWait: " + actualWait + " stop:" + stop + " running:" + running + " queue size:" + queues.size();
        }

        public void run() {
            synchronized(lock) {
                running = true;
                lock.notifyAll();
            }
            long minWait = 0;
            long lastActivity = -1;
            long current = System.currentTimeMillis();
            for (;;) {
                synchronized(lock) {
                    if (!updated) {
                        // if we haven't been updated, we need to wait until a
                        // call to update() or until we need to send the next
                        // command (this time is specified in minWait, if non-
                        // zero)
                        try {
                            if (minWait == 0) {
                                long sincelast;
                                if (lastActivity == -1) sincelast = 0;
                                else sincelast = current - lastActivity;

                                actualWait = Math.max(1, timeout - sincelast);
                                setName(getString());
                                lock.wait(actualWait);
                            } else {
                                actualWait = minWait;
                                setName(getString());
                                lock.wait(minWait);
                            }
                        } catch (InterruptedException nobigdeal) { }
                    }

                    // and set this flag back to off while we're in the lock
                    updated = false;

                    if (stop) {
                        if (logger.logFineEnabled()) {
                            logger.logFine("Stopping queue runner cycle due to "
                                    + "stopCurrentRun() call");
                        }
                        stop = false;
                        running = false;
                        break;
                    }

                    // now we see if we've done something useful in the past n
                    // seconds, where n is the user-specified timeout. if we
                    // haven't, we stop the thread. this must be done inside
                    // this lock or else it would be possible to miss queue
                    // updates.
                    current = System.currentTimeMillis();
                    if (lastActivity != -1 && minWait == 0) {
                        // determine how long ago it was since we did something
                        // useful
                        long since = current - lastActivity;
                        if (since > timeout) {
                            // we haven't been useful in too long; let's shut
                            // down
                            if (logger.logFineEnabled()) {
                                logger.logFine("Stopping queue runner cycle "
                                        + "due to inactivity: " + since + "ms");
                            }
                            running = false;
                            break;
                        }
                    }
                    lastActivity = current;
                }

                // now we go through the queues to send any "ready" commands and
                // figure out when the next command should be sent (so we can
                // wait that long in the next iteration of the outer loop)
                minWait = 0;

                if (queues.isEmpty()) continue;

                for (RateQueue queue : queues) {
                    boolean finished;
                    long wait = 0;
                    synchronized (queue) {
                        // if the queue is paused or there aren't any requests,
                        // we can skip it
                        if (queue.getParentMgr().isPaused()
                                || !queue.hasRequests()) {
                            continue;
                        }

                        // if there are one or more commands that can be sent
                        // right now, dequeue them
                        if (isReady(queue)) dequeueReady(queue);

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

                    // and if there's nothing more to wait for, move on to the
                    // next queue
                    if (finished) continue;

                    // we make sure wait isn't zero, because that would cause us
                    // to wait forever, which we don't want to do unless we
                    // explicitly decide to do so.
                    if (wait < 1) wait = 1;

                    // now change the minimum waiting time if necessary
                    if (minWait == 0 || minWait > wait) minWait = wait;
                }
            }
        }
    }

    /**
     * Ensures that queue runners cannot be instantiated from outside the
     * package.
     */
    QueueRunner() { }

    /**
     * Returns the "optimal wait time" for the given queue.
     *
     * @param queue a rate queue
     * @return the optimal wait time for the given queue
     *
     * @see RateClassMonitor#getOptimalWaitTime()
     */
    private long getWaitTime(RateQueue queue) {
        return queue.getRateClassMonitor().getOptimalWaitTime();
    }

    /**
     * Returns whether the given queue is "ready" to send the next request. (A
     * rate queue is "ready" if its {@linkplain #getWaitTime(RateQueue) wait
     * time} is zero.
     *
     * @param queue a rate queue
     * @return whether the given queue is "ready"
     */
    private boolean isReady(RateQueue queue) {
        return getWaitTime(queue) <= 0;
    }

    /**
     * Dequeues all "ready" requests in the given rate queue. This is only
     * executed from the QueueRunner thread.
     *
     * @param queue a rate queue
     */
    private void dequeueReady(RateQueue queue) {
        ConnectionQueueMgr connMgr = queue.getParentMgr();
        RateLimitingQueueMgr rateMgr = connMgr.getParentQueueMgr();

        List<SnacRequest> requests = new ArrayList<SnacRequest>();
        synchronized(queue) {
            while (queue.hasRequests() && isReady(queue)) {
                requests.add(queue.dequeue());
            }
        }
        ClientSnacProcessor processor = connMgr.getSnacProcessor();
        for (SnacRequest request : requests) {
            rateMgr.sendSnac(processor, request);
        }
    }

    /**
     * Tells the queue runner thread that the given connection queue manager has
     * been updated. (This indicates to the queue runner that it should
     * recalculate when to next send SNAC requests in queues under the given
     * queue manager.)
     *
     * @param updated the connection queue manager that has been updated
     */
    void update(ConnectionQueueMgr updated) {
        forceUpdate();
    }

    /**
     * Tells the queue runner that the given rate queue has been updated. (This
     * indicates to the queue runner that it should recalculate when to next
     * send SNAC requests in the given queue.)
     *
     * @param updated the rate queue that has been updated
     */
    void update(RateQueue updated) {
        forceUpdate();
    }

    /**
     * Tells the queue runner thread that a major change has taken place. This
     * indicates to the queue runner that it should recalculate when to next
     * send SNAC requests in all registered queues.
     */
    void update() {
        forceUpdate();
    }

    /**
     * Tells the queue runner to recalculate SNAC queue wait times.
     */
    private void forceUpdate() {
        synchronized(lock) {
            updated = true;
            updateLock();
        }
    }

    /**
     * This method must be called while holding a lock on {@link #lock}.
     * TODO: document updateLock
     */
    private void updateLock() {
        assert Thread.holdsLock(lock);

        checkThread();
        lock.notifyAll();
    }

    /**
     * This method must be called while holding a lock on {@link #lock}.
     * TODO: document checkThread
     */
    private void checkThread() {
        assert Thread.holdsLock(lock);

        if (running) return;
        if (queues.isEmpty()) return;

        if (logger.logFineEnabled()) {
            logger.logFine("Starting queue runner due to activity");
        }
        Thread thread = new QueueRunnerT();
        thread.setDaemon(true);
        thread.start();
        while (!running) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                // this is okay, we just might need to keep waiting
            }
        }
    }

    public boolean stopCurrentRun() {
        synchronized(lock) {
            if (!running) return false;

            // leave a message for the thread to stop and then wake it up
            stop = true;
            lock.notifyAll();

            return true;
        }
    }

    public long getTimeout() {
        synchronized(lock) {
            return timeout;
        }
    }

    public void setTimeout(long timeout) {
        synchronized(lock) {
            this.timeout = timeout;
            // we don't call updateLock because we don't want the thread to be
            // re-started no matter what
            lock.notifyAll();
        }
    }

    /**
     * Adds the given queue to this queue runner's queue list.
     *
     * @param queue the queue to add
     */
    void addQueue(RateQueue queue) {
        DefensiveTools.checkNull(queue, "queue");

        queues.add(queue);

        update(queue);
    }

    /**
     * Adds the given queues to this queue runner's queue list.
     *
     * @param rateQueues the queues to add
     */
    void addQueues(Collection<RateQueue> rateQueues) {
        // we need to copy these, because the elements may be set to null
        // between a null check and the addAll
        List<RateQueue> safeRateQueues =
                DefensiveTools.getSafeNonnullListCopy(
                        rateQueues, "rateQueues");
        queues.addAll(safeRateQueues);

        update();
    }

    /**
     * Removes the given queue, if present, from this queue runner's queue list.
     *
     * @param queue the queue to remove
     */
    void removeQueue(RateQueue queue) {
        DefensiveTools.checkNull(queue, "queue");

        queues.remove(queue);
    }

    /**
     * Removes the given queues, if present, from this queue runner's queue
     * list.
     *
     * @param rateQueues the queues to remove
     */
    void removeQueues(Collection<RateQueue> rateQueues) {
        DefensiveTools.checkNull(rateQueues, "rateQueues");

        queues.removeAll(rateQueues);
    }

    public String toString() {
        return "QueueRunner: queues=" + queues;
    }
}
