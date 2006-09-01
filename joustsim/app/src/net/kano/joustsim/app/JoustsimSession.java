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

package net.kano.joustsim.app;

import net.kano.joscar.DefensiveTools;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.app.config.GlobalPrefs;
import net.kano.joustsim.app.config.LocalPreferencesManager;
import net.kano.joustsim.app.config.PrefTools;
import net.kano.joustsim.oscar.AimSession;
import net.kano.joustsim.oscar.AppSession;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JoustsimSession implements AppSession {
  private static final Logger logger
      = Logger.getLogger(JoustsimSession.class.getName());

  private final File baseDir;
  private final File configDir;
  private final File localPrefsDir;
  private final File globalPrefsDir;

  private final GlobalPrefs globalPrefs;
  private boolean loadedGlobalPrefs = false;

  private Map<Screenname, LocalPreferencesManager> prefs
      = new HashMap<Screenname, LocalPreferencesManager>();
  private Map<Screenname, List<AimSession>> sessions
      = new HashMap<Screenname, List<AimSession>>();

  private Thread shutdownHook = null;

  public JoustsimSession(File baseDir) throws IllegalArgumentException {
    DefensiveTools.checkNull(baseDir, "baseDir");

    if (baseDir.exists()) {
      if (!baseDir.isDirectory()) {
        throw new IllegalArgumentException(baseDir.getPath()
            + " is not a directory");
      }
    } else {
      baseDir.mkdirs();
      if (!baseDir.isDirectory()) {
        throw new IllegalArgumentException(baseDir.getPath()
            + " is not a directory and cannot be created");
      }
    }

    this.baseDir = baseDir;
    File configDir = PrefTools.getConfigDir(baseDir);
    this.configDir = configDir;
    this.localPrefsDir = PrefTools.getLocalConfigDir(configDir);
    this.globalPrefsDir = PrefTools.getGlobalConfigDir(configDir);

    this.globalPrefs = new GlobalPrefs(configDir);
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
      logger.log(Level.WARNING, "Couldn't save global prefs", e);
    }
    for (LocalPreferencesManager mgr : prefs.values()) {
      mgr.saveAllPrefs();
    }
  }


  public AimSession openAimSession(Screenname sn) {
    AimSession sess = new JoustsimAimSession(this, sn);

    synchronized (this) {
      List<AimSession> snsesses = sessions.get(sn);
      if (snsesses == null) {
        snsesses = new ArrayList<AimSession>();
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
    DefensiveTools.checkNull(sn, "sn");
    File prefsDir = getLocalPrefsDir(sn);
    if (prefsDir == null) return null;

    LocalPreferencesManager appPrefs = prefs.get(sn);
    if (appPrefs == null) {
      appPrefs = new LocalPreferencesManager(sn, prefsDir);
      prefs.put(sn, appPrefs);
    }
    return appPrefs;
  }

  public synchronized LocalPreferencesManager getLocalPrefsIfExist(
      Screenname sn) {
    File prefsDir = getLocalPrefsDir(sn);
    if (prefsDir == null) return null;
    if (prefsDir.isDirectory()) {
      return getLocalPrefs(sn);
    } else {
      return null;
    }
  }

  public synchronized boolean deleteLocalPrefs(Screenname sn) {
    File prefsDir = getLocalPrefsDir(sn);
    if (prefsDir == null) return false;

    boolean deleted = PrefTools.deleteDir(prefsDir);
    if (deleted) prefs.remove(sn);
    return deleted;
  }

  public synchronized Screenname[] getKnownScreennames() {
    globalPrefs.reloadIfNecessary();
    String[] possible = globalPrefs.getKnownScreennames();
    Collection<LocalPreferencesManager> loaded = prefs.values();
    Set<Screenname> known = new HashSet<Screenname>(
        possible.length + loaded.size());

    for (String sn : possible) {
      known.add(new Screenname(sn));
    }

    for (LocalPreferencesManager prefs : loaded) {
      String fmt = prefs.getGeneralPrefs().getScreennameFormat();
      Screenname sn;
      if (fmt == null) {
        sn = prefs.getScreenname();
      } else {
        sn = new Screenname(fmt);
      }
      known.add(sn);
    }
    return known.toArray(new Screenname[known.size()]);
  }

  private File getLocalPrefsDir(Screenname sn) {
    return PrefTools.getLocalPrefsDirForScreenname(this.localPrefsDir, sn);
  }

  public boolean hasLocalPrefs(Screenname sn) {
    return getLocalPrefsDir(sn).isDirectory();
  }
}
