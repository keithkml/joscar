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

package net.kano.joscar.rvcmd.getfile;

import net.kano.joscar.rvcmd.AbstractRequestRvCmd;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.BinaryTools;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.tlv.TlvChain;
import net.kano.joscar.tlv.Tlv;

import java.io.OutputStream;
import java.io.IOException;

public class GetFileReqRvCmd extends AbstractRequestRvCmd {
    /*
    AIM 5.1:      00 12 00 01 00 00 00 00 00 00 00 00
    AIM 5.2 beta: 00 12 00 02 00 00 00 01 00
    */

    public static final String CHARSET_DEFAULT = "us-ascii";
    public static final int CODE_DEFAULT = 0x0012;
    public static final int PROTOVERSION_DEFAULT = 0x0002;
    public static final long FLAG_EXPAND_DYNAMIC = 0x00000001;
    public static final ByteBlock EXTRABLOCK_DEFAULT
            = ByteBlock.wrap(new byte[] { 0x00 });

    private static final int TYPE_CHARSET = 0x2712;

    private final RvConnectionInfo connInfo;
    private final String charset;
    private final int code;
    private final int protoVersion;
    private final long flags;
    private final ByteBlock extraBlock;

    public GetFileReqRvCmd(RecvRvIcbm icbm) {
        super(icbm);

        TlvChain chain = getRvTlvs();

        connInfo = RvConnectionInfo.readConnectionInfo(chain);

        charset = chain.getString(TYPE_CHARSET);

        ByteBlock block = getServiceData();

        if (block == null) {
            code = -1;
            protoVersion = -1;
            flags = -1;
            extraBlock = null;
        } else {
            code = BinaryTools.getUShort(block, 0);
            protoVersion = BinaryTools.getUShort(block, 2);
            flags = BinaryTools.getUInt(block, 4);

            if (block.getLength() > 8) {
                extraBlock = ByteBlock.wrap(block.subBlock(8).toByteArray());
            } else {
                extraBlock = null;
            }
        }
    }

    public GetFileReqRvCmd(long icbmMessageId, RvConnectionInfo connInfo) {
        this(icbmMessageId, connInfo, FLAG_EXPAND_DYNAMIC);
    }

    public GetFileReqRvCmd(long icbmMessageId, RvConnectionInfo connInfo,
            long flags) {
        this(icbmMessageId, connInfo, CHARSET_DEFAULT, CODE_DEFAULT,
                PROTOVERSION_DEFAULT, flags, EXTRABLOCK_DEFAULT);
    }

    public GetFileReqRvCmd(long icbmMessageId, RvConnectionInfo connInfo,
            String charset, int code, int protoVersion, long flags,
            ByteBlock extraBlock) {
        super(icbmMessageId, CapabilityBlock.BLOCK_FILE_GET);

        DefensiveTools.checkRange(code, "code", -1);
        DefensiveTools.checkRange(protoVersion, "protoVersion", -1);
        DefensiveTools.checkRange(flags, "flags", -1);

        this.connInfo = connInfo;
        this.charset = charset;
        this.code = code;
        this.protoVersion = protoVersion;
        this.flags = flags;
        this.extraBlock = extraBlock;
    }

    public final RvConnectionInfo getConnInfo() { return connInfo; }

    public final String getCharset() { return charset; }

    public final int getCode() { return code; }

    public final int getProtoVersion() { return protoVersion; }

    public final long getFlags() { return flags; }

    public final ByteBlock getExtraBlock() { return extraBlock; }

    protected void writeRvTlvs(OutputStream out) throws IOException {
        if (connInfo != null) {
            connInfo.write(out);
        }
        if (charset != null) {
            Tlv.getStringInstance(TYPE_CHARSET, charset).write(out);
        }
    }

    protected boolean hasServiceData() {
        return true;
    }

    protected void writeServiceData(OutputStream out) throws IOException {
        if (code != -1 && protoVersion != -1 && flags != -1) {
            BinaryTools.writeUShort(out, code);
            BinaryTools.writeUShort(out, protoVersion);
            BinaryTools.writeUInt(out, flags);
            if (extraBlock != null) extraBlock.write(out);
        }
    }

    public String toString() {
        return "GetFileReqRvCmd: connInfo=<" + connInfo + ">, code=0x"
                + Integer.toHexString(code) + ", proto=0x"
                + Integer.toHexString(protoVersion) + ", flags=0x"
                + Long.toHexString(flags) + ", extraBlock="
                + (extraBlock == null ? null
                : BinaryTools.describeData(extraBlock));
    }
}
