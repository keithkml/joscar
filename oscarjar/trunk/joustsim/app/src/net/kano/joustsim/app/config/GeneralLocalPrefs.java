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

import net.kano.joustsim.Screenname;
import net.kano.joscar.DefensiveTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class GeneralLocalPrefs implements Preferences {
    private final net.kano.joustsim.Screenname screenname;
    private final File prefsDir;
    private final File prefsFile;

    private String screennameFormat;
    private String savedPassword;
    private static final String PROP_PASSWORD = "password";
    private static final String PROP_SNFORMAT = "sn-format";

    public GeneralLocalPrefs(net.kano.joustsim.Screenname screenname, File localPrefsDir) {
        DefensiveTools.checkNull(screenname, "screenname");
        DefensiveTools.checkNull(localPrefsDir, "prefsDir");

        this.screenname = screenname;
        this.prefsDir = localPrefsDir;
        this.prefsFile = new File(localPrefsDir, "general-prefs.properties");
    }

    public void loadPrefs() throws IOException, FileNotFoundException {
        Properties props = PrefTools.loadProperties(prefsFile);
        loadPrefs(props);
    }

    private synchronized void loadPrefs(Properties props) {
        screennameFormat = props.getProperty(PROP_SNFORMAT);
        String pass = props.getProperty(PROP_PASSWORD);
        if (pass != null) {
            savedPassword = PrefTools.getBase64Decoded(pass);
        } else {
            savedPassword = null;
        }
    }

    public synchronized void savePrefs()
            throws FileNotFoundException, IOException {
        prefsDir.mkdirs();
        prefsFile.createNewFile();

        if (!prefsFile.canWrite()) {
            throw new FileNotFoundException(prefsFile.getAbsolutePath());
        }

        PrefTools.writeProperties(prefsFile, getPrefsProperties(),
                "General local preferences");
    }

    private synchronized Properties getPrefsProperties() {
        Properties props = new Properties();
        if (screennameFormat != null) {
            props.setProperty(PROP_SNFORMAT, screennameFormat);
        }
        if (savedPassword != null && savedPassword.length() > 0) {
            props.setProperty(PROP_PASSWORD,
                    PrefTools.getBase64Encoded(savedPassword));
        }
        return props;
    }

    public synchronized String getScreennameFormat() {
        return screennameFormat;
    }

    public synchronized void setScreennameFormat(String screennameFormat) {
        if (screennameFormat != null && !screenname.matches(screennameFormat)) {
            throw new IllegalArgumentException(screennameFormat + " is not a "
                    + "valid screenname format for " + screenname);
        }
        this.screennameFormat = screennameFormat;
    }

    public synchronized String getSavedPassword() {
        return savedPassword;
    }

    public synchronized void setSavedPassword(String savedPassword) {
        this.savedPassword = savedPassword;
    }
}
