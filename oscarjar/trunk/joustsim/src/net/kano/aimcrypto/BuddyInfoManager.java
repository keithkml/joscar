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

package net.kano.aimcrypto;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.DirInfo;
import net.kano.aimcrypto.connection.AimConnection;
import net.kano.aimcrypto.connection.oscar.service.BuddyServiceListener;
import net.kano.aimcrypto.connection.oscar.service.BuddyService;
import net.kano.aimcrypto.connection.oscar.service.InfoListener;
import net.kano.aimcrypto.connection.oscar.service.InfoService;

import java.util.Map;
import java.util.HashMap;

public class BuddyInfoManager {
    private final AimConnection conn;
    private Map buddyInfos = new HashMap();

    public BuddyInfoManager(AimConnection conn) {
        DefensiveTools.checkNull(conn, "conn");

        this.conn = conn;
        conn.getBuddyService().addBuddyListener(new BuddyServiceListener() {
            public void gotBuddyStatus(BuddyService service, Screenname buddy,
                    FullUserInfo info) {
                setBuddyStatus(buddy, info);
            }

            public void buddyOffline(BuddyService service, Screenname buddy) {
                BuddyInfo buddyInfo = getBuddyInfo(buddy);
                if (buddyInfo != null) buddyInfo.setOnline(false);
            }
        });
        conn.getInfoService().addInfoListener(new InfoListener() {
            public void gotDirectoryInfo(InfoService service, Screenname buddy,
                    DirInfo info) {
                BuddyInfo buddyInfo = getBuddyInfo(buddy);
                if (buddyInfo != null) buddyInfo.setDirInfo(info);
            }

            public void gotAwayMessage(InfoService service, Screenname buddy,
                    String awayMsg) {
            }

            public void gotUserProfile(InfoService service, Screenname buddy,
                    String infoString) {
            }

            public void gotSecurityInfo(InfoService service, Screenname buddy,
                    BuddySecurityInfo securityInfo) {
            }
        });
    }

    public synchronized BuddyInfo getBuddyInfo(Screenname buddy) {
        return (BuddyInfo) buddyInfos.get(buddy);
    }
}
