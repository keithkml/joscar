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

public class SsiDataCheck extends SsiCommand {
    private final long lastmod;
    private final int itemCount;

    protected SsiDataCheck(SnacPacket packet) {
        super(CMD_DATA_CHECK);

        ByteBlock snacData = packet.getData();

        lastmod = BinaryTools.getUInt(snacData, 0);
        itemCount = BinaryTools.getUShort(snacData, 4);
    }

    public SsiDataCheck(long lastmod, int itemCount) {
        super(CMD_DATA_CHECK);

        this.lastmod = lastmod;
        this.itemCount = itemCount;
    }

    public final long getLastmod() {
        return lastmod;
    }

    public final int getItemCount() {
        return itemCount;
    }

    public void writeData(OutputStream out) throws IOException {
        if (lastmod != -1) {
            BinaryTools.writeUInt(out, lastmod);
            if (itemCount != -1) BinaryTools.writeUShort(out, itemCount);
        }
    }

    public String toString() {
        return "SsiDataCheck: lastmod=" + lastmod + ", " + itemCount + " items";
    }
}
