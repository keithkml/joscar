/*
 *  Copyright (c) 2004, The Joust Project
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
 *  File created by keith @ Jan 25, 2004
 *
 */

package net.kano.joustsim.oscar.oscar.service.bos;

import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snaccmd.CertificateInfo;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joscar.snaccmd.conn.MyInfoRequest;
import net.kano.joscar.snaccmd.conn.SetEncryptionInfoCmd;
import net.kano.joscar.snaccmd.conn.SetIdleCmd;
import net.kano.joscar.snaccmd.conn.YourInfoCmd;

import java.util.Date;

public class MainBosService extends BosService {
    private Date idleSince = null;

    public MainBosService(AimConnection aimConnection,
            OscarConnection oscarConnection) {
        super(aimConnection, oscarConnection);
    }

    protected void serverReady() {
        ExtraInfoBlock[] blocks = new ExtraInfoBlock[] {
            new ExtraInfoBlock(ExtraInfoBlock.TYPE_CERTINFO_HASHA,
                    new ExtraInfoData(ExtraInfoData.FLAG_HASH_PRESENT,
                            CertificateInfo.HASHA_DEFAULT)),

            new ExtraInfoBlock(ExtraInfoBlock.TYPE_CERTINFO_HASHB,
                    new ExtraInfoData(ExtraInfoData.FLAG_HASH_PRESENT,
                            CertificateInfo.HASHB_DEFAULT)),
        };
        sendSnac(new SetEncryptionInfoCmd(blocks));
        sendSnac(new MyInfoRequest());
    }

    public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
        SnacCommand snac = snacPacketEvent.getSnacCommand();

        if (snac instanceof YourInfoCmd) {
            YourInfoCmd yic = (YourInfoCmd) snac;
            String formattedsn = yic.getUserInfo().getScreenname();
            //TODO: pass your info command to listeners (create listeners, too)
        }

        super.handleSnacPacket(snacPacketEvent);
    }

    public void setIdleSince(Date at) throws IllegalArgumentException {
        DefensiveTools.checkNull(at, "at");

        long idlems = System.currentTimeMillis() - at.getTime();
        if (idlems < 0) {
            throw new IllegalArgumentException("attempted to set idle time "
                    + "to " + at + ", which was " + idlems + "ms ago");
        }
        long idleSecs = idlems / 1000;

        sendSnac(new SetIdleCmd(idleSecs));
        setIdleSinceDate(at);
    }

    public void setUnidle() {
        setIdleSinceDate(null);
        sendSnac(new SetIdleCmd(0));
    }

    private synchronized void setIdleSinceDate(Date since) {
        this.idleSince = since;
    }

    public synchronized Date getIdleSince() { return idleSince; }
}
