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

package net.kano.joscar.rvproto.directim;

import net.kano.joscar.LiveWritable;
import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.snaccmd.icbm.ImEncodedString;
import net.kano.joscar.snaccmd.icbm.ImEncoding;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

public class DirectImHeader implements LiveWritable {
    public static final String DCVERSION_DEFAULT = "ODC2";

    public static final long FLAG_TYPING = 0x08;
    public static final long FLAG_TYPED = 0x04;
    public static final long FLAG_TYPINGPACKET = 0x02;
    public static final long FLAG_AUTORESPONSE = 0x01;

    public static DirectImHeader createTypingHeader() {
        DirectImHeader hdr = new DirectImHeader();

        hdr.setDefaults();
        hdr.setFlags(FLAG_TYPINGPACKET | FLAG_TYPING);

        return hdr;
    }

    public static DirectImHeader createTypedHeader() {
        DirectImHeader hdr = new DirectImHeader();

        hdr.setDefaults();
        hdr.setFlags(FLAG_TYPINGPACKET | FLAG_TYPED);

        return hdr;
    }

    public static DirectImHeader createTypingErasedHeader() {
        DirectImHeader hdr = new DirectImHeader();

        hdr.setDefaults();
        hdr.setFlags(FLAG_TYPINGPACKET);

        return hdr;
    }

    public static DirectImHeader createMessageHeader(ImEncodedString message) {
        return createMessageHeader(false, message);
    }

    public static DirectImHeader createMessageHeader(boolean autoresponse,
            ImEncodedString message) {
        DirectImHeader hdr = new DirectImHeader();

        hdr.setDefaults();
        hdr.setFlags(autoresponse ? FLAG_AUTORESPONSE : 0);
        hdr.setEncoding(message.getEncoding());
        hdr.setDataLength(message.getBytes().length);

        return hdr;
    }
    private String dcVersion;
    private long messageId;
    private long dataLength;
    private ImEncoding encoding;
    private long flags;
    private String sn;
    private int headerSize;

    public static DirectImHeader readDirectIMHeader(InputStream in)
            throws IOException {
        // read the six-byte meta-header containing the ODC version and the
        // length of the real header
        byte[] miniHeader = new byte[6];
        for (int i = 0; i < miniHeader.length;) {
            int count = in.read(miniHeader, i, miniHeader.length - i);

            if (count == -1) return null;

            i += count;
        }

        // create a header object
        DirectImHeader hdr = new DirectImHeader();

        // extract those two values (the version and the header length)
        ByteBlock miniHeaderBlock = ByteBlock.wrap(miniHeader);
        ByteBlock verBlock = miniHeaderBlock.subBlock(0, 4);
        
        hdr.setDcVersion(BinaryTools.getAsciiString(verBlock));

        int headerLen = BinaryTools.getUShort(miniHeaderBlock, 4);
        if (headerLen < 6) return null;

        hdr.setHeaderSize(headerLen);

        // now read the real header. note that headerLen includes the length of
        // the mini-header.
        byte[] headerData = new byte[headerLen - 6];
        for (int i = 0; i < headerData.length;) {
            int count = in.read(headerData, i, headerData.length - i);

            if (count == -1) return null;

            i += count;
        }

        // okay.
        ByteBlock header = ByteBlock.wrap(headerData);

        hdr.setMessageId(BinaryTools.getLong(header, 6));
        hdr.setDataLength(BinaryTools.getUInt(header, 22));
        int charsetCode = BinaryTools.getUShort(header, 26);
        int charsetSubcode = BinaryTools.getUShort(header, 28);
        hdr.setEncoding(new ImEncoding(charsetCode, charsetSubcode));
        hdr.setFlags(BinaryTools.getUInt(header, 30));

        ByteBlock snBlock = header.subBlock(38, 16);
        hdr.setScreenname(BinaryTools.getNullPadded(snBlock).getString());

        return hdr;
    }

    public synchronized final String getDcVersion() { return dcVersion; }

    public synchronized final long getMessageId() { return messageId; }

    public synchronized final long getDataLength() { return dataLength; }

    public synchronized final ImEncoding getEncoding() { return encoding; }

    public synchronized final long getFlags() { return flags; }

    public synchronized final String getScreenname() { return sn; }

    public synchronized final int getHeaderSize() { return headerSize; }

    public synchronized final void setDcVersion(String dcVersion) {
        this.dcVersion = dcVersion;
    }

    public synchronized final void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public synchronized final void setDataLength(long dataLength) {
        this.dataLength = dataLength;
    }

    public synchronized final void setEncoding(ImEncoding encoding) {
        this.encoding = encoding;
    }

    public synchronized final void setFlags(long flags) {
        this.flags = flags;
    }

    public synchronized final void setScreenname(String sn) { this.sn = sn; }

    protected synchronized final void setHeaderSize(int headerSize) {
        this.headerSize = headerSize;
    }

    public synchronized final void setDefaults() {
        this.dcVersion = DCVERSION_DEFAULT;
    }

    public void write(OutputStream out) throws IOException {
        ByteArrayOutputStream hout = new ByteArrayOutputStream(76);

        hout.write(BinaryTools.getAsciiBytes(dcVersion));
        BinaryTools.writeUShort(hout, 76);

        BinaryTools.writeUShort(hout, 1);
        BinaryTools.writeUShort(hout, 6);
        BinaryTools.writeUShort(hout, 0);

        BinaryTools.writeLong(hout, messageId);
        hout.write(new byte[8]);
        BinaryTools.writeUInt(hout, dataLength);

        BinaryTools.writeUShort(hout, encoding.getCharsetCode());
        BinaryTools.writeUShort(hout, encoding.getCharsetSubcode());

        BinaryTools.writeUInt(hout, flags);
        BinaryTools.writeUInt(hout, 0);

        ByteBlock snBlock = ByteBlock.wrap(BinaryTools.getAsciiBytes(sn));
        BinaryTools.writeNullPadded(hout, snBlock, 16);

        hout.write(new byte[16]);

        hout.writeTo(out);
    }

    public String toString() {
        return "DirectIMHeader: " +
                "msgid=" + messageId +
                ", dataLen=" + dataLength +
                ", encoding=" + encoding +
                ", flags=0x" + Long.toHexString(flags) +
                ", sn='" + sn + "'" +
                ", headerSize=" + headerSize;
    }

}
