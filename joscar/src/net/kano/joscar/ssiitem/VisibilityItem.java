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
 *  File created by keith @ Mar 30, 2003
 *
 */

package net.kano.joscar.ssiitem;

import net.kano.joscar.snaccmd.ssi.SsiItem;
import net.kano.joscar.tlv.MutableTlvChain;
import net.kano.joscar.tlv.TlvChain;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;

public class VisibilityItem extends AbstractItem {
    public static final long MASK_SHOW_IDLE_TIME = 0x00000400L;
    public static final long MASK_SHOW_TYPING =    0x00400000L;

    private static final int TYPE_VIS_MASK = 0x00c9;

    private static final String NAME_DEFAULT = "";
    private static final int GROUPID_DEFAULT = 0x0000;

    private final int id;
    private final long flags;

    public static VisibilityItem readVisiblityItem(SsiItem item) {
        int id = item.getSubId();

        TlvChain chain = TlvChain.readChain(item.getData());

        Tlv visMaskTlv = chain.getLastTlv(TYPE_VIS_MASK);
        long flags = -1;
        if (visMaskTlv != null) flags = visMaskTlv.getDataAsUInt();

        MutableTlvChain extras = new MutableTlvChain(chain);
        extras.removeTlvs(new int[] { TYPE_VIS_MASK });

        return new VisibilityItem(id, flags, extras);
    }

    public VisibilityItem(VisibilityItem other) {
        this(other.id, other.flags, other.copyExtraTlvs());
    }

    public VisibilityItem(int id, long flags) {
        this(id, flags, null);
    }

    public VisibilityItem(int id, long flags, TlvChain extraTlvs) {
        super(extraTlvs);
        this.id = id;
        this.flags = flags;
    }

    public final int getId() { return id; }

    public final long getVisFlags() { return flags; }

    public SsiItem getSsiItem() {
        MutableTlvChain chain = new MutableTlvChain();

        if (flags != -1) {
            byte[] flagBytes = BinaryTools.getUInt(flags);
            ByteBlock flagsBlock = ByteBlock.wrap(flagBytes);
            chain.addTlv(new Tlv(TYPE_VIS_MASK, flagsBlock));
        }

        return generateItem(NAME_DEFAULT, GROUPID_DEFAULT, id,
                SsiItem.TYPE_VISIBILITY, chain);
    }

    public String toString() {
        return "VisibilityItem: id=0x" + Integer.toHexString(id) + ", flags=0x"
                + Long.toHexString(flags);
    }
}
