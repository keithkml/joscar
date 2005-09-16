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
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.icon.IconServiceArbiter;
import net.kano.joustsim.oscar.oscar.service.bos.MainBosService;
import net.kano.joustsim.oscar.oscar.service.bos.MainBosServiceListener;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class MyBuddyIconManager {
    private static final Logger LOGGER = Logger
            .getLogger(MyBuddyIconManager.class.getName());

    private AimConnection conn;

    private ByteBlock wantedIconData = null;
    private ExtraInfoData wantedIconHash = null;
    private MainBosServiceListener bosListener = new MainBosServiceListener() {
        public void handleYourInfo(MainBosService service, FullUserInfo userInfo) {
        }

        public void handleYourExtraInfo(List<ExtraInfoBlock> extraInfos) {
            for (ExtraInfoBlock block : extraInfos) {
                if (!(block.getType() == ExtraInfoBlock.TYPE_ICONHASH)) continue;

                handleMyIconBlock(block);
            }
        }
    };

    public MyBuddyIconManager(AimConnection connection) {
        this.conn = connection;
        conn.addOpenedServiceListener(new OpenedServiceListener() {
            public void openedServices(AimConnection cocnn,
                    Collection<? extends Service> services) {
                for (Service service : services) {
                    if (service instanceof MainBosService) {
                        MainBosService bosService = (MainBosService) service;
                        bosService.addMainBosServiceListener(bosListener);
                    }
                }
            }

            public void closedServices(AimConnection conn,
                    Collection<? extends Service> services) {
            }
        });
    }

    private void handleMyIconBlock(ExtraInfoBlock block) {
        ExtraInfoData hash = block.getExtraData();
        if (wantedIconData != null && wantedIconHash != null
                && hash.getData().equals(wantedIconHash.getData())) {
            if ((hash.getFlags() & ExtraInfoData.FLAG_UPLOAD_ICON) != 0) {
                if (wantedIconData == null) {
                    LOGGER.fine("Server asked us to upload "
                            + "icon for " + hash + " but we don't "
                            + "want icon data");
                } else {
                    ExternalServiceManager externalServiceManager = conn
                            .getExternalServiceManager();
                    IconServiceArbiter iconServiceArbiter = externalServiceManager
                            .getIconServiceArbiter();
                    iconServiceArbiter.uploadIcon(wantedIconData);
                }
            }
        } else {
            LOGGER.fine("Ignoring obsolete icon upload "
                    + "request from server for " + hash);
        }
    }

    public void requestSetIcon(ExtraInfoData iconInfo) {
        wantedIconData = null;
        reallySetIcon(iconInfo);
    }

    public void requestSetIcon(ByteBlock iconData) {
        DefensiveTools.checkNull(iconData, "iconData");

        wantedIconData = iconData;
        ExtraInfoData iconInfo = conn.getBuddyIconTracker()
                .addToCache(conn.getScreenname(), iconData);
        reallySetIcon(iconInfo);
    }

    public void requestClearIcon() {
        wantedIconData = null;
        reallySetIcon(new ExtraInfoData(ExtraInfoData.FLAG_DEFAULT,
                ExtraInfoData.HASH_SPECIAL));
    }

    private void reallySetIcon(ExtraInfoData iconInfo) {
        DefensiveTools.checkNull(iconInfo, "iconInfo");

        wantedIconHash = iconInfo;
        conn.getSsiService().getBuddyIconItemManager().setIcon(iconInfo);
    }
}
