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
 *  File created by keith @ Apr 25, 2003
 *
 */

package net.kano.joscar.rvproto.ft;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.LiveWritable;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.rvcmd.SegmentedFilename;
import net.kano.joscar.snaccmd.icbm.ImEncodedString;
import net.kano.joscar.snaccmd.icbm.ImEncoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class FileSendHeader implements LiveWritable {
    public static final String FTVERSION_DEFAULT = "OFT2";

    public static final int HEADERTYPE_SENDHEADER = 0x0101;
    public static final int HEADERTYPE_ACK = 0x0202;
    public static final int HEADERTYPE_RECEIVED = 0x0204;

    public static final int HEADERTYPE_FILELIST_SENDLIST = 0x1108;
    public static final int HEADERTYPE_FILELIST_ACK = 0x1209;
    public static final int HEADERTYPE_FILELIST_RECEIVED = 0x120b;
    public static final int HEADERTYPE_FILELIST_REQFILE = 0x120c;
    public static final int HEADERTYPE_FILELIST_REQDIR = 0x120e;

    public static final int ENCRYPTION_NONE = 0x0000;
    public static final int COMPRESSION_NONE = 0x0000;

    public static final String CLIENTID_OLD = "OFT_Windows ICBMFT V1.1 32";
    public static final String CLIENTID_DEFAULT = "Cool FileXfer";

    public static final int FLAG_DEFAULT = 0x20;
    public static final int FLAG_DONE = 0x01;
    public static final int FLAG_FILELIST = 0x10;

    public static final ByteBlock DUMMY_DEFAULT = ByteBlock.wrap(new byte[69]);
    public static final ByteBlock MACFILEINFO_DEFAULT
            = ByteBlock.wrap(new byte[16]);


    public static FileSendHeader readFileSendHeader(InputStream in)
            throws IOException {
        // first we read the mini-header which contains a file transfer version
        // and the length of the whole header
        byte[] header = new byte[6];
        for (int i = 0; i < header.length;) {
            int count = in.read(header, i, header.length - i);

            if (count == -1) return null;

            i += count;
        }

        ByteBlock ftVerBlock = ByteBlock.wrap(header, 0, 4);
        String ftversion = BinaryTools.getAsciiString(ftVerBlock);

        int headerLen = BinaryTools.getUShort(ByteBlock.wrap(header), 4);

        // then we read the full header by reading the rest of the bytes
        // whose length was given in the mini-header
        byte[] bigheader = new byte[headerLen - 6];
        for (int i = 0; i < bigheader.length;) {
            int count = in.read(bigheader, i, bigheader.length - i);

            if (count == -1) return null;

            i += count;
        }

        // I think now is a good time to create the header object.
        FileSendHeader fsh = new FileSendHeader();
        fsh.setFtVersion(ftversion);
        fsh.setHeaderSize(headerLen);

        ByteBlock block = ByteBlock.wrap(bigheader);
        fsh.setHeaderType(BinaryTools.getUShort(block, 0));
        fsh.setIcbmMessageId(BinaryTools.getLong(block, 2));
        fsh.setEncryption(BinaryTools.getUShort(block, 10));
        fsh.setCompression(BinaryTools.getUShort(block, 12));

        fsh.setFileCount(BinaryTools.getUShort(block, 14));
        fsh.setFilesLeft(BinaryTools.getUShort(block, 16));
        fsh.setPartCount(BinaryTools.getUShort(block, 18));
        fsh.setPartsLeft(BinaryTools.getUShort(block, 20));

        fsh.setTotalFileSize(BinaryTools.getUInt(block, 22));
        fsh.setFileSize(BinaryTools.getUInt(block, 26));
        fsh.setLastmod(BinaryTools.getUInt(block, 30));
        fsh.setChecksum(BinaryTools.getUInt(block, 34));

        fsh.setResForkReceivedChecksum(BinaryTools.getUInt(block, 38));
        fsh.setResForkSize(BinaryTools.getUInt(block, 42));
        fsh.setCreated(BinaryTools.getUInt(block, 46));
        fsh.setResForkChecksum(BinaryTools.getUInt(block, 50));

        fsh.setNumReceived(BinaryTools.getUInt(block, 54));
        fsh.setReceivedChecksum(BinaryTools.getUInt(block, 58));

        // the client ID block is stored as 32 bytes of ASCII text padded to the
        // right with nulls. we read the 32 bytes and then search for a null,
        // chopping the non-null portion off into a new block and creating a
        // string out of it.
        ByteBlock clientidBlock = block.subBlock(62, 32);
        fsh.setClientid(BinaryTools.getNullPadded(clientidBlock).getString());

        ByteBlock rest = block.subBlock(94);
        fsh.setFlags(BinaryTools.getUByte(rest, 0));
        fsh.setListNameOffset(BinaryTools.getUByte(rest, 1));
        fsh.setListSizeOffset(BinaryTools.getUByte(rest, 2));

        fsh.setDummyBlock(rest.subBlock(3, 69));
        fsh.setMacFileInfo(rest.subBlock(72, 16));

        int charset = BinaryTools.getUShort(rest, 88);
        int charsubset = BinaryTools.getUShort(rest, 90);

        // okay, first things first, this is stored as a null-terminated string.
        // we loop until we get to the null, then chop the non-null part into
        // its own block.
        ByteBlock filenameBlock = rest.subBlock(92);
        int firstNull;
        for (firstNull = 0; firstNull < filenameBlock.getLength();
             firstNull++) {
            if (filenameBlock.get(firstNull) == 0) break;
        }
        filenameBlock = filenameBlock.subBlock(0, firstNull);
        rest = rest.subBlock(92 + filenameBlock.getLength());

        ImEncoding encoding = new ImEncoding(charset, charsubset);
        SegmentedFilename segmented = SegmentedFilename.createFromFTFilename(
                ImEncodedString.readImEncodedString(encoding, filenameBlock));

        fsh.setFilename(segmented);

        return fsh;
    }

    private String ftVersion = null;
    private int headerType = -1;
    private long icbmMessageId = 0;
    private int encryption = -1;
    private int compression = -1;
    private int fileCount = -1;
    private int filesLeft = -1;
    private int partCount = -1;
    private int partsLeft = -1;
    private long totalFileSize = -1;
    private long fileSize = -1;
    private long lastmod = -1;
    private long checksum = -1;
    private long resForkReceivedChecksum = -1;
    private long resForkSize = -1;
    private long created = -1;
    private long resForkChecksum = -1;
    private long numReceived = -1;
    private long receivedChecksum = -1;
    private String clientid = null;
    private int flags = -1;
    private int listNameOffset = -1;
    private int listSizeOffset = -1;
    private ByteBlock dummyBlock = null;
    private ByteBlock macFileInfo = null;
    private SegmentedFilename filename = null;
    private int headerSize = -1;

    public FileSendHeader() { }

    public FileSendHeader(FileSendHeader other) {
        ftVersion = other.ftVersion;
        headerType = other.headerType;
        icbmMessageId = other.icbmMessageId;
        encryption = other.encryption;
        compression = other.compression;
        fileCount = other.fileCount;
        filesLeft = other.filesLeft;
        partCount = other.partCount;
        partsLeft = other.partsLeft;
        totalFileSize = other.totalFileSize;
        fileSize = other.fileSize;
        lastmod = other.lastmod;
        checksum = other.checksum;
        resForkReceivedChecksum = other.resForkReceivedChecksum;
        resForkSize = other.resForkSize;
        created = other.created;
        resForkChecksum = other.resForkChecksum;
        numReceived = other.numReceived;
        receivedChecksum = other.receivedChecksum;
        clientid = other.clientid;
        flags = other.flags;
        listNameOffset = other.listNameOffset;
        listSizeOffset = other.listSizeOffset;
        dummyBlock = other.dummyBlock;
        macFileInfo = other.macFileInfo;
        filename = other.filename;
    }

    public synchronized String getFtVersion() { return ftVersion; }

    public synchronized int getHeaderType() { return headerType; }

    public synchronized long getIcbmMessageId() { return icbmMessageId; }

    public synchronized int getEncryption() { return encryption; }

    public synchronized int getCompression() { return compression; }

    public synchronized int getFileCount() { return fileCount; }

    public synchronized int getFilesLeft() { return filesLeft; }

    public synchronized int getPartCount() { return partCount; }

    public synchronized int getPartsLeft() { return partsLeft; }

    public synchronized long getTotalFileSize() { return totalFileSize; }

    public synchronized long getFileSize() { return fileSize; }

    public synchronized long getLastmod() { return lastmod; }

    public synchronized long getChecksum() { return checksum; }

    public synchronized long getResForkReceivedChecksum() {
        return resForkReceivedChecksum;
    }

    public synchronized long getResForkSize() { return resForkSize; }

    public synchronized long getCreated() { return created; }

    public synchronized long getResForkChecksum() {
        return resForkChecksum;
    }

    public synchronized long getNumReceived() { return numReceived; }

    public synchronized long getReceivedChecksum() {
        return receivedChecksum;
    }

    public synchronized String getClientid() { return clientid; }

    public synchronized int getFlags() { return flags; }

    public synchronized int getListNameOffset() { return listNameOffset; }

    public synchronized int getListSizeOffset() { return listSizeOffset; }

    public synchronized ByteBlock getDummyBlock() { return dummyBlock; }

    public synchronized ByteBlock getMacFileInfo() { return macFileInfo; }

    public synchronized SegmentedFilename getFilename() {
        return filename;
    }

    public synchronized int getHeaderSize() { return headerSize; }

    public synchronized void setFtVersion(String ftVersion) {
        this.ftVersion = ftVersion;
    }

    public synchronized void setHeaderType(int headerType) {
        this.headerType = headerType;
    }

    public synchronized void setIcbmMessageId(long icbmMessageId) {
        this.icbmMessageId = icbmMessageId;
    }

    public synchronized void setEncryption(int encryption) {
        this.encryption = encryption;
    }

    public synchronized void setCompression(int compression) {
        this.compression = compression;
    }

    public synchronized void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public synchronized void setFilesLeft(int filesLeft) {
        this.filesLeft = filesLeft;
    }

    public synchronized void setPartCount(int partCount) {
        this.partCount = partCount;
    }

    public synchronized void setPartsLeft(int partsLeft) {
        this.partsLeft = partsLeft;
    }

    public synchronized void setTotalFileSize(long totalFileSize) {
        this.totalFileSize = totalFileSize;
    }

    public synchronized void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public synchronized void setLastmod(long lastmod) {
        this.lastmod = lastmod;
    }

    public synchronized void setChecksum(long checksum) {
        this.checksum = checksum;
    }

    public synchronized void setResForkReceivedChecksum(
            long resForkReceivedChecksum) {
        this.resForkReceivedChecksum = resForkReceivedChecksum;
    }

    public synchronized void setResForkSize(long resForkSize) {
        this.resForkSize = resForkSize;
    }

    public synchronized void setCreated(long created) {
        this.created = created;
    }

    public synchronized void setResForkChecksum(long resForkChecksum) {
        this.resForkChecksum = resForkChecksum;
    }

    public synchronized void setNumReceived(long numReceived) {
        this.numReceived = numReceived;
    }

    public synchronized void setReceivedChecksum(long receivedChecksum) {
        this.receivedChecksum = receivedChecksum;
    }

    public synchronized void setClientid(String clientid) {
        this.clientid = clientid;
    }

    public synchronized void setFlags(int flags) { this.flags = flags; }

    public synchronized void setListNameOffset(int listNameOffset) {
        this.listNameOffset = listNameOffset;
    }

    public synchronized void setListSizeOffset(int listSizeOffset) {
        this.listSizeOffset = listSizeOffset;
    }

    public synchronized void setDummyBlock(ByteBlock dummyBlock) {
        this.dummyBlock = dummyBlock;
    }

    public synchronized void setMacFileInfo(ByteBlock macFileInfo) {
        this.macFileInfo = macFileInfo;
    }

    public synchronized void setFilename(SegmentedFilename filename) {
        this.filename = filename;;
    }

    private synchronized void setHeaderSize(int headerSize) {
        this.headerSize = headerSize;
    }

    /**
     * Does not set filename or headerType!
     */
    public synchronized final void setDefaults() {
        this.clientid = CLIENTID_DEFAULT;
        this.compression = COMPRESSION_NONE;
        this.encryption = ENCRYPTION_NONE;
        this.icbmMessageId = 0;
        this.dummyBlock = DUMMY_DEFAULT;
        this.flags = FLAG_DEFAULT;
        this.ftVersion = FTVERSION_DEFAULT;
        this.resForkChecksum = 0;
        this.resForkReceivedChecksum = 0;
        this.resForkSize = 0;
        this.created = 0;
        this.lastmod = 0;
        this.checksum = 0;
        this.listNameOffset = 0;
        this.listSizeOffset = 0;
        this.macFileInfo = MACFILEINFO_DEFAULT;
        this.numReceived = 0;
        this.fileCount = 0;
        this.fileSize = 0;
        this.filesLeft = 0;
        this.partCount = 0;
        this.partsLeft = 0;
        this.receivedChecksum = 0;
        this.totalFileSize = 0;
    }

    private synchronized void checkValidity() {
        DefensiveTools.checkNull(ftVersion, "ftVersion");
        DefensiveTools.checkRange(headerType, "headerType", 0);
        DefensiveTools.checkRange(encryption, "encryption", 0);
        DefensiveTools.checkRange(compression, "compression", 0);
        DefensiveTools.checkRange(fileCount, "fileCount", 0);
        DefensiveTools.checkRange(filesLeft, "filesLeft", 0);
        DefensiveTools.checkRange(partCount, "partCount", 0);
        DefensiveTools.checkRange(partsLeft, "partsLeft", 0);
        DefensiveTools.checkRange(totalFileSize, "totalFileSize", 0);
        DefensiveTools.checkRange(fileSize, "fileSize", 0);
        DefensiveTools.checkRange(lastmod, "lastmod", 0);
        DefensiveTools.checkRange(checksum, "checksum", 0);
        DefensiveTools.checkRange(resForkReceivedChecksum,
                "resForkReceivedChecksum", 0);
        DefensiveTools.checkRange(resForkSize, "resForkSize", 0);
        DefensiveTools.checkRange(created, "created", 0);
        DefensiveTools.checkRange(resForkChecksum, "resForkChecksum", 0);
        DefensiveTools.checkRange(numReceived, "numReceived", 0);
        DefensiveTools.checkRange(receivedChecksum, "receivedChecksum", 0);
        DefensiveTools.checkNull(clientid, "clientid");
        DefensiveTools.checkRange(flags, "flags", 0);
        DefensiveTools.checkRange(listNameOffset, "listNameOffset", 0);
        DefensiveTools.checkRange(listSizeOffset, "listSizeOffset", 0);
        DefensiveTools.checkNull(dummyBlock, "dummyBlock");
        DefensiveTools.checkNull(macFileInfo, "macFileInfo");
        // whew.
    }

    public synchronized void write(OutputStream out) throws IOException {
        checkValidity();

        // build the header block
        ByteArrayOutputStream header = new ByteArrayOutputStream(300);
        BinaryTools.writeUShort(header, headerType);
        BinaryTools.writeLong(header, icbmMessageId);
        BinaryTools.writeUShort(header, encryption);
        BinaryTools.writeUShort(header, compression);

        BinaryTools.writeUShort(header, fileCount);
        BinaryTools.writeUShort(header, filesLeft);
        BinaryTools.writeUShort(header, partCount);
        BinaryTools.writeUShort(header, partsLeft);

        BinaryTools.writeUInt(header, totalFileSize);
        BinaryTools.writeUInt(header, fileSize);
        BinaryTools.writeUInt(header, lastmod);
        BinaryTools.writeUInt(header, checksum);

        BinaryTools.writeUInt(header, resForkReceivedChecksum);
        BinaryTools.writeUInt(header, resForkSize);
        BinaryTools.writeUInt(header, created);
        BinaryTools.writeUInt(header, resForkChecksum);

        BinaryTools.writeUInt(header, numReceived);
        BinaryTools.writeUInt(header, receivedChecksum);

        // this needs to be 32 bytes...
        ByteBlock clientidBytes
                = ByteBlock.wrap(BinaryTools.getAsciiBytes(clientid));
        BinaryTools.writeNullPadded(header, clientidBytes, 32);

        BinaryTools.writeUByte(header, flags);
        BinaryTools.writeUByte(header, listNameOffset);
        BinaryTools.writeUByte(header, listSizeOffset);

        // this needs to be (sigh) 69 bytes
        BinaryTools.writeNullPadded(header, dummyBlock, 69);

        // this needs to be 16 bytes
        BinaryTools.writeNullPadded(header, macFileInfo, 16);

        // write the segmented filename
        String filenameStr;
        if (filename == null) filenameStr = "";
        else filenameStr = filename.toFTFilename();

        ImEncodedString encInfo = ImEncodedString.encodeString(filenameStr);
        ImEncoding encoding = encInfo.getEncoding();

        BinaryTools.writeUShort(header, encoding.getCharsetCode());
        BinaryTools.writeUShort(header, encoding.getCharsetSubcode());

        byte[] fnBytes = encInfo.getBytes();
        header.write(fnBytes);
        // pad this so it's (at least) 63 bytes
        for (int i = fnBytes.length; i < 63; i++) {
            header.write(0);
        }
        // and write a final null
        header.write(0);

        // and write the packet we created above to the stream
        ByteArrayOutputStream fullBuffer
                = new ByteArrayOutputStream(header.size() + 6);
        fullBuffer.write(BinaryTools.getAsciiBytes(ftVersion));
        BinaryTools.writeUShort(fullBuffer, header.size() + 6);
        header.writeTo(fullBuffer);

        // then write that packet to the stream, so it's all in one happy TCP
        // packet
        fullBuffer.writeTo(out);
    }

    public synchronized String toString() {
        return "FileSendHeader:" +
                "\n ftVersion='" + ftVersion + "'" +
                "\n headerType=0x" + Integer.toHexString(headerType) +
                "\n icbmMessageId=" + icbmMessageId +
                "\n encryption=" + encryption +
                "\n compression=" + compression +
                "\n fileCount=" + fileCount +
                "\n filesLeft=" + filesLeft +
                "\n partCount=" + partCount +
                "\n partsLeft=" + partsLeft +
                "\n totalSize=" + totalFileSize +
                "\n fileSize=" + fileSize +
                "\n lastmod=" + lastmod +
                "\n checksum=" + checksum +
                "\n resForkReceivedChecksum=" + resForkReceivedChecksum +
                "\n resForkSize=" + resForkSize +
                "\n created=" + created +
                "\n resForkChecksum=" + resForkChecksum +
                "\n numReceived=" + numReceived +
                "\n receivedChecksum=" + receivedChecksum +
                "\n clientid='" + clientid + "'" +
                "\n flags=0x" + Integer.toHexString(flags) +
                "\n listNameOffset=" + listNameOffset +
                "\n listSizeOffset=" + listSizeOffset +
                "\n macFileInfo=" + BinaryTools.describeData(macFileInfo) +
                "\n filename=" + filename;
    }
}
