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

package net.kano.joustsim.app.config;

import net.kano.joustsim.Screenname;
import net.kano.joustsim.trust.CertificateTrustManager;
import net.kano.joustsim.trust.PrivateKeysPreferences;
import net.kano.joustsim.trust.SignerTrustManager;
import net.kano.joustsim.trust.TrustPreferences;
import net.kano.joscar.DefensiveTools;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalPreferencesManager implements TrustPreferences {
    private static final Logger logger
            = Logger.getLogger(LocalPreferencesManager.class.getName());

    private final net.kano.joustsim.Screenname screenname;
    private final File configDir;
    private final File keysDir;
    private final File trustDir;
    private final File trustedCertsDir;
    private final File trustedSignersDir;

    private final GeneralLocalPrefs generalPrefs;
    private final PrivateKeysManager localKeysManager;
    private final PermanentCertificateTrustManager certificateTrustManager;
    private final PermanentSignerTrustManager signerTrustManager;

    private boolean loadedGeneralPrefs = false;
    private boolean loadedLocalKeys = false;

    public LocalPreferencesManager(net.kano.joustsim.Screenname screenname, File localConfigDir) {
        DefensiveTools.checkNull(screenname, "screenname");
        DefensiveTools.checkNull(localConfigDir, "baseDir");

        this.screenname = screenname;
        this.configDir = localConfigDir;

        this.keysDir = PrefTools.getLocalKeysDir(localConfigDir);
        File trustDir = PrefTools.getLocalTrustDir(localConfigDir);
        this.trustDir = trustDir;
        this.trustedCertsDir = PrefTools.getTrustedCertsDir(trustDir);
        this.trustedSignersDir = PrefTools.getTrustedSignersDir(trustDir);

        this.generalPrefs = new GeneralLocalPrefs(screenname, localConfigDir);
        this.localKeysManager = new PrivateKeysManager(screenname, keysDir);
        this.certificateTrustManager = new PermanentCertificateTrustManager(
                screenname, trustedCertsDir);
        this.signerTrustManager = new PermanentSignerTrustManager(
                screenname, trustedSignersDir);
    }

    public net.kano.joustsim.Screenname getScreenname() { return screenname; }

    public boolean saveAllPrefs() {
        boolean perfect = true;

        boolean loadedGeneralPrefs;
        boolean loadedLocalKeys;

        synchronized(this) {
            loadedGeneralPrefs = this.loadedGeneralPrefs;
            loadedLocalKeys = this.loadedLocalKeys;
        }

        try {
            if (loadedGeneralPrefs) generalPrefs.savePrefs();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't save general preferences for "
                    + screenname, e);
            perfect = false;
        }
        try {
            if (loadedLocalKeys) localKeysManager.savePrefs();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't save local keys preferences "
                    + "for " + screenname, e);
            perfect = false;
        }
        return perfect;
    }

    public GeneralLocalPrefs getGeneralPrefs() {
        boolean needsLoading;

        synchronized(this) {
            if (!loadedGeneralPrefs) {
                loadedGeneralPrefs= true;
                needsLoading = true;
            } else {
                needsLoading = false;
            }
        }
        if (needsLoading) {
            try {
                generalPrefs.loadPrefs();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Couldn't load general preferences "
                        + "for " + screenname, e);
            }
        }
        return generalPrefs;
    }

    public PrivateKeysPreferences getPrivateKeysPreferences() {
        return getPrivateKeysManager();
    }

    public PrivateKeysManager getPrivateKeysManager() {
        boolean needsLoading;

        synchronized(this) {
            if (!loadedLocalKeys) {
                loadedLocalKeys = true;
                needsLoading = true;
            } else {
                needsLoading = false;
            }
        }
        if (needsLoading) {
            try {
                localKeysManager.loadPrefs();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Couldn't load local keys "
                        + "preferences for " + screenname, e);
            }
        }
        return localKeysManager;
    }

    public CertificateTrustManager getCertificateTrustManager() {
        return getPermanentCertificateTrustManager();
    }

    public PermanentCertificateTrustManager
            getPermanentCertificateTrustManager() {
        return certificateTrustManager;
    }

    public SignerTrustManager getSignerTrustManager() {
        return getPermanentSignerTrustManager();
    }

    public PermanentSignerTrustManager getPermanentSignerTrustManager() {
        return signerTrustManager;
    }

    public boolean loadEverything() {
        boolean perfect = true;
        try {
            getPrivateKeysManager().reloadIfNecessary();
        } catch (LoadingException e) {
            logger.log(Level.WARNING, "Couldn't load local keys for "
                    + screenname, e);
            perfect = false;
        }
        try {
            getPermanentCertificateTrustManager().reloadIfNecessary();
        } catch (LoadingException e) {
            logger.log(Level.WARNING, "Couldn't load trusted certificates for "
                    + screenname, e);
            perfect = false;
        }
        try {
            getPermanentSignerTrustManager().reloadIfNecessary();
        } catch (LoadingException e) {
            logger.log(Level.WARNING, "Couldn't load trusted signers for "
                    + screenname, e);
            perfect = false;
        }
        return perfect;
    }
}
