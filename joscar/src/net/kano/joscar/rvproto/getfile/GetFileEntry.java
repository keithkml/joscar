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
 *  File created by Keith @ 5:23:34 AM
 *
 */

package net.kano.joscar.rvproto.getfile;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.LiveWritable;
import net.kano.joscar.rvcmd.SegmentedFilename;
import net.kano.joscar.tlv.ImmutableTlvChain;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

public class GetFileEntry implements LiveWritable {
    public static final long FLAG_DIR = 0x0001;

    private static final int TYPE_LASTMOD = 0x0101;
    private static final int TYPE_FILESIZE = 0x0303;
    private static final int TYPE_FILENAME = 0x0404;
    private static final int TYPE_FLAGS = 0x0900;
    private static final int TYPE_SENTINEL = 0x0909;

    public static final GetFileEntry readGetFileEntry(Tlv[] tlvs, int offset) {
        DefensiveTools.checkNull(tlvs, "tlvs");
        DefensiveTools.checkRange(offset, "offset", 0, tlvs.length);

        boolean gotLastmod = false;
        int lastTlv = -1;
        for (int i = offset; i < tlvs.length; i++) {
            int type = tlvs[i].getType();
            if (type == TYPE_LASTMOD) {
                if (!gotLastmod) {
                    gotLastmod = true;
                } else {
                    lastTlv = i - 1;
                    break;
                }
            } else if (type == TYPE_SENTINEL) {
                lastTlv = i;
                break;
            }
        }

        int totalTlvCount;
        if (lastTlv == -1) totalTlvCount = tlvs.length - offset;
        else totalTlvCount = lastTlv - offset + 1;

        if (totalTlvCount == 0) return null;

        TlvChain chain = ImmutableTlvChain.createChain(tlvs, offset,
                totalTlvCount);

        SegmentedFilename filename = null;
        String ftFilenameStr = chain.getString(TYPE_FILENAME);
        if (ftFilenameStr != null) {
            filename = SegmentedFilename.createFromFTFilename(ftFilenameStr);
        }

        Tlv lastmodTlv = chain.getFirstTlv(TYPE_LASTMOD);
        long lastmod = -1;
        if (lastmodTlv != null) {
            lastmod = lastmodTlv.getDataAsUInt();
        }

        Tlv filesizeTlv = chain.getFirstTlv(TYPE_FILESIZE);
        long filesize = -1;
        if (filesizeTlv != null) {
            filesize = filesizeTlv.getDataAsUInt();
        }

        Tlv flagsTlv = chain.getFirstTlv(TYPE_FLAGS);
        long flags = 0;
        if (flagsTlv != null) {
            flags = flagsTlv.getDataAsUInt();
            if (flags == -1) flags = 0;
        }

        return new GetFileEntry(filename, filesize, lastmod, flags,
                totalTlvCount);
    }

    private final SegmentedFilename filename;
    private final long filesize;
    private final long lastmod;
    private final long flags;
    private final int totalTlvCount;

    private GetFileEntry(SegmentedFilename filename, long filesize,
            long lastmod, long flags, int totalTlvCount) {
        this.filename = filename;
        this.filesize = filesize;
        this.lastmod = lastmod;
        this.flags = flags;
        this.totalTlvCount = totalTlvCount;
    }

    public GetFileEntry(File file) {
        this(SegmentedFilename.createFromNativeFilename(file.getPath()), file);
    }

    public GetFileEntry(SegmentedFilename filename, File file) {
        this(filename, file.length(), file.lastModified() / 1000,
                file.isDirectory() ? FLAG_DIR : 0);
    }

    public GetFileEntry(SegmentedFilename filename, long filesize, long lastmod,
            long flags) {
        DefensiveTools.checkNull(filename, "filename");
        DefensiveTools.checkRange(lastmod, "lastmod", -1);
        DefensiveTools.checkRange(filesize, "filesize", -1);
        DefensiveTools.checkRange(filesize, "flags", -1);

        if (flags == -1) flags = 0;

        this.lastmod = lastmod;
        this.filesize = filesize;
        this.filename = filename;
        this.flags = flags;
        totalTlvCount = -1;
    }

    public final SegmentedFilename getFilename() { return filename; }

    public final long getFilesize() { return filesize; }

    public final long getLastmod() { return lastmod; }

    public final long getFlags() { return flags; }

    public final int getTotalTlvCount() { return totalTlvCount; }

    public void write(OutputStream out) throws IOException {
        if (lastmod != -1) {
            Tlv.getUIntInstance(TYPE_LASTMOD, lastmod).write(out);
        }
        if (filesize != -1) {
            Tlv.getUIntInstance(TYPE_FILESIZE, filesize).write(out);
        }
        Tlv.getUShortInstance(0x0505, 0x0000).write(out);
        if (filename != null) {
            String ftFilename = filename.toFTFilename();
            Tlv.getStringInstance(TYPE_FILENAME, ftFilename).write(out);
        }
        if (flags != 0) {
            Tlv.getUIntInstance(TYPE_FLAGS, flags).write(out);
        }
        new Tlv(TYPE_SENTINEL).write(out);
    }

    public String toString() {
        return "GetFileEntry: file=<" + filename + ">, "
                + ((float) filesize / 1024)
                + " KB, last modified " + new Date(lastmod * 1000)
                + " (flags=" + flags + ")";
    }
}