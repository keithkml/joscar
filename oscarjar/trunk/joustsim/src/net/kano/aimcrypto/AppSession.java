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
 *  File created by keith @ Jan 14, 2004
 *
 */

package net.kano.aimcrypto;

import net.kano.aimcrypto.config.GlobalPrefs;
import net.kano.aimcrypto.config.LocalPreferencesManager;
import net.kano.aimcrypto.connection.oscar.service.info.BuddyCertificateManager;
import net.kano.joscar.DefensiveTools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AppSession {
    private final File baseDir;
    private final File configDir;
    private final File localPrefsDir;
    private final File globalPrefsDir;

    private final GlobalPrefs globalPrefs;
    private boolean loadedGlobalPrefs = false;

    private Map prefs = new HashMap();
    private Map sessions = new HashMap();

    private Thread shutdownHook = null;

    public AppSession(File baseDir) throws IllegalArgumentException {
        DefensiveTools.checkNull(baseDir, "baseDir");

        if (baseDir.exists()) {
            if (!baseDir.isDirectory()) {
                throw new IllegalArgumentException(baseDir.getPath()
                        + " is not a directory");
            }
        } else {
            baseDir.mkdir();
            if (!baseDir.isDirectory()) {
                throw new IllegalArgumentException(baseDir.getPath()
                        + " is not a directory and cannot be created");
            }
        }

        this.baseDir = baseDir;
        this.configDir = new File(baseDir, "config");
        this.localPrefsDir = new File(configDir, "local");
        this.globalPrefsDir = new File(configDir, "global");

        this.globalPrefs = new GlobalPrefs(configDir, globalPrefsDir);
    }


    public synchronized void setSavePrefsOnExit(boolean save) {
        if (shutdownHook == null) {
            if (!save) return;
            shutdownHook = new Thread() {
                public void run() {
                    saveAllPrefs();
                }
            };
        }

        Runtime runtime = Runtime.getRuntime();
        if (save) {
            runtime.addShutdownHook(shutdownHook);
        } else {
            runtime.removeShutdownHook(shutdownHook);
            shutdownHook = null;
        }
    }

    private synchronized void saveAllPrefs() {
        try {
            globalPrefs.savePrefs();
        } catch (Exception e) {
            //TODO: saving prefs failed
            e.printStackTrace();
        }
        for (Iterator it = prefs.values().iterator(); it.hasNext();) {
            LocalPreferencesManager prefs = (LocalPreferencesManager) it.next();
            prefs.saveAllPrefs();
        }
    }


    public AimSession openAimSession(Screenname sn) {
        AimSession sess = new AimSession(this, sn);

        synchronized(this) {
            List snsesses = (List) sessions.get(sn);
            if (snsesses == null) {
                snsesses = new ArrayList();
                sessions.put(sn, snsesses);
            }

            snsesses.add(sess);
        }
        return sess;
    }

    public synchronized GlobalPrefs getGlobalPrefs() {
        if (!loadedGlobalPrefs) {
            loadedGlobalPrefs = true;
            globalPrefs.loadPrefs();
        }
        return globalPrefs;
    }

    public synchronized LocalPreferencesManager getLocalPrefs(Screenname sn) {
        LocalPreferencesManager appPrefs = (LocalPreferencesManager) prefs.get(sn);
        if (appPrefs == null) {
            appPrefs = new LocalPreferencesManager(sn, getLocalPrefsDir(sn));
            prefs.put(sn, appPrefs);
        }
        return appPrefs;
    }

    private File getLocalPrefsDir(Screenname sn) {
        return new File(localPrefsDir, sn.getNormal());
    }

    public boolean hasLocalPrefs(Screenname sn) {
        return getLocalPrefsDir(sn).isDirectory();
    }

    public BuddyCertificateManager getCertificateManager() {
        //TODO: appsession's certmanager
        return null;
    }
}
