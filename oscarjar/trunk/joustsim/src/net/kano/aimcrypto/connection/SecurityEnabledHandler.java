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
 *  File created by keith @ Feb 7, 2004
 *
 */

package net.kano.aimcrypto.connection;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.snaccmd.CertificateInfo;
import net.kano.aimcrypto.AimSession;
import net.kano.aimcrypto.connection.oscar.service.Service;
import net.kano.aimcrypto.connection.oscar.service.info.InfoService;
import net.kano.aimcrypto.config.PrivateKeysInfo;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;

public class SecurityEnabledHandler implements CapabilityHandler {
    private final AimConnection conn;
    private boolean boundInfoService = false;

    public SecurityEnabledHandler(AimConnection conn) {
        DefensiveTools.checkNull(conn, "conn");

        this.conn = conn;
        conn.addNewServiceListener(new NewServiceListener() {
            public void openedServices(AimConnection conn, Service[] services) {
                bindToInfoService();
            }
        });
    }

    private synchronized void bindToInfoService() {
        InfoService infoService;
        synchronized(this) {
            if (boundInfoService) return;
            infoService = conn.getInfoService();
            if (infoService == null) return;
            boundInfoService = true;
        }

        infoService.setCertificateInfo(generateLocalCertificateInfo());
    }

    private CertificateInfo generateLocalCertificateInfo() {
        AimSession session = conn.getAimSession();
        PrivateKeysInfo keys = session.getPrivateKeysInfo();
        if (keys == null) {
            System.out.println("no private keys!");
            return null;
        }

        X509Certificate signing = keys.getSigningCertificate();
        X509Certificate encrypting = keys.getEncryptionCertificate();

        if (signing == null || encrypting == null) {
            System.out.println("no signing or encrypting! " + signing + ", "
                    + encrypting);
            return null;
        }

        CertificateInfo certInfo = null;
        if (signing == encrypting) {
            try {
                byte[] encCert = signing.getEncoded();
                certInfo = new CertificateInfo(ByteBlock.wrap(encCert));
            } catch (CertificateEncodingException e) {
                //TODO: handle certificate errors
                e.printStackTrace();
            }

        } else {
            try {
                byte[] encSigning = signing.getEncoded();
                byte[] encEncrypting = encrypting.getEncoded();
                certInfo = new CertificateInfo(ByteBlock.wrap(encEncrypting),
                        ByteBlock.wrap(encSigning));
            } catch (CertificateEncodingException e1) {
                //TODO: handle certificate errors
                e1.printStackTrace();
            }
        }
        return certInfo;
    }

    public void handleAdded(CapabilityManager manager) {
    }

    public void handleRemoved(CapabilityManager manager) {
    }
}
