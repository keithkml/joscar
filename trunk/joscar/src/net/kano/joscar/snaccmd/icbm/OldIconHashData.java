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
 *  File created by keith @ Mar 6, 2003
 *
 */

package net.kano.joscar.snaccmd.icbm;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.Writable;
import net.kano.joscar.DefensiveTools;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

/**
 * A data structure containing a set of information about a buddy icon. This is
 * called "old" icon hash data because of the new buddy icon system (the
 * {@linkplain net.kano.joscar.snaccmd.icon <code>0x10</code> service}).
 */
public final class OldIconHashData implements Writable {
    /**
     * Returns an icon sum from the given buddy icon data.
     *
     * @param data the raw buddy icon data
     * @return an icon sum suitable for use in an <code>OldIconHashData</code>
     */
    public static int computeIconSum(byte[] data) {
        DefensiveTools.checkNull(data, "data");

        long sum = 0;
        int i;

        for (i = 0; i + 1 < data.length; i += 2) {
            sum += ((data[i + 1] & 0xff) << 8) + (data[i] & 0xff);
        }

        // don't forget the last byte!
        if (i < data.length) {
            sum += data[i] & 0xff;
        }

        sum = ((sum & 0xffff0000L) >> 16) + (sum & 0x0000ffffL);

        return (int) sum;
    }

    /**
     * Returns an icon hash data block read from the given block of binary
     * data.
     *
     * @param block the block of data from which to read
     * @return an icon hash data block read from the given block
     */
    protected static OldIconHashData readOldIconHashData(ByteBlock block) {
        DefensiveTools.checkNull(block, "block");

        long size = BinaryTools.getUInt(block, 0);
        int sum = BinaryTools.getUShort(block, 6);
        long stamp = BinaryTools.getUInt(block, 8);

        return new OldIconHashData(size, sum, stamp);
    }

    /** The file size of the icon. */
    private final long size;
    /** The icon's "icon sum." */
    private final int sum;
    /** The icon's timestamp. */
    private final long timestamp;

    /**
     * Creates a new icon hash data object with the given properties.
     *
     * @param size the file size of the icon
     * @param sum a {@linkplain #computeIconSum hash} of the icon data
     * @param timestamp the time at which the icon was modified last, in seconds
     *        since the unix epoch
     */
    public OldIconHashData(long size, int sum, long timestamp) {
        DefensiveTools.checkRange(size, "size", 0);
        DefensiveTools.checkRange(sum, "sum", 0);
        DefensiveTools.checkRange(timestamp, "timestamp", 0);

        this.size = size;
        this.sum = sum;
        this.timestamp = timestamp;
    }

    /**
     * Returns the file size of the icon, as sent in this object.
     *
     * @return the icon's size, in bytes
     */
    public final long getSize() {
        return size;
    }

    /**
     * Returns an {@linkplain #computeIconSum "icon sum"} of the icon data,
     * as sent in this object.
     *
     * @return the icon's "icon sum"
     */
    public final int getSum() {
        return sum;
    }

    /**
     * Returns the time at which the icon was modified, in seconds since the
     * unix epoch, as sent in this object.
     *
     * @return the time at which this icon was modified
     */
    public final long getTimestamp() {
        return timestamp;
    }

    public long getWritableLength() {
        return 12;
    }

    public void write(OutputStream out) throws IOException {
        BinaryTools.writeUInt(out, size);
        BinaryTools.writeUShort(out, sum);
        BinaryTools.writeUInt(out, timestamp);
    }

    public String toString() {
        return "OldIconHashData: size=" + size + " bytes, sum=" + sum
                + ", lastmod=" + new Date(timestamp * 1000);
    }
}
