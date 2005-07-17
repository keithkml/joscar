/*
 *  Copyright (c) 2002-2003, The Joust Project
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

package net.kano.joscardemo;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.flap.FlapPacketEvent;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.net.ClientConnEvent;
import net.kano.joscar.net.ConnDescriptor;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.CertificateInfo;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.InfoData;
import net.kano.joscar.snaccmd.ShortCapabilityBlock;
import net.kano.joscar.snaccmd.conn.MyInfoRequest;
import net.kano.joscar.snaccmd.conn.ServerReadyCmd;
import net.kano.joscar.snaccmd.conn.ServiceRedirect;
import net.kano.joscar.snaccmd.conn.SetEncryptionInfoCmd;
import net.kano.joscar.snaccmd.conn.YourInfoCmd;
import net.kano.joscar.snaccmd.icbm.ParamInfo;
import net.kano.joscar.snaccmd.icbm.ParamInfoCmd;
import net.kano.joscar.snaccmd.icbm.ParamInfoRequest;
import net.kano.joscar.snaccmd.icbm.RecvTypingNotification;
import net.kano.joscar.snaccmd.icbm.SendTypingNotification;
import net.kano.joscar.snaccmd.icbm.SetParamInfoCmd;
import net.kano.joscar.snaccmd.loc.LocRightsCmd;
import net.kano.joscar.snaccmd.loc.LocRightsRequest;
import net.kano.joscar.snaccmd.loc.SetInfoCmd;
import net.kano.joscar.snaccmd.loc.UserInfoCmd;
import net.kano.joscar.snaccmd.ssi.ActivateSsiCmd;
import net.kano.joscar.snaccmd.ssi.SsiDataCmd;
import net.kano.joscar.snaccmd.ssi.SsiDataRequest;
import net.kano.joscar.snaccmd.ssi.SsiItem;
import net.kano.joscar.snaccmd.ssi.SsiRightsRequest;
import net.kano.joscar.snaccmd.ssi.SsiRightsCmd;
import net.kano.joscar.ssiitem.DefaultSsiItemObjFactory;
import net.kano.joscar.ssiitem.SsiItemObj;
import net.kano.joscar.ssiitem.SsiItemObjectFactory;

import java.io.ByteArrayInputStream;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

public class BosFlapConn extends BasicConn {
    private SsiItemObjectFactory itemFactory = new DefaultSsiItemObjFactory();

    private static final List<CapabilityBlock> MY_CAPS = Arrays.asList(
        CapabilityBlock.BLOCK_CHAT,
        CapabilityBlock.BLOCK_DIRECTIM,
        CapabilityBlock.BLOCK_FILE_GET,
        CapabilityBlock.BLOCK_FILE_SEND,
        CapabilityBlock.BLOCK_GAMES,
        CapabilityBlock.BLOCK_GAMES2,
        CapabilityBlock.BLOCK_ICON,
        CapabilityBlock.BLOCK_SENDBUDDYLIST,
        CapabilityBlock.BLOCK_TRILLIANCRYPT,
        CapabilityBlock.BLOCK_VOICE,
        CapabilityBlock.BLOCK_ADDINS,
        CapabilityBlock.BLOCK_ICQCOMPATIBLE,
        CapabilityBlock.BLOCK_SHORTCAPS,
        CapabilityBlock.BLOCK_ENCRYPTION,

        // ShortCapabilityBlock: f0 04 (unknown),
        // ShortCapabilityBlock: f0 05 (unknown),
        // ShortCapabilityBlock: 01 02 (unknown),
        // ShortCapabilityBlock: 01 03 (unknown),
        // ShortCapabilityBlock: f0 02 (unknown),
        // ShortCapabilityBlock: f0 03 (unknown),
        // 09 46 01 05 4c 7f 11 d1 82 22 44 45 45 53 54 00

        // Camera (no icon?)
        ShortCapabilityBlock.getCapFromShortBytes(0x01, 0x02),
        ShortCapabilityBlock.getCapFromShortBytes(0xf0, 0x02),

        // Microphone (dim icon)
        ShortCapabilityBlock.getCapFromShortBytes(0x01, 0x03),
        ShortCapabilityBlock.getCapFromShortBytes(0xf0, 0x03),
        // Conferencing (no icon)
        ShortCapabilityBlock.getCapFromShortBytes(0xf0, 0x04),

        // Conferencing available (makes icon not dim)
        ShortCapabilityBlock.getCapFromShortBytes(0xf0, 0x05),
        new CapabilityBlock(
                0x09, 0x46, 0x01, 0x05, 0x4c, 0x7f, 0x11, 0xd1,
                0x82, 0x22, 0x44, 0x45, 0x45, 0x53, 0x54, 0x00)

//        CapabilityBlock.BLOCK_SOMETHING,
    );

    public BosFlapConn(ConnDescriptor cd, JoscarTester tester,
            ByteBlock cookie) {
        super(cd, tester, cookie);
    }

    protected void handleStateChange(ClientConnEvent e) {
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
//            request(new SetIdleCmd(123456789));
//            request(new SetExtraInfoCmd("I'm Available for chat!!!!"));
            request(new ParamInfoRequest());
            request(new LocRightsRequest());
            request(new SsiRightsRequest());
            request(new SsiDataRequest());

        } else if (cmd instanceof RecvTypingNotification) {
            RecvTypingNotification rtn = (RecvTypingNotification) cmd;

            request(new SendTypingNotification(rtn.getScreenname(),
                    rtn.getTypingState()));
        }
    }

    protected void handleSnacResponse(SnacResponseEvent e) {
        super.handleSnacResponse(e);

        SnacCommand cmd = e.getSnacCommand();

        if (cmd instanceof LocRightsCmd) {
            CertificateInfo certInfo = null;
            Certificate cert = tester.getSecureSession().getMyCertificate();
            if (cert != null) {
                try {
                    byte[] encoded = cert.getEncoded();
                    certInfo = new CertificateInfo(ByteBlock.wrap(encoded));
                } catch (CertificateEncodingException e1) {
                    e1.printStackTrace();
                }
            }
            request(new SetInfoCmd(new InfoData("yo", null, MY_CAPS, certInfo)));
            request(new SetEncryptionInfoCmd(Arrays.asList(
                new ExtraInfoBlock(ExtraInfoBlock.TYPE_CERTINFO_HASHA,
                        new ExtraInfoData(
                            ExtraInfoData.FLAG_HASH_PRESENT,
                            CertificateInfo.HASHA_DEFAULT)),
                new ExtraInfoBlock(ExtraInfoBlock.TYPE_CERTINFO_HASHB,
                        new ExtraInfoData(
                            ExtraInfoData.FLAG_HASH_PRESENT,
                            CertificateInfo.HASHB_DEFAULT)))));
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
            System.out.println("user info for " + sn + ": "
                    + uic.getInfoData());

            CertificateInfo certInfo = uic.getInfoData().getCertificateInfo();
            storeCert(sn, certInfo);

        } else if (cmd instanceof ServiceRedirect) {
            ServiceRedirect sr = (ServiceRedirect) cmd;

            System.out.println("connecting to " + sr.getRedirectHost()
                    + " for 0x" + Integer.toHexString(sr.getSnacFamily()));

            tester.connectToService(sr.getSnacFamily(), sr.getRedirectHost(),
                    sr.getCookie());

        } else if (cmd instanceof SsiRightsCmd) {
            SsiRightsCmd src = (SsiRightsCmd) cmd;

            int[] maxima = src.getMaxima();

            for (int i = 0; i < maxima.length; i++) {
                int max = maxima[i];
                System.out.println("- Max SSI items of type 0x"
                        + Integer.toHexString(i) + ": " + max);
            }

        } else if (cmd instanceof SsiDataCmd) {
            SsiDataCmd sdc = (SsiDataCmd) cmd;

            List<SsiItem> items = sdc.getItems();
            System.out.println("SSI items: " + items.size());
            for (SsiItem item : items) {
                SsiItemObj obj = itemFactory.getItemObj(item);
                System.out.println("- " + (obj == null ? (Object) item
                        : (Object) obj));
            }

            if (items.size() == 0 || sdc.getLastModDate() != 0) {
                System.out.println("done with SSI");
                request(new ActivateSsiCmd());
                clientReady();
            }
//        } else if (cmd instanceof SsiRightsCmd) {
//            SsiRightsCmd src = (SsiRightsCmd) cmd;
//
//            System.out.println("SSI maxima:");
//            int[] maxima = src.getMaxima();
//            for (int i = 0; i < maxima.length; i++) {
//                int max = maxima[i];
//
//                System.out.println("- 0x" + Integer.toHexString(i) + ": " + max);
//            }
        }
    }

    private void storeCert(String sn, CertificateInfo certInfo) {
        if (certInfo == null) return;

        ByteBlock certData = certInfo.getCommonCertData();

        try {
            CertificateFactory factory
                    = CertificateFactory.getInstance("X.509", "BC");
            ByteArrayInputStream stream
                    = new ByteArrayInputStream(certData.toByteArray());
            X509Certificate cert = (X509Certificate)
                    factory.generateCertificate(stream);

            tester.getSecureSession().setCert(sn, cert);

            System.out.println("got certificate for " + sn + ": "
                    + cert.getSubjectX500Principal().getName());


        } catch (CertificateException e1) {
            e1.printStackTrace();
        } catch (NoSuchProviderException e1) {
            e1.printStackTrace();
        }
    }
}
