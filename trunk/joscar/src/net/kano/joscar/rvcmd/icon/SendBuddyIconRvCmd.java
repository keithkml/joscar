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
 *  File created by keith @ Apr 28, 2003
 *
 */

package net.kano.joscar.rvcmd.icon;

import net.kano.joscar.rvcmd.AbstractRequestRvCmd;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.snaccmd.icbm.OldIconHashInfo;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.LiveWritable;
import net.kano.joscar.BinaryTools;

import java.io.OutputStream;
import java.io.IOException;

public class SendBuddyIconRvCmd extends AbstractRequestRvCmd {
    public static final String ICONIDSTRING_DEFAULT = "AVT1picture.id";

    private final OldIconHashInfo hash;
    private final ByteBlock iconData;
    private final LiveWritable iconWriter;
    private final String iconId;

    public SendBuddyIconRvCmd(RecvRvIcbm icbm) {
        super(icbm);

        iconWriter = null;

        ByteBlock serviceData = getServiceData();

        OldIconHashInfo hash = null;
        ByteBlock iconData = null;
        String iconId = null;
        if (serviceData != null) {
            hash = OldIconHashInfo.readIconHashFromRvData(serviceData);
            if (hash != null) {
                int hashLen = hash.getRvDataFormatLength();
                int iconSize = (int) hash.getIconSize();

                if (serviceData.getLength() >= hashLen + iconSize) {
                    iconData = serviceData.subBlock(hashLen, iconSize);

                    ByteBlock iconIdBlock = serviceData.subBlock(hashLen
                            + iconSize);
                    iconId = BinaryTools.getAsciiString(iconIdBlock);
                }
            }
        }

        this.hash = hash;
        this.iconData = iconData;
        this.iconId = iconId;
    }

    public SendBuddyIconRvCmd(OldIconHashInfo hash,
            LiveWritable iconDataWriter) {
        this(hash, iconDataWriter, ICONIDSTRING_DEFAULT);
    }

    public SendBuddyIconRvCmd(OldIconHashInfo hash, LiveWritable iconDataWriter,
            String iconIdString) {
        super(CapabilityBlock.BLOCK_ICON);

        DefensiveTools.checkNull(hash, "hash");
        DefensiveTools.checkNull(iconDataWriter, "iconDataWriter");

        this.hash = hash;
        this.iconData = null;
        this.iconWriter = iconDataWriter;
        this.iconId = iconIdString;
    }

    public final OldIconHashInfo getIconHash() { return hash; }

    public final ByteBlock getIconData() { return iconData; }

    public final String getIconIdString() { return iconId; }

    protected void writeRvTlvs(OutputStream out) throws IOException { }

    protected void writeServiceData(OutputStream out) throws IOException {
        hash.writeToRvData(out);

        if (iconWriter != null) iconWriter.write(out);
        else iconData.write(out);

        if (iconId != null) out.write(BinaryTools.getAsciiBytes(iconId));
    }

    public String toString() {
        return "SendBuddyIconRvCmd: hash=" + hash + ", icon="
                + (iconData == null ? -1 : iconData.getLength()) + " bytes (id="
                + iconId + ")";
    }
}
