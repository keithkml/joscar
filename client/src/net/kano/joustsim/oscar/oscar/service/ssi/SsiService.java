package net.kano.joustsim.oscar.oscar.service.ssi;

import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.Service;
import org.jetbrains.annotations.Nullable;

public interface SsiService extends Service {

  void addBuddyAuthorizationListener(BuddyAuthorizationListener listener);
  void removeBuddyAuthorizationListener(BuddyAuthorizationListener listener);

  void requestBuddyAuthorization(Screenname sn, @Nullable String msg);
  void replyBuddyAuthorization(Screenname sn, boolean accept, @Nullable String msg);
  void sendFutureBuddyAuthorization(Screenname sn, @Nullable String msg);

  MutableBuddyList getBuddyList();

  PermissionList getPermissionList();

  ServerStoredSettings getServerStoredSettings();

  MyBuddyIconItemManager getBuddyIconItemManager();

  void addItemChangeListener(SsiItemChangeListener listener);

  void removeListener(SsiItemChangeListener listener);
}
