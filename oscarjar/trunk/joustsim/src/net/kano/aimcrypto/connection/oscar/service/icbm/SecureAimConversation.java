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

import net.kano.aimcrypto.BuddySecurityInfo;
import net.kano.aimcrypto.PrivateKeysInfo;
import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.connection.AimConnection;
import net.kano.aimcrypto.connection.oscar.service.icbm.SecureAimDecoder.DecryptedMessageInfo;
import net.kano.aimcrypto.connection.oscar.service.info.CertificateAdapter;
import net.kano.aimcrypto.connection.oscar.service.info.CertificateManager;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.icbm.InstantMessage;
import org.bouncycastle.cms.CMSException;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SecureAimConversation extends Conversation {
    private final AimConnection conn;
    private SecureAimEncoder encoder = new SecureAimEncoder();
    private SecureAimDecoder decoder = new SecureAimDecoder();
    private PrivateKeysInfo privates = null;
    private BuddySecurityInfo currentSecurityInfo = null;
    private Set trustedHashes = new HashSet();

    private List inQueue = new LinkedList();
    private boolean trusted = false;

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
        CertificateManager certManager = conn.getCertificateManager();
        certManager.addListener(new CertificateAdapter() {
            public void gotUnknownCertificateChange(CertificateManager manager,
                    Screenname buddy, ByteBlock newHash) {
                if (!buddy.equals(getBuddy())) return;

                System.out.println("requesting security info for " + buddy);
                conn.getInfoService().requestSecurityInfo(buddy);

            }

            public void buddyTrusted(CertificateManager certificateManager,
                    Screenname buddy, ByteBlock trustedhash, BuddySecurityInfo info) {
                if (!buddy.equals(getBuddy())) return;

                encoder.setBuddyCerts(info);
                decoder.setBuddyCerts(info);
                setTrusted(true);
            }

            public void trustRevoked(CertificateManager manager, Screenname buddy,
                    ByteBlock hash) {
                if (!buddy.equals(getBuddy())) return;

                encoder.setBuddyCerts(null);
                decoder.setBuddyCerts(null);
                setTrusted(false);
            }
        });
        if (certManager.getSecurityInfo(getBuddy()) == null) {
            conn.getInfoService().requestSecurityInfo(getBuddy());
        }
        trusted = certManager.isTrusted(getBuddy());
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
            e.printStackTrace();
            return;
        } catch (NoBuddyKeysException e) {
            e.printStackTrace();
            return;
        }
        if (decrypted == null) {
            System.err.println("decrypted == null");
            return;
        }

        String decryptedMsg = decrypted.getMessage();
        BuddySecurityInfo securityInfo
                = (BuddySecurityInfo) decrypted.getSecurityInfo();

        DecryptedAimMessageInfo dinfo
                = DecryptedAimMessageInfo.getInstance(info, decryptedMsg,
                        securityInfo);

        fireIncomingEvent(dinfo);
    }
}
