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

package net.kano.aimcrypto.connection.oscar.service.icbm;

import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.config.BuddyCertificateInfo;
import net.kano.aimcrypto.config.PrivateKeysInfo;
import net.kano.aimcrypto.connection.AimConnection;
import net.kano.aimcrypto.connection.BuddyInfo;
import net.kano.aimcrypto.connection.BuddyInfoManager;
import net.kano.aimcrypto.connection.BuddyInfoTracker;
import net.kano.aimcrypto.connection.BuddyInfoTrackerListener;
import net.kano.aimcrypto.connection.GlobalBuddyInfoListener;
import net.kano.aimcrypto.connection.oscar.service.icbm.SecureAimDecoder.DecryptedMessageInfo;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.icbm.InstantMessage;
import org.bouncycastle.cms.CMSException;

import java.beans.PropertyChangeEvent;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class SecureAimConversation extends Conversation {
    private final AimConnection conn;
    private final BuddyInfoManager buddyInfoMgr;
    private final BuddyInfoTracker buddyInfoTracker;

    private SecureAimEncoder encoder = new SecureAimEncoder();
    private SecureAimDecoder decoder = new SecureAimDecoder();

    private final BuddyInfoTrackerListener trackerListener
            = new BuddyInfoTrackerListener() {
            };
    private final GlobalBuddyInfoListener buddyInfoListener
            = new GlobalBuddyInfoListener() {
        public void newBuddyInfo(BuddyInfoManager manager, Screenname buddy,
                BuddyInfo info) {
        }

        public void buddyInfoChanged(BuddyInfoManager manager, Screenname buddy,
                BuddyInfo info, PropertyChangeEvent event) {
            if (!buddy.equals(getBuddy())) return;

            if (event.getPropertyName().equals(BuddyInfo.PROP_CERTIFICATE_INFO)) {
                setBuddyCerts((BuddyCertificateInfo) event.getNewValue());
            }
        }

        public void receivedStatusUpdate(BuddyInfoManager manager,
                Screenname buddy, BuddyInfo info) {
        }
    };

    private PrivateKeysInfo privates = null;
    private BuddyCertificateInfo buddyCertInfo = null;

    protected SecureAimConversation(AimConnection conn, Screenname buddy) {
        super(buddy);

        DefensiveTools.checkNull(conn, "session");

        this.conn = conn;
        buddyInfoMgr = conn.getBuddyInfoManager();
        buddyInfoTracker = conn.getBuddyInfoTracker();
    }

    protected synchronized void initialize() {
        PrivateKeysInfo pkinfo = conn.getAimSession().getPrivateKeysInfo();
        privates = pkinfo;
        encoder.setLocalKeys(pkinfo);
        decoder.setLocalKeys(pkinfo);
    }

    private synchronized void setBuddyCerts(BuddyCertificateInfo certInfo) {
        buddyCertInfo = certInfo;
    }

    protected synchronized void opened() {
        Screenname buddy = getBuddy();
        System.out.println("secure conversation opened");

        buddyInfoMgr.addGlobalBuddyInfoListener(buddyInfoListener);
        buddyInfoTracker.addTracker(buddy, trackerListener);
        BuddyInfo buddyInfo = buddyInfoMgr.getBuddyInfo(buddy);
        setBuddyCerts(buddyInfo == null ? null : buddyInfo.getCertificateInfo());
    }

    protected void closed() {
        buddyInfoMgr.removeGlobalBuddyInfoListener(buddyInfoListener);
        buddyInfoTracker.removeTracker(getBuddy(), trackerListener);
    }

    public synchronized void sendMessage(Message msg)
            throws ConversationException {
        IcbmService icbmService = conn.getIcbmService();
        if (icbmService == null) {
            throw new ConversationException("no ICBM service present to "
                    + "send through", this);
        }

        byte[] encrypted;
        try {
            encoder.setBuddyCerts(buddyCertInfo);
            encrypted = encoder.encryptMsg(msg.getMessageBody());

        } catch (NoBuddyKeysException e) {
            throw new NotTrustedException(e, this);

        } catch (Exception e) {
            throw new EncryptionFailedException(e, this);
        }

        InstantMessage im = new InstantMessage(ByteBlock.wrap(encrypted));
        icbmService.sendIM(getBuddy(), im, msg.isAutoResponse());
    }

    protected void handleIncomingMessage(MessageInfo minfo) {
        open();

        if (!(minfo instanceof EncryptedAimMessageInfo)) {
            throw new IllegalArgumentException("SecureAimConversation can't "
                    + "handle incoming message objects of type "
                    + minfo.getClass().getName() + " (" + minfo + ")");
        }

        EncryptedAimMessageInfo info = (EncryptedAimMessageInfo) minfo;
        EncryptedAimMessage msg = (EncryptedAimMessage) info.getMessage();

        BuddyCertificateInfo certInfo = info.getCertificateInfo();
        DecryptedMessageInfo decrypted = null;
        Exception exception = null;
        synchronized(this) {
            try {
                decoder.setBuddyCerts(info.getCertificateInfo());
                decrypted = decoder.decryptMessage(msg.getEncryptedForm());
            } catch (Exception e) {
                exception = e;
            }
        }

        if (exception != null) {
            if (exception instanceof NoSuchProviderException
                    || exception instanceof NoSuchAlgorithmException
                    || exception instanceof CMSException) {
                fireUndecryptableEvent(info, certInfo,
                        UndecryptableAimMessageInfo.Reason.DECRYPT_ERROR,
                        exception);

            } else if (exception instanceof InvalidSignatureException) {
                fireUndecryptableEvent(info,
                        certInfo,
                        UndecryptableAimMessageInfo.Reason.BAD_SIGNATURE,
                        exception);

            } else if (exception instanceof NoLocalKeysException
                    || exception instanceof NoBuddyKeysException) {
                fireDecryptableEvent(info, certInfo);

            } else {
                fireUndecryptableEvent(info, certInfo,
                        UndecryptableAimMessageInfo.Reason.UNKNOWN, exception);
            }
            return;
        }

        String decryptedMsg = decrypted == null ? null : decrypted.getMessage();

        if (decrypted == null || decryptedMsg == null) {
            fireUndecryptableEvent(info, certInfo,
                    UndecryptableAimMessageInfo.Reason.UNKNOWN, null);
            return;
        }


        DecryptedAimMessageInfo dinfo = DecryptedAimMessageInfo.getInstance(
                info, decryptedMsg, certInfo);

        fireIncomingEvent(dinfo);
    }

    private void fireUndecryptableEvent(EncryptedAimMessageInfo info,
            BuddyCertificateInfo certInfo,
            UndecryptableAimMessageInfo.Reason reason, Exception exception) {
        UndecryptableAimMessageInfo minfo
                = UndecryptableAimMessageInfo.getInstance(info, certInfo,
                        reason, exception);
        fireIncomingEvent(minfo);
    }

    private void fireDecryptableEvent(EncryptedAimMessageInfo info,
            BuddyCertificateInfo certInfo) {
        fireIncomingEvent(DecryptableAimMessageInfo.getInstance(info,
                certInfo));
    }
}
