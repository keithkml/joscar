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

package net.kano.aimcrypto.connection.oscar;

import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.connection.oscar.loginstatus.AuthFailureInfo;
import net.kano.aimcrypto.connection.oscar.loginstatus.ClosedEarlyFailureInfo;
import net.kano.aimcrypto.connection.oscar.loginstatus.DisconnectedFailureInfo;
import net.kano.aimcrypto.connection.oscar.loginstatus.FlapErrorFailureInfo;
import net.kano.aimcrypto.connection.oscar.loginstatus.LoginFailureInfo;
import net.kano.aimcrypto.connection.oscar.loginstatus.LoginSuccessInfo;
import net.kano.aimcrypto.connection.oscar.loginstatus.SnacErrorFailureInfo;
import net.kano.aimcrypto.connection.oscar.loginstatus.TimeoutFailureInfo;
import net.kano.aimcrypto.connection.oscar.service.LoginService;
import net.kano.aimcrypto.connection.oscar.service.Service;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flap.FlapCommand;
import net.kano.joscar.flap.FlapPacketEvent;
import net.kano.joscar.flapcmd.CloseFlapCmd;
import net.kano.joscar.flapcmd.FlapErrorCmd;
import net.kano.joscar.flapcmd.LoginFlapCmd;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snaccmd.auth.AuthRequest;
import net.kano.joscar.snaccmd.auth.AuthResponse;
import net.kano.joscar.snaccmd.auth.ClientVersionInfo;
import net.kano.joscar.snaccmd.auth.KeyRequest;
import net.kano.joscar.snaccmd.auth.KeyResponse;
import net.kano.joscar.snaccmd.auth.AuthCommand;
import net.kano.joscar.snaccmd.error.SnacError;

import java.util.Timer;
import java.util.TimerTask;

public class LoginConnection extends OscarConnection {
    public static final int TIMEOUT_DEFAULT = 15;

    private int timeoutSecs = TIMEOUT_DEFAULT;
    private Timer timer = new Timer(true);

    public LoginConnection(String host, int port) {
        super(host, port);
    }

    public synchronized void setTimeoutSecs(int timeoutSecs) {
        checkFieldModify();
        DefensiveTools.checkRange(timeoutSecs, "timeoutSecs", 0);

        this.timeoutSecs = timeoutSecs;
    }

    public synchronized int getTimeout() {
        return timeoutSecs;
    }

    private void cancelLogin() {
        LoginService ls = getLoginService();
        if (ls != null) ls.timeout(getTimeout());
        disconnect();
    }

    protected void beforeConnect() {
        setSnacFamilies(new int[] { AuthCommand.FAMILY_AUTH });
        int secs = getTimeout();
        timer.schedule(new TimerTask() {
            public void run() {
                cancelLogin();
            }
        }, secs * 1000);
    }

    protected void disconnected() {
        timer.cancel();
    }

    public LoginService getLoginService() {
        Service service = getService(AuthCommand.FAMILY_AUTH);
        if (service instanceof LoginService) return (LoginService) service;
        else return null;
    }
}
