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

import net.kano.joscar.DefensiveTools;
import net.kano.aimcrypto.exceptions.BadKeyPrefsException;
import net.kano.aimcrypto.exceptions.BadKeysException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

public class AppSession {
    private static final DirFilter DIR_FILTER = new DirFilter();

    private Map securityInfos = new HashMap();
    private Map sessions = new HashMap();

    private File baseDir;
    private File securityDir;

    public AppSession(File baseDir) throws IllegalArgumentException {
        DefensiveTools.checkNull(baseDir, "baseDir");

        if (baseDir.exists()) {
            if (!baseDir.isDirectory()) {
                throw new IllegalArgumentException(baseDir.getPath()
                        + " is not a directory");
            }
        } else {
            baseDir.mkdir();
        }

        this.baseDir = baseDir;

        securityDir = new File(baseDir, "security");
    }

    public synchronized PrivateSecurityInfo getSecurityInfo(Screenname sn) {
        PrivateSecurityInfo si = (PrivateSecurityInfo) securityInfos.get(sn);
        if (si == null) {
            si = new PrivateSecurityInfo(sn);
            securityInfos.put(sn, si);
        }
        return si;
    }

    /*
    public void loadSecurityInfos() {
        File[] files = securityDir.listFiles(DIR_FILTER);
        for (int i = 0; i < files.length; i++) {
            File dir = files[i];
            Screenname sn = new Screenname(dir.getName());
            File ks = new File(dir, "keys.p12");
            File prefs = new File(dir, "prefs");
            if (ks.exists() && prefs.exists()) {
                try {
                    loadKeys(sn, ks, prefs);
                } catch (MalformedURLException nobigdeal) {
                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                } catch (BadKeysException e) {
                }
            }
        }
    }
    */

    public PrivateSecurityInfo loadKeys(Screenname sn)
            throws IOException, FileNotFoundException, BadKeysException,
            BadKeyPrefsException {
        DefensiveTools.checkNull(sn, "sn");

        PrivateSecurityInfo si = getSecurityInfo(sn);

        File ks = new File(securityDir, "keys.p12");
        File prefs = new File(securityDir, "key-prefs.properties");

        if (!ks.exists()) throw new FileNotFoundException(ks.getPath());
        if (!prefs.exists()) throw new FileNotFoundException(prefs.getPath());

        Properties props = new Properties();
        props.load(new FileInputStream(prefs));
        String alias = props.getProperty("alias");
        String pass = props.getProperty("pass");
        if (alias == null || pass == null) {
            throw new BadKeyPrefsException(alias != null, pass != null);
        }
        si.loadKeysFromP12(ks.toURI().toURL(), alias, pass);

        return si;
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

    private static class DirFilter implements FileFilter {
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    }
}
