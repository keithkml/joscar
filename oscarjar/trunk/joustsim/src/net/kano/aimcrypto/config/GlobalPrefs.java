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

package net.kano.aimcrypto.config;

import net.kano.joscar.DefensiveTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileFilter;
import java.util.List;
import java.util.ArrayList;

public class GlobalPrefs implements Preferences {
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

    private String[] knownScreennames = SNS_EMPTY;

    public GlobalPrefs(File configDir, File globalPrefsDir) {
        DefensiveTools.checkNull(configDir, "configDir");
        DefensiveTools.checkNull(globalPrefsDir, "globalPrefsDir");

        this.configDir = configDir;
        //TODO: replace new File() with something more portable, in general
        this.localPrefsDir = new File(configDir, "local");
        this.globalPrefsDir = globalPrefsDir;
        this.globalPrefsFile = new File(globalPrefsDir, "prefs.properties");
    }

    public synchronized void loadPrefs() {
        loadKnownScreennames();
    }

    private void loadKnownScreennames() {
        File[] files = localPrefsDir.listFiles(FILTER_VISIBLE_DIRS);
        if (files == null || files.length == 0) return;

        List known = new ArrayList();
        for (int i = 0; i < files.length; i++) {
            known.add(files[i].getName());
        }
        knownScreennames = (String[]) known.toArray(new String[known.size()]);
    }

    public void savePrefs() throws FileNotFoundException, IOException {
    }

    public String[] getKnownScreennames() {
        return (String[]) knownScreennames.clone();
    }
}
