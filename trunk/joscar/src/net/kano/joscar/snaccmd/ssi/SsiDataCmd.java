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

package net.kano.joscar.snaccmd.ssi;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.flapcmd.SnacPacket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class SsiDataCmd extends SsiCommand {
    public static final int VERSION_DEFAULT = 0x00;

    private final int version;
    private final SsiItem[] items;
    private final long lastmod;

    protected SsiDataCmd(SnacPacket packet) {
        super(CMD_SSI_DATA);

        ByteBlock snacData = packet.getData();

        version = BinaryTools.getUByte(snacData, 0);

        int itemCount = BinaryTools.getUShort(snacData, 1);

        List itemList = new LinkedList();

        ByteBlock block = snacData.subBlock(3);

        for (int i = 0; i < itemCount; i++) {
            SsiItem item = SsiItem.readSsiItem(block);
            if (item == null) break;

            itemList.add(item);

            block = block.subBlock(item.getTotalSize());
        }

        items = (SsiItem[]) itemList.toArray(new SsiItem[0]);

        lastmod = BinaryTools.getUInt(block, 0);
    }

    public SsiDataCmd(int version, SsiItem[] items, long lastmod) {
        super(CMD_SSI_DATA);

        this.version = version;
        this.items = items;
        this.lastmod = lastmod;
    }

    public final int getSsiVersion() {
        return version;
    }

    public final SsiItem[] getItems() {
        return items;
    }

    public final long getLastModDate() {
        return lastmod;
    }

    public void writeData(OutputStream out) throws IOException {
        BinaryTools.writeUByte(out, version);
        BinaryTools.writeUShort(out, items.length);
        for (int i = 0; i < items.length; i++) {
            items[i].write(out);
        }
        BinaryTools.writeUInt(out, lastmod);
    }

    public String toString() {
        return "SsiDataCmd (ssi version=" + version + "): " + items.length
                + " items, modified " + new Date(lastmod * 1000);
    }
}
