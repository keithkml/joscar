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

public class PrivacyItem extends AbstractItem {
    public static final int MODE_ALLOW_ALL = 0x01;
    public static final int MODE_BLOCK_ALL = 0x02;
    public static final int MODE_ALLOW_PERMITS = 0x03;
    public static final int MODE_BLOCK_DENIES = 0x04;
    public static final int MODE_ALLOW_BUDDIES = 0x05;

    public static final long VISMASK_HIDE_WIRELESS = 0x00000002L;

    private static final String NAME_DEFAULT = "";
    private static final int GROUPID_DEFAULT = 0x0000;

    private static final int TYPE_PRIVACY_MODE = 0x00ca;
    private static final int TYPE_CLASS_MASK = 0x00cb;
    private static final int TYPE_VISIBILE_MASK = 0x00cc;

    private final int id;
    private final int privacyMode;
    private final long classMask;
    private final long visibleMask;

    public static PrivacyItem readPrivacyItem(SsiItem item) {
        TlvChain chain = TlvChain.readChain(item.getData());

        Tlv typeTlv = chain.getLastTlv(TYPE_PRIVACY_MODE);
        int type = -1;
        if (typeTlv != null) {
            type = BinaryTools.getUByte(typeTlv.getData(), 0);
        }

        Tlv classMaskTlv = chain.getLastTlv(TYPE_CLASS_MASK);
        long classMask = -1;
        if (classMaskTlv != null) {
            classMask = BinaryTools.getUInt(classMaskTlv.getData(), 0);
        }

        Tlv visibileMaskTlv = chain.getLastTlv(TYPE_VISIBILE_MASK);
        long visibleMask = -1;
        if (visibileMaskTlv != null) {
            visibleMask = BinaryTools.getUInt(visibileMaskTlv.getData(), 0);
        }

        MutableTlvChain extraTlvs = new MutableTlvChain(chain);

        extraTlvs.removeTlvs(new int[] {
            TYPE_PRIVACY_MODE, TYPE_CLASS_MASK, TYPE_VISIBILE_MASK
        });

        return new PrivacyItem(item.getBuddyId(), type, classMask, visibleMask,
                extraTlvs);
    }

    public PrivacyItem(PrivacyItem other) {
        this(other.id, other.privacyMode, other.classMask,
                other.visibleMask, other.copyExtraTlvs());
    }

    public PrivacyItem(int id, int mode, long classMask, long visibleMask,
            TlvChain extraTlvs) {
        super(extraTlvs);
        this.id = id;
        this.privacyMode = mode;
        this.classMask = classMask;
        this.visibleMask = visibleMask;
    }

    public PrivacyItem(int id, int mode, long classMask, long visibleMask) {
        this(id, mode, classMask, visibleMask, null);
    }

    public final int getId() { return id; }

    public final int getPrivacyMode() {
        return privacyMode;
    }

    public final long getClassMask() {
        return classMask;
    }

    public final long getVisibleMask() {
        return visibleMask;
    }

    public SsiItem getSsiItem() {
        MutableTlvChain chain = new MutableTlvChain();

        if (privacyMode != -1) {
            chain.addTlv(new Tlv(TYPE_PRIVACY_MODE,
                    ByteBlock.wrap(BinaryTools.getUByte(privacyMode))));
        }
        if (classMask != -1) {
            chain.addTlv(Tlv.getUIntInstance(TYPE_CLASS_MASK, classMask));
        }
        if (visibleMask != -1) {
            chain.addTlv(Tlv.getUIntInstance(TYPE_VISIBILE_MASK, visibleMask));
        }

        return generateItem(NAME_DEFAULT, GROUPID_DEFAULT, id,
                SsiItem.TYPE_PRIVACY, chain);
    }

    public String toString() {
        return "PrivacyItem: id=0x" + Integer.toHexString(id)
                + ", mode=" + privacyMode
                + ", classMask=0x" + Long.toHexString(classMask)
                + ", visMask=0x" + Long.toHexString(visibleMask);
    }
}
