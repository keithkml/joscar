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

package net.kano.joustsim.oscar;

import net.kano.joustsim.Screenname;
import net.kano.joscar.DefensiveTools;

public class AimConnectionProperties {
    public static final String HOST_DEFAULT = "login.oscar.aol.com";
    public static final int PORT_DEFAULT = 5190;

    private String loginHost = HOST_DEFAULT;
    private int loginPort = PORT_DEFAULT;
    private Screenname screenname = null;
    private String pass = null;

    public AimConnectionProperties(Screenname sn, String pass) {
        this.screenname = sn;
        this.pass = pass;
    }

    public Screenname getScreenname() { return screenname; }

    public void setScreenname(Screenname sn) {
        DefensiveTools.checkNull(sn, "sn");
        this.screenname = sn;
    }

    public String getLoginHost() { return loginHost; }

    public void setLoginHost(String loginHost) {
        DefensiveTools.checkNull(loginHost, "loginHost");
        this.loginHost = loginHost;
    }

    public int getLoginPort() { return loginPort; }

    public void setLoginPort(int loginPort) {
        DefensiveTools.checkRange(loginPort, "loginPort", 0);
        this.loginPort = loginPort;
    }

    public String getPass() { return pass; }

    public void setPass(String pass) {
        DefensiveTools.checkNull(pass, "pass");
        this.pass = pass;
    }

    public boolean isComplete() {
        return loginHost != null
                && loginPort >= 0
                && screenname != null
                && pass != null;
    }
}
