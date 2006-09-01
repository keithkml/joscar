package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: klea
 * Date: Jan 13, 2006
 * Time: 2:09:52 PM
 * To change this template use File | Settings | File Templates.
 */
public interface RvConnection {
  Screenname getBuddyScreenname();

  boolean close();

  void close(RvConnectionEvent error);

  void addEventListener(RvConnectionEventListener listener);

  void removeEventListener(RvConnectionEventListener listener);

  EventPost getEventPost();

  RvConnectionSettings getSettings();

  Screenname getMyScreenname();

  RvSessionConnectionInfo getRvSessionInfo();

  TimeoutHandler getTimeoutHandler();

  RvConnectionState getState();

  boolean isOpen();
}
