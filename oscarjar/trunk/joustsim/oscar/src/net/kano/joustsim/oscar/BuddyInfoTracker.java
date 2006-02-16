/*
 *  Copyright (c) 2004, The Joust Project
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
 *  File created by keith @ Feb 9, 2004
 *
 */

package net.kano.joustsim.oscar;

import net.kano.joscar.DefensiveTools;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.info.InfoService;
import net.kano.joustsim.oscar.oscar.service.ssi.Buddy;
import net.kano.joustsim.oscar.oscar.service.ssi.BuddyList;
import net.kano.joustsim.oscar.oscar.service.ssi.BuddyListLayoutListener;
import net.kano.joustsim.oscar.oscar.service.ssi.Group;
import net.kano.joustsim.oscar.oscar.service.ssi.SsiService;
import net.kano.joustsim.trust.BuddyCertificateInfo;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BuddyInfoTracker {
  private final AimConnection conn;
  private final BuddyInfoManager buddyInfoMgr;

  private Map<Screenname, Set<BuddyInfoTrackerListener>> trackers
      = new HashMap<Screenname, Set<BuddyInfoTrackerListener>>();
  private boolean initializedSsi = false;
  private boolean buddiesUpdated = true;
  private Set<Screenname> buddies = new HashSet<Screenname>();

  public BuddyInfoTracker(AimConnection conn) {
    DefensiveTools.checkNull(conn, "conn");

    this.conn = conn;
    BuddyInfoManager buddyInfoMgr = conn.getBuddyInfoManager();
    this.buddyInfoMgr = buddyInfoMgr;
    conn.addOpenedServiceListener(new OpenedServiceListener() {
      public void openedServices(AimConnection conn,
          Collection<? extends Service> services) {
        if (!initializedSsi) {
          SsiService ssi = conn.getSsiService();
          if (ssi != null) {
            initializedSsi = true;
            ssi.getBuddyList().addLayoutListener(new BuddyListLayoutListener() {
              public void groupsReordered(BuddyList list, List<? extends Group> oldOrder,
                  List<? extends Group> newOrder) {
              }

              public void groupAdded(BuddyList list, List<? extends Group> oldItems,
                  List<? extends Group> newItems, Group group,
                  List<? extends Buddy> buddies) {
                setBuddiesUpdated();
              }

              public void groupRemoved(BuddyList list, List<? extends Group> oldItems,
                  List<? extends Group> newItems, Group group) {
                setBuddiesUpdated();
              }

              public void buddyAdded(BuddyList list, Group group,
                  List<? extends Buddy> oldItems, List<? extends Buddy> newItems,
                  Buddy buddy) {
                setBuddiesUpdated();
              }

              public void buddyRemoved(BuddyList list, Group group,
                  List<? extends Buddy> oldItems, List<? extends Buddy> newItems,
                  Buddy buddy) {
                setBuddiesUpdated();
              }

              public void buddiesReordered(BuddyList list, Group group,
                  List<? extends Buddy> oldBuddies, List<? extends Buddy> newBuddies) {
              }
            });
          }
        }
      }

      public void closedServices(AimConnection conn,
          Collection<? extends Service> services) {
      }
    });
    buddyInfoMgr.addGlobalBuddyInfoListener(new GlobalBuddyInfoListener() {
      public void newBuddyInfo(BuddyInfoManager manager, Screenname buddy,
          BuddyInfo info) {
      }

      public void buddyInfoChanged(BuddyInfoManager manager,
          Screenname buddy, BuddyInfo info,
          PropertyChangeEvent event) {
        if (!isTracked(buddy)) return;

        String prop = event.getPropertyName();
        if (prop.equals(BuddyInfo.PROP_CERTIFICATE_INFO)) {
          BuddyCertificateInfo certInfo
              = (BuddyCertificateInfo) event.getNewValue();
          if (certInfo != null && !certInfo.isUpToDate()) {
            InfoService infoService = getInfoService();
            if (infoService != null) {
              infoService.requestCertificateInfo(buddy);
            }
          }
        }
      }

      public void receivedStatusUpdate(BuddyInfoManager manager,
          Screenname buddy, BuddyInfo info) {
        if (!isTracked(buddy)) return;

        if (info.isAway()) {
          InfoService infoService = getInfoService();
          if (infoService != null) {
            infoService.requestAwayMessage(buddy);
          }
        }
      }
    });
  }

  private synchronized void setBuddiesUpdated() {
    buddiesUpdated = true;
  }

  private InfoService getInfoService() {
    return conn.getInfoService();
  }

  public boolean addTracker(Screenname buddy,
      BuddyInfoTrackerListener listener) {
    DefensiveTools.checkNull(buddy, "buddy");
    DefensiveTools.checkNull(listener, "listener");

    boolean startTracking = false;
    boolean added;
    synchronized (this) {
      Set<BuddyInfoTrackerListener> btrackers = trackers.get(buddy);
      if (btrackers == null) {
        btrackers = new HashSet<BuddyInfoTrackerListener>();
        trackers.put(buddy, btrackers);
        startTracking = true;
      }
      added = btrackers.add(listener);
    }

    //noinspection SimplifiableConditionalExpression
    assert startTracking ? added : true;

    if (startTracking) startTracking(buddy);

    return added;
  }

  public boolean removeTracker(Screenname buddy,
      BuddyInfoTrackerListener listener) {
    DefensiveTools.checkNull(buddy, "buddy");
    DefensiveTools.checkNull(listener, "listener");

    boolean stopTracking;
    synchronized (this) {
      Set<BuddyInfoTrackerListener> btrackers = trackers.get(buddy);
      if (btrackers == null) return false;

      boolean removed = btrackers.remove(listener);
      if (!removed) return false;

      // if there aren't any trackers left, we should remove the entry and
      // stop tracking
      stopTracking = btrackers.isEmpty();
      if (stopTracking) trackers.remove(buddy);
    }
    if (stopTracking) stopTracking(buddy);
    return true;
  }

  private void startTracking(Screenname buddy) {
    assert !Thread.holdsLock(this);
    synchronized (this) {
      assert trackers.containsKey(buddy);
    }

    DefensiveTools.checkNull(buddy, "buddy");

    BuddyInfo buddyInfo = buddyInfoMgr.getBuddyInfo(buddy);
    InfoService infoService = getInfoService();
    if (!buddyInfo.isCertificateInfoCurrent()) {
      infoService.requestCertificateInfo(buddy);
    }
    if (buddyInfo.isAway() && buddyInfo.getAwayMessage() == null) {
      infoService.requestAwayMessage(buddy);
    }
  }

  private void stopTracking(Screenname buddy) {
    assert !Thread.holdsLock(this);
    synchronized (this) {
      assert !trackers.containsKey(buddy);
    }

    DefensiveTools.checkNull(buddy, "buddy");

    // there's nothing to do, since we don't register any listeners at the
    // buddy level
  }

  public synchronized boolean isTracked(Screenname sn) {
    if (sn.equals(conn.getScreenname())) return true;
    if (buddiesUpdated) {
      SsiService ssi = conn.getSsiService();
      if (ssi != null) {
        buddies.clear();
        for (Group group : ssi.getBuddyList().getGroups()) {
          for (Buddy buddy : group.getBuddiesCopy()) {
            buddies.add(buddy.getScreenname());
          }
        }
        buddiesUpdated = false;
      }
    }
    if (buddies.contains(sn)) return true;
    return trackers.containsKey(sn);
  }
}
