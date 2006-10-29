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
import net.kano.joscar.ratelim.ConnectionQueueMgrImpl;
import net.kano.joscar.ratelim.RateQueue;
import net.kano.joscar.snac.CmdType;
import static net.kano.joscar.snaccmd.loc.LocCommand.CMD_NEW_GET_INFO;
import static net.kano.joscar.snaccmd.loc.LocCommand.FAMILY_LOC;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.info.InfoService;
import net.kano.joustsim.oscar.oscar.service.ssi.Buddy;
import net.kano.joustsim.oscar.oscar.service.ssi.BuddyList;
import net.kano.joustsim.oscar.oscar.service.ssi.BuddyListLayoutListener;
import net.kano.joustsim.oscar.oscar.service.ssi.Group;
import net.kano.joustsim.oscar.oscar.service.ssi.SsiService;
import net.kano.joustsim.trust.BuddyCertificateInfo;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BuddyInfoTracker {
  private static final Logger LOGGER = Logger
      .getLogger(BuddyInfoTracker.class.getName());

  public static final long DEFAULT_MINIMUM_TRACK_INTERVAL = 30 * 1000;

  private final AimConnection conn;
  private final BuddyInfoManager buddyInfoMgr;

  private Map<Screenname, TrackedBuddyInfo> trackers
      = new HashMap<Screenname, TrackedBuddyInfo>();
  private boolean initializedSsi = false;
  private Set<Screenname> buddies = new HashSet<Screenname>();
  private long minimumTrackInterval = DEFAULT_MINIMUM_TRACK_INTERVAL;
  private final Thread thread;
  private Comparator<TrackedBuddyInfo> lastCheckedComparator
      = new Comparator<TrackedBuddyInfo>() {
    public int compare(TrackedBuddyInfo o1,
        TrackedBuddyInfo o2) {
      if (o1.lastChecked < o2.lastChecked) return 1;
      if (o1.lastChecked > o2.lastChecked) return -1;
      return 0;
    }
  };

  public BuddyInfoTracker(AimConnection connection) {
    DefensiveTools.checkNull(connection, "connection");

    this.conn = connection;
    BuddyInfoManager buddyInfoMgr = connection.getBuddyInfoManager();
    this.buddyInfoMgr = buddyInfoMgr;
    connection.addOpenedServiceListener(new OpenedServiceListener() {
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
                handleBuddiesUpdated();
              }

              public void groupRemoved(BuddyList list, List<? extends Group> oldItems,
                  List<? extends Group> newItems, Group group) {
                handleBuddiesUpdated();
              }

              public void buddyAdded(BuddyList list, Group group,
                  List<? extends Buddy> oldItems, List<? extends Buddy> newItems,
                  Buddy buddy) {
                handleBuddiesUpdated();
              }

              public void buddyRemoved(BuddyList list, Group group,
                  List<? extends Buddy> oldItems, List<? extends Buddy> newItems,
                  Buddy buddy) {
                handleBuddiesUpdated();
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
    thread = new Thread(new TrackingThread(), "Buddy info tracker");
    thread.setDaemon(true);
    thread.start();
  }

  private class TrackedBuddyInfo {
    private final Screenname screenname;
    public volatile long lastChecked = 0;
    public final Set<BuddyInfoTrackerListener> trackers
        = new HashSet<BuddyInfoTrackerListener>();

    public TrackedBuddyInfo(Screenname screenname) {
      this.screenname = screenname;
    }

    public boolean addListener(BuddyInfoTrackerListener listener) {
      assert Thread.holdsLock(BuddyInfoTracker.this);
      return trackers.add(listener);
    }

    public boolean removeListener(BuddyInfoTrackerListener listener) {
      assert Thread.holdsLock(BuddyInfoTracker.this);
      return trackers.remove(listener);
    }

    public boolean hasListeners() {
      assert Thread.holdsLock(BuddyInfoTracker.this);
      return trackers.isEmpty();
    }
  }

  private synchronized void handleBuddiesUpdated() {
    SsiService ssi = conn.getSsiService();
    if (ssi != null) {
      Set<Screenname> old = new HashSet<Screenname>(buddies);
      buddies.clear();
      for (Group group : ssi.getBuddyList().getGroups()) {
        for (Buddy buddy : group.getBuddiesCopy()) {
          Screenname sn = buddy.getScreenname();
          buddyInfoMgr.getBuddyInfo(sn).setOnBuddyList(true);
          buddies.add(sn);
        }
      }
      old.removeAll(buddies);
      for (Screenname screenname : old) {
        buddyInfoMgr.getBuddyInfo(screenname).setOnBuddyList(false);
      }
    }
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
      TrackedBuddyInfo btrackers = trackers.get(buddy);
      if (btrackers == null) {
        btrackers = new TrackedBuddyInfo(buddy);
        trackers.put(buddy, btrackers);
        startTracking = true;
      }
      added = btrackers.addListener(listener);
    }

    //noinspection SimplifiableConditionalExpression
    assert startTracking ? added : true;

    if (startTracking) startTracking(buddy);
    thread.interrupt();

    return added;
  }

  public boolean removeTracker(Screenname buddy,
      BuddyInfoTrackerListener listener) {
    DefensiveTools.checkNull(buddy, "buddy");
    DefensiveTools.checkNull(listener, "listener");

    boolean stopTracking;
    synchronized (this) {
      TrackedBuddyInfo btrackers = trackers.get(buddy);
      if (btrackers == null) return false;

      boolean removed = btrackers.removeListener(listener);
      if (!removed) return false;

      // if there aren't any trackers left, we should remove the entry and
      // stop tracking
      stopTracking = btrackers.hasListeners();
      if (stopTracking) trackers.remove(buddy);
    }
    if (stopTracking) stopTracking(buddy);
    return true;
  }

  private void startTracking(Screenname buddy) {
    assert !Thread.holdsLock(this);
    assert isExplicitlyTracked(buddy);

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
    assert !isExplicitlyTracked(buddy);

    DefensiveTools.checkNull(buddy, "buddy");

    // there's nothing to do, since we don't register any listeners at the
    // buddy level
  }

  public synchronized boolean isTracked(Screenname sn) {
    if (sn.equals(conn.getScreenname())) return true;
    if (buddies.contains(sn)) return true;
    return isExplicitlyTracked(sn);
  }

  private synchronized boolean isExplicitlyTracked(Screenname sn) {
    return trackers.containsKey(sn);
  }

  public synchronized long getMinimumTrackInterval() {
    return minimumTrackInterval;
  }

  public synchronized void setMinimumTrackInterval(long minimumTrackInterval) {
    this.minimumTrackInterval = minimumTrackInterval;
  }

  private class TrackingThread implements Runnable {
    public void run() {
      while (conn.getState() != State.FAILED
          && conn.getState() != State.DISCONNECTED) {
        try {
          becool();
        } catch (Exception e) {
          LOGGER.log(Level.WARNING, "Error running buddy tracker", e);
        }
        try {
          Thread.sleep(10000);
        } catch (InterruptedException ignored) {
        }
      }
      LOGGER.fine("Shutting down buddy tracker thread for " + conn);
    }

    private void becool() {
      InfoService infoService = conn.getInfoService();
      if (infoService == null) return;

      OscarConnection oscar = infoService.getOscarConnection();
      ConnectionQueueMgrImpl queueMgr = oscar.getRateManager()
          .getQueueMgr(oscar.getSnacProcessor());
      RateQueue infoQueue = queueMgr
          .getRateQueue(new CmdType(FAMILY_LOC, CMD_NEW_GET_INFO));
      if (infoQueue == null) return;

      long now = System.currentTimeMillis();
      List<Screenname> request;
      synchronized (BuddyInfoTracker.this) {
        Set<TrackedBuddyInfo> want = new TreeSet<TrackedBuddyInfo>(
            lastCheckedComparator);
        for (TrackedBuddyInfo info : trackers.values()) {
          if (!buddyInfoMgr.getBuddyInfo(info.screenname).isOnBuddyList()) {
            if (now - info.lastChecked > minimumTrackInterval) {
              want.add(info);
            }
          }
        }
        request = new ArrayList<Screenname>();
        int possible = infoQueue.getRateClassMonitor().getPossibleCmdCount() - 1;
        int i = 0;
        for (TrackedBuddyInfo info : want) {
          if (i >= possible) break;
          LOGGER.fine("Requesting tracked buddy " + info.screenname
              + "'s awaymsg after " + (now - info.lastChecked / 1000)
              + "sec");

          info.lastChecked = now;
          request.add(info.screenname);
          i++;
        }
      }
      for (Screenname screenname : request) {
        infoService.requestAwayMessage(screenname);
      }
    }
  }
}
