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

package net.kano.aimcrypto.connection.oscar.service.icbm;

import net.kano.aimcrypto.connection.AimConnection;
import net.kano.aimcrypto.connection.BuddyInfo;
import net.kano.aimcrypto.connection.BuddyInfoManager;
import net.kano.aimcrypto.connection.oscar.OscarConnection;
import net.kano.aimcrypto.connection.oscar.service.icbm.Conversation;
import net.kano.aimcrypto.connection.oscar.service.icbm.EncryptedAimMessage;
import net.kano.aimcrypto.connection.oscar.service.icbm.EncryptedAimMessageInfo;
import net.kano.aimcrypto.connection.oscar.service.icbm.IcbmListener;
import net.kano.aimcrypto.connection.oscar.service.Service;
import net.kano.aimcrypto.Screenname;
import net.kano.joscar.snaccmd.icbm.IcbmCommand;
import net.kano.joscar.snaccmd.icbm.ParamInfoRequest;
import net.kano.joscar.snaccmd.icbm.ParamInfoCmd;
import net.kano.joscar.snaccmd.icbm.ParamInfo;
import net.kano.joscar.snaccmd.icbm.SetParamInfoCmd;
import net.kano.joscar.snaccmd.icbm.RecvImIcbm;
import net.kano.joscar.snaccmd.icbm.MissedMessagesCmd;
import net.kano.joscar.snaccmd.icbm.MissedMsgInfo;
import net.kano.joscar.snaccmd.icbm.InstantMessage;
import net.kano.joscar.snaccmd.icbm.SendImIcbm;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.ByteBlock;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class IcbmService extends Service {
    private ParamInfo paramInfo = null;
    private SecureAimEncoder encoder = null;

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

            handleParamInfo(pic);

        } else if (snac instanceof RecvImIcbm) {
            RecvImIcbm icbm = (RecvImIcbm) snac;
            handleImIcbm(icbm);

        } else if (snac instanceof MissedMessagesCmd) {
            MissedMessagesCmd mc = (MissedMessagesCmd) snac;
            handleMissedMessages(mc);
        }
    }

    private void handleParamInfo(ParamInfoCmd pic) {
        // we need to change from the default parameter infos to something
        // cooler, so we do it here
        ParamInfo pi = pic.getParamInfo();
        long newflags = pi.getFlags()
                | ParamInfo.FLAG_CHANMSGS_ALLOWED
                | ParamInfo.FLAG_MISSEDCALLS_ALLOWED
                | ParamInfo.FLAG_TYPING_NOTIFICATION;

        ParamInfo newparams = new ParamInfo(newflags, 8000, 999, 999, 0);
        this.paramInfo = newparams;

        sendSnac(new SetParamInfoCmd(newparams));

        setReady();
    }

    private void handleMissedMessages(MissedMessagesCmd mc) {
        MissedMsgInfo[] msgs = mc.getMissedMsgInfos();
        for (int i = 0; i < msgs.length; i++) {
            MissedMsgInfo msg = msgs[i];

            Screenname sn = new Screenname(msg.getUserInfo().getScreenname());
            ImConversation conv = getImConversation(sn);

            //TODO: handle icbm buddy info

            conv.handleMissedMsg(MissedImInfo.getInstance(getScreenname(),
                    msg));
        }
    }

    private void handleImIcbm(RecvImIcbm icbm) {
        FullUserInfo senderInfo = icbm.getSenderInfo();
        if (senderInfo == null) return;
        Screenname sender = new Screenname(senderInfo.getScreenname());

        InstantMessage message = icbm.getMessage();
        if (message == null) return;

        if (message.isEncrypted()) {
            SecureAimConversation conv = getSecureAimConversation(sender);

            EncryptedAimMessage msg = EncryptedAimMessage.getInstance(icbm);
            if (msg == null) return;

            BuddyInfoManager bim = getAimConnection().getBuddyInfoManager();
            BuddyInfo buddyInfo = bim.getBuddyInfo(sender);
            ByteBlock senderHash;
            if (buddyInfo == null) senderHash = null;
            else senderHash = buddyInfo.getCertificateInfoHash();

            EncryptedAimMessageInfo minfo = EncryptedAimMessageInfo.getInstance(
                    getScreenname(), icbm, senderHash);
            if (minfo == null) return;

            System.out.println("passing encrypted IM info to " + conv);
            conv.handleIncomingMessage(minfo);

        } else {
            ImConversation conv = getImConversation(sender);

//                getAimConnection().getBuddyInfoManager().getBuddyInfo(sn);

            ImMessageInfo msg = ImMessageInfo.getInstance(getScreenname(),
                    icbm);
            System.out.println("passing normal IM to " + conv);
            conv.handleIncomingMessage(msg);
        }
    }

    private Map secureAimConvs = new HashMap();

    public SecureAimConversation getSecureAimConversation(Screenname sn) {
        boolean isnew = false;
        SecureAimConversation conv;
        synchronized(this) {
            conv = (SecureAimConversation) secureAimConvs.get(sn);
            if (conv == null) {
                isnew = true;
                conv = new SecureAimConversation(getAimConnection(), sn);
                secureAimConvs.put(sn, conv);
            }
        }
        // we need to initialize this outside of the lock to prevent deadlocks
        if (isnew) initConversation(conv);

        return conv;
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

    private void initConversation(Conversation conv) {
        conv.initialize();

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            IcbmListener listener = (IcbmListener) it.next();
            listener.newConversation(this, conv);
        }
    }

    void sendIM(Screenname buddy, String body, boolean autoresponse) {
        sendIM(buddy, new InstantMessage(body), autoresponse);
    }

    void sendIM(Screenname buddy, InstantMessage im,
            boolean autoresponse) {
        sendSnac(new SendImIcbm(buddy.getFormatted(), im, autoresponse, 0,
                false, null, null, true));
    }
}
