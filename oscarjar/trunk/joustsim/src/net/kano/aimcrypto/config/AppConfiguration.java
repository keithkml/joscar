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
 *  File created by keith @ Feb 1, 2004
 *
 */

package net.kano.aimcrypto.config;

import net.kano.joscar.DefensiveTools;
import net.kano.aimcrypto.connection.oscar.service.info.BuddyCertificateManager;
import net.kano.aimcrypto.config.LocalKeysManager;
import net.kano.aimcrypto.Screenname;

import java.io.File;
import java.io.IOException;

public class AppConfiguration {
    private final Screenname screenname;
    private final File configDir;
    private final File keysDir;
    private final File trustDir;
    private final File trustedCertsDir;

    private final LocalKeysManager keysManager;
    private final PermanentCertificateTrustManager certificateTrustManager;

    private boolean loadedLocalKeys = false;
    private boolean loadedCertManager = false;

    public AppConfiguration(Screenname screenname, File baseDir) {
        DefensiveTools.checkNull(baseDir, "baseDir");
        DefensiveTools.checkNull(screenname, "screenname");

        this.configDir = baseDir;
        this.screenname = screenname;
        this.keysDir = new File(configDir, "local-keys");
        this.trustDir = new File(configDir, "trust");
        this.trustedCertsDir = new File(trustDir, "trusted-certs");
        this.keysManager = new LocalKeysManager(screenname, keysDir);
        this.certificateTrustManager = new PermanentCertificateTrustManager(
                screenname, trustedCertsDir);
    }

    public synchronized boolean saveAllPrefs() {
        // make sure these exist
        configDir.mkdirs();
        keysDir.mkdirs();

        boolean perfect = true;
        if (keysManager != null) {
            try {
                keysManager.savePrefs();
            } catch (IOException e) {
                //TODO: saving security info failed
                perfect = false;
            }
        }
//        if (certificateTrustManager != null) {
//            try {
//                certificateTrustManager.savePrefs();
//            } catch (IOException e) {
//                perfect = false;
//            }
//        }
        return perfect;
    }

    public LocalKeysManager getLocalKeysManager() {
        synchronized(this) {
            if (!loadedLocalKeys) {
                loadedLocalKeys = true;
                try {
                    keysManager.loadPrefs();
                } catch (IOException e) {
                    //TODO: loading prefs failed
                    e.printStackTrace();
                }
            }
        }
        return keysManager;
    }

    public CertificateTrustManager getCertificateTrustmanager() {
        synchronized(this) {
            if (!loadedCertManager) {
                loadedCertManager = true;
                try {
                    certificateTrustManager.reloadIfNecessary();
                } catch (LoadingException e) {
                    //TODO: loading trusted certs failed
                    e.printStackTrace();
                }
            }
        }
        return certificateTrustManager;
    }
}
