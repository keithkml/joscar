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
 *  File created by keith @ Apr 24, 2003
 *
 */

package net.kano.joscar.rvcmd.sendfile;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.LiveWritable;

import java.io.IOException;
import java.io.OutputStream;

public class FileSendBlock implements LiveWritable {
    public static final int SENDTYPE_SINGLEFILE = 0x01;
    public static final int SENDTYPE_DIR = 0x02;

    private final int sendType;
    private final int fileCount;
    private final long totalFileSize;
    private final String filename;

    public static FileSendBlock readFileSendBlock(ByteBlock block) {
        DefensiveTools.checkNull(block, "block");

        int type = BinaryTools.getUShort(block, 0);
        int count = BinaryTools.getUShort(block, 2);
        long size = BinaryTools.getUInt(block, 4);

        ByteBlock filenameBlock = block.subBlock(8);
        int firstNull;
        for (firstNull = 0; firstNull < filenameBlock.getLength();
             firstNull++) {
            if (filenameBlock.get(firstNull) == 0) break;
        }

        String name = null;
        name = BinaryTools.getAsciiString(filenameBlock.subBlock(0,
                firstNull));

        return new FileSendBlock(type, name, count, size);
    }

    public FileSendBlock(String filename, long totalFileSize) {
        this(SENDTYPE_SINGLEFILE, filename, 1, totalFileSize);
    }

    public FileSendBlock(int sendType, String filename, int fileCount,
            long totalFileSize) {
        DefensiveTools.checkRange(sendType, "sendType", 0);
        DefensiveTools.checkRange(fileCount, "fileCount", 0);
        DefensiveTools.checkRange(totalFileSize, "totalFileSize", 0);
        DefensiveTools.checkNull(filename, "filename");

        this.sendType = sendType;
        this.fileCount = fileCount;
        this.totalFileSize = totalFileSize;
        this.filename = filename;
    }

    public final int getSendType() { return sendType; }

    public final int getFileCount() { return fileCount; }

    public final long getTotalFileSize() { return totalFileSize; }

    public final String getFilename() { return filename; }

    public void write(OutputStream out) throws IOException {
        BinaryTools.writeUShort(out, sendType);
        BinaryTools.writeUShort(out, fileCount);
        BinaryTools.writeUInt(out, totalFileSize);
        out.write(BinaryTools.getAsciiBytes(filename));

        // we write 46 nulls here. fun fun.
        out.write(new byte[46]);
    }

    public String toString() {
        return "FileSendBlock: type=" + sendType + (
                fileCount > 1
                ? ", " + fileCount + " files under " + filename
                : ": " + filename
                ) + ": "
                + totalFileSize + " bytes total";
    }
}
