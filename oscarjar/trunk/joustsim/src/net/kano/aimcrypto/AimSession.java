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

import net.kano.aimcrypto.connection.AimConnection;
import net.kano.aimcrypto.connection.AimConnectionProperties;
import net.kano.aimcrypto.config.PrivateKeysInfo;
import net.kano.joscar.DefensiveTools;

public class AimSession {
    private final AppSession appSession;
    private final Screenname screenname;
    private AimConnection connection = null;

    private PrivateKeysInfo privateKeysInfo = null;

    AimSession(AppSession appSession, Screenname screenname) {
        DefensiveTools.checkNull(appSession, "appSession");
        DefensiveTools.checkNull(screenname, "screenname");

        this.appSession = appSession;
        this.screenname = screenname;
    }

    public final AppSession getAppSession() { return appSession; }

    public final Screenname getScreenname() { return screenname; }

    public PrivateKeysInfo getPrivateKeysInfo() { return privateKeysInfo; }

    public void setPrivateKeysInfo(PrivateKeysInfo privateKeysInfo) {
        this.privateKeysInfo = privateKeysInfo;
    }

    public AimConnection openConnection(AimConnectionProperties props) {
        //TODO: close old connection
        AimConnection conn = new AimConnection(appSession, this, props);
        synchronized(this) {
            this.connection = conn;
        }
        return conn;
    }

    public synchronized AimConnection getConnection() { return connection; }

    public void close() {
        AimConnection conn = getConnection();
        if (conn != null) conn.disconnect();
    }
}
