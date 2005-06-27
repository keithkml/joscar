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
 *  File created by keith @ Jun 10, 2004
 *
 */

package net.kano.joustsim.oscar;

import net.kano.joustsim.Screenname;
import net.kano.joustsim.trust.TrustPreferences;

public class DefaultAimSession implements AimSession {
    private final AppSession appSession;
    private final Screenname screenname;
    private final TrustPreferences trustPreferences;

    private AimConnection connection = null;

    public DefaultAimSession(Screenname screenname) {
        this(new DefaultAppSession(), screenname); 
    }

    public DefaultAimSession(AppSession appSession, Screenname screenname) {
        this(appSession, screenname, null);
    }

    public DefaultAimSession(AppSession appSession, Screenname screenname,
            TrustPreferences trustPreferences) {
        this.appSession = appSession;
        this.screenname = screenname;
        this.trustPreferences = trustPreferences;
    }

    public AppSession getAppSession() { return appSession; }

    public Screenname getScreenname() { return screenname; }


    public AimConnection openConnection(AimConnectionProperties props) {
        closeConnection();
        AimConnection conn = new AimConnection(this,
                getTrustPreferences(), props);
        synchronized(this) {
            this.connection = conn;
        }
        return conn;
    }

    public synchronized AimConnection getConnection() { return connection; }

    public void closeConnection() {
        AimConnection conn = getConnection();
        if (conn != null) conn.disconnect();
    }

    public TrustPreferences getTrustPreferences() { return trustPreferences; }
}
