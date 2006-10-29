package net.kano.joscar.ratelim;

import net.kano.joscar.snac.ClientSnacProcessor;

public interface ConnectionQueueMgr {
  /**
     * Returns whether the SNAC queue for the associated connection is currently
   * paused.
   *
   * @return whether the SNAC queue for the associated connection is currently
   *         paused
   *
   * @see ClientSnacProcessor#pause()
   * @see ClientSnacProcessor#unpause()
   */
  boolean isPaused();
}
