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

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.rvproto.rvproxy.cmd.AbstractRvProxyCmd;
import net.kano.joscar.rvproto.rvproxy.cmd.RvProxyHeader;
import net.kano.joscar.snaccmd.OscarTools;
import net.kano.joscar.snaccmd.StringBlock;

import java.io.IOException;
import java.io.OutputStream;

public class RvProxyInitSendCmd extends AbstractRvProxyCmd {
    private final String sn;
    private final Long icbmMessageId;

    protected RvProxyInitSendCmd(RvProxyHeader header) {
        super(header);

        ByteBlock data = header.getData();

        StringBlock snInfo = OscarTools.readScreenname(data);

        if (snInfo != null) {
            sn = snInfo.getString();

            ByteBlock rest = data.subBlock(snInfo.getTotalSize());

            icbmMessageId = new Long(BinaryTools.getLong(rest, 0));
        } else {
            sn = null;
            icbmMessageId =null;
        }
    }

    public RvProxyInitSendCmd(String sn, long icbmMessageId) {
        super(RvProxyHeader.HEADERTYPE_INIT_SEND);

        DefensiveTools.checkNull(sn, "sn");

        this.sn = sn;
        this.icbmMessageId = new Long(icbmMessageId);
    }

    public final String getScreenname() { return sn; }

    public final Long getIcbmMessageId() { return icbmMessageId; }

    public void writeData(OutputStream out) throws IOException {
        if (sn != null) {
            OscarTools.writeScreenname(out, sn);

            if (icbmMessageId != null) {
                BinaryTools.writeLong(out, icbmMessageId.longValue());
            }
        }
    }

    public String toString() {
        return "RvProxyInitSendCmd: sn=" + sn
                + ", icbmMessageId=" + icbmMessageId;
    }
}