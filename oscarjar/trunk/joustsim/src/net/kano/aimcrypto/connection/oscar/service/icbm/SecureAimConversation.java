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
import net.kano.aimcrypto.connection.oscar.service.icbm.SecureAimDecoder.DecryptedMessageInfo;
import net.kano.aimcrypto.connection.oscar.service.info.BuddyTrustAdapter;
import net.kano.aimcrypto.connection.oscar.service.info.BuddyTrustManager;
import net.kano.aimcrypto.connection.oscar.service.info.InfoResponseAdapter;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.icbm.InstantMessage;
import org.bouncycastle.cms.CMSException;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SecureAimConversation extends Conversation {
    private final AimConnection conn;
    private SecureAimEncoder encoder = new SecureAimEncoder();
    private SecureAimDecoder decoder = new SecureAimDecoder();
    private PrivateKeysInfo privates = null;
    private BuddyCertificateInfo currentSecurityInfo = null;

    private boolean trusted = false;

    private List undecryptedQueue = new LinkedList();

    protected SecureAimConversation(AimConnection conn, Screenname buddy) {
        super(buddy);

        DefensiveTools.checkNull(conn, "session");

        this.conn = conn;

        setAlwaysOpen();

        privates = conn.getAimSession().getPrivateKeysInfo();
        encoder.setLocalKeys(privates);
        decoder.setLocalKeys(privates);
    }

    protected void initialize() {
        BuddyTrustManager trustManager = conn.getBuddyTrustManager();
        trustManager.addBuddyTrustListener(new BuddyTrustAdapter() {
            public void gotTrustedCertificateChange(BuddyTrustManager manager,
                    Screenname buddy, BuddyCertificateInfo info) {
                if (!buddy.equals(getBuddy())) return;

                System.out.println("got trusted cert for " + buddy);
                currentSecurityInfo = info;
            }

            public void gotUntrustedCertificateChange(BuddyTrustManager manager,
                    Screenname buddy, BuddyCertificateInfo info) {
                if (!buddy.equals(getBuddy())) return;

                System.out.println("got untrusted cert for " + buddy);
                currentSecurityInfo = info;
            }

            public void gotUnknownCertificateChange(BuddyTrustManager manager,
                    Screenname buddy, ByteBlock newHash) {
                if (!buddy.equals(getBuddy())) return;

                currentSecurityInfo = null;

                System.out.println("requesting security info for " + buddy);
                requestCertInfo();
            }

            public void buddyTrusted(BuddyTrustManager certificateManager,
                    Screenname buddy, ByteBlock trustedhash, BuddyCertificateInfo info) {
                System.out.println("got thing for " + buddy);
                System.out.println("inside listener for: " + getBuddy());
                if (!buddy.equals(getBuddy())) return;

                System.out.println("buddy is now trusted: " + buddy);
                storeBuddyInfo(info);
            }

            public void buddyTrustRevoked(BuddyTrustManager certificateManager,
                    Screenname buddy, ByteBlock hash, BuddyCertificateInfo info) {
                if (!buddy.equals(getBuddy())) return;

                System.out.println("buddy is no longer trusted: " + buddy);
                encoder.setBuddyCerts(null);
                decoder.setBuddyCerts(null);
                setTrusted(false);
            }
        });
        Screenname buddy = getBuddy();
        BuddyCertificateInfo info = trustManager.getCurrentCertificateInfo(buddy);
        if (info == null) {
            System.out.println("requesting initial security info from " + buddy);
            requestCertInfo();
        } else {
            System.out.println("already have security info for " + buddy + "!");
            storeBuddyInfo(info);
        }
        trusted = trustManager.isTrusted(buddy);
    }

    private void requestCertInfo() {
        //TODO: don't actually request info here
        conn.getInfoService().requestCertificateInfo(getBuddy(),
                new InfoResponseAdapter());
    }

    private void storeBuddyInfo(BuddyCertificateInfo info) {
        encoder.setBuddyCerts(info);
        decoder.setBuddyCerts(info);
        setTrusted(true);
    }

    private synchronized void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

    public synchronized boolean isTrusted() { return trusted; }

    public void sendMessage(Message msg) throws ConversationException {
        if (!isTrusted()) throw new NotTrustedException(this);

        IcbmService icbmService = conn.getIcbmService();
        if (icbmService == null) {
            throw new ConversationException("there's no ICBM service open to "
                    + "send through", this);
        }

        byte[] encrypted;
        try {
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
        if (!(minfo instanceof EncryptedAimMessageInfo)) {
            throw new IllegalArgumentException("SecureAimConversation can't "
                    + "handle message information objects of type "
                    + minfo.getClass().getName() + " (" + minfo + ")");
        }

        EncryptedAimMessageInfo info = (EncryptedAimMessageInfo) minfo;
        EncryptedAimMessage msg = (EncryptedAimMessage) info.getMessage();

        DecryptedMessageInfo decrypted;
        try {
            decrypted = decoder.decryptMessage(msg.getEncryptedForm());
        } catch (CMSException e) {
            e.printStackTrace();
            return;
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
            return;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        } catch (InvalidSignatureException e) {
            e.printStackTrace();
            return;
        } catch (NoLocalKeysException e) {
            fireIncomingEvent(DecryptableAimMessageInfo.getInstance(info, currentSecurityInfo));
            return;
        } catch (NoBuddyKeysException e) {
            fireIncomingEvent(DecryptableAimMessageInfo.getInstance(info, currentSecurityInfo));
            e.printStackTrace();
            return;
        }
        if (decrypted == null) {
            //TODO: decryption failed
//            fireDecryptionFailedEvent(new DecryptionFailureInfo());
            return;
        }

        String decryptedMsg = decrypted.getMessage();
        BuddyCertificateInfo securityInfo
                = (BuddyCertificateInfo) decrypted.getSecurityInfo();

        DecryptedAimMessageInfo dinfo
                = DecryptedAimMessageInfo.getInstance(info, decryptedMsg,
                        securityInfo);

        fireIncomingEvent(dinfo);
    }

    private void fireDecryptionFailedEvent(DecryptionFailureInfo info) {
        for (Iterator it = getListeners().iterator(); it.hasNext();) {
            ConversationListener listener = (ConversationListener) it.next();
            if (listener instanceof SecureAimConversationListener) {
                SecureAimConversationListener sl
                        = (SecureAimConversationListener) listener;

            }
        }
    }
}
