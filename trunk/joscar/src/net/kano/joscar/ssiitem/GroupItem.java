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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class GroupItem extends AbstractItem {
    private static final int BUDDYID_DEFAULT = 0x0000;

    private static final int TYPE_BUDDIES = 0x00c8;

    private final String name;
    private final int id;
    private final int[] buddies;

    public static GroupItem readGroupItem(SsiItem item) {
        String name = item.getName();

        int id = item.getParentId();

        TlvChain chain = TlvChain.readChain(item.getData());

        Tlv buddiesTlv = chain.getLastTlv(TYPE_BUDDIES);

        int[] buddies = null;
        if (buddiesTlv != null) {
            ByteBlock buddyBlock = buddiesTlv.getData();

            buddies = new int[buddyBlock.getLength() / 2];

            for (int i = 0; i < buddies.length; i++) {
                buddies[i] = BinaryTools.getUShort(buddyBlock, i*2);
            }
        }

        MutableTlvChain extraTlvs = new MutableTlvChain(chain);

        extraTlvs.removeTlvs(new int[] { TYPE_BUDDIES });

        return new GroupItem(name, id, buddies, extraTlvs);
    }

    public GroupItem(GroupItem other) {
        this(other.name, other.id,
                other.buddies == null ? null : (int[]) other.buddies.clone(),
                other.copyExtraTlvs());
    }

    public GroupItem(String name, int id) {
        this(name, id, null, null);
    }

    public GroupItem(String name, int id, int[] buddies) {
        this(name, id, buddies, null);
    }

    public GroupItem(String name, int id, int[] buddies, TlvChain extraTlvs) {
        super(extraTlvs);
        this.name = name;
        this.id = id;
        this.buddies = buddies;
    }

    public final String getGroupName() {
        return name;
    }

    public final int getId() {
        return id;
    }

    public final int[] getBuddies() {
        return buddies;
    }

    public SsiItem getSsiItem() {
        MutableTlvChain chain = new MutableTlvChain();

        if (buddies != null && buddies.length > 0) {
            ByteArrayOutputStream out
                    = new ByteArrayOutputStream(buddies.length * 2);

            try {
                for (int i = 0; i < buddies.length; i++) {
                    BinaryTools.writeUShort(out, buddies[i]);
                }
            } catch (IOException impossible) { }

            ByteBlock tlvData = ByteBlock.wrap(out.toByteArray());
            chain.addTlv(new Tlv(TYPE_BUDDIES, tlvData));
        }

        return generateItem(name, id, BUDDYID_DEFAULT, SsiItem.TYPE_GROUP,
                chain);
    }

    public String toString() {
        String buddyStr;
        if (buddies != null) {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < buddies.length; i++) {
                buffer.append("0x");
                buffer.append(Integer.toHexString(buddies[i]));
                buffer.append(", ");
            }
            buddyStr = buffer.toString();
        } else {
            buddyStr = "none";
        }
        return "GroupItem for " + name + ", groupid=0x"
                + Integer.toHexString(id) + ", buddies: " + buddyStr;
    }
}
