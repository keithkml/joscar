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
 *  File created by keith @ Feb 4, 2004
 *
 */

package net.kano.joustsim.app.config;

import net.kano.joscar.DefensiveTools;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GlobalPrefs implements Preferences, FileBasedResource {
    private static final FileFilter FILTER_VISIBLE_DIRS = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isDirectory() && !pathname.isHidden();
        }
    };
    private static final String[] SNS_EMPTY = new String[0];

    private final File configDir;
    private final File localPrefsDir;
    private final File globalPrefsDir;
    private final File globalPrefsFile;

    private final KnownScreennamesLoader snLoader;

    private String[] knownScreennames = SNS_EMPTY;

    public GlobalPrefs(File configDir) {
        DefensiveTools.checkNull(configDir, "configDir");

        this.configDir = configDir;
        this.localPrefsDir = PrefTools.getLocalConfigDir(configDir);
        this.globalPrefsDir = PrefTools.getGlobalConfigDir(configDir);
        this.globalPrefsFile = new File(globalPrefsDir, "prefs.properties");
        this.snLoader = new KnownScreennamesLoader();
    }

    public synchronized void loadPrefs() {
        reloadIfNecessary();
    }

    public void savePrefs() throws FileNotFoundException, IOException {
    }

    public boolean isUpToDate() {
        return snLoader.isUpToDate();
    }

    public boolean reloadIfNecessary() {
        return snLoader.reloadIfNecessary();
    }

    public void reload() {
        snLoader.reload();
    }

    public synchronized String[] getKnownScreennames() {
        return (String[]) knownScreennames.clone();
    }

    private class KnownScreennamesLoader extends DefaultFileBasedResource {
        public KnownScreennamesLoader() {
            super(localPrefsDir);
        }

        public synchronized boolean reloadIfNecessary() {
            try {
                return super.reloadIfNecessary();
            } catch (LoadingException e) {
                return false;
            }
        }

        public synchronized void reload() {
            File[] files = localPrefsDir.listFiles(FILTER_VISIBLE_DIRS);
            if (files == null || files.length == 0) return;

            List known = new ArrayList();
            for (int i = 0; i < files.length; i++) {
                known.add(files[i].getName());
            }
            knownScreennames = (String[]) known.toArray(new String[known.size()]);
        }
    }
}
