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

import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.config.BuddyCertificateInfo;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.DirInfo;
import net.kano.joscar.snaccmd.icbm.OldIconHashInfo;

import java.beans.PropertyChangeSupport;
import java.util.Date;
import java.util.Iterator;

public final class BuddyInfo {
    public static final String PROP_CERTIFICATE_INFO = "certificateInfo";
    public static final String PROP_ONLINE = "online";
    public static final String PROP_DIRECTORY_INFO = "dirInfo";
    public static final String PROP_ONLINE_SINCE = "onlineSince";
    public static final String PROP_AWAY = "away";
    public static final String PROP_CAPABILITIES = "capabilities";
    public static final String PROP_IDLE_SINCE = "idleSince";
    public static final String PROP_WARNING_LEVEL = "warningLevel";
    public static final String PROP_AWAY_MESSAGE = "awayMessage";
    public static final String PROP_USER_PROFILE = "userProfile";
    public static final String PROP_OLD_ICON_INFO = "oldIconInfo";
    public static final String PROP_LAST_AIM_EXPRESSION = "lastAimExpression";
    public static final String PROP_SUPPORTS_TYPING_NOTIFICATIONS
            = "supportsTypingNotifications";
    public static final String PROP_WANTS_OUR_ICON = "wantsOurIcon";

    private static final CapabilityBlock[] CAPS_EMPTY = new CapabilityBlock[0];
    
    private final Screenname screenname;

    private BuddyCertificateInfo certificateInfo = null;
    private boolean online = true;
    private DirInfo directoryInfo = null;
    private Date onlineSince = null;
    private boolean away = false;
    private CapabilityBlock[] capabilities = CAPS_EMPTY;
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
    private CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();

    public BuddyInfo(Screenname screenname) {
        DefensiveTools.checkNull(screenname, "screenname");

        this.screenname = screenname;
    }

    public void addPropertyListener(BuddyInfoChangeListener l) {
        pcs.addPropertyChangeListener(l);
        listeners.add(l);
    }

    public void removePropertyListener(BuddyInfoChangeListener l) {
        pcs.removePropertyChangeListener(l);
        listeners.remove(l);
    }

    public Screenname getScreenname() { return screenname; }

    void setCertificateInfo(BuddyCertificateInfo certificateInfo) {
        BuddyCertificateInfo old;
        synchronized (this) {
            old = this.certificateInfo;
            this.certificateInfo = certificateInfo;
        }
        fireObjectChange(PROP_CERTIFICATE_INFO, old, certificateInfo);
    }

    public synchronized BuddyCertificateInfo getCertificateInfo() {
        return certificateInfo;
    }

    void setOnline(boolean online) {
        boolean old;
        synchronized (this) {
            old = this.online;
            this.online = online;
        }
        pcs.firePropertyChange(PROP_ONLINE, old, online);
    }

    public synchronized boolean isOnline() { return online; }

    void setDirectoryInfo(DirInfo directoryInfo) {
        DirInfo old;
        synchronized (this) {
            old = this.directoryInfo;
            this.directoryInfo = directoryInfo;
        }
        fireObjectChange(PROP_DIRECTORY_INFO, old, directoryInfo);
    }

    public synchronized DirInfo getDirectoryInfo() { return directoryInfo; }

    void setOnlineSince(Date onlineSince) {
        Date old;
        synchronized (this) {
            old = this.onlineSince;
            this.onlineSince = onlineSince;
        }
        fireObjectChange(PROP_ONLINE_SINCE, old, onlineSince);
    }

    public synchronized Date getOnlineSince() { return onlineSince; }

    void setAway(boolean away) {
        boolean old;
        synchronized (this) {
            old = this.away;
            this.away = away;
        }
        pcs.firePropertyChange(PROP_AWAY, old, away);
    }

    public synchronized boolean isAway() { return away; }

    void setCapabilities(CapabilityBlock[] capabilities) {
        DefensiveTools.checkNull(capabilities, "capabilities");

        CapabilityBlock[] cloned = (CapabilityBlock[]) capabilities.clone();
        CapabilityBlock[] old;
        synchronized (this) {
            old = this.capabilities;
            this.capabilities = cloned;
        }
        pcs.firePropertyChange(PROP_CAPABILITIES, old, capabilities);
    }

    public synchronized CapabilityBlock[] getCapabilities() {
        return (CapabilityBlock[]) capabilities.clone();
    }

    void setIdleSince(Date idleSince) {
        Date old;
        synchronized (this) {
            old = this.idleSince;
            this.idleSince = idleSince;
        }
        fireObjectChange(PROP_IDLE_SINCE, old, idleSince);
    }

    public synchronized Date getIdleSince() { return idleSince; }

    void setWarningLevel(int warningLevel) {
        int old;
        synchronized (this) {
            old = this.warningLevel;
            this.warningLevel = warningLevel;
        }
        pcs.firePropertyChange(PROP_WARNING_LEVEL, old, warningLevel);
    }

    public synchronized int getWarningLevel() { return warningLevel; }

    void setAwayMessage(String awayMessage) {
        String old;
        synchronized (this) {
            old = this.awayMessage;
            this.awayMessage = awayMessage;
        }
        fireObjectChange(PROP_AWAY_MESSAGE, old, awayMessage);
    }

    public synchronized String getAwayMessage() { return awayMessage; }

    void setUserProfile(String userProfile) {
        String old;
        synchronized (this) {
            old = this.userProfile;
            this.userProfile = userProfile;
        }
        fireObjectChange(PROP_USER_PROFILE, old, userProfile);
    }

    public synchronized String getUserProfile() { return userProfile; }

    void setOldIconInfo(OldIconHashInfo oldIconInfo) {
        OldIconHashInfo old;
        synchronized(this) {
            old = this.oldIconInfo;
            this.oldIconInfo = oldIconInfo;
        }
        fireObjectChange(PROP_OLD_ICON_INFO, old, oldIconInfo);
    }

    public synchronized OldIconHashInfo getOldIconInfo() { return oldIconInfo; }

    void setLastAimExpression(String lastAimExpression) {
        String old;
        synchronized(this) {
            old = this.lastAimExpression;
            this.lastAimExpression = lastAimExpression;
        }
        fireObjectChange(PROP_LAST_AIM_EXPRESSION, old, lastAimExpression);
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
        pcs.firePropertyChange(PROP_SUPPORTS_TYPING_NOTIFICATIONS, old,
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
        pcs.firePropertyChange(PROP_WANTS_OUR_ICON, old, wantsOurIcon);
    }

    public synchronized boolean wantsOurIcon() { return wantsOurIcon; }

    void receivedBuddyStatusUpdate() {
        assert !Thread.holdsLock(this);

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            BuddyInfoChangeListener listener = (BuddyInfoChangeListener) it.next();
            listener.receivedBuddyStatusUpdate(this);
        }
    }

    public synchronized boolean isCertificateInfoCurrent() {
        BuddyCertificateInfo certInfo = certificateInfo;
        return certInfo == null || certInfo.isUpToDate();
    }

    private void fireObjectChange(String property,
            Object oldval, Object newval) {
        if (oldval != newval) pcs.firePropertyChange(property, oldval, newval);
    }
}
