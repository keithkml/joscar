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

import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.info.BuddyTrustManager;
import net.kano.joustsim.oscar.oscar.service.info.InfoService;
import net.kano.joustsim.trust.BuddyCertificateInfo;
import net.kano.joscar.DefensiveTools;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BuddyInfoTracker {
    private final AimConnection conn;
    private final BuddyInfoManager buddyInfoMgr;
    private final BuddyTrustManager buddyTrustManager;

    private Set globalTrackers = new HashSet();
    private Map trackers = new HashMap();

    public BuddyInfoTracker(AimConnection conn) {
        DefensiveTools.checkNull(conn, "conn");

        this.conn = conn;
        BuddyInfoManager buddyInfoMgr = conn.getBuddyInfoManager();
        this.buddyInfoMgr = buddyInfoMgr;
        this.buddyTrustManager = conn.getBuddyTrustManager();
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

    private InfoService getInfoService() {
        InfoService infoService
                = this.conn.getInfoService();
        return infoService;
    }

    public boolean addTracker(Screenname buddy,
            BuddyInfoTrackerListener listener) {
        DefensiveTools.checkNull(buddy, "buddy");
        DefensiveTools.checkNull(listener, "listener");

        boolean startTracking = false;
        boolean added;
        Set btrackers;
        synchronized (this) {
            btrackers = (Set) trackers.get(buddy);
            if (btrackers == null) {
                btrackers = new HashSet();
                trackers.put(buddy, btrackers);
                startTracking = true;
            }
            added = btrackers.add(listener);
        }

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
            Set btrackers = (Set) trackers.get(buddy);
            if (btrackers == null) return false;

            boolean removed = btrackers.remove(buddy);
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
        assert trackers.containsKey(buddy);

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
        assert !trackers.containsKey(buddy);

        DefensiveTools.checkNull(buddy, "buddy");

        // there's nothing to do, since we don't register any listeners at the
        // buddy level
    }

    public synchronized boolean isTracked(Screenname buddy) {
        return trackers.containsKey(buddy);
    }
}
