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

package net.kano.aimcrypto.connection.oscar.service;

import net.kano.aimcrypto.AimSession;
import net.kano.aimcrypto.BuddySecurityInfo;
import net.kano.aimcrypto.PrivateKeysInfo;
import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.connection.AimConnection;
import net.kano.aimcrypto.connection.oscar.OscarConnection;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snaccmd.CertificateInfo;
import net.kano.joscar.snaccmd.DirInfo;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.InfoData;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joscar.snaccmd.loc.DirInfoCmd;
import net.kano.joscar.snaccmd.loc.GetDirInfoCmd;
import net.kano.joscar.snaccmd.loc.GetInfoCmd;
import net.kano.joscar.snaccmd.loc.LocCommand;
import net.kano.joscar.snaccmd.loc.SetInfoCmd;
import net.kano.joscar.snaccmd.loc.UserInfoCmd;

import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Iterator;

public class InfoService extends Service {
    private CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();

    private String awayMessage = null;
    private String userProfile = null;

    public InfoService(AimConnection aimConnection,
            OscarConnection oscarConnection) {
        super(aimConnection, oscarConnection, LocCommand.FAMILY_LOC);
    }

    public void addInfoListener(InfoListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeInfoListener(InfoListener l) {
        listeners.remove(l);
    }

    public void connected() {
        CertificateInfo certInfo = generateCertificateInfo();
        InfoData infoData = new InfoData(awayMessage, userProfile, null,
                certInfo);
        sendSnac(new SetInfoCmd(infoData));
    }

    private CertificateInfo generateCertificateInfo() {
        AimSession session = getAimConnection().getAimSession();
        PrivateKeysInfo keys = session.getPrivateKeysInfo();

        X509Certificate signing = keys.getSigningCert();
        X509Certificate encrypting = keys.getEncryptionCert();

        if (signing == null || encrypting == null) return null;

        CertificateInfo certInfo = null;
        try {
            byte[] encSigning = signing.getEncoded();
            byte[] encEncrypting = encrypting.getEncoded();
            certInfo = new CertificateInfo(ByteBlock.wrap(encEncrypting),
                    ByteBlock.wrap(encSigning));
        } catch (CertificateEncodingException e1) {
            //TODO: handle certificate errors
        }
        return certInfo;
    }

    public SnacFamilyInfo getSnacFamilyInfo() {
        return LocCommand.FAMILY_INFO;
    }

    public void requestUserProfile(Screenname sn) {
        sendSnac(new GetInfoCmd(GetInfoCmd.FLAG_INFO, sn.getFormatted()));
    }

    public void requestAwayMessage(Screenname sn) {
        sendSnac(new GetInfoCmd(GetInfoCmd.FLAG_AWAYMSG, sn.getFormatted()));
    }

    public void requestSecurityInfo(Screenname sn) {
        sendSnac(new GetInfoCmd(GetInfoCmd.FLAG_CERT, sn.getFormatted()));
    }

    public void requestDirectoryInfo(Screenname sn) {
        sendSnac(new GetDirInfoCmd(sn.getFormatted()));
    }

    public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
        SnacCommand snac = snacPacketEvent.getSnacCommand();

        if (snac instanceof UserInfoCmd) {
            UserInfoCmd uic = (UserInfoCmd) snac;
            handleUserInfoCmd(uic);

        } else if (snac instanceof DirInfoCmd) {
            DirInfoCmd dic = (DirInfoCmd) snac;
            handleDirInfoCmd(dic);
        }
    }

    private void handleDirInfoCmd(DirInfoCmd dic) {
        DirInfo dirinfo = dic.getDirInfo();
        if (dirinfo == null) return;

        String snText = dirinfo.getScreenname();
        if (snText == null) return;

        Screenname sn = new Screenname(snText);
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            InfoListener listener = (InfoListener) it.next();

            listener.gotDirectoryInfo(this, sn, dirinfo);
        }
    }

    private void handleUserInfoCmd(UserInfoCmd uic) {
        FullUserInfo userInfo = uic.getUserInfo();
        if (userInfo == null) return;

        String snText = userInfo.getScreenname();
        if (snText == null) return;

        InfoData infodata = uic.getInfoData();
        if (infodata == null) return;

        Screenname sn = new Screenname(snText);

        String awayMsg = infodata.getAwayMessage();
        if (awayMsg != null) {
            for (Iterator it = listeners.iterator(); it.hasNext();) {
                InfoListener listener = (InfoListener) it.next();
                listener.gotAwayMessage(this, sn, awayMsg);
            }
        }

        String profile = infodata.getInfo();
        if (profile != null) {
            for (Iterator it = listeners.iterator(); it.hasNext();) {
                InfoListener listener = (InfoListener) it.next();
                listener.gotUserProfile(this, sn, profile);
            }
        }

        CertificateInfo certInfo = infodata.getCertificateInfo();
        if (certInfo != null) {
            ByteBlock signingData;
            ByteBlock encryptionData;
            if (certInfo.isCommon()) {
                signingData = certInfo.getCommonCertData();
                encryptionData = certInfo.getCommonCertData();
            } else {
                signingData = certInfo.getSignCertData();
                encryptionData = certInfo.getEncCertData();
            }
            if (signingData == null || encryptionData == null) {
                //TODO: report wrong signing and/or encryption certs
                return;
            }

            X509Certificate signing;
            X509Certificate encryption;
            try {
                signing = decodeCertificate(signingData);
                encryption = decodeCertificate(encryptionData);
            } catch (Exception e) {
                //TODO: report any errors thrown while decoding certificates
                return;
            }
            BuddySecurityInfo securityInfo = new BuddySecurityInfo(sn,
                     ByteBlock.wrap(CertificateInfo.getCertInfoHash(certInfo)),
                    encryption, signing);

            for (Iterator it = listeners.iterator(); it.hasNext();) {
                InfoListener listener = (InfoListener) it.next();
                listener.gotSecurityInfo(this, sn, securityInfo);
            }
        }
    }

    private static X509Certificate decodeCertificate(ByteBlock certData)
            throws NoSuchProviderException, CertificateException {

        CertificateFactory factory
                = CertificateFactory.getInstance("X.509", "BC");
        InputStream is = ByteBlock.createInputStream(certData);
        return (X509Certificate) factory.generateCertificate(is);
    }
}
