/*
 *  Copyright (c) 2002-2003, The Joust Project
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
 *  File created by keith @ Mar 1, 2003
 *
 */

package net.kano.joscar.snaccmd;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.LiveWritable;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A data structure used to transmit (MD5) hashes of buddy icon files. This
 * structure is used by the new buddy icon system, SNAC family {@link
 * net.kano.joscar.snaccmd.icon 0x10}.
 */
public final class IconHashInfo implements LiveWritable {
    /**
     * A value sent when "setting" an icon by adding or modifying a {@linkplain
     * net.kano.joscar.ssiitem.IconItem buddy icon item}. Note that this code
     * ({@value}) has a different meaning when received from the server; see
     * {@link #CODE_ICON_EMPTY} for that meaning.
     */
    public static final int CODE_SET_ICON = 0x0000;

    /**
     * A value sent by the server in the icon info list in a {@linkplain
     * FullUserInfo user info block} indicating that the user has no icon.
     * The {@linkplain #getIconHash icon hash} is normally {@linkplain
     * #HASH_SPECIAL the "no icon block"} when this code value is present. Note
     * that this value ({@value}) has a different meaning when setting a buddy
     * icon as a client; see {@link #CODE_SET_ICON} for that meaning.
     */
    public static final int CODE_ICON_EMPTY = 0x0000;

    /**
     * A value sent by the server in the icon info list in a {@linkplain
     * FullUserInfo user info block} indicating that the user has an icon, and
     * that the icon hash block represents the MD5 hash of his or her buddy
     * icon.
     */
    public static final int CODE_ICON_PRESENT = 0x0001;

    /**
     * A value sent by the server in a {@linkplain
     * net.kano.joscar.snaccmd.conn.YourIconAck set-icon acknowledgement packet}
     * indicating that the client should upload the buddy icon that it just set
     * to the <code>0x10</code> server.
     */
    public static final int CODE_UPLOAD_ICON_PLS = 0x0040;

    /**
     * A value sent by the server in a {@linkplain
     * net.kano.joscar.snaccmd.conn.YourIconAck set-icon acknowledgement packet}
     * indicating that the client need not upload the buddy icon that it just
     * set. This generally means an "official" AOL icon was set or the server
     * has this icon cached for some other reason.
     */
    public static final int CODE_ALREADY_HAVE_ICON = 0x0081;

    /**
     * A "special" icon hash which appears to indicate that no icon has been
     * set. A block equal to this will be returned by <code>getIconHash</code>
     * in such a case; it can be used as the <code>hash</code> argument to
     * <code>IconHashInfo</code>'s constructor to indicate you are clearing
     * your icon. None of this is fully understood yet, as the <code>0x10</code>
     * service is rather new to OSCAR. Later on it may be discovered that this
     * block may instead hold a totally different meaning, but what was stated
     * above appears to be true thus far.
     */
    public static final ByteBlock HASH_SPECIAL = ByteBlock.wrap(
            new byte[] { 0x02, 0x01, (byte) 0xd2, 0x04, 0x72 });

    /**
     * Reads an icon hash info block from the given data block. The total number
     * of bytes read can be retrieved by calling <code>getTotalSize</code> on
     * the returned object. Note that this will return <code>null</code> if no
     * valid icon hash info block can be read.
     *
     * @param block the block containing an icon hash info block
     * @return an icon hash info object read from the given data block, or
     *         <code>null</code> if no valid block could be read
     */
    public static IconHashInfo readIconHashInfo(ByteBlock block) {
        DefensiveTools.checkNull(block, "block");

        if (block.getLength() < 1) return null;

        int code = BinaryTools.getUByte(block, 0);

        if (block.getLength() < 2) return new IconHashInfo(code, null, 1);

        int len = BinaryTools.getUByte(block, 1);

        if (block.getLength() < len + 2) return new IconHashInfo(code, null, 1);

        ByteBlock hash = block.subBlock(2, len);

        return new IconHashInfo(code, hash, 2 + len);
    }

    /**
     * The code associated with this hash info block.
     */
    private final int code;

    /**
     * The buddy icon MD5 hash.
     */
    private final ByteBlock hash;

    /**
     * The total size of this block, in bytes, as read from a block of binary
     * data.
     */
    private final int totalSize;

    /**
     * Creates a new icon hash info object with the given properties.
     *
     * @param code the icon hash info's code
     * @param hash the MD5 hash of the associated buddy icon
     * @param totalSize the total size of this object, as read from a block of
     *        binary data
     */
    private IconHashInfo(int code, ByteBlock hash, int totalSize) {
        DefensiveTools.checkRange(code, "code", 0);
        DefensiveTools.checkRange(totalSize, "totalSize", -1);

        this.code = code;
        this.hash = hash;
        this.totalSize = totalSize;
    }

    /**
     * Creates a new icon hash info object with the given icon hash and a code
     * of {@link #CODE_SET_ICON}.
     *
     * @param hash the MD5 of the icon hash to send, or {@link #HASH_SPECIAL}
     *        (or any other value, if you want)
     */
    public IconHashInfo(ByteBlock hash) {
        this(CODE_SET_ICON, hash);
    }

    /**
     * Creates a new icon hash info object with the given code and given icon
     * hash.
     *
     * @param code some sort of code (see {@link #CODE_SET_ICON} for details
     *        on the code normally sent to the server by WinAIM)
     * @param hash an MD5 hash of a buddy icon
     */
    public IconHashInfo(int code, ByteBlock hash) {
        this(code, hash, -1);
    }

    /**
     * Returns the "code" associated with this hash info object. Will normally
     * be one of {@link #CODE_ICON_EMPTY} and {@link #CODE_ICON_PRESENT} if in
     * a {@link FullUserInfo} or one of {@link #CODE_ALREADY_HAVE_ICON} and
     * {@link #CODE_UPLOAD_ICON_PLS} if in a {@link
     * net.kano.joscar.snaccmd.conn.YourIconAck}.
     *
     * @return this icon hash info object's associated "code"
     */
    public final int getCode() {
        return code;
    }

    /**
     * Returns the MD5 hash of a buddy icon file, or <code>HASH_SPECIAL</code>
     * in cases described in detail in the {@linkplain #HASH_SPECIAL
     * <code>HASH_SPECIAL</code> documentation}.
     *
     * @return the MD5 hash of the associated buddy icon file
     */
    public final ByteBlock getIconHash() {
        return hash;
    }

    /**
     * Returns the size, in bytes, of this object, as read from a byte block.
     * Will be <code>-1</code> if this object was not read from a data block
     * with <code>readIconHashInfo</code>.
     *
     * @return the total size of this object, in bytes
     */
    public final int getTotalSize() {
        return totalSize;
    }

    public void write(OutputStream out) throws IOException {
        BinaryTools.writeUByte(out, code);
        if (hash != null) {
            BinaryTools.writeUByte(out, hash.getLength());
            hash.write(out);
        }
    }

    public String toString() {
        return "IconHashInfo: code=0x" + Integer.toHexString(code) + ", hash="
                + BinaryTools.describeData(hash);
    }
}
