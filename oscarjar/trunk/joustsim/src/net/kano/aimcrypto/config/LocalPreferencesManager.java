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

public class LocalPreferencesManager {
    private final Screenname screenname;
    private final File configDir;
    private final File keysDir;
    private final File trustDir;
    private final File trustedCertsDir;
    private final File trustedSignersDir;

    private final GeneralLocalPrefs generalPrefs;
    private final LocalKeysManager localKeysManager;
    private final PermanentCertificateTrustManager certificateTrustManager;
    private final PermanentSignerTrustManager signerTrustManager;

    private boolean loadedGeneralPrefs = false;
    private boolean loadedLocalKeys = false;
    private boolean loadedCertManager = false;
    private boolean loadedSignerManager = false;

    public LocalPreferencesManager(Screenname screenname, File localConfigDir) {
        DefensiveTools.checkNull(screenname, "screenname");
        DefensiveTools.checkNull(localConfigDir, "baseDir");

        this.screenname = screenname;
        this.configDir = localConfigDir;

        this.keysDir = new File(this.configDir, "local-keys");
        this.trustDir = new File(this.configDir, "trust");
        this.trustedCertsDir = new File(trustDir, "trusted-certs");
        this.trustedSignersDir = new File(trustDir, "trusted-signers");

        this.generalPrefs = new GeneralLocalPrefs(screenname, this.configDir);
        this.localKeysManager = new LocalKeysManager(screenname, keysDir);
        this.certificateTrustManager = new PermanentCertificateTrustManager(
                screenname, trustedCertsDir);
        this.signerTrustManager = new PermanentSignerTrustManager(
                screenname, trustedSignersDir);
    }

    public Screenname getScreenname() {
        return screenname;
    }

    public synchronized boolean saveAllPrefs() {
        boolean perfect = true;
        try {
            if (loadedGeneralPrefs) generalPrefs.savePrefs();
        } catch (Exception e) {
            //TODO: saving general prefs failed
            perfect = false;
        }
        try {
            if (loadedLocalKeys) localKeysManager.savePrefs();
        } catch (Exception e) {
            //TODO: saving security info failed
            perfect = false;
        }
        return perfect;
    }

    public GeneralLocalPrefs getGeneralPrefs() {
        synchronized(this) {
            if (!loadedGeneralPrefs) {
                loadedGeneralPrefs = true;
                try {
                    generalPrefs.loadPrefs();
                } catch (IOException e) {
                    //TODO: loading general prefs failed
                    e.printStackTrace();
                }
            }
        }
        return generalPrefs;
    }

    public LocalKeysManager getLocalKeysManager() {
        synchronized(this) {
            if (!loadedLocalKeys) {
                loadedLocalKeys = true;
                try {
                    localKeysManager.loadPrefs();
                } catch (IOException e) {
                    //TODO: loading prefs failed
                    e.printStackTrace();
                }
            }
        }
        return localKeysManager;
    }

    public PermanentCertificateTrustManager getStoredCertificateTrustManager() {
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

    public PermanentSignerTrustManager getStoredSignerTrustManager() {
        synchronized(this) {
            if (!loadedSignerManager) {
                loadedSignerManager = true;
                try {
                    signerTrustManager.reloadIfNecessary();
                } catch (LoadingException e) {
                    //TODO: loading trusted signers failed
                    e.printStackTrace();
                }
            }
        }
        return signerTrustManager;
    }
}
