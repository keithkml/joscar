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
 *  File created by keith @ Mar 3, 2003
 *
 */

package net.kano.joscar.ssiitem;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.snaccmd.ssi.SsiItem;
import net.kano.joscar.tlv.MutableTlvChain;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;

public class BuddyItem extends AbstractItem {
    public static final int MASK_ACTION_POPUP = 0x01;
    public static final int MASK_ACTION_PLAY_SOUND = 0x02;

    public static final int MASK_WHEN_ONLINE = 0x01;
    public static final int MASK_WHEN_UNIDLE = 0x02;
    public static final int MASK_WHEN_UNAWAY = 0x04;

    private static final int TYPE_ALIAS = 0x0131;
    private static final int TYPE_COMMENT = 0x013c;
    private static final int TYPE_ALERT_SOUND = 0x013e;
    private static final int TYPE_ALERT_FLAGS = 0x13d;

    private final String sn;
    private final int parent;
    private final int id;

    private final String alias;
    private final String comment;

    private final int alertActionMask;
    private final int alertWhenMask;
    private final String alertSound;

    public static BuddyItem readBuddyItem(SsiItem item) {
        String sn = item.getName();

        int parent = item.getGroupId();
        int id = item.getBuddyId();

        TlvChain chain = TlvChain.readChain(item.getData());

        String alias = chain.getString(TYPE_ALIAS);
        String comment = chain.getString(TYPE_COMMENT);
        String alertSound = chain.getString(TYPE_ALERT_SOUND);

        Tlv alertTlv = chain.getLastTlv(TYPE_ALERT_FLAGS);

        int alertActionMask = -1;
        int alertWhenMask = -1;
        if (alertTlv != null) {
            ByteBlock alertMaskData = alertTlv.getData();

            alertActionMask = BinaryTools.getUByte(alertMaskData, 0);
            alertWhenMask = BinaryTools.getUByte(alertMaskData, 1);
        }

        MutableTlvChain extraTlvs = new MutableTlvChain(chain);

        extraTlvs.removeTlvs(new int[] {
            TYPE_ALIAS, TYPE_COMMENT, TYPE_ALERT_SOUND, TYPE_ALERT_FLAGS
        });

        return new BuddyItem(sn, parent, id, alias, comment, alertWhenMask, alertActionMask,
                alertSound, extraTlvs);
    }

    public BuddyItem(BuddyItem other) {
        this(other.sn, other.parent, other.id, other.alias, other.comment,
                other.alertWhenMask, other.alertActionMask, other.alertSound,
                other.copyExtraTlvs());
    }

    public BuddyItem(String sn, int parent, int id, String alias,
            String comment, int alertWhenMask, int alertActionMask,
            String alertSound, TlvChain extraTlvs) {
        super(extraTlvs);

        this.sn = sn;
        this.parent = parent;
        this.id = id;
        this.alias = alias;
        this.comment = comment;
        this.alertActionMask = alertActionMask;
        this.alertWhenMask = alertWhenMask;
        this.alertSound = alertSound;
    }

    public BuddyItem(String sn, int parent, int id, String alias,
            String comment, int alertWhenMask, int alertActionMask,
            String alertSound) {
        this(sn, parent, id, alias, comment, alertWhenMask, alertActionMask,
                alertSound, null);
    }

    public BuddyItem(String sn, int parent, int id) {
        this(sn, parent, id, null, null, 0, 0, null);
    }

    public final String getScreenname() {
        return sn;
    }

    public final int getParent() {
        return parent;
    }

    public final int getId() {
        return id;
    }

    public final String getAlias() {
        return alias;
    }

    public final String getComment() {
        return comment;
    }

    public final int getAlertActionMask() {
        return alertActionMask;
    }

    public final int getAlertWhenMask() {
        return alertWhenMask;
    }

    public final String getAlertSound() {
        return alertSound;
    }

    public SsiItem getSsiItem() {
        MutableTlvChain chain = new MutableTlvChain();

        if (alias != null) {
            chain.addTlv(Tlv.getStringInstance(TYPE_ALIAS, alias));
        }
        if (comment != null) {
            chain.addTlv(Tlv.getStringInstance(TYPE_COMMENT, comment));
        }
        if (alertActionMask != -1 && alertWhenMask != -1) {
            // this is the most elegant statement I've ever written.
            ByteBlock block = ByteBlock.wrap(new byte[] {
                BinaryTools.getUByte(alertActionMask)[0],
                BinaryTools.getUByte(alertWhenMask)[0]
            });
            chain.addTlv(new Tlv(TYPE_ALERT_FLAGS, block));
        }
        if (alertSound != null) {
            chain.addTlv(Tlv.getStringInstance(TYPE_ALERT_SOUND, alertSound));
        }

        return generateItem(sn, parent, id, SsiItem.TYPE_BUDDY, chain);
    }

    public String toString() {
        boolean popupAlert = false, alertOnSignon = false,
                alertOnUnidle = false, alertOnBack = false, playSound = false;

        if (alertActionMask != -1) {
            // the first byte contains what kind of popup..
            popupAlert = (alertActionMask & MASK_ACTION_POPUP) != 0;
            playSound = (alertActionMask & MASK_ACTION_PLAY_SOUND) != 0;
        }

        if (alertWhenMask != -1) {
            // the second contains when to pop up
            alertOnSignon = (alertWhenMask & MASK_WHEN_ONLINE) != 0;
            alertOnUnidle = (alertWhenMask & MASK_WHEN_UNIDLE) != 0;
            alertOnBack = (alertWhenMask & MASK_WHEN_UNAWAY) != 0;
        }

        return "BuddyItem for " + sn + " (buddy 0x" + Integer.toHexString(id)
                + " in group 0x" + Integer.toHexString(parent) + "): alias="
                + alias + ", comment=\"" + comment + "\", alerts: "
                + (popupAlert ? "[popup alert] " : "")
                + (playSound ? "[play " + alertSound + "] " : "")
                + (alertOnSignon ? "[on signon] " : "")
                + (alertOnUnidle ? "[on unidle] " : "")
                + (alertOnBack ? "[on unaway] " : "");
    }
}
