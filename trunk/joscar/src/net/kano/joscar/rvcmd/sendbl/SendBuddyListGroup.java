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
 *  File created by keith @ Apr 27, 2003
 *
 */

package net.kano.joscar.rvcmd.sendbl;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.LiveWritable;
import net.kano.joscar.snaccmd.StringBlock;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SendBuddyListGroup implements LiveWritable {
    public static SendBuddyListGroup[] readBuddyListGroups(ByteBlock block) {
        List groups = new LinkedList();
        for (;;) {
            SendBuddyListGroup group = readBuddyListGroup(block);

            if (group == null) break;

            groups.add(group);

            block = block.subBlock(group.getTotalSize());
        }

        return (SendBuddyListGroup[]) groups.toArray(new SendBuddyListGroup[0]);
    }

    public static SendBuddyListGroup readBuddyListGroup(ByteBlock block) {
        DefensiveTools.checkNull(block, "block");

        StringBlock groupName = readString(block);

        if (groupName == null
                || block.getLength() < groupName.getTotalSize() + 2) {
            return null;
        }

        int buddyCount = BinaryTools.getUShort(block, groupName.getTotalSize());

        String[] buddies = new String[buddyCount];
        ByteBlock rest = block.subBlock(groupName.getTotalSize() + 2);

        int size = groupName.getTotalSize() + 2;

        for (int i = 0; i < buddies.length; i++) {
            StringBlock buddyString = readString(rest);

            if (buddyString == null) return null;

            buddies[i] = buddyString.getString();

            rest = rest.subBlock(buddyString.getTotalSize());
            size += buddyString.getTotalSize();
        }

        return new SendBuddyListGroup(groupName.getString(), buddies, size);
    }

    private static final StringBlock readString(ByteBlock block) {
        if (block.getLength() < 2) return null;

        int len = BinaryTools.getUShort(block, 0);

        if (block.getLength() < len + 2) return null;

        String str = BinaryTools.getAsciiString(block.subBlock(2, len));

        return new StringBlock(str, 2 + len);
    }

    private static final void writeString(OutputStream out, String str)
            throws IOException {
        byte[] bytes = BinaryTools.getAsciiBytes(str);

        BinaryTools.writeUShort(out, bytes.length);
        out.write(bytes);
    }

    private final String groupName;
    private final String[] buddies;
    private final int totalSize;

    private SendBuddyListGroup(String groupName, String[] buddies,
            int totalSize) {
        this.groupName = groupName;
        this.buddies = (String[]) buddies.clone();
        this.totalSize = totalSize;
    }

    public SendBuddyListGroup(String groupName, String[] buddies) {
        DefensiveTools.checkNull(groupName, "groupName");
        DefensiveTools.checkNull(buddies, "buddies");

        for (int i = 0; i < buddies.length; i++) {
            DefensiveTools.checkNull(buddies[i], "buddies[] elements");
        }

        this.groupName = groupName;
        this.buddies = (String[]) buddies.clone();
        totalSize = -1;
    }

    public final String getGroupName() { return groupName; }

    public final String[] getBuddies() {
        return (String[]) buddies.clone();
    }

    public final int getTotalSize() { return totalSize; }

    public void write(OutputStream out) throws IOException {
        writeString(out, groupName);
        BinaryTools.writeUShort(out, buddies.length);
        for (int i = 0; i < buddies.length; i++) {
            writeString(out, buddies[i]);
        }
    }

    public String toString() {
        return "SendBuddyListGroup for group '" + groupName + "': "
                + Arrays.asList(buddies);
    }
}
