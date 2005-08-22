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

import net.kano.joscar.ByteBlock;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snac.SnacRequestAdapter;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snaccmd.CertificateInfo;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.conn.ExtraInfoAck;
import net.kano.joscar.snaccmd.conn.MyInfoRequest;
import net.kano.joscar.snaccmd.conn.ServiceRedirect;
import net.kano.joscar.snaccmd.conn.ServiceRequest;
import net.kano.joscar.snaccmd.conn.SetEncryptionInfoCmd;
import net.kano.joscar.snaccmd.conn.SetExtraInfoCmd;
import net.kano.joscar.snaccmd.conn.SetIdleCmd;
import net.kano.joscar.snaccmd.conn.YourInfoCmd;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

//TODO: many commands send back a YourInfoCmd. we should make this more MVC so setIdleSince does not write to a field, but an incoming YourInfoCmd does not
public class MainBosService extends BosService {
    private static final Logger LOGGER = Logger
            .getLogger(MainBosService.class.getName());

    private CopyOnWriteArrayList<MainBosServiceListener> listeners
            = new CopyOnWriteArrayList<MainBosServiceListener>();
    private Date idleSince = null;
    private boolean visibleStatus = true;
    private String availMsg = null;

    public MainBosService(AimConnection aimConnection,
            OscarConnection oscarConnection) {
        super(aimConnection, oscarConnection);
    }

    protected void serverReady() {
        List<ExtraInfoBlock> blocks = Arrays.asList(
                new ExtraInfoBlock(ExtraInfoBlock.TYPE_CERTINFO_HASHA,
                        new ExtraInfoData(ExtraInfoData.FLAG_HASH_PRESENT,
                                CertificateInfo.HASHA_DEFAULT)),

                new ExtraInfoBlock(ExtraInfoBlock.TYPE_CERTINFO_HASHB,
                        new ExtraInfoData(ExtraInfoData.FLAG_HASH_PRESENT,
                                CertificateInfo.HASHB_DEFAULT)));
        sendSnac(new SetEncryptionInfoCmd(blocks));
        sendSnac(new MyInfoRequest());
    }

    public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
        SnacCommand snac = snacPacketEvent.getSnacCommand();

        if (snac instanceof YourInfoCmd) {
            YourInfoCmd yic = (YourInfoCmd) snac;
            for (MainBosServiceListener listener : listeners) {
                listener.handleYourInfo(this, yic.getUserInfo());
            }
        } else if (snac instanceof ExtraInfoAck) {
            //TODO: write myiconmanager to upload and download our icon when necessary
            ExtraInfoAck ack = (ExtraInfoAck) snac;
            for (MainBosServiceListener listener : listeners) {
                listener.handleYourExtraInfo(ack.getExtraInfos());
            }
        }

        super.handleSnacPacket(snacPacketEvent);
    }


    public void setIdleSince(@NotNull Date at) throws IllegalArgumentException {
        DefensiveTools.checkNull(at, "at");

        long idlems = System.currentTimeMillis() - at.getTime();
        if (idlems < 0) {
            throw new IllegalArgumentException("attempted to set idle time "
                    + "to " + at + ", which was " + idlems + "ms ago");
        }
        long idleSecs = idlems / 1000;

        setIdleSinceDate(at);
        sendSnac(new SetIdleCmd(idleSecs));
    }

    public void setUnidle() {
        setIdleSinceDate(null);
        sendSnac(new SetIdleCmd(0));
    }

    private synchronized void setIdleSinceDate(Date since) {
        this.idleSince = since;
    }

    public synchronized Date getIdleSince() { return idleSince; }

    public synchronized void setVisibleStatus(boolean visible) {
        if (visibleStatus == visible) return;
        this.visibleStatus = visible;
        long flag = visibleStatus ? FullUserInfo.ICQSTATUS_DEFAULT
                : FullUserInfo.ICQSTATUS_INVISIBLE;
        sendSnac(new SetExtraInfoCmd(flag));
    }

    public synchronized void setStatusMessage(@Nullable String msg) {
        if (availMsg == null ? msg == null : availMsg.equals(msg)) return;
        availMsg = msg;
        String useMsg = availMsg == null ? "" : availMsg;
        ExtraInfoBlock availBlock = new ExtraInfoBlock(ExtraInfoBlock.TYPE_AVAILMSG,
                ExtraInfoData.getAvailableMessageBlock(useMsg));
        sendSnac(new SetExtraInfoCmd(Arrays.asList(availBlock)));
        ExtraInfoBlock secretBlock = new ExtraInfoBlock(0x0009,
                new ExtraInfoData(ExtraInfoData.FLAG_AVAILMSG_PRESENT,
                        ByteBlock.wrap(new byte[4])));
        sendSnac(new SetExtraInfoCmd(Arrays.asList(secretBlock)));
    }

    public void addMainBosServiceListener(MainBosServiceListener listener) {
        listeners.add(listener);
    }

    public void removeMainBosServiceListener(MainBosServiceListener listener) {
        listeners.remove(listener);
    }

    public void requestService(final int service,
            final OpenedExternalServiceListener listener) {
        sendSnacRequest(new ServiceRequest(service), new SnacRequestAdapter() {
            public void handleResponse(SnacResponseEvent e) {
                SnacCommand cmd = e.getSnacCommand();
                if (cmd instanceof ServiceRedirect) {
                    ServiceRedirect redirect = (ServiceRedirect) cmd;
                    int returnedFamily = redirect.getSnacFamily();
                    if (service != returnedFamily) {
                        LOGGER.warning("server returned service "
                                + returnedFamily + " when we requested " + service);
                    }
                    listener.handleServiceRedirect(MainBosService.this,
                            service, redirect.getRedirectHost(),
                            redirect.getRedirectPort(),
                            redirect.getCookie());
                } else {
                    LOGGER.warning("unexpected response to service request: " + cmd);
                }
            }
        });
    }
}
