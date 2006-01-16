package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joscar.rvcmd.InvitationMessage;

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

  InvitationMessage getInvitationMessage();

  boolean cancel();

  void addTransferListener(RvConnectionEventListener listener);

  void removeTransferListener(RvConnectionEventListener listener);

  EventPost getEventPost();

  boolean isProxyRequestTrusted();

  void setProxyRequestTrusted(boolean trusted);

  boolean isOnlyUsingProxy();

  void setOnlyUsingProxy(boolean onlyUsingProxy);

  void setDefaultPerConnectionTimeout(long millis);

  void setPerConnectionTimeout(ConnectionType type, long millis);

  long getDefaultPerConnectionTimeout();

  long getPerConnectionTimeout(ConnectionType type);
}
