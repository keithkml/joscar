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
import net.kano.joscar.tlv.AbstractTlvChain;
import net.kano.joscar.tlv.ImmutableTlvChain;

/**
 * An SSI item object representing a buddy on the user's buddy list. A buddy
 * item contains a set of buddy alert flags (for the different types of alerts),
 * an alert sound filename, a buddy comment, and (though WinAIM does not yet
 * support it) an "alias" or "display name" for the buddy.
 */
public class BuddyItem extends AbstractItem {
    /**
     * An alert action flag indicating that a window should be popped up when
     * the buddy alert is activated.
     */
    public static final int MASK_ACTION_POPUP = 0x01;
    /**
     * An alert action flag indicating that a sound should be played when the
     * buddy alert is activated. The sound file is specified in {@link
     * #getAlertSound getAlertSound}.
     */
    public static final int MASK_ACTION_PLAY_SOUND = 0x02;

    /**
     * An alert flag indicating that the buddy's alert should be activated when
     * he or she signs on.
     */
    public static final int MASK_WHEN_ONLINE = 0x01;
    /**
     * An alert flag indicating that the buddy's alert should be activated when
     * he or she comes back from being idle.
     */
    public static final int MASK_WHEN_UNIDLE = 0x02;
    /**
     * An alert flag indicating that the buddy's alert should be activated when
     * he or she comes back from being away.
     */
    public static final int MASK_WHEN_UNAWAY = 0x04;

    /** A TLV type containing the user's "alias," or "display name." */
    private static final int TYPE_ALIAS = 0x0131;
    /** A TLV type containing the user's "buddy comment." */
    private static final int TYPE_COMMENT = 0x013c;
    /**
     * A TLV type containing the filename of a sound to play when an alert for
     * this buddy is activated.
     */
    private static final int TYPE_ALERT_SOUND = 0x013e;
    /** A TLV type containing a set of buddy alert flags. */
    private static final int TYPE_ALERT_FLAGS = 0x13d;

    /** The buddy's screenname. */
    private final String sn;
    /** The ID of the parent group of this buddy. */
    private final int parent;
    /** The ID of this buddy in its parent group. */
    private final int id;

    /** The buddy's "alias." */
    private final String alias;
    /** The buddy's buddy comment. */
    private final String comment;

    /** A bit mask for what to do when an alert is activated. */
    private final int alertActionMask;
    /** A bit mask for when to activate a buddy alert for this buddy. */
    private final int alertWhenMask;
    /** A sound to play when an alert is activated. */
    private final String alertSound;

    public static BuddyItem readBuddyItem(SsiItem item) {
        String sn = item.getName();

        int parent = item.getParentId();
        int id = item.getSubId();

        AbstractTlvChain chain = ImmutableTlvChain.readChain(item.getData());

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

        return new BuddyItem(sn, parent, id, alias, comment, alertWhenMask,
                alertActionMask, alertSound, extraTlvs);
    }

    public BuddyItem(BuddyItem other) {
        this(other.sn, other.parent, other.id, other.alias, other.comment,
                other.alertWhenMask, other.alertActionMask, other.alertSound,
                other.copyExtraTlvs());
    }

    public BuddyItem(String sn, int parent, int id, String alias,
            String comment, int alertWhenMask, int alertActionMask,
            String alertSound, AbstractTlvChain extraTlvs) {
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
