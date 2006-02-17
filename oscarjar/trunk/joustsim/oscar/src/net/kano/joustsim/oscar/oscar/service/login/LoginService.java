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
 *  File created by keith @ Jan 15, 2004
 *
 */

package net.kano.joustsim.oscar.oscar.service.login;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flap.FlapCommand;
import net.kano.joscar.flap.FlapPacketEvent;
import net.kano.joscar.flapcmd.CloseFlapCmd;
import net.kano.joscar.flapcmd.FlapErrorCmd;
import net.kano.joscar.flapcmd.LoginFlapCmd;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snaccmd.auth.AuthCommand;
import net.kano.joscar.snaccmd.auth.AuthRequest;
import net.kano.joscar.snaccmd.auth.AuthResponse;
import net.kano.joscar.snaccmd.auth.ClientVersionInfo;
import net.kano.joscar.snaccmd.auth.KeyRequest;
import net.kano.joscar.snaccmd.auth.KeyResponse;
import net.kano.joscar.snaccmd.auth.SecuridRequest;
import net.kano.joscar.snaccmd.auth.SecuridResponse;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joscar.snaccmd.error.SnacError;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.LoginServiceListener;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.loginstatus.AuthFailureInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.ClosedEarlyFailureInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.DisconnectedFailureInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.FlapErrorFailureInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.LoginFailureInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.LoginSuccessInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.NoSecuridFailure;
import net.kano.joustsim.oscar.oscar.loginstatus.SnacErrorFailureInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.TimeoutFailureInfo;
import net.kano.joustsim.oscar.oscar.service.Service;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public class LoginService extends Service {
  private static final Logger logger = Logger
      .getLogger(LoginService.class.getName());

  public static final ClientVersionInfo VERSIONINFO_WINAIM
      = new ClientVersionInfo(
      "AOL Instant Messenger, version 5.5.3415/WIN32",
      -1, 5, 5, 0, 3415, 239);
  public static final ClientVersionInfo VERSIONINFO_ICHAT
      = new ClientVersionInfo("Apple iChat", 0x311a, 1, 0, 0, 0x0184, 0xc6);

  private final Screenname screenname;
  private final String password;
  private ClientVersionInfo versionInfo = VERSIONINFO_ICHAT;

  private CopyOnWriteArrayList<LoginServiceListener> listeners
      = new CopyOnWriteArrayList<LoginServiceListener>();

  private boolean notified = false;
  private volatile @Nullable SecuridProvider securidProvider = null;

  public LoginService(AimConnection aimConnection,
      OscarConnection oscarConnection, Screenname screenname,
      String password) {
    super(aimConnection, oscarConnection, AuthCommand.FAMILY_AUTH);

    DefensiveTools.checkNull(screenname, "screenname");
    DefensiveTools.checkNull(password, "password");

    this.screenname = screenname;
    this.password = password;

    setReady();
  }

  public SnacFamilyInfo getSnacFamilyInfo() {
    return AuthCommand.FAMILY_INFO;
  }

  public void addLoginListener(LoginServiceListener l) {
    listeners.addIfAbsent(l);
  }

  public void removeLoginListener(LoginServiceListener l) {
    listeners.remove(l);
  }

  public synchronized ClientVersionInfo getVersionInfo() { return versionInfo; }

  public synchronized void setVersionInfo(ClientVersionInfo versionInfo) {
    this.versionInfo = versionInfo;
  }

  public SecuridProvider getSecuridProvider() {
    return securidProvider;
  }

  public void setSecuridProvider(SecuridProvider securidProvider) {
    this.securidProvider = securidProvider;
  }

  private void fireLoginSucceeded(LoginSuccessInfo info) {
    logger.fine("Login process succeeded: " + info);

    synchronized (this) {
      if (notified) return;
      notified = true;
    }
    for (LoginServiceListener listener : listeners) {
      listener.loginSucceeded(info);
    }
    setFinished();
  }

  private void fireLoginFailed(LoginFailureInfo info) {
    logger.fine("Login failed: " + info.getClass().getName()
        + ": " + info);

    synchronized (this) {
      if (notified) return;
      notified = true;
    }
    for (LoginServiceListener listener : listeners) {
      listener.loginFailed(info);
    }
    setFinished();
  }

  public void timeout(int timeout) {
    if (!getNotified()) {
      fireLoginFailed(new TimeoutFailureInfo(timeout));
    }
  }

  private synchronized boolean getNotified() { return notified; }

  public void connected() {
    logger.fine("Sending key request");

    sendFlap(new LoginFlapCmd());
    sendSnac(new KeyRequest(screenname.getFormatted()));
  }

  protected void finishUp() {
    if (!getNotified()) {
      fireLoginFailed(
          new DisconnectedFailureInfo(getAimConnection().wantedDisconnect()));
    }
  }

  public void handleFlapPacket(FlapPacketEvent flapPacketEvent) {
    FlapCommand flap = flapPacketEvent.getFlapCommand();

    if (flap instanceof CloseFlapCmd) {
      CloseFlapCmd fc = (CloseFlapCmd) flap;
      fireLoginFailed(new ClosedEarlyFailureInfo(fc));

    } else if (flap instanceof FlapErrorCmd) {
      FlapErrorCmd fe = (FlapErrorCmd) flap;
      fireLoginFailed(new FlapErrorFailureInfo(fe));
    }
  }

  public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
    SnacCommand snac = snacPacketEvent.getSnacCommand();

    if (snac instanceof KeyResponse) {
      KeyResponse kr = (KeyResponse) snac;

      logger.fine("Sending authorization request");

      sendSnac(new AuthRequest(screenname.getFormatted(), password,
          getVersionInfo(), kr.getKey()));

    } else if (snac instanceof AuthResponse) {
      AuthResponse ar = (AuthResponse) snac;
      if (ar.getErrorCode() != -1) {
        fireLoginFailed(new AuthFailureInfo(ar));
      } else {
        fireLoginSucceeded(new LoginSuccessInfo(ar));
      }
    } else if (snac instanceof SecuridRequest) {
      final SecuridProvider provider = securidProvider;
      if (provider == null) {
        fireLoginFailed(new NoSecuridFailure());

      } else {
        // we start a new thread so this method can block, because it will
        // likely pop up a dialog for the user
        Thread thread = new Thread(new Runnable() {
          public void run() {
            String securid = provider.getSecurid();
            if (securid == null) {
              fireLoginFailed(new NoSecuridFailure());

            } else {
              sendSnac(new SecuridResponse(securid));
            }
          }
        });
        thread.setDaemon(true);
        thread.start();
      }

    } else if (snac instanceof SnacError) {
      SnacError se = (SnacError) snac;
      fireLoginFailed(new SnacErrorFailureInfo(se));
    }
  }
}
