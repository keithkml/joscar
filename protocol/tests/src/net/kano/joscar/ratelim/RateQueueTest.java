/*
 * Copyright (c) 2006, The Joust Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 * - Neither the name of the Joust Project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * File created by klea
 */

package net.kano.joscar.ratelim;

import junit.framework.TestCase;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snaccmd.conn.RateClassInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class RateQueueTest extends TestCase {
  private RateQueue queue;
  private int sent;
  private int possible;


  protected void setUp() throws Exception {
    super.setUp();
    sent = 0;
    possible = 0;
    queue = new RateQueue(new NeverPausedConnectionQueueMgr(),
        new DummyRateClassMonitor(),
        new CountingAndImmediatelyDequeueingSender());
  }

  /**
   * Tests that when {@code sendAndDequeueReadyRequestsIfPossible} is called
   * multiple times before SNAC commands are actually sent, it does not send
   * more commands than it's supposed to send according to the rate class
   * monitor. This simulates a situation where multiple threads could call
   * {@code sendAndDequeueReadyRequestsIfPossible} simultaneously.
   * <br><br>
   * For more information on this behavior, see
   * {@link RateQueue#dequeueReadyRequests()} 
   * <br><br>
   * The behavior is simulated by calling
   * {@code sendAndDequeueReadyRequestsIfPossible} inside the
   * {@code SnacRequestSender}'s send method, before registering the commands as
   * sent (which would update the rate monitor).
   */
  public void testDequeuedRequestListWorks() {
    for (int i = 0; i < 8; i++) {
      queue.enqueue(new SnacRequest(new DummySnacCommand()));
    }
    possible = 2;
    queue.sendAndDequeueReadyRequestsIfPossible();
    assertEquals(2, sent);
    assertEquals(6, queue.getQueueSize());
    possible = 1;
    queue.sendAndDequeueReadyRequestsIfPossible();
    assertEquals(3, sent);
    assertEquals(5, queue.getQueueSize());
    possible = 6;
    queue.sendAndDequeueReadyRequestsIfPossible();
    assertEquals(8, sent);
    assertEquals(0, queue.getQueueSize());
    queue.sendAndDequeueReadyRequestsIfPossible();
    assertEquals(8, sent);
    assertEquals(0, queue.getQueueSize());
  }

  private class DummyRateClassMonitor implements RateClassMonitor {
    public int getPossibleCmdCount() {
      return possible;
    }

    // the rest of the methods throw UnsupportedOperationException

    public RateClassInfo getRateInfo() {
      throw new UnsupportedOperationException();
    }

    public int getErrorMargin() {
      throw new UnsupportedOperationException();
    }

    public int getLocalErrorMargin() {
      throw new UnsupportedOperationException();
    }

    public void setErrorMargin(int errorMargin) {
      throw new UnsupportedOperationException();
    }

    public boolean isLimited() {
      throw new UnsupportedOperationException();
    }

    public long getLastRateAvg() {
      throw new UnsupportedOperationException();
    }

    public long getPotentialAvg() {
      throw new UnsupportedOperationException();
    }

    public long getPotentialAvg(long time) {
      throw new UnsupportedOperationException();
    }

    public long getOptimalWaitTime() {
      throw new UnsupportedOperationException();
    }

    public long getTimeUntil(long minAvg) {
      throw new UnsupportedOperationException();
    }

    public int getMaxCmdCount() {
      throw new UnsupportedOperationException();
    }
  }

  private static class DummySnacCommand extends SnacCommand {
    public DummySnacCommand() {
      super(0, 0);
    }

    public void writeData(OutputStream out) throws IOException {
      }
  }

  private static class NeverPausedConnectionQueueMgr
      implements ConnectionQueueMgr {
    public boolean isPaused() {
      return false;
    }
  }

  private class CountingAndImmediatelyDequeueingSender
      implements SnacRequestSender {
    public void sendRequests(List<SnacRequest> toSend) {
      queue.sendAndDequeueReadyRequestsIfPossible();
      // we don't have access to the SnacRequest's listeners so we can't fire
      // actual "sent" events, so we fake it
      for (SnacRequest request : toSend) {
        queue.removePending(request);
        sent++;
      }
    }
  }
}
