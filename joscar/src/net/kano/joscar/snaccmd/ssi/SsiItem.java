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
import net.kano.joscar.LiveWritable;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

public class SsiItem implements LiveWritable, Serializable {
    public static final int TYPE_BUDDY = 0x0000;
    public static final int TYPE_GROUP = 0x0001;
    public static final int TYPE_PERMIT = 0x0002;
    public static final int TYPE_DENY = 0x0003;
    public static final int TYPE_PRIVACY = 0x0004;
    public static final int TYPE_VISIBILITY = 0x0005;
    public static final int TYPE_ICON_INFO = 0x0014;

    private final String name;
    private final int groupid;
    private final int buddyid;
    private final int type;
    private final ByteBlock data;
    private transient final int totalSize;

    protected static SsiItem readSsiItem(ByteBlock block) {
        if (block.getLength() < 10) return null;

        int nameLen = BinaryTools.getUShort(block, 0);
        if (block.getLength() < 2 + nameLen) return null;

        ByteBlock nameBlock = block.subBlock(2, nameLen);
        String name = BinaryTools.getAsciiString(nameBlock);

        ByteBlock rest = block.subBlock(2 + nameLen);
        if (rest.getLength() < 8) return null;

        int groupid = BinaryTools.getUShort(rest, 0);
        int buddyid = BinaryTools.getUShort(rest, 2);
        int type = BinaryTools.getUShort(rest, 4);

        int datalen = BinaryTools.getUShort(rest, 6);
        if (rest.getLength() < 1 + datalen) return null;

        ByteBlock data = rest.subBlock(8, datalen);

        int size = data.getOffset() + data.getLength() - block.getOffset();

        return new SsiItem(name, groupid, buddyid, type, data, size);
    }

    protected SsiItem(String name, int groupid, int buddyid, int type,
            ByteBlock data, int totalSize) {
        this.name = name;
        this.groupid = groupid;
        this.buddyid = buddyid;
        this.type = type;
        this.data = data;
        this.totalSize = totalSize;
    }

    public SsiItem(String name, int groupid, int buddyid, int type,
            ByteBlock data) {
        this(name, groupid, buddyid, type, data, -1);
    }

    public final String getName() {
        return name;
    }

    public final int getGroupId() {
        return groupid;
    }

    public final int getBuddyId() {
        return buddyid;
    }

    public final int getItemType() {
        return type;
    }

    public final ByteBlock getData() {
        return data;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void write(OutputStream out) throws IOException {
        byte[] namebytes = BinaryTools.getAsciiBytes(name);
        BinaryTools.writeUShort(out, namebytes.length);
        out.write(namebytes);

        BinaryTools.writeUShort(out, groupid);
        BinaryTools.writeUShort(out, buddyid);
        BinaryTools.writeUShort(out, type);

        BinaryTools.writeUShort(out, data.getLength());
        data.write(out);
    }

    public String toString() {
        return "SsiItem '" + name + "', type=0x" + Integer.toHexString(type)
                + ", group 0x" + Integer.toHexString(groupid) + ", buddy 0x"
                + Integer.toHexString(buddyid);
    }
}
