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
 *  File created by keith @ Jan 17, 2004
 *
 */

package net.kano.aimcrypto.connection.oscar.service;

import net.kano.aimcrypto.connection.AimConnection;
import net.kano.aimcrypto.connection.oscar.OscarConnection;
import net.kano.aimcrypto.Screenname;
import net.kano.joscar.snaccmd.icbm.IcbmCommand;
import net.kano.joscar.snaccmd.icbm.ParamInfoRequest;
import net.kano.joscar.snaccmd.icbm.ParamInfoCmd;
import net.kano.joscar.snaccmd.icbm.ParamInfo;
import net.kano.joscar.snaccmd.icbm.SetParamInfoCmd;
import net.kano.joscar.snaccmd.icbm.RecvImIcbm;
import net.kano.joscar.snaccmd.icbm.MissedMessagesCmd;
import net.kano.joscar.snaccmd.icbm.MissedMsgInfo;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.CopyOnWriteArrayList;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class IcbmService extends Service {
    private ParamInfo paramInfo = null;

    private CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();

    public IcbmService(AimConnection aimConnection,
            OscarConnection oscarConnection) {
        super(aimConnection, oscarConnection, IcbmCommand.FAMILY_ICBM);
    }

    public void addIcbmListener(IcbmListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeIcbmListener(IcbmListener l) {
        listeners.remove(l);
    }

    public SnacFamilyInfo getSnacFamilyInfo() {
        return IcbmCommand.FAMILY_INFO;
    }

    public void connected() {
        sendSnac(new ParamInfoRequest());
    }

    public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
        SnacCommand snac = snacPacketEvent.getSnacCommand();

        if (snac instanceof ParamInfoCmd) {
            ParamInfoCmd pic = (ParamInfoCmd) snac;

            // we need to change from the default parameter infos to something
            // cooler

            ParamInfo pi = pic.getParamInfo();
            long newflags = pi.getFlags()
                    | ParamInfo.FLAG_CHANMSGS_ALLOWED
                    | ParamInfo.FLAG_MISSEDCALLS_ALLOWED
                    | ParamInfo.FLAG_TYPING_NOTIFICATION;
            ParamInfo newparams = new ParamInfo(newflags, 8000, 999, 999, 0);
            this.paramInfo = newparams;
            sendSnac(new SetParamInfoCmd(newparams));
            ready();

        } else if (snac instanceof RecvImIcbm) {
            RecvImIcbm icbm = (RecvImIcbm) snac;
            Screenname sn = new Screenname(icbm.getSenderInfo().getScreenname());

            if (icbm.getMessage().isEncrypted()) {
            } else {
                ImConversation conv = getImConversation(sn);
                ImMessageInfo msg = ImMessageInfo.getInstance(
                        getScreenname(), icbm);
                conv.fireIncomingEvent(msg);
            }
            
        } else if (snac instanceof MissedMessagesCmd) {
            MissedMessagesCmd mc = (MissedMessagesCmd) snac;
            MissedMsgInfo[] msgs = mc.getMissedMsgInfos();
            for (int i = 0; i < msgs.length; i++) {
                MissedMsgInfo msg = msgs[i];

                Screenname sn = new Screenname(msg.getUserInfo().getScreenname());
                ImConversation conv = getImConversation(sn);
                conv.handleMissedMsg(MissedImInfo.getInstance(getScreenname(), msg));
            }
        }
    }

    private Map imconvs = new HashMap();

    public synchronized ImConversation getImConversation(Screenname sn) {
        boolean isnew = false;
        ImConversation conv;
        synchronized(this) {
            conv = (ImConversation) imconvs.get(sn);
            if (conv == null) {
                isnew = true;
                conv = new ImConversation(getAimConnection(), sn);
                imconvs.put(sn, conv);
            }
        }
        // we need to initialize this outside of the lock to prevent deadlocks
        if (isnew) initConversation(conv);

        return conv;
    }

    private void initConversation(ImConversation conv) {
        conv.initialize();
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            IcbmListener listener = (IcbmListener) it.next();
            listener.newConversation(this, conv);
        }
    }
}
