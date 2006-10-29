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
import net.kano.joscar.snaccmd.conn.RateChange;
import net.kano.joscar.snaccmd.conn.RateClassInfo;

import java.util.Arrays;

/**
 * Tests {@link RateClassMonitorImpl}
 */
public class RateClassMonitorImplTest extends TestCase {
  private static final int MAX = 6000;
  private static final int LIMITED = 3000;

  private long time;

  protected void setUp() throws Exception {
    time = 20;
  }

  /**
   * Tests that the rate average is computed correctly.
   * <ol>
   * <li>Since the {@code lastTime} was 100ms ago (time -80), and the initial
   * average is 5000, the first average is simply 5000.</li>
   * <li>A message is sent at time 20, so now the rate should be
   * {@code ((5000 * 4) + 100) / 5 = 4020}</li>
   * <li>Another message is sent at time 20, so now the rate should be
   * {@code ((4020 * 4) + 0) / 5 = 3236}</li>
   * <li>Another message is sent at time 20, so now the rate should be
   * {@code ((3236 * 4) + 0) / 5 = 2608}</li>
   * </ol>
   */
  public void testRateAverage() {
    RateClassMonitorImpl monitor = createRateClassMonitor(5000);
    for (int rate : Arrays.asList(5000, 4020, 3216, 2572)) {
      assertEquals(rate, monitor.getLastRateAvg());
      monitor.updateRate(time);
    }
  }

  public void testOptimalWaitTimeWhenNotLimited() {
    for (int startingAvg = MAX; startingAvg > LIMITED; startingAvg--) {
      assertWaitTimeCorrectAndOptimal(startingAvg);
    }
  }

  public void testOptimalWaitTimeWhenLimited() {
    for (int startingAvg = LIMITED; startingAvg >= 0; startingAvg--) {
      assertWaitTimeCorrectAndOptimal(startingAvg);
    }
  }

  /**
   * Asserts that {@code getOptimalWaitTime} returns a time which is both
   * <em>correct</em> and <em>optimal</em>. A <em>correct</em> wait time does
   * not result in being rate limited. An <em>optimal</em> wait time is a time
   * such that waiting 10ms less would result in being rate limited.
   */
  private void assertWaitTimeCorrectAndOptimal(int startingAvg) {
    RateClassMonitor monitor = createRateClassMonitor(startingAvg);
    long optimalWaitTime = monitor.getOptimalWaitTime();

    // assert that waiting the "optimal" amount of time would not result in rate
    // limiting
    long optimalTime = time + optimalWaitTime;
    assertPotentialAverageCorrectness(monitor, optimalTime, true);

    // assert that waiting 10ms longer would also not result in rate limiting
    assertPotentialAverageCorrectness(monitor, optimalTime + 10, true);

    if (optimalWaitTime > 10) {
      // assert that waiting 10ms less *would* result in rate limiting - if not,
      // the wait time must not be optimal
      assertPotentialAverageCorrectness(monitor, optimalTime - 10, false);
    }
  }

  private void assertPotentialAverageCorrectness(RateClassMonitor monitor,
                                                 long time, boolean correct) {
    long calculated = monitor.getPotentialAvg(time);
    RateClassInfo rateInfo = monitor.getRateInfo();
    String notStr = correct ? "" : "not ";
    String prefix = "For starting avg. " + monitor.getLastRateAvg()
        + ", average was " + calculated + ", should " + notStr + "be > ";
    if (monitor.getLastRateAvg() < rateInfo.getLimitedAvg()) {
      long clearAvg = rateInfo.getClearAvg();
      assertEquals(prefix + clearAvg,
          correct, calculated >= clearAvg);
    } else {
      long limitedAvg = rateInfo.getLimitedAvg();
      assertEquals(prefix + limitedAvg,
          correct, calculated >= limitedAvg);
    }
  }

  public void testPossibleCmdCountIsOptimal() {
    for (int startingAvg = MAX; startingAvg > LIMITED; startingAvg--) {
      RateClassMonitorImpl monitor = createRateClassMonitor(startingAvg);
      int possible = monitor.getPossibleCmdCount();
      updateRate(monitor, possible);
      long average = monitor.getLastRateAvg();
      long limited = monitor.getRateInfo().getLimitedAvg();
      assertTrue("For starting average " + startingAvg + ", sending "
          + possible + " cmds resulted in rate of " + average,
          average > limited);
      monitor.updateRate(time);
      average = monitor.getLastRateAvg();
      assertTrue("For starting average " + startingAvg + ", sending an extra "
          + "cmd after " + possible + " cmds resulted in rate of " + average,
          average <= limited);
    }
  }

  private void updateRate(RateClassMonitorImpl monitor, int numberOfCommands) {
    for (int i = 0; i < numberOfCommands; i++) {
      monitor.updateRate(time);
    }
  }

  public void testPossibleCmdCountReturnsZeroWhenLimited() {
    for (int startingAvg = LIMITED; startingAvg >= 0; startingAvg--) {
      RateClassMonitor monitor = createRateClassMonitor(startingAvg);
      assertEquals(0, monitor.getPossibleCmdCount());
    }
  }

  public void testLimitedViaRateChange() {
    RateClassMonitorImpl monitor = createRateClassMonitor(LIMITED + 10);
    assertFalse(monitor.isLimited());
    monitor.updateRateInfo(RateChange.CODE_LIMITED,
        createRateClassInfo(LIMITED - 10, RateChange.CODE_LIMITED));
    assertTrue(monitor.isLimited());
    monitor.updateRateInfo(RateChange.CODE_LIMIT_CLEARED,
        createRateClassInfo(LIMITED - 10, RateChange.CODE_LIMIT_CLEARED));
    assertFalse(monitor.isLimited());
  }

  public void testLimitedImmediately() {
    RateClassMonitorImpl monitor = createRateClassMonitor(LIMITED - 10,
        RateChange.CODE_LIMITED);
    assertTrue(monitor.isLimited());
    monitor.updateRateInfo(RateChange.CODE_LIMIT_CLEARED,
        createRateClassInfo(LIMITED + 10, RateChange.CODE_LIMIT_CLEARED));
    assertFalse(monitor.isLimited());
  }

  /**
   * Tests that isLimited() correctly returns false when the calculated rate
   * indicates that we are no longer limited, even if the server hasn't sent a
   * "limit cleared" event (yet).
   */
  public void testNoLongerLimitedGuess() {
    RateClassMonitor monitor = createRateClassMonitor(LIMITED - 100,
        RateChange.CODE_LIMITED);
    assertTrue(monitor.isLimited());
    time += monitor.getOptimalWaitTime();
    assertFalse(monitor.isLimited());
  }

  private RateClassMonitorImpl createRateClassMonitor(int currentAvg) {
    return createRateClassMonitor(currentAvg,
        currentAvg < LIMITED ? RateChange.CODE_LIMITED : -1);
  }

  private RateClassMonitorImpl createRateClassMonitor(
      int currentAvg, int currentState) {
    RateClassMonitorImpl monitor = new RateClassMonitorImpl(null,
        createRateClassInfo(currentAvg, currentState),
        new MyRateClassListener(), new MyTimeProvider());
    monitor.setErrorMargin(0);
    return monitor;
  }

  private RateClassInfo createRateClassInfo(int currentAvg, int currentState) {
    return new RateClassInfo(1, 5, 4100, 4000, LIMITED, 2000,
        currentAvg, MAX, 100, currentState);
  }

  private class MyRateClassListener implements RateClassListener {
    public void handleLimitedEvent(RateClassMonitor monitor,
                                   boolean limited) {
    }
  }

  private class MyTimeProvider implements TimeProvider {
    public long getCurrentTime() {
      return time;
    }
  }
}
