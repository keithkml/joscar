/*
 *  Copyright (c) 2006, The Joust Project
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
 */

package net.kano.joscar.ratelim;

import junit.framework.TestCase;

public class QueueRunnerTest extends TestCase {
    public void testDequeueOnce() {
        assertDequeued(1);
    }

    public void testDequeueMultiple() {
        assertDequeued(100);
    }

    public void testTimeout() throws InterruptedException {
        QueueRunner<MockEventQueue> runner = createAndClearQueue();
        assertTimesOut(runner);
    }

    public void testRestartAfterTimeout() throws InterruptedException {
        QueueRunner<MockEventQueue> runner = createAndClearQueue();
        assertTimesOut(runner);
        runner.setTimeout(50000);
        runner.getQueue().addEvents(1);
        assertTrue(runner.isRunning());
    }

    public void testTimesOutImmediately() throws InterruptedException {
        QueueRunner<MockEventQueue> runner
                = QueueRunner.create(new MockEventQueue(0));
        assertTimesOut(runner);
    }

    private void assertTimesOut(QueueRunner<MockEventQueue> runner)
            throws InterruptedException {
        assertTrue(runner.isRunning());
        forceTimeout(runner);
        assertFalse(runner.isRunning());
    }

    private QueueRunner<MockEventQueue> createAndClearQueue() {
        MockEventQueue queue = new MockEventQueue(1);
        QueueRunner<MockEventQueue> runner = QueueRunner.create(queue);
        runner.setTimeout(50000);
        queue.waitForEmpty(5000);
        return runner;
    }

    private void forceTimeout(QueueRunner<MockEventQueue> runner)
            throws InterruptedException {
        runner.setTimeout(0);
        // sleep for a maximum of 5 minutes
        for (int i = 0; i < 500 && runner.isRunning(); i++) {
            Thread.sleep(10);
        }
    }

    private void assertDequeued(int events) {
        MockEventQueue queue = new MockEventQueue(events);
        QueueRunner.create(queue);
        assertTrue(queue.waitForEmpty(5000));
    }

    private static class MockEventQueue extends AbstractFutureEventQueue {
        private final Object lock = new Object();
        private int toFlush;

        public MockEventQueue(int events) {
            toFlush = events;
        }

        public long flushQueues() {
            synchronized (lock) {
                if (toFlush > 0) {
                    toFlush--;
                }
                boolean empty = isEmpty();
                if (empty) {
                    lock.notifyAll();
                    return -1;
                } else {
                    return 1;
                }
            }
        }

        private synchronized boolean isEmpty() {
            return toFlush == 0;
        }

        public boolean waitForEmpty(long time) {
            synchronized (lock) {
                try {
                    lock.wait(time);
                } catch (InterruptedException e) {
                    // we shouldn't be interrupted
                    throw new IllegalStateException(e);
                }
                return isEmpty();
            }
        }

        public boolean hasQueues() {
            return true;
        }

        public void addEvents(int events) {
            synchronized (this) {
                toFlush += events;
            }
            updateQueueRunners();
        }
    }
}
