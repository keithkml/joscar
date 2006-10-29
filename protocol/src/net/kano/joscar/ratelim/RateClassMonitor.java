package net.kano.joscar.ratelim;

import net.kano.joscar.snaccmd.conn.RateClassInfo;

public interface RateClassMonitor {
  /**
     * Returns the rate information associated with this monitor's associated
   * rate class.
   *
   * @return this monitor's associated rate class's rate information
   */
  RateClassInfo getRateInfo();

  /**
     * Returns this monitor's error margin. If this monitor's error margin is
   * set to <code>-1</code>, the error margin of this monitor's parent
   * <code>RateMonitor</code> will be returned.
   *
   * @return this monitor's error margin
   */
  int getErrorMargin();

  /**
     * Returns this monitor's locally set error margin. This value defaults to
   * <code>-1</code>, which indicates that the error margin should be
   * "inherited" from this rate class monitor's parent
   * <code>RateMonitor</code>.
   *
   * @return this monitor's locally set error margin, or <code>-1</code> if
   *         this monitor's error margin is currently inherited from its
   *         parent rate monitor
   */
  int getLocalErrorMargin();

  /**
     * Sets this monitor's error margin. Note that if the given margin is
   * <code>-1</code> this monitor's error margin will be "inherited" from this
   * monitor's parent <code>RateMonitor</code>.
   *
   * @param errorMargin an error margin value
   */
  void setErrorMargin(int errorMargin);

  /**
     * Returns whether this rate monitor's associated rate class is currently
   * rate-limited.
   *
   * @return whether this rate monitor's associated rate class is currently
   *         rate-limited
   */
  boolean isLimited();

  /**
     * Returns what the rate average was when the last command in the associated
   * rate class was sent.
   *
   * @return the rate average at the time of the last command send
   */
  long getLastRateAvg();

  /**
     * Returns what the rate average <i>would</i> be if a command were sent at
   * the current time.
   *
   * @return the potential rate average
   */
  long getPotentialAvg();

  /**
     * Returns what the rate average <i>would</i> be if a command were sent at
   * the given time.
   *
   * @param time the time at which a hypothetical command would be sent, in
   *        milliseconds since the unix epoch
   * @return the potential rate average
   */
  long getPotentialAvg(long time);

  /**
     * Returns how long one "should" wait before sending a command in this
   * monitor's associated rate class (to avoid being rate limited). This
   * algorithm attempts to stay above the rate limit (or the clear limit, if
   * {@linkplain #isLimited currently rate limited}) plus the {@linkplain
   * #getErrorMargin error margin}. Note that this method will never return
   * a value less than zero.
   *
   * @return how long one should wait before sending a command in the
   *         associated rate class
   */
  long getOptimalWaitTime();

  /**
     * Returns how long one must wait before sending a command in this monitor's
   * associated rate class to keep the current average above the given
   * average. This method ignores the {@linkplain #getErrorMargin error
   * margin}; it returns exactly how long one must wait for the rate average
   * to be equal to or above the given average. Note that this method will
   * never return a value less than zero.
   *
   * @param minAvg the "target" average
   * @return how long one must wait before sending a command to stay above the
   *         given average
   */
  long getTimeUntil(long minAvg);

  /**
     * Returns the number of commands in this rate class that <i>could</i> be
   * sent immediately, without being rate limited.
   *
   * @return the number of commands in this rate class that could be sent
   *         immediately without being rate limited
   */
  int getPossibleCmdCount();

  /**
     * Returns the <i>maximum</i> number of commands in this rate class that
   * could ever be sent at the same time without being rate-limited. This
   * method is similar to {@link #getPossibleCmdCount()} but differs in that
   * this method essentially returns the upper limit for the return value
   * of <code>getPossibleCmdCount()</code>: it returns the maximum number of
   * commands that could <i>ever</i> be sent at once; that is, what
   * <code>getPossibleCmdCount()</code> <i>would</i> return if the current
   * rate average were at its maximum.
   *
   * @return the maximum number of commands that could be sent in this rate
   *         class simultaneously without being rate limited
   */
  int getMaxCmdCount();
}
