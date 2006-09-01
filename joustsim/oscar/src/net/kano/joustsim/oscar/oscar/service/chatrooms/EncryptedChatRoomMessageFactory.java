/*
 *  Copyright (c) 2005, The Joust Project
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
 */

package net.kano.joustsim.oscar.oscar.service.chatrooms;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.snaccmd.chat.ChatMsg;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.trust.PrivateKeys;
import net.kano.joustsim.trust.KeyPair;
import net.kano.joustsim.trust.TrustPreferences;

import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;
import javax.crypto.spec.IvParameterSpec;

import org.bouncycastle.asn1.cms.EncryptedContentInfo;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.BERConstructedOctetString;
import org.bouncycastle.asn1.BERTaggedObject;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.BERSequence;
import org.bouncycastle.asn1.pkcs.EncryptedData;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Random;
import java.util.Locale;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.InvalidKeyException;
import java.security.InvalidAlgorithmParameterException;

public class EncryptedChatRoomMessageFactory implements ChatRoomMessageFactory {
    private AimConnection aimConnection;
    private ChatRoomService roomService;
    private SecretKey key;
    private SecureRandom random = new SecureRandom();

    public EncryptedChatRoomMessageFactory(AimConnection aimConnection,
            ChatRoomService roomService, SecretKey key) {
        this.aimConnection = aimConnection;
        this.roomService = roomService;
        this.key = key;
    }

    public ChatMessage createMessage(ChatRoomService service, ChatRoomUser user,
            ChatMsg message) {
        ByteBlock data = message.getMessageData();

        return null;
    }

    public ChatMsg encodeMessage(String message) throws EncodingException {
        byte[] data;
        try {
            data = getEncodedMessageData(message);
        } catch (IOException e) {
            throw new EncodingException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new EncodingException(e);
        } catch (NoSuchProviderException e) {
            throw new EncodingException(e);
        } catch (NoSuchPaddingException e) {
            throw new EncodingException(e);
        } catch (InvalidKeyException e) {
            throw new EncodingException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new EncodingException(e);
        } catch (IllegalBlockSizeException e) {
            throw new EncodingException(e);
        } catch (BadPaddingException e) {
            throw new EncodingException(e);
        } catch (CMSException e) {
            throw new EncodingException(e);
        }
        return new ChatMsg(ChatMsg.CONTENTTYPE_SECURE,
                ChatMsg.CONTENTENCODING_DEFAULT, "UTF-8", ByteBlock.wrap(data),
                Locale.getDefault());
    }

    private byte[] getEncodedMessageData(String message) throws IOException,
            NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, CMSException {
        byte[] dataToEncrypt = getCmsSignedBlock(message);

        byte[] iv = new byte[16];
        random.nextBytes(iv);

        Cipher c = Cipher.getInstance("2.16.840.1.101.3.4.1.42", "BC");
        c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

        byte[] encrypted = c.doFinal(dataToEncrypt);

        EncryptedContentInfo eci = new EncryptedContentInfo(
                new DERObjectIdentifier("1.2.840.113549.1.7.1"),
                new AlgorithmIdentifier(
                        new DERObjectIdentifier("2.16.840.1.101.3.4.1.42"),
                        new DEROctetString(iv)),
                new BERConstructedOctetString(encrypted));
        EncryptedData ed = new EncryptedData(eci.getContentType(),
                eci.getContentEncryptionAlgorithm(),
                eci.getEncryptedContent());

        BERTaggedObject bert = new BERTaggedObject(0, ed.getDERObject());
        DERObjectIdentifier rootid
                = new DERObjectIdentifier("1.2.840.113549.1.7.6");
        ASN1EncodableVector vec = new ASN1EncodableVector();
        vec.add(rootid);
        vec.add(bert);
        ByteArrayOutputStream fout = new ByteArrayOutputStream();
        ASN1OutputStream out = new ASN1OutputStream(fout);
        out.writeObject(new BERSequence(vec));
        out.close();
        return fout.toByteArray();
    }


    private byte[] getCmsSignedBlock(String msg)
            throws IOException, NoSuchProviderException,
            NoSuchAlgorithmException, CMSException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(bout, "US-ASCII");
        osw.write("Content-Transfer-Encoding: binary\r\n"
                + "Content-Type: text/x-aolrtf; charset=us-ascii\r\n"
                + "Content-Language: en\r\n"
                + "\r\n");
        osw.flush();
        bout.write(msg.getBytes("UTF-8"));

        byte[] dataToSign = bout.toByteArray();
        byte[] signedData = signData(dataToSign);

        bout = new ByteArrayOutputStream();
        osw = new OutputStreamWriter(bout, "US-ASCII");
        osw.write("Content-Transfer-Encoding: binary\r\n"
                + "Content-Type: application/pkcs7-mime; charset=us-ascii\r\n"
                + "Content-Language: en\r\n"
                + "\r\n");
        osw.flush();
        bout.write(signedData);
        return bout.toByteArray();
    }

    private byte[] signData(byte[] dataToSign) throws NoSuchProviderException,
            NoSuchAlgorithmException, CMSException, IOException {
        CMSSignedDataGenerator sgen = new CMSSignedDataGenerator();
        TrustPreferences localPrefs = aimConnection.getLocalPrefs();
        KeyPair signingKeys = localPrefs.getPrivateKeysPreferences()
                .getKeysInfo().getSigningKeys();
        sgen.addSigner(signingKeys.getPrivateKey(),
                signingKeys.getPublicCertificate(),
                CMSSignedDataGenerator.DIGEST_MD5);
        CMSSignedData csd = sgen.generate(
                new CMSProcessableByteArray(dataToSign),
                true, "BC");
        return csd.getEncoded();

    }
/*
    void method() {
        try {
            InputStream in = ByteBlock.createInputStream(data);
            ASN1InputStream ain = new ASN1InputStream(in);

            ASN1Sequence seq = (ASN1Sequence) ain.readObject();
            BERTaggedObject bert = (BERTaggedObject) seq.getObjectAt(1);
            ASN1Sequence seq2 = (ASN1Sequence) bert.getObject();
            EncryptedData encd = new EncryptedData(seq2);
            EncryptedContentInfo enci = encd.getEncryptedContentInfo();
            byte[] encryptedData = enci.getEncryptedContent().getOctets();

            AlgorithmIdentifier alg = enci.getContentEncryptionAlgorithm();

            byte[] iv = ((ASN1OctetString) alg.getParameters()).getOctets();

            Cipher c = Cipher.getInstance(alg.getObjectId().getId(), "BC");
            c.init(Cipher.DECRYPT_MODE, getChatKey(chat),
                    new IvParameterSpec(iv));

            ByteBlock result = ByteBlock.wrap(c.doFinal(encryptedData));

            OscarTools.HttpHeaderInfo hinfo = OscarTools.parseHttpHeader(result);
            InputStream csdin = ByteBlock.createInputStream(hinfo.getData());
            CMSSignedData csd = new CMSSignedData(csdin);
            X509Certificate cert = getCert(sn);
            if (cert != null) {
                Collection signers = csd.getSignerInfos().getSigners();
                for (Object signer : signers) {
                    SignerInformation si = (SignerInformation) signer;
                    boolean verified = si.verify(cert, "BC");
                    if (!verified) {
                        System.err.println("NOTE: message not verified");
                    }
                }
            } else {
                System.err.println("[couldn't verify message because I don't "
                        + "have a cert for " + sn + "]");
            }
            byte[] scBytes = (byte[]) csd.getSignedContent().getContent();
            ByteBlock signedContent = ByteBlock.wrap(scBytes);
            OscarTools.HttpHeaderInfo hinfo2
                    = OscarTools.parseHttpHeader(signedContent);
            return OscarTools.getInfoString(hinfo2.getData(),
                    hinfo2.getHeaders().get("content-type"));
        } catch (Exception e) {
            throw new SecureSessionException(e);
        }

    }*/
}
