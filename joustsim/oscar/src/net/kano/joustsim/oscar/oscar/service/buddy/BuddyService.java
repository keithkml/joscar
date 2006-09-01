package net.kano.joustsim.oscar.oscar.service.buddy;

import net.kano.joustsim.oscar.oscar.service.Service;

public interface BuddyService extends Service {
  void addBuddyListener(BuddyServiceListener l);

  void removeBuddyListener(BuddyServiceListener l);
}
