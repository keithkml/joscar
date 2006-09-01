package net.kano.joustsim.oscar.oscar.service.ssi;

import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.Service;
import org.jetbrains.annotations.Nullable;

public interface SsiService extends Service {
  void requestBuddyAuthorization(Screenname sn, @Nullable String msg);

  MutableBuddyList getBuddyList();

  PermissionList getPermissionList();

  ServerStoredSettings getServerStoredSettings();

  MyBuddyIconItemManager getBuddyIconItemManager();

  void addItemChangeListener(SsiItemChangeListener listener);

  void removeListener(SsiItemChangeListener listener);
}
