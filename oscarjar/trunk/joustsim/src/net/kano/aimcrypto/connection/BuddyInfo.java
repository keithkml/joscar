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

package net.kano.aimcrypto.connection;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.snaccmd.DirInfo;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.icbm.OldIconHashInfo;
import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.BuddySecurityInfo;

import java.util.Date;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

public final class BuddyInfo {
    private Screenname screenname;
    private BuddySecurityInfo securityInfo = null;
    private boolean online = true;
    private DirInfo dirInfo = null;
    private Date onlineSince = null;
    private boolean away = false;
    private CapabilityBlock[] capabilities = null;
    private ByteBlock certificateInfoHash = null;
    private Date idleSince = null;
    private int warningLevel = -1;
    private String awayMessage = null;
    private String userProfile = null;

    // icbm info
    private OldIconHashInfo oldIconInfo = null;
    private String lastAimExpression = null;
    private boolean supportsTypingNotifications = false;
    private boolean wantsOurIcon = false;

    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public BuddyInfo(Screenname screenname) {
        DefensiveTools.checkNull(screenname, "screenname");

        this.screenname = screenname;
    }

    public void addPropertyListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    public synchronized Screenname getScreenname() { return screenname; }

    void setSecurityInfo(BuddySecurityInfo securityInfo) {
        BuddySecurityInfo old;
        synchronized (this) {
            old = this.securityInfo;
            this.securityInfo = securityInfo;
        }
        pcs.firePropertyChange("securityInfo", old, securityInfo);
    }

    public synchronized boolean isSecurityInfoCurrent() {
        ByteBlock advHash = certificateInfoHash;
        if (securityInfo == null) {
            if (advHash == null) return true;
            else return false;
        }
        ByteBlock storedHash = securityInfo.getCertificateInfoHash();

        return (advHash == null && storedHash == null) || (storedHash != null
                && storedHash.equals(advHash));
    }

    public synchronized BuddySecurityInfo getSecurityInfo() {
        return securityInfo;
    }

    void setOnline(boolean online) {
        boolean old;
        synchronized (this) {
            old = this.online;
            this.online = online;
        }
        pcs.firePropertyChange("online", old, online);
    }

    public synchronized boolean isOnline() { return online; }

    void setDirInfo(DirInfo dirInfo) {
        DirInfo old;
        synchronized (this) {
            old = this.dirInfo;
            this.dirInfo = dirInfo;
        }
        pcs.firePropertyChange("dirInfo", old, dirInfo);
    }

    public synchronized DirInfo getDirInfo() { return dirInfo; }

    void setOnlineSince(Date onlineSince) {
        Date old;
        synchronized (this) {
            old = this.onlineSince;
            this.onlineSince = onlineSince;
        }
        pcs.firePropertyChange("onlineSince", old, onlineSince);
    }

    public synchronized Date getOnlineSince() { return onlineSince; }

    void setAway(boolean away) {
        boolean old;
        synchronized (this) {
            old = this.away;
            this.away = away;
        }
        pcs.firePropertyChange("away", old, away);
    }

    public synchronized boolean isAway() { return away; }

    void setCapabilities(CapabilityBlock[] capabilities) {
        CapabilityBlock[] old;
        synchronized (this) {
            old = this.capabilities;
            this.capabilities = (CapabilityBlock[]) capabilities.clone();
        }
        pcs.firePropertyChange("capabilities", old, capabilities);
    }

    public synchronized CapabilityBlock[] getCapabilities() {
        return (CapabilityBlock[]) capabilities.clone();
    }

    void setCertificateInfoHash(ByteBlock certificateInfoHash) {
        ByteBlock old;
        synchronized (this) {
            old = this.certificateInfoHash;
            this.certificateInfoHash = certificateInfoHash;
        }
        pcs.firePropertyChange("certificateInfoHash", old, certificateInfoHash);
    }

    public synchronized ByteBlock getCertificateInfoHash() {
        return certificateInfoHash;
    }

    void setIdleSince(Date idleSince) {
        Date old;
        synchronized (this) {
            old = this.idleSince;
            this.idleSince = idleSince;
        }
        pcs.firePropertyChange("idleSince", old, idleSince);
    }

    public synchronized Date getIdleSince() { return idleSince; }

    void setWarningLevel(int warningLevel) {
        int old;
        synchronized (this) {
            old = this.warningLevel;
            this.warningLevel = warningLevel;
        }
        pcs.firePropertyChange("warningLevel", old, warningLevel);
    }

    public synchronized int getWarningLevel() { return warningLevel; }

    void setAwayMessage(String awayMessage) {
        String old;
        synchronized (this) {
            old = this.awayMessage;
            this.awayMessage = awayMessage;
        }
        pcs.firePropertyChange("awayMessage", old, awayMessage);
    }

    public synchronized String getAwayMessage() { return awayMessage; }

    void setUserProfile(String userProfile) {
        String old;
        synchronized (this) {
            old = this.userProfile;
            this.userProfile = userProfile;
        }
        pcs.firePropertyChange("userProfile", old, userProfile);
    }

    public synchronized String getUserProfile() { return userProfile; }

    void setOldIconInfo(OldIconHashInfo oldIconInfo) {
        OldIconHashInfo old;
        synchronized(this) {
            old = this.oldIconInfo;
            this.oldIconInfo = oldIconInfo;
        }
        pcs.firePropertyChange("oldIconInfo", old, oldIconInfo);
    }

    public synchronized OldIconHashInfo getOldIconInfo() { return oldIconInfo; }

    void setLastAimExpression(String lastAimExpression) {
        String old;
        synchronized(this) {
            old = this.lastAimExpression;
            this.lastAimExpression = lastAimExpression;
        }
        pcs.firePropertyChange("lastAimExpression", old, lastAimExpression);
    }

    public synchronized String getLastAimExpression() {
        return lastAimExpression;
    }

    void setSupportsTypingNotifications(boolean supportsTypingNotifications) {
        boolean old;
        synchronized (this) {
            old = this.supportsTypingNotifications;
            this.supportsTypingNotifications = supportsTypingNotifications;
        }
        pcs.firePropertyChange("supportsTypingNotifications", old,
                supportsTypingNotifications);
    }

    public synchronized boolean supportsTypingNotifications() {
        return supportsTypingNotifications;
    }

    void setWantsOurIcon(boolean wantsOurIcon) {
        boolean old;
        synchronized (this) {
            old = this.wantsOurIcon;
            this.wantsOurIcon = wantsOurIcon;
        }
        pcs.firePropertyChange("wantsOurIcon", old, wantsOurIcon);
    }

    public synchronized boolean wantsOurIcon() { return wantsOurIcon; }
}
