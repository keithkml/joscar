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
 *  File created by keith @ Mar 2, 2003
 *
 */

package net.kano.joscar.snaccmd;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.LiveWritable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents "extra icon information," as I call it, which contains a single
 * result code and an <code>IconHashInfo</code>. This structure is used in
 * various SNAC commands.
 */
public final class ExtraIconInfo implements LiveWritable {
    /**
     * Some sort of result code.
     */
    private final int extraCode;

    /**
     * The embedded icon hash information.
     */
    private final IconHashInfo iconInfo;

    /**
     * The total size of this structure, if read from a data block with
     * <code>readExtraIconInfo</code>.
     */
    private final int totalSize;

    /**
     * Reads a series of <code>ExtraIconInfo</code>s from the given block. The
     * total number of bytes read is the sum of calling
     * <code>getTotalSize</code> on each element of the returned array.
     *
     * @param block the block containing zero or more extra icon info blocks
     * @return a list of zero or more extra icon info objects read from the
     *         given data block
     */
    public static ExtraIconInfo[] readExtraIconInfos(ByteBlock block) {
        List infos = new LinkedList();

        for (;;) {
            ExtraIconInfo info = readExtraIconInfo(block);
            if (info == null) break;

            infos.add(info);

            block = block.subBlock(info.getTotalSize());
        }

        return (ExtraIconInfo[]) infos.toArray(new ExtraIconInfo[0]);
    }

    /**
     * Reads an extra icon info block from the given data block.
     *
     * @param block the data block from which to read
     * @return an extra icon info object read from the given data block, or
     *         <code>null</code> if no valid object could be read
     */
    public static ExtraIconInfo readExtraIconInfo(ByteBlock block) {
        if (block.getLength() < 2) return null;

        int code = BinaryTools.getUShort(block, 0);

        ByteBlock hashBlock = block.subBlock(2);
        IconHashInfo iconInfo = IconHashInfo.readIconHashInfo(hashBlock);

        int size = 2;
        if (iconInfo != null) size += iconInfo.getTotalSize();

        return new ExtraIconInfo(code, iconInfo, size);
    }

    /**
     * Creates a new extra icon info object with the given properties.
     *
     * @param code the "result code" associated with this extra icon info object
     * @param iconInfo the icon has information associated with this object
     * @param totalSize the total size of this object, if read from a block
     *        of data
     */
    protected ExtraIconInfo(int code, IconHashInfo iconInfo, int totalSize) {
        this.extraCode = code;
        this.iconInfo = iconInfo;
        this.totalSize = totalSize;
    }

    /**
     * Creates an extra icon information object with the given result code and
     * icon hash information block. As of this writing the significance of
     * the result code is not known.
     *
     * @param code a result code
     * @param iconInfo an icon hash information block
     */
    public ExtraIconInfo(int code, IconHashInfo iconInfo) {
        this(code, iconInfo, -1);
    }

    /**
     * Returns some sort of result code associated with this object's icon
     * hash. It appears that this "code" might in fact be the "name" given to
     * the icon when storing it in one's server-stored information (buddy icons
     * are named such that the {@linkplain IconHashInfo#HASH_SPECIAL "no icon"
     * block} is icon "0" and subsequent icons are labelled "1", "2", and so on.
     *  Watch this space for updates.
     *
     * @return some sort of code associated with this object's associated icon
     *         information
     */
    public final int getExtraCode() {
        return extraCode;
    }

    /**
     * The icon hash information embedded in this object.
     *
     * @return this object's associated icon hash information
     */
    public final IconHashInfo getIconHashInfo() {
        return iconInfo;
    }

    /**
     * Returns the total size, in bytes, of this object. Will be <code>-1</code>
     * if this object was not read using <code>readExtraIconInfo</code> or
     * <code>readExtraIconInfos</code>.
     *
     * @return the total size, in bytes, of this object, if read from a data
     *         block
     */
    public final int getTotalSize() {
        return totalSize;
    }

    public void write(OutputStream out) throws IOException {
        BinaryTools.writeUShort(out, extraCode);
        if (iconInfo != null) iconInfo.write(out);
    }

    public String toString() {
        return "ExtraIconInfo: code=0x" + Long.toHexString(extraCode)
                + ", iconInfo=<" + iconInfo + ">";
    }
}
