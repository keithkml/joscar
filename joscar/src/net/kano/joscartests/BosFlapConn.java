/*
 *  Copyright (c) 2002, The Joust Project
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
 *  File created by keith @ Mar 26, 2003
 *
 */

package net.kano.joscartests;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.flap.FlapConnEvent;
import net.kano.joscar.flap.FlapPacketEvent;
import net.kano.joscar.snac.SnacCommand;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.InfoData;
import net.kano.joscar.snaccmd.conn.*;
import net.kano.joscar.snaccmd.icbm.ParamInfo;
import net.kano.joscar.snaccmd.icbm.ParamInfoCmd;
import net.kano.joscar.snaccmd.icbm.ParamInfoReq;
import net.kano.joscar.snaccmd.icbm.SetParamInfoCmd;
import net.kano.joscar.snaccmd.loc.LocRightsCmd;
import net.kano.joscar.snaccmd.loc.LocRightsRequest;
import net.kano.joscar.snaccmd.loc.SetInfoCmd;
import net.kano.joscar.snaccmd.loc.UserInfoCmd;
import net.kano.joscar.snaccmd.ssi.*;
import net.kano.joscar.ssiitem.DefaultSsiItemObjFactory;
import net.kano.joscar.ssiitem.SsiItemObj;
import net.kano.joscar.ssiitem.SsiItemObjectFactory;

import java.net.InetAddress;

public class BosFlapConn extends BasicConn {
    protected SsiItemObjectFactory itemFactory = new DefaultSsiItemObjFactory();


    public BosFlapConn(JoscarTester tester, ByteBlock cookie) {
        super(tester, cookie);
    }

    public BosFlapConn(String host, int port, JoscarTester tester,
            ByteBlock cookie) {
        super(host, port, tester, cookie);
    }

    public BosFlapConn(InetAddress ip, int port,
            JoscarTester tester, ByteBlock cookie) {
        super(ip, port, tester, cookie);
    }

    protected void handleStateChange(FlapConnEvent e) {
        System.out.println("main connection state changed from "
                + e.getOldState() + " to " + e.getNewState() + ": "
                + e.getReason());
    }

    protected void handleFlapPacket(FlapPacketEvent e) {
        super.handleFlapPacket(e);
    }

    protected void handleSnacPacket(SnacPacketEvent e) {
        super.handleSnacPacket(e);

        SnacCommand cmd = e.getSnacCommand();

        if (cmd instanceof ServerReadyCmd) {
            request(new SetIdleCmd(12345678));
            request(new ParamInfoReq());
            request(new LocRightsRequest());
            request(new SsiRightsRequest());
            request(new SsiDataRequest());
        }
    }

    protected void handleSnacResponse(SnacResponseEvent e) {
        super.handleSnacResponse(e);

        SnacCommand cmd = e.getSnacCommand();

        if (cmd instanceof LocRightsCmd) {
            request(new SetInfoCmd(new InfoData("this is my infoz lol",
                    "im away?", new CapabilityBlock[] {
                        CapabilityBlock.BLOCK_CHAT,
                        CapabilityBlock.BLOCK_DIRECTIM,
                        CapabilityBlock.BLOCK_FILE_GET,
                        CapabilityBlock.BLOCK_FILE_SEND,
                        CapabilityBlock.BLOCK_GAMES,
                        CapabilityBlock.BLOCK_ICON,
                        CapabilityBlock.BLOCK_SENDBUDDYLIST,
                        CapabilityBlock.BLOCK_TRILLIANCRYPT,
                        CapabilityBlock.BLOCK_VOICE
                    })));

            request(new MyInfoRequest());

        } else if (cmd instanceof ParamInfoCmd) {
            ParamInfoCmd pic = (ParamInfoCmd) cmd;

            ParamInfo info = pic.getParamInfo();

            request(new SetParamInfoCmd(new ParamInfo(0,
                    info.getFlags() | ParamInfo.FLAG_TYPING_NOTIFICATION, 8000,
                    info.getMaxSenderWarning(), info.getMaxReceiverWarning(),
                    0)));

        } else if (cmd instanceof YourInfoCmd) {
            YourInfoCmd yic = (YourInfoCmd) cmd;

            FullUserInfo info = yic.getUserInfo();

            System.out.println("got my user info: " + info);

        } else if (cmd instanceof UserInfoCmd) {
            UserInfoCmd uic = (UserInfoCmd) cmd;

            String sn = uic.getUserInfo().getScreenname();
            System.out.println("got user info for " + sn + "! "
                    + uic.getInfoData());

        } else if (cmd instanceof ServiceRedirect) {
            ServiceRedirect sr = (ServiceRedirect) cmd;

            System.out.println("connecting to " + sr.getRedirectHost()
                    + " for 0x" + Integer.toHexString(sr.getSnacFamily()));

            tester.connectToService(sr.getSnacFamily(), sr.getRedirectHost(),
                    sr.getCookie());

//        } else if (cmd instanceof SsiRightsCmd) {
//            SsiRightsCmd src = (SsiRightsCmd) cmd;

//            int[] maxima = src.getMaxima();

        } else if (cmd instanceof SsiDataCmd) {
            SsiDataCmd sdc = (SsiDataCmd) cmd;

            SsiItem[] items = sdc.getItems();
            for (int i = 0; i < items.length; i++) {
                SsiItemObj obj = itemFactory.getItemObj(items[i]);
                System.out.println("- " + (obj == null ? (Object) items[i]
                        : (Object) obj));
            }

            if (sdc.getLastModDate() != 0) {
                System.out.println("done with SSI");
                request(new ActivateSsiCmd());
                clientReady();
            }
        }
    }
}
