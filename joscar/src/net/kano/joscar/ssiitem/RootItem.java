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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RootItem extends AbstractItem {
    private static final String NAME_DEFAULT = "";
    private static final int GROUPID_DEFAULT = 0x0000;
    private static final int BUDDYID_DEFAULT = 0x0000;

    private static final int TYPE_GROUPIDS = 0x00c8;

    private final int[] groupids;

    public static RootItem readRootItem(SsiItem item) {
        AbstractTlvChain chain = ImmutableTlvChain.readChain(item.getData());

        Tlv groupTlv = chain.getLastTlv(TYPE_GROUPIDS);

        int[] groupids = null;
        if (groupTlv != null) {
            ByteBlock groupBlock = groupTlv.getData();

            groupids = new int[groupBlock.getLength() / 2];

            for (int i = 0; i < groupids.length; i++) {
                groupids[i] = BinaryTools.getUShort(groupBlock, i*2);
            }
        }

        MutableTlvChain extraTlvs = new MutableTlvChain(chain);

        extraTlvs.removeTlvs(new int[] { TYPE_GROUPIDS });

        return new RootItem(groupids, extraTlvs);
    }

    public RootItem(RootItem other) {
        this(other.groupids == null ? null : (int[]) other.groupids.clone(),
                other.copyExtraTlvs());
    }

    public RootItem() {
        this(null, null);
    }

    public RootItem(int[] groupids) {
        this(groupids, null);
    }

    public RootItem(int[] groupids, AbstractTlvChain extraTlvs) {
        super(extraTlvs);

        this.groupids = groupids;
    }

    public final int[] getGroupids() {
        return groupids;
    }

    public SsiItem getSsiItem() {
        MutableTlvChain chain = new MutableTlvChain();

        if (groupids != null && groupids.length > 0) {
            ByteArrayOutputStream out
                = new ByteArrayOutputStream(groupids.length * 2);

            try {
                for (int i = 0; i < groupids.length; i++) {
                    BinaryTools.writeUShort(out, groupids[i]);
                }
            } catch (IOException impossible) { }

            ByteBlock tlvData = ByteBlock.wrap(out.toByteArray());

            chain.addTlv(new Tlv(TYPE_GROUPIDS, tlvData));
        }

        return generateItem(NAME_DEFAULT, GROUPID_DEFAULT, BUDDYID_DEFAULT,
                SsiItem.TYPE_GROUP, chain);
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < groupids.length; i++) {
            buffer.append("0x");
            buffer.append(Integer.toHexString(groupids[i]));
            buffer.append(", ");
        }

        return "RootItem with groupids: " + buffer.toString();
    }
}
