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
 *  File created by keith @ Jan 25, 2004
 *
 */

package net.kano.aimcrypto.connection;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.DirInfo;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.ShortCapabilityBlock;
import net.kano.aimcrypto.connection.AimConnection;
import net.kano.aimcrypto.connection.oscar.service.buddy.BuddyServiceListener;
import net.kano.aimcrypto.connection.oscar.service.buddy.BuddyService;
import net.kano.aimcrypto.connection.oscar.service.info.InfoServiceListener;
import net.kano.aimcrypto.connection.oscar.service.info.InfoService;
import net.kano.aimcrypto.connection.oscar.service.info.InfoService;
import net.kano.aimcrypto.connection.oscar.service.buddy.BuddyService;
import net.kano.aimcrypto.connection.oscar.service.buddy.BuddyServiceListener;
import net.kano.aimcrypto.connection.oscar.service.Service;
import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.config.BuddyCertificateInfo;
import net.kano.aimcrypto.config.BuddyCertificateInfo;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.Iterator;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class BuddyInfoManager {
    private final AimConnection conn;
    private Map buddyInfos = new HashMap();

    private boolean initedBuddyService = false;
    private boolean initedInfoService = false;

    private CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();

    private PropertyChangeListener pcl = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            BuddyInfo info = (BuddyInfo) evt.getSource();
            Screenname sn = info.getScreenname();

            for (Iterator it = listeners.iterator(); it.hasNext();) {
                GlobalBuddyInfoListener l = (GlobalBuddyInfoListener) it.next();

                l.buddyInfoChanged(BuddyInfoManager.this, sn, info, evt);
            }
        }
    };

    public BuddyInfoManager(AimConnection conn) {
        DefensiveTools.checkNull(conn, "conn");

        this.conn = conn;
        conn.addNewServiceListener(new NewServiceListener() {
            public void openedServices(AimConnection conn, Service[] services) {
                initBuddyService();
                initInfoService();
            }
        });
        initBuddyService();
        initInfoService();
    }

    private void initBuddyService() {
        BuddyService bs = conn.getBuddyService();
        if (bs == null) return;

        synchronized(this) {
            if (initedBuddyService) return;
            initedBuddyService = true;
        }

        bs.addBuddyListener(new BuddyServiceListener() {
            public void gotBuddyStatus(BuddyService service, Screenname buddy,
                    FullUserInfo info) {
                setBuddyStatus(buddy, info);
            }

            public void buddyOffline(BuddyService service, Screenname buddy) {
                BuddyInfo buddyInfo = getBuddyInfoInstance(buddy);
                if (buddyInfo != null) buddyInfo.setOnline(false);
            }
        });
    }

    private void initInfoService() {
        InfoService infoService = conn.getInfoService();
        if (infoService == null) return;

        synchronized(this) {
            if (initedInfoService) return;
            initedInfoService = true;
        }

        infoService.addInfoListener(new InfoServiceListener() {
            public void handleDirectoryInfo(InfoService service, Screenname buddy,
                    DirInfo info) {
                BuddyInfo buddyInfo = getBuddyInfoInstance(buddy);
                buddyInfo.setDirectoryInfo(info);
                System.out.println("got dir info");
            }

            public void handleAwayMessage(InfoService service, Screenname buddy,
                    String awayMsg) {
                BuddyInfo buddyInfo = getBuddyInfoInstance(buddy);
                buddyInfo.setAwayMessage(awayMsg);
                System.out.println("got away msg");
            }

            public void handleUserProfile(InfoService service, Screenname buddy,
                    String infoString) {
                BuddyInfo buddyInfo = getBuddyInfoInstance(buddy);
                buddyInfo.setUserProfile(infoString);
                System.out.println("got info string");
            }

            public void handleCertificateInfo(InfoService service, Screenname buddy,
                    BuddyCertificateInfo securityInfo) {
                BuddyInfo buddyInfo = getBuddyInfoInstance(buddy);
                buddyInfo.setCertificateInfo(securityInfo);
                System.out.println("got security info");
            }
        });
    }

    public void addGlobalBuddyInfoListener(GlobalBuddyInfoListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeGlobalBuddyInfoListener(GlobalBuddyInfoListener l) {
        listeners.remove(l);
    }

    private void setBuddyStatus(Screenname buddy, FullUserInfo info) {
        BuddyInfo buddyInfo = getBuddyInfoInstance(buddy);

        Date onSince = info.getOnSince();
        if (onSince != null) buddyInfo.setOnlineSince(onSince);

        Boolean awayStatus = info.getAwayStatus();
        if (awayStatus != null) buddyInfo.setAway(awayStatus.booleanValue());

        CapabilityBlock[] caps = info.getCapabilityBlocks();
        ShortCapabilityBlock[] shortCaps = info.getShortCapabilityBlocks();
        if (caps != null || shortCaps != null) {
            int numLong = caps == null ? 0 : caps.length;
            int numShort = shortCaps == null ? 0 : shortCaps.length;

            CapabilityBlock[] blocks = new CapabilityBlock[numLong + numShort];
            if (caps != null) {
                System.arraycopy(caps, 0, blocks, 0, caps.length);
            }
            if (shortCaps != null) {
                for (int i = 0, j = numLong; i < shortCaps.length; i++, j++) {
                    blocks[j] = shortCaps[i].toCapabilityBlock();
                }
            }
            buddyInfo.setCapabilities(caps);
        }

        ByteBlock certHash = info.getCertInfoHash();
        buddyInfo.setCertificateInfoHash(certHash);

        int idleMins = info.getIdleMins();
        Date idleSince;
        if (idleMins == -1) {
            idleSince = null;
        } else {
            int idlems = idleMins * 1000 * 60;
            idleSince = new Date(System.currentTimeMillis() - idlems);
        }
        buddyInfo.setIdleSince(idleSince);

        int warningLevelx10 = info.getWarningLevel();
        if (warningLevelx10 != -1) {
            int rounder = (warningLevelx10 % 10) >= 5 ? 1 : 0;
            int warningLevel = (warningLevelx10 / 10) + rounder;
            buddyInfo.setWarningLevel(warningLevel);
        }
    }

    private synchronized BuddyInfo getBuddyInfoInstance(Screenname buddy) {
        BuddyInfo info = (BuddyInfo) buddyInfos.get(buddy);
        if (info == null) {
            info = new BuddyInfo(buddy);
            buddyInfos.put(buddy, info);
            info.addPropertyListener(pcl);
        }
        return info;
    }

    public synchronized BuddyInfo getBuddyInfo(Screenname buddy) {
        return (BuddyInfo) buddyInfos.get(buddy);
    }
}
