/*
 *  Copyright (c) 2002, The Joust Project
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
 *  File created by keith @ Apr 12, 2003
 *
 */

package net.kano.joscartests;

import net.kano.joscar.snaccmd.FullUserInfo;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Date;

public class OnlineUserInfo {
//    private static Image aimIcon, awayIcon;
//    private static ColorConvertOp grayscaler = new ColorConvertOp(
//            ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

    static {
//        ClassLoader loader = OnlineUserInfo.class.getClassLoader();
//        Toolkit toolkit = Toolkit.getDefaultToolkit();
//        URL aimIconUrl = loader.getResource("images/icons/aim.png");
//        URL awayIconUrl = loader.getResource("images/icons/away.png");
//        aimIcon = toolkit.createImage(aimIconUrl);
//        awayIcon = toolkit.createImage(awayIconUrl);
    }

    private boolean online = false;
    private FullUserInfo userInfo = null;
    private Date idleSince = null;
//    private Image icon = null;

    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
//    private Icon imageIcon = null;

    public synchronized void addPropertyChangeListener(
            PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public synchronized void removePropertyChangeListener(
            PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
//
//    public synchronized void setIcon(Image image) {
//        Image old = this.icon;
//        this.icon = image;
//
//        updateImageIcon();
//
//        pcs.firePropertyChange("icon", old, this.icon);
//    }

    public synchronized void setOnline(boolean online) {
        boolean old = this.online;
        this.online = online;

        updateImageIcon();
        pcs.firePropertyChange("online", old, online);
    }

    public synchronized void setUserInfo(FullUserInfo info) {
        if (info == null) throw new NullPointerException();

        FullUserInfo old = userInfo;
        userInfo = info;
        int idleMins = userInfo.getIdleMins();
        Date oldIdleSince = idleSince;
        if (idleMins > 0) {
            idleSince = new Date(System.currentTimeMillis()
                    - (idleMins*60*1000));
        } else {
            idleSince = null;
        }

        updateImageIcon();

        pcs.firePropertyChange("away", old == null ? false
                : isAway(old.getAwayStatus()),
                isAway(userInfo.getAwayStatus()));
        pcs.firePropertyChange("idleSince", oldIdleSince, idleSince);
    }

    private synchronized void updateImageIcon() {
//        imageIcon = genImageIcon(this);
    }

//    public static Icon genImageIcon(OnlineUserInfo info) {
//        Image icon = info.getIcon();
//        if (icon == null) icon = aimIcon;
//        Image scaledIcon = icon.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
//        int width = 32;
//        int height = 32;
//        BufferedImage bi = null;
//        if (info.isAway()) {
//            BufferedImage img = new BufferedImage(width, height,
//                    BufferedImage.TYPE_4BYTE_ABGR);
//            Graphics g = img.getGraphics();
//            g.drawImage(scaledIcon, 0, 0, width, height, null);
//            int aw = awayIcon.getWidth(null);
//            int ah = awayIcon.getHeight(null);
//            g.drawImage(awayIcon, width-aw, height-ah, aw, ah, null);
//            bi = img;
//        }
//        if (info.getIdleSince() != null) {
//            if (bi == null) {
//                bi = new BufferedImage(width, height,
//                        BufferedImage.TYPE_4BYTE_ABGR);
//                bi.getGraphics().drawImage(scaledIcon, 0, 0, width, height,
//                        null);
//            }
//
//            bi = grayscaler.filter(bi, null);
//        }
//        if (bi != null) scaledIcon = bi;
//        ImageIcon imageIcon = new ImageIcon(scaledIcon);
//        return imageIcon;
//    }

//    public synchronized Icon getImageIcon() { return imageIcon; }

    public synchronized String getScreenname() {
        return userInfo == null ? null : userInfo.getScreenname();
    }
    public synchronized boolean isOnline() { return online; }
    private static boolean isAway(Boolean status) {
        return status != null && status.booleanValue();
    }
    public synchronized boolean isAway() {
        return userInfo != null && isAway(userInfo.getAwayStatus());
    }
    public synchronized boolean isIdle() {
        return userInfo != null && userInfo.getIdleMins() > 0;
    }
    public synchronized Date getIdleSince() { return idleSince; }
//    public synchronized Image getIcon() { return icon; }
}
