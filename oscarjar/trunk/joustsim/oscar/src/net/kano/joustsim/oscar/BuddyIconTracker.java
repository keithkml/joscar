/*
 *  Copyright (c) 2005, The Joust Project
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
 */

package net.kano.joustsim.oscar;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.icon.IconRequestListener;
import net.kano.joustsim.oscar.oscar.service.icon.IconService;
import net.kano.joustsim.oscar.oscar.service.icon.IconServiceArbiter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeEvent;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class BuddyIconTracker {
  private static final Logger LOGGER = Logger
      .getLogger(BuddyIconTracker.class.getName());

  private static final long RE_REQUEST_INTERVAL = 60 * 1000;

  private final AimConnection conn;
  private final Map<BuddyIconRequest,Long> pendingRequests
      = new HashMap<BuddyIconRequest, Long>();
  private final Map<ExtraInfoData, ByteBlock> iconCache
      = new HashMap<ExtraInfoData, ByteBlock>();

  private final IconRequestListener iconRequestListener
      = new MyIconRequestListener();
  private final Timer timer = new Timer(true);

  private boolean enabled = true;

  public BuddyIconTracker(AimConnection aconn) {
    this.conn = aconn;
    BuddyInfoManager mgr = conn.getBuddyInfoManager();
    mgr.addGlobalBuddyInfoListener(new MyGlobalBuddyInfoListener());
    timer.schedule(new RerequestIconsTask(), 5000, 5000);
  }

  public synchronized boolean isEnabled() {
    return enabled;
  }

  public synchronized void setEnabled(boolean enabled) {
    this.enabled = enabled;
    if (!enabled) iconCache.clear();
  }

  private synchronized void clearRequest(ExtraInfoData block, Screenname buddy) {
    pendingRequests.remove(new BuddyIconRequest(buddy, block));
  }

  private synchronized void updateRequestTime(ExtraInfoData block, Screenname buddy) {
    pendingRequests.put(new BuddyIconRequest(buddy, block),
        System.currentTimeMillis());
  }

  public synchronized long getRequestTime(ExtraInfoData block, Screenname buddy) {
    Long time = pendingRequests.get(new BuddyIconRequest(buddy, block));
    return time == null ? 0 : time;
  }

  public @Nullable synchronized ByteBlock getIconDataForHash(
      ExtraInfoData hash) {
    return iconCache.get(hash);
  }

  public @Nullable ByteBlock getBuddyIconData(Screenname screenname) {
    BuddyInfo buddyInfo = conn.getBuddyInfoManager().getBuddyInfo(screenname);

    ExtraInfoData hash = buddyInfo.getIconHash();
    if (hash == null) return null;

    return getIconDataForHash(hash);
  }

  public ExtraInfoData addToCache(Screenname buddy, ByteBlock iconData) {
    DefensiveTools.checkNull(iconData, "iconData");

    ExtraInfoData iconInfo = new ExtraInfoData(
        ExtraInfoData.FLAG_HASH_PRESENT, computeIconHash(iconData));
    storeInCache(iconInfo, buddy, iconData);
    return iconInfo;
  }

  private synchronized void storeInCache(ExtraInfoData hash, Screenname buddy,
      @NotNull ByteBlock iconData) {
    LOGGER.fine("Cached icon data for " + hash);
    clearRequest(hash, buddy);
    iconCache.put(hash, ByteBlock.wrap(iconData.toByteArray()));
  }

  private static ByteBlock computeIconHash(ByteBlock iconData) {
    ByteBlock hash;
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      hash = ByteBlock.wrap(digest.digest(iconData.toByteArray()));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
    return hash;
  }

  private void requestIcon(ExtraInfoData newHash, Screenname buddy) {
    IconServiceArbiter iconArbiter =
        conn.getExternalServiceManager().getIconServiceArbiter();
    if (iconArbiter != null) {
      if (buddy != null) {
        LOGGER.info("Requesting buddy icon for " + buddy);
      }
      iconArbiter.addIconRequestListener(iconRequestListener);
      updateRequestTime(newHash, buddy);
      iconArbiter.requestIcon(buddy, newHash);
    } else {
      LOGGER.warning("icon arbiter is null!");
    }
  }

  private void storeBuddyIconData(Screenname buddy, ExtraInfoData iconInfo,
      ByteBlock iconData) {
    BuddyInfo buddyInfo = conn.getBuddyInfoManager().getBuddyInfo(buddy);
    buddyInfo.setIconDataIfHashMatches(iconInfo, iconData);
  }

  private static class BuddyIconRequest {
    private final Screenname screenname;
    private final ExtraInfoData data;

    public BuddyIconRequest(Screenname screenname, ExtraInfoData data) {
      this.screenname = screenname;
      this.data = data;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      BuddyIconRequest that = (BuddyIconRequest) o;

      return data.equals(that.data) && screenname.equals(that.screenname);
    }

    public int hashCode() {
      return 31 * screenname.hashCode() + data.hashCode();
    }
  }

  private class MyGlobalBuddyInfoListener implements GlobalBuddyInfoListener {
    public void newBuddyInfo(BuddyInfoManager manager, Screenname buddy,
        BuddyInfo info) {
      if (!isEnabled()) return;
      handleNewIconHashForBuddy(buddy, info.getIconHash());
    }

    public void buddyInfoChanged(BuddyInfoManager manager,
        Screenname buddy, BuddyInfo info,
        PropertyChangeEvent event) {
      if (!isEnabled()) return;

      if (event.getPropertyName().equals(BuddyInfo.PROP_ICON_HASH)) {
        ExtraInfoData newHash = (ExtraInfoData) event.getNewValue();
        handleNewIconHashForBuddy(buddy, newHash);
      }
    }

    private void handleNewIconHashForBuddy(Screenname buddy,
        ExtraInfoData newHash) {
      LOGGER.fine("Got new icon hash for " + buddy + ": " + newHash);

      if (newHash == null) {
        storeBuddyIconData(buddy, newHash, null);

      } else {
        ByteBlock iconData = getIconDataForHash(newHash);
        if (iconData == null) {
          requestIcon(newHash, buddy);

        } else {
          LOGGER.finer("Icon data was already cached for " + buddy);
          storeBuddyIconData(buddy, newHash, iconData);
        }

      }
    }

    public void receivedStatusUpdate(BuddyInfoManager manager,
        Screenname buddy, BuddyInfo info) {
    }
  }

  private class RerequestIconsTask extends TimerTask {
    public void run() {
      List<BuddyIconRequest> rereq = new ArrayList<BuddyIconRequest>();
      synchronized (BuddyIconTracker.this) {
        for (Map.Entry<BuddyIconRequest,Long> entry : pendingRequests.entrySet()) {
          if (System.currentTimeMillis() - entry.getValue() > RE_REQUEST_INTERVAL) {
            rereq.add(entry.getKey());
          }
        }
        for (BuddyIconRequest request : rereq) {
          pendingRequests.put(request, System.currentTimeMillis());
        }
      }
      for (BuddyIconRequest request : rereq) {
        LOGGER.fine("Re-requesting buddy icon for " + request.screenname
            + " (" + request.data + ")");
        requestIcon(request.data, request.screenname);
      }
    }
  }

  private class MyIconRequestListener implements IconRequestListener {
    public void buddyIconCleared(IconService service,
        Screenname screenname, ExtraInfoData data) {
      if (!isEnabled()) return;

      LOGGER.fine("Buddy icon cleared for " + screenname + ": " + data);
      storeBuddyIconData(screenname, data, null);
    }

    public void buddyIconUpdated(IconService service, Screenname buddy,
        ExtraInfoData hash, ByteBlock iconData) {
      if (!isEnabled()) return;

      storeInCache(hash, buddy, iconData);
      BuddyInfo buddyInfo = conn.getBuddyInfoManager().getBuddyInfo(buddy);
      LOGGER.fine("Storing buddy icon for " + buddy);
      if (!buddyInfo.setIconDataIfHashMatches(hash, iconData)) {
        LOGGER.info("Buddy icon data for " + buddy + " set too "
            + "late - hash " + hash + " no longer matches");
      }
    }
  }
}
