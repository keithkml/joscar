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

import net.kano.joscar.ByteBlock;
import net.kano.joscar.snaccmd.IconHashInfo;
import net.kano.joscar.snaccmd.ssi.SsiItem;
import net.kano.joscar.tlv.MutableTlvChain;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.AbstractTlvChain;
import net.kano.joscar.tlv.ImmutableTlvChain;

public class IconItem extends AbstractItem {
    public static final String NAME_DEFAULT = "1";
    public static final String ALIAS_DEFAULT = "";

    private static final int GROUPID_DEFAULT = 0x0000;

    private static final int TYPE_ICON_HASH = 0x00d5;
    private static final int TYPE_ALIAS = 0x0131;

    private final String name;
    private final int id;
    private final IconHashInfo iconInfo;
    private final String alias;

    public static IconItem readIconItem(SsiItem item) {
        String name = item.getName();

        AbstractTlvChain chain = ImmutableTlvChain.readChain(item.getData());

        Tlv iconTlv = chain.getLastTlv(TYPE_ICON_HASH);

        IconHashInfo iconInfo = null;
        if (iconTlv != null) {
            ByteBlock block = iconTlv.getData();

            iconInfo = IconHashInfo.readIconHashInfo(block);
        }

        String alias = chain.getString(TYPE_ALIAS);

        MutableTlvChain extraTlvs = new MutableTlvChain(chain);

        extraTlvs.removeTlvs(new int[] { TYPE_ICON_HASH });

        return new IconItem(name, item.getSubId(), iconInfo, alias,
                extraTlvs);
    }

    public IconItem(IconItem other) {
        this(other.name, other.id, other.iconInfo, other.alias,
                other.copyExtraTlvs());
    }

    public IconItem(String name, int id, IconHashInfo info) {
        this(name, id, info, ALIAS_DEFAULT, null);
    }

    public IconItem(String name, int id, IconHashInfo iconInfo, String alias,
            AbstractTlvChain extraTlvs) {
        super(extraTlvs);
        this.name = name;
        this.id = id;
        this.iconInfo = iconInfo;
        this.alias = alias;
    }

    public final String getName() {
        return name;
    }

    public final int getId() { return id; }

    public final IconHashInfo getIconInfo() {
        return iconInfo;
    }

    public final String getAlias() { return alias; }

    public SsiItem getSsiItem() {
        MutableTlvChain chain = new MutableTlvChain();

        if (iconInfo != null) {
            ByteBlock iconData = ByteBlock.createByteBlock(iconInfo);
            chain.addTlv(new Tlv(TYPE_ICON_HASH, iconData));
        }

        if (alias != null) {
            chain.addTlv(Tlv.getStringInstance(TYPE_ALIAS, alias));
        }

        return generateItem(name, GROUPID_DEFAULT, id, SsiItem.TYPE_ICON_INFO,
                chain);
    }

    public String toString() {
        return "IconItem: name=" + name + ", id=0x" + Integer.toHexString(id)
                + ", alias='" + alias + "', iconinfo=" + iconInfo;
    }
}
