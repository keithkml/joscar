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

package net.kano.joscar.rvproto.rvproxy.cmd;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.BinaryTools;
import net.kano.joscar.LiveWritable;
import net.kano.joscar.rvproto.rvproxy.cmd.RvProxyCmd;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

public final class RvProxyHeader implements LiveWritable {
    public static final int HEADERVERSION_DEFAULT = 0x044a;

    public static final int HEADERTYPE_ERROR = 0x0001;
    public static final int HEADERTYPE_INIT_SEND = 0x0002;
    public static final int HEADERTYPE_ACK = 0x0003;
    public static final int HEADERTYPE_INIT_RECV = 0x0004;
    public static final int HEADERTYPE_READY = 0x0005;

    public static final int FLAGS_DEFAULT_FROM_SERVER = 0x0220;
    public static final int FLAGS_DEFAULT_FROM_CLIENT = 0x0000;

    public static RvProxyHeader readRvProxyHeader(InputStream in)
            throws IOException {
        DefensiveTools.checkNull(in, "in");

        byte[] lenBytes = new byte[2];
        for (int i = 0; i < lenBytes.length;) {
            int count = in.read(lenBytes, i, lenBytes.length - i);

            if (count == -1) return null;

            i += count;
        }

        ByteBlock lenBlock = ByteBlock.wrap(lenBytes);

        int restLength = BinaryTools.getUShort(lenBlock, 0);

        int headerSize = restLength + 2;

        if (restLength < 10) return null;

        byte[] restBytes = new byte[restLength];

        for (int i = 0; i < restBytes.length;) {
            int count = in.read(restBytes, i, restBytes.length - i);

            if (count == -1) return null;

            i += count;
        }

        ByteBlock rest = ByteBlock.wrap(restBytes);

        int headerVersion = BinaryTools.getUShort(rest, 0);
        int headerType = BinaryTools.getUShort(rest, 2);
        int flags = BinaryTools.getUShort(rest, 8);

        ByteBlock data = rest.subBlock(10);

        return new RvProxyHeader(headerVersion, headerType, flags, data,
                headerSize);
    }

    private final int headerVersion;
    private final int headerType;
    private final int flags;
    private final ByteBlock data;
    private final int headerSize;

    private final LiveWritable dataWriter;

    protected RvProxyHeader(int headerVersion, int headerType, int flags,
            ByteBlock data, int headerSize) {
        DefensiveTools.checkRange(headerVersion, "headerVersion", 0);
        DefensiveTools.checkRange(headerType, "headerType", 0);
        DefensiveTools.checkRange(flags, "flags", 0);
        DefensiveTools.checkRange(headerSize, "headerSize", -1);

        this.headerVersion = headerVersion;
        this.headerType = headerType;
        this.flags = flags;
        this.data = data;
        this.headerSize = headerSize;
        this.dataWriter = null;
    }

    public RvProxyHeader(final RvProxyCmd rvProxyCmd) {
        this.headerVersion = rvProxyCmd.getHeaderVersion();
        this.headerType = rvProxyCmd.getHeaderType();
        this.flags = rvProxyCmd.getFlags();
        this.data = null;
        this.dataWriter = new LiveWritable() {
            public void write(OutputStream out) throws IOException {
                rvProxyCmd.writeData(out);
            }
        };
        this.headerSize = -1;
    }

    public final int getHeaderVersion() { return headerVersion; }

    public final int getHeaderType() { return headerType; }

    public final int getFlags() { return flags; }

    public final ByteBlock getData() { return data; }

    public final int getHeaderSize() { return headerSize; }

    public void write(OutputStream out) throws IOException {
        ByteArrayOutputStream hout = new ByteArrayOutputStream(50);

        BinaryTools.writeUShort(hout, headerVersion);
        BinaryTools.writeUShort(hout, headerType);
        BinaryTools.writeUInt(hout, 0);
        BinaryTools.writeUShort(hout, flags);

        if (dataWriter != null) dataWriter.write(hout);
        else if (data != null) data.write(hout);

        BinaryTools.writeUShort(out, hout.size());
        hout.writeTo(out);
    }

    public String toString() {
        return "RvProxyHeader: headerType=0x" + Integer.toHexString(headerType)
                + ", flags=0x" + Integer.toHexString(flags);
    }
}