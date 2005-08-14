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
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.icon.IconRequestListener;
import net.kano.joustsim.oscar.oscar.service.icon.IconService;
import net.kano.joustsim.oscar.oscar.service.icon.IconServiceArbiter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class BuddyIconTracker {
    private static final Logger LOGGER = Logger
            .getLogger(BuddyIconTracker.class.getName());

    private AimConnection conn;
    private Map<ExtraInfoBlock, ByteBlock> cache
            = new HashMap<ExtraInfoBlock, ByteBlock>();
    private CopyOnWriteArrayList<BuddyIconChangeListener> listeners
            = new CopyOnWriteArrayList<BuddyIconChangeListener>();
    private boolean enabled = true;

    private IconRequestListener iconRequestListener = new IconRequestListener() {
        public void buddyIconCleared(IconService service,
                Screenname screenname) {
            if (!isEnabled()) return;

            storeBuddyIconData(screenname, null);
        }

        public void buddyIconUpdated(IconService service, Screenname screenname,
                ExtraInfoBlock hash, ByteBlock iconData) {
            if (!isEnabled()) return;

//            ByteBlock computedHash = cacheIcon(iconData);
//            if (!hash.equals(computedHash)) {
//                storeInCache(hash, iconData);
//                LOGGER.warning("Computed hash " + computedHash + " does not "
//                        + "match server hash " + hash + " for " + screenname);
//            }
            //TODO: check to see if hash matches data hash
            storeInCache(hash, iconData);

        for (BuddyInfo info : conn.getBuddyInfoManager().getKnownBuddyInfos()) {
            ExtraInfoBlock buddyHash = info.getIconHash();
            if (buddyHash != null && buddyHash.equals(hash)) {
                info.setIconData(iconData);
            }
        }
        }
    };

    public BuddyIconTracker(AimConnection aconn) {
        this.conn = aconn;
        BuddyInfoManager mgr = conn.getBuddyInfoManager();
        mgr.addGlobalBuddyInfoListener(new GlobalBuddyInfoListener() {
            public void newBuddyInfo(BuddyInfoManager manager, Screenname buddy,
                    BuddyInfo info) {
                if (!isEnabled()) return;
                handleNewIconHashForBuddy(buddy, info.getIconHash());
            }

            public void buddyInfoChanged(BuddyInfoManager manager,
                    Screenname buddy,BuddyInfo info,
                    PropertyChangeEvent event) {
                if (!isEnabled()) return;
                if (event.getPropertyName().equals(BuddyInfo.PROP_ICON_HASH)) {
                    ExtraInfoBlock newHash = (ExtraInfoBlock) event.getNewValue();
                    handleNewIconHashForBuddy(buddy, newHash);
                }
            }

            public void receivedStatusUpdate(BuddyInfoManager manager,
                    Screenname buddy, BuddyInfo info) {
            }
        });
    }

    private void handleNewIconHashForBuddy(Screenname buddy, ExtraInfoBlock newHash) {
        if (newHash != null /* && !newHash.equals(ExtraInfoData.HASH_SPECIAL)*/) {
            ByteBlock iconData = getIconDataForHash(newHash);
            if (iconData == null) {
                IconServiceArbiter iconArbiter =
                        conn.getIconServiceArbiter();
                if (iconArbiter != null) {
                    iconArbiter.addIconRequestListener(iconRequestListener);
                    iconArbiter.requestIcon(buddy, newHash);
                }
            }
        } else {
            storeBuddyIconData(buddy, null);
        }
    }

    private void storeBuddyIconData(Screenname buddy, ByteBlock iconData) {
        conn.getBuddyInfoManager().getBuddyInfo(buddy).setIconData(iconData);
    }

    public @Nullable synchronized ByteBlock getIconDataForHash(ExtraInfoBlock hash) {
        return cache.get(hash);
    }

//    public ByteBlock cacheIcon(ByteBlock iconData) {
//        ByteBlock hash;
//        try {
//            MessageDigest digest = MessageDigest.getInstance("MD5");
//            hash = ByteBlock.wrap(digest.digest(iconData.toByteArray()));
//        } catch (NoSuchAlgorithmException e) {
//            throw new IllegalStateException(e);
//        }
//        storeInCache(hash, iconData);
//        for (BuddyInfo info : conn.getBuddyInfoManager().getKnownBuddyInfos()) {
//            ExtraInfoBlock buddyHash = info.getIconHash();
//            if (buddyHash != null && buddyHash.equals(hash)) {
//                info.setIconData(iconData);
//            }
//        }
//        return hash;
//    }

    private synchronized void storeInCache(ExtraInfoBlock hash,
            @NotNull ByteBlock iconData) {
        cache.put(hash, ByteBlock.wrap(iconData.toByteArray()));
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) cache.clear();
    }

    public @Nullable ByteBlock getBuddyIconData(Screenname screenname) {
        BuddyInfo buddyInfo = conn.getBuddyInfoManager().getBuddyInfo(screenname);
        if (buddyInfo == null) return null;

        ExtraInfoBlock hash = buddyInfo.getIconHash();
        if (hash == null) return null;

        return getIconDataForHash(hash);
    }
}
