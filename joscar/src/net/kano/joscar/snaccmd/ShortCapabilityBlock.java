/*
 *  Copyright (c) 2003, The Joust Project
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
 *  File created by keith @ Aug 11, 2003
 *
 */

package net.kano.joscar.snaccmd;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.Writable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

public class ShortCapabilityBlock implements Writable {
    public static ShortCapabilityBlock[] readShortCaps(ByteBlock block) {
        DefensiveTools.checkNull(block, "block");

        List caps = new LinkedList();

        for (int i = 0; i < block.getLength(); i += 2) {
            caps.add(new ShortCapabilityBlock(block.subBlock(i, 2)));
        }

        return (ShortCapabilityBlock[])
                caps.toArray(new ShortCapabilityBlock[0]);
    }

    public static boolean couldBeShortBlock(CapabilityBlock cap) {
        ByteBlock block = cap.getBlock();
        return block.subBlock(0, 2).equals(BYTEBLOCK_DEFAULT.subBlock(0, 2))
                && block.subBlock(4).equals(BYTEBLOCK_DEFAULT.subBlock(4));
    }

    public static ShortCapabilityBlock getShortBlock(CapabilityBlock cap) {
        if (!couldBeShortBlock(cap)) {
            throw new IllegalArgumentException("Capability block '" + cap
                    + "' cannot be converted to a short capability block");
        }

        return new ShortCapabilityBlock(cap.getBlock().subBlock(2, 2));
    }

    private static final byte[] BLOCK_DEFAULT = new byte[] {
            0x09, 0x46, 0x00, 0x00, 0x4c, 0x7f, 0x11, (byte) 0xd1,
            (byte) 0x82, 0x22, 0x44, 0x45, 0x53, 0x54, 0x00, 0x00};
    private static final ByteBlock BYTEBLOCK_DEFAULT
            = ByteBlock.wrap(BLOCK_DEFAULT);

    private final ByteBlock data;

    public ShortCapabilityBlock(ByteBlock data) {
        DefensiveTools.checkNull(data, "data");
        if (data.getLength() != 2) {
            throw new IllegalArgumentException("short capability data block "
                    + "must contain only two bytes (it has "
                    + data.getLength() + ")");
        }

        this.data = data;
    }

    public ByteBlock getData() { return data; }

    public CapabilityBlock toCapabilityBlock() {
        byte[] block = (byte[]) BLOCK_DEFAULT.clone();
        System.arraycopy(data.toByteArray(), 0, block, 2, 2);

        return new CapabilityBlock(ByteBlock.wrap(block));
    }

    public long getWritableLength() { return 2; }

    public void write(OutputStream out) throws IOException {
        data.write(out);
    }

    public boolean equals(Object obj) {
        if (obj instanceof ShortCapabilityBlock) {
            ShortCapabilityBlock scb = (ShortCapabilityBlock) obj;

            return scb.data.equals(data);
        } else {
            if (obj instanceof CapabilityBlock) {
                CapabilityBlock cb = (CapabilityBlock) obj;

                return couldBeShortBlock(cb)
                        && getShortBlock(cb).equals(this);
            } else {
                return false;
            }
        }
    }

    public int hashCode() {
        return BinaryTools.getUShort(data, 0);
    }

    public String toString() {
        return "ShortCapabilityBlock: " + BinaryTools.describeData(data);
    }
}
