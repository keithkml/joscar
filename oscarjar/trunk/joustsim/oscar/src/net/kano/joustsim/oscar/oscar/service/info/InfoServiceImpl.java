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
 *  File created by keith @ Jan 25, 2004
 *
 */

package net.kano.joustsim.oscar.oscar.service.info;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.CertificateInfo;
import net.kano.joscar.snaccmd.DirInfo;
import net.kano.joscar.snaccmd.InfoData;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joscar.snaccmd.loc.LocCommand;
import net.kano.joscar.snaccmd.loc.SetInfoCmd;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.CapabilityHandler;
import net.kano.joustsim.oscar.CapabilityListener;
import net.kano.joustsim.oscar.CapabilityManager;
import net.kano.joustsim.oscar.CapabilityManagerListener;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.AbstractService;
import net.kano.joustsim.trust.BuddyCertificateInfo;

import java.util.List;

public class InfoServiceImpl extends AbstractService implements
    MutableInfoService {
  private static final CertificateInfo CERTINFO_EMPTY
      = new CertificateInfo(null);

  private CopyOnWriteArrayList<InfoServiceListener> listeners
      = new CopyOnWriteArrayList<InfoServiceListener>();

  private final InfoResponseListener infoRequestListener
      = new InfoResponseAdapter() {
    public void handleUserProfile(InfoService service, Screenname buddy,
        String userInfo) {
      assert InfoServiceImpl.this == service;

      for (InfoServiceListener listener : listeners) {
        listener.handleUserProfile(service, buddy, userInfo);
      }
    }

    public void handleAwayMessage(InfoService service, Screenname buddy,
        String awayMessage) {
      assert InfoServiceImpl.this == service;

      for (InfoServiceListener listener : listeners) {
        listener.handleAwayMessage(service, buddy, awayMessage);
      }
    }

    public void handleCertificateInfo(InfoService service, Screenname buddy,
        BuddyCertificateInfo certInfo) {
      assert InfoServiceImpl.this == service;

      for (InfoServiceListener listener : listeners) {
        listener.handleCertificateInfo(service, buddy, certInfo);
      }
    }

    public void handleDirectoryInfo(InfoService service, Screenname buddy,
        DirInfo dirInfo) {
      assert InfoServiceImpl.this == service;

      for (InfoServiceListener listener : listeners) {
        listener.handleDirectoryInfo(service, buddy, dirInfo);
      }
    }
  };
  private final CapabilityManager capabilityManager;
  private CapabilityListener individualCapListener = new CapabilityListener() {
    public void capabilityEnabled(CapabilityHandler handler, boolean enabled) {
      updateCaps();
    }
  };
  private final CapabilityManagerListener capListener
      = new CapabilityManagerListener() {
    public void capabilityHandlerAdded(CapabilityManager manager,
        CapabilityBlock block, CapabilityHandler handler) {
      handler.addCapabilityListener(individualCapListener);
      updateCaps();
    }

    public void capabilityHandlerRemoved(CapabilityManager manager,
        CapabilityBlock block, CapabilityHandler handler) {
      handler.removeCapabilityListener(individualCapListener);
      updateCaps();
    }
  };

  private InfoRequestManager profileRequestManager
      = new UserProfileRequestManager(this);
  private InfoRequestManager awayMsgRequestManager
      = new AwayMessageRequestManager(this);
  private InfoRequestManager certInfoRequestManager
      = new CertificateInfoRequestManager(this);
  private InfoRequestManager dirInfoRequestManager
      = new DirectoryInfoRequestManager(this);

  private String awayMessage = null;
  private String userProfile = null;
  private CertificateInfo certificateInfo = null;

  public InfoServiceImpl(AimConnection aimConnection,
      OscarConnection oscarConnection) {
    super(aimConnection, oscarConnection, LocCommand.FAMILY_LOC);

    capabilityManager = getAimConnection().getCapabilityManager();
    capabilityManager.addCapabilityListener(capListener);
  }

  public SnacFamilyInfo getSnacFamilyInfo() {
    return LocCommand.FAMILY_INFO;
  }

  public void connected() {
    InfoData infoData;
    synchronized (this) {
      List<CapabilityBlock> caps = capabilityManager.getEnabledCapabilities();
      infoData = new InfoData(awayMessage, userProfile, caps, certificateInfo);
    }
    sendSnac(new SetInfoCmd(infoData));

    setReady();
  }

  protected void finishUp() {
    capabilityManager.removeCapabilityListener(capListener);
  }

  public void addInfoListener(InfoServiceListener l) {
    listeners.addIfAbsent(l);
  }

  public void removeInfoListener(InfoServiceListener l) {
    listeners.remove(l);
  }

  public synchronized String getLastSetAwayMessage() { return awayMessage; }

  //TODO(klea): find max info length, throw exception if too long
  //            or return the truncated version or something
  public void setAwayMessage(String awayMessage) {
    synchronized (this) {
      // we don't want to waste time checking the strings for equality,
      // but we can check the references for equality to catch some
      // unnecessary cases
      //noinspection StringEquality
      if (this.awayMessage == awayMessage) return;

      this.awayMessage = awayMessage;
    }
    sendSnac(new SetInfoCmd(new InfoData(
        null, awayMessage == null ? InfoData.NOT_AWAY : awayMessage,
        null, null)));
  }

  public synchronized String getLastSetUserProfile() { return userProfile; }

  public void setUserProfile(String userProfile) {
    DefensiveTools.checkNull(userProfile, "userProfile");

    synchronized (this) {
      // we don't want to waste time checking the strings for equality,
      // but we can check the references for equality to catch some
      // unnecessary cases
      //noinspection StringEquality
      if (this.userProfile == userProfile) return;

      this.userProfile = userProfile;
    }
    sendSnac(new SetInfoCmd(new InfoData(
        userProfile == null ? "" : userProfile,
        null, null, null)));
  }

  public synchronized CertificateInfo getCurrentCertificateInfo() {
    return certificateInfo;
  }

  public void setCertificateInfo(CertificateInfo certificateInfo) {
    synchronized (this) {
      // we don't want to waste time checking the info blocks for equality
      // but we can check the references for equality to catch some
      // unnecessary cases
      if (this.certificateInfo == certificateInfo) return;

      this.certificateInfo = certificateInfo;
    }
    sendSnac(new SetInfoCmd(new InfoData(null, null, null,
        certificateInfo == null ? CERTINFO_EMPTY : certificateInfo)));
  }

  public void requestUserProfile(Screenname buddy) {
    profileRequestManager.request(buddy);
  }

  public void requestUserProfile(Screenname buddy,
      InfoResponseListener listener) {
    profileRequestManager.request(buddy, listener);
  }

  public void requestAwayMessage(Screenname buddy) {
    awayMsgRequestManager.request(buddy);
  }

  public void requestAwayMessage(Screenname buddy,
      InfoResponseListener listener) {
    awayMsgRequestManager.request(buddy, listener);
  }

  public void requestCertificateInfo(Screenname buddy) {
    certInfoRequestManager.request(buddy);
  }

  public void requestCertificateInfo(Screenname buddy,
      InfoResponseListener listener) {
    certInfoRequestManager.request(buddy, listener);
  }

  public void requestDirectoryInfo(Screenname buddy) {
    dirInfoRequestManager.request(buddy);
  }

  public void requestDirectoryInfo(Screenname buddy,
      InfoResponseListener listener) {
    dirInfoRequestManager.request(buddy, listener);
  }

  private void updateCaps() {
    sendSnac(new SetInfoCmd(new InfoData(null, null,
        capabilityManager.getEnabledCapabilities(), null)));
  }

  public InfoResponseListener getInfoRequestListener() {
    return infoRequestListener;
  }
}
