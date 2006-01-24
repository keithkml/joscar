package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;

/**
 * Created by IntelliJ IDEA.
 * User: klea
 * Date: Jan 13, 2006
 * Time: 2:09:52 PM
 * To change this template use File | Settings | File Templates.
 */
public interface RvConnection {
  RvConnectionManager getRvConnectionManager();

  Screenname getBuddyScreenname();

  boolean close();

  void addTransferListener(RvConnectionEventListener listener);
  void removeTransferListener(RvConnectionEventListener listener);

  EventPost getEventPost();

  RvConnectionSettings getSettings();
}
