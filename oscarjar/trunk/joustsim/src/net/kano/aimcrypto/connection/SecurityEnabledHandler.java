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

import net.kano.aimcrypto.config.LocalKeysManager;
import net.kano.aimcrypto.config.PrivateKeysInfo;
import net.kano.aimcrypto.connection.oscar.service.Service;
import net.kano.aimcrypto.connection.oscar.service.info.InfoService;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.CertificateInfo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SecurityEnabledHandler implements CapabilityHandler {
    private static final Logger logger
            = Logger.getLogger(SecurityEnabledHandler.class.getName());

    private final AimConnection conn;
    private boolean boundInfoService = false;
    private final LocalKeysManager keysMgr;

    public SecurityEnabledHandler(AimConnection conn) {
        DefensiveTools.checkNull(conn, "conn");

        this.conn = conn;
        conn.addNewServiceListener(new NewServiceListener() {
            public void openedServices(AimConnection conn, Service[] services) {
                bindToInfoService();
            }
        });
        keysMgr = conn.getAimSession().getLocalPrefs().getLocalKeysManager();
        keysMgr.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                String prop = evt.getPropertyName();
                if (prop.equals(LocalKeysManager.PROP_KEYS_INFO)) {
                    updateCertInfo();
                }
            }
        });
    }

    private void bindToInfoService() {
        InfoService infoService;
        synchronized(this) {
            if (boundInfoService) return;
            infoService = conn.getInfoService();
            if (infoService == null) return;
            boundInfoService = true;
        }

        updateCertInfo();
    }

    private void updateCertInfo() {
        InfoService infoService = conn.getInfoService();
        if (infoService == null) return;

        infoService.setCertificateInfo(generateLocalCertificateInfo());
    }

    private CertificateInfo generateLocalCertificateInfo() {
        PrivateKeysInfo keys = keysMgr.getKeysInfo();
        if (keys == null) {
            logger.fine("User has no private keys");
            return null;
        }

        X509Certificate signing = keys.getSigningCertificate();
        X509Certificate encrypting = keys.getEncryptionCertificate();

        if (signing == null && encrypting == null) {
            logger.fine("User has no signing or encrypting key, but has some "
                    + "kind of private keys info stored");
            return null;
        }
        if (signing == null) {
            logger.fine("User has no signing key");
            return null;
        }
        if (encrypting == null) {
            logger.fine("User has no encrypting key");
            return null;
        }

        CertificateInfo certInfo;
        if (signing == encrypting) {
            try {
                byte[] encCert = signing.getEncoded();
                certInfo = new CertificateInfo(ByteBlock.wrap(encCert));
            } catch (CertificateEncodingException e) {
                logger.log(Level.WARNING, "Could not encode common certificate "
                        + "to upload to server", e);
                return null;
            }

        } else {
        byte[] encSigning;
        byte[] encEncrypting;
            try {
                encSigning = signing.getEncoded();
            } catch (CertificateEncodingException e1) {
                logger.log(Level.WARNING, "Could not encode signing "
                        + "certificate to upload to server", e1);
                return null;
            }
            try {
                encEncrypting = encrypting.getEncoded();
            } catch (CertificateEncodingException e1) {
                logger.log(Level.WARNING, "Could not encode encrypting "
                        + "certificate to upload to server", e1);
                return null;

            }
            certInfo = new CertificateInfo(ByteBlock.wrap(encEncrypting),
                    ByteBlock.wrap(encSigning));
        }
        return certInfo;
    }

    public void handleAdded(CapabilityManager manager) {
    }

    public void handleRemoved(CapabilityManager manager) {
    }
}
