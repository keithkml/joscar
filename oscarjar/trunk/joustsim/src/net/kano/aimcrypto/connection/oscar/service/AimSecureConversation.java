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

package net.kano.aimcrypto.connection.oscar.service;

import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.PrivateSecurityInfo;
import net.kano.aimcrypto.AimSession;
import net.kano.aimcrypto.BuddySecurityInfo;
import net.kano.aimcrypto.connection.AimConnection;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.MinimalEncoder;
import net.kano.joscar.EncodedStringInfo;
import net.kano.joscar.DefensiveTools;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSSignedData;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.security.NoSuchProviderException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class AimSecureConversation extends Conversation {
    private final AimSession aimSession;
    private AimConnection conn;
    private PrivateSecurityInfo privates = null;
    private BuddySecurityInfo buddyInfo = null;

    public AimSecureConversation(AimSession session, Screenname buddy) {
        super(buddy);
        DefensiveTools.checkNull(session, "session");

        this.aimSession = session;
    }

    protected void initialize() {
        aimSession.getBuddySecurityInfo(getBuddy());
    }

    public void sendMessage(Message msg) throws ConversationNotOpenException {
        ensureOpen();
    }

    protected static byte[] encryptMsg(PrivateKey signingKey,
            X509Certificate localCert, String msg)
            throws NoSuchProviderException, NoSuchAlgorithmException,
            CMSException, IOException {

        byte[] signedDataBlock = cmsSignString(signingKey, localCert, msg);

        CMSEnvelopedDataGenerator gen = new CMSEnvelopedDataGenerator();
        gen.addKeyTransRecipient(localCert);
        CMSEnvelopedData envData = gen.generate(
                new CMSProcessableByteArray(signedDataBlock),
                "2.16.840.1.101.3.4.1.2", "BC");

        return envData.getEncoded();
    }

    protected static byte[] cmsSignString(PrivateKey signingKey,
            X509Certificate localCert, String msg) throws IOException,
            NoSuchProviderException, NoSuchAlgorithmException, CMSException {

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(bout, "US-ASCII");
        EncodedStringInfo encodeInfo = MinimalEncoder.encodeMinimally(msg);
        String charset = encodeInfo.getCharset().toLowerCase();
        osw.write("Content-Transfer-Encoding: binary\r\n"
                + "Content-Type: text/x-aolrtf; charset=" + charset + "\r\n"
                + "Content-Language: en\r\n"
                + "\r\n");
        osw.flush();
        bout.write(encodeInfo.getData());

        byte[] dataToSign = bout.toByteArray();
        byte[] signedData = signData(signingKey, localCert, dataToSign);

        bout = new ByteArrayOutputStream();
        osw = new OutputStreamWriter(bout, "US-ASCII");
        osw.write("Content-Transfer-Encoding: binary\r\n"
                + "Content-Type: application/pkcs7-mime; charset=us-ascii\r\n"
                + "Content-Language: en\r\n"
                + "\r\n");
        osw.flush();
        bout.write(signedData);
        byte[] dataToEncrypt = bout.toByteArray();
        return dataToEncrypt;
    }

    protected static byte[] signData(PrivateKey signingKey,
            X509Certificate localCert, byte[] dataToSign)
            throws NoSuchProviderException, NoSuchAlgorithmException,
            CMSException, IOException {

        byte[] signedData;
        CMSSignedDataGenerator sgen = new CMSSignedDataGenerator();
        sgen.addSigner(signingKey, localCert,
                CMSSignedDataGenerator.DIGEST_MD5);
        CMSSignedData csd = sgen.generate(
                new CMSProcessableByteArray(dataToSign),
                true, "BC");
        signedData = csd.getEncoded();
        return signedData;
    }
}
