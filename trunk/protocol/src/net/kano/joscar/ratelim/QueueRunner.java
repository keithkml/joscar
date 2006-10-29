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
import net.kano.joscar.logging.Logger;
import net.kano.joscar.logging.LoggingSystem;
import org.jetbrains.annotations.Nullable;

/**
 * "Runs" a set of <code>RateQueue</code>s, dequeuing SNACs at appropriate
 * times. This class will create and destroy threads on its own based on
 * activity.
 */
@SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized"})
public final class QueueRunner<Q extends FutureEventQueue> {
    public static final long TIMEOUT_DEFAULT = 3*60*1000;

    private static final Logger logger
            = LoggingSystem.getLogger("net.kano.joscar.ratelim.QueueRunner");


    public static <Q extends FutureEventQueue> QueueRunner<Q> create(Q queue) {
        return new QueueRunner<Q>(queue);
    }

    private final Object lock = new Object();

    private final Q queue;

    private long timeout = TIMEOUT_DEFAULT;

    private boolean shouldCheckQueues = true;
    private boolean shouldStop = false;
    private @Nullable QueueRunnerThread thread = null;

    private QueueRunner(Q queue) {
        DefensiveTools.checkNull(queue, "queue");
        this.queue = queue;
        queue.registerQueueRunner(this);
        update();
    }

    public Q getQueue() { return queue; }

    public long getTimeout() {
        synchronized(lock) {
            return timeout;
        }
    }

    public void setTimeout(long timeout) {
        synchronized(lock) {
            this.timeout = timeout;
            lock.notifyAll();
        }
    }

    /**
     * Indicates that a major change has taken place. This
     * indicates to the queue runner that it should recalculate when to next
     * send SNAC requests in all registered queues.
     */
    public void update() {
        synchronized(lock) {
            shouldCheckQueues = true;
            startThreadIfNecessary();
            lock.notifyAll();
        }
    }

    /**
     * Ensures that a thread is running to process the queues, if necessary.
     * This method must be called while holding a lock on {@link #lock}.
     */
    private void startThreadIfNecessary() {
        assert Thread.holdsLock(lock);

        if ((thread != null && thread.running) || !queue.hasQueues()) {
            return;
        }

        if (logger.logFineEnabled()) {
            logger.logFine("Starting queue runner due to activity");
        }
        startThread();
    }

    private void startThread() {
        QueueRunnerThread thread = new QueueRunnerThread();
        thread.setDaemon(true);
        thread.start();
        while (!thread.running) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                // this is okay, we just might need to keep waiting
            }
        }
        this.thread = thread;
    }

    public boolean stopCurrentRun() {
        synchronized(lock) {
            if (thread == null || !thread.running) return false;

            // leave a message for the thread to stop and then wake it up
            shouldStop = true;
            lock.notifyAll();

            return true;
        }
    }

    protected void waitForLock(long wait) {
        assert wait != 0;
        try {
            lock.wait(wait);
        } catch (InterruptedException nobigdeal) {
            // this is okay, there's no harm in getting out early
        }
    }

    public String toString() {
        return "QueueRunner: queue=" + queue;
    }

    public boolean isRunning() {
        return thread != null && thread.running;
    }

    private class QueueRunnerThread extends Thread {
        private volatile boolean running = false;

        public QueueRunnerThread() {
            super("Queue Runner");
        }

        public void run() {
            try {
                setRunning(true);

                long nextDequeueTime = -1;
                long lastActivity = System.currentTimeMillis();
                for (;;) {
                    synchronized(lock) {
                        waitForUpdate(nextDequeueTime, lastActivity);

                        if (shouldStop) {
                            if (logger.logFineEnabled()) {
                                logger.logFine("Stopping queue runner due to "
                                        + "stopCurrentRun() call; queue is " + queue.toString());
                            }
                            shouldStop = false;
                            break;
                        }

                        if (nextDequeueTime == -1) {
                            if (shouldStopDueToInactivity(lastActivity)) {
                                if (logger.logFineEnabled()) {
                                    logger.logFine("Stopping queue runner due to "
                                            + "inactivity: "
                                            + (System.currentTimeMillis()
                                            - lastActivity) + "ms; queue is " + queue.toString());
                                }
                                break;
                            }
                        } else {
                            lastActivity = System.currentTimeMillis();
                        }
                    }

                    nextDequeueTime = queue.flushQueues();
                }
            } finally {
                setRunning(false);
            }
        }

        private void setRunning(boolean running) {
            synchronized(lock) {
                this.running = running;
                lock.notifyAll();
            }
        }

        private boolean shouldStopDueToInactivity(long lastActivity) {
            assert Thread.holdsLock(lock);

            if (lastActivity == -1) return false;

            long inactiveTime = System.currentTimeMillis() - lastActivity;
            return inactiveTime > timeout;

        }

        private void waitForUpdate(long minWait, long lastActivity) {
            assert Thread.holdsLock(lock);

            if (shouldCheckQueues) {
                // we were updated.
                shouldCheckQueues = false;
                return;
            }

            long wait;
            if (minWait == -1) {
                wait = computeWaitTime(lastActivity);
            } else {
                wait = minWait;
            }
            wait = Math.max(1, Math.min(timeout, wait));
            setName(makeStatusString(wait));
            waitForLock(wait);

            // it doesn't matter if update was called while we were waiting,
            // because we're going to check the queues anyway
            shouldCheckQueues = false;
        }

        private long computeWaitTime(long lastActivity) {
            assert Thread.holdsLock(lock);

            long sincelast;
            if (lastActivity == -1) {
                sincelast = 0;
            } else {
                sincelast = System.currentTimeMillis() - lastActivity;
            }

            return Math.max(1, timeout - sincelast);
        }

        private String makeStatusString(long wait) {
            assert Thread.holdsLock(lock);

            return "Queue Runner@" + hashCode() + " currentWait: " + wait
                    + " shouldStop:" + shouldStop + " running:" + running;
        }
    }
}
