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
 *  File created by Keith @ 3:58:22 AM
 *
 */

package net.kano.joscar.rvcmd.voice;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.rvcmd.AbstractRequestRvCmd;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.tlv.TlvChain;

import java.io.IOException;
import java.io.OutputStream;

public class VoiceReqRvCmd extends AbstractRequestRvCmd {
    public static final long VERSION_DEFAULT = 0x00000001;

    private final long version;
    private final RvConnectionInfo connInfo;

    public VoiceReqRvCmd(RecvRvIcbm icbm) {
        super(icbm);

        TlvChain chain = getRvTlvs();

        connInfo = RvConnectionInfo.readConnectionInfo(chain);

        ByteBlock data = getServiceData();
        if (data != null) {
            version = BinaryTools.getUInt(data, 0);
        } else {
            version = -1;
        }
    }

    public VoiceReqRvCmd(RvConnectionInfo connInfo) {
        this(VERSION_DEFAULT, connInfo);
    }

    public VoiceReqRvCmd(long version, RvConnectionInfo connInfo) {
        super(CapabilityBlock.BLOCK_VOICE);

        DefensiveTools.checkRange(version, "version", -1);

        this.connInfo = connInfo;
        this.version = version;
    }

    public final long getVersion() { return version; }

    public final RvConnectionInfo getConnInfo() { return connInfo; }

    protected void writeRvTlvs(OutputStream out) throws IOException {
        connInfo.write(out);
    }

    protected boolean hasServiceData() { return true; }

    protected void writeServiceData(OutputStream out) throws IOException {
        if (version != -1) BinaryTools.writeUInt(out, version);
    }

    public String toString() {
        return "VoiceReqRvCmd: version=" + version + ", connInfo=" + connInfo;
    }
}