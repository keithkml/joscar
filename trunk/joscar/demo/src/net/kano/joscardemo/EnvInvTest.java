/*
 *  Copyright (c) 2003, The Joust Project
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
 *  File created by Keith @ 5:21:28 PM
 *
 */
package net.kano.joscardemo;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.FileWritable;
import net.kano.joscar.OscarTools;
import net.kano.joscar.snaccmd.MiniRoomInfo;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.BERTaggedObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.cms.EncryptedContentInfo;
import org.bouncycastle.asn1.cms.EncryptedData;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.cms.KeyTransRecipientInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Collection;
import java.util.Iterator;

public class EnvInvTest {
    private static X509Certificate otherCert;
    private static RSAPrivateKey privateKey;
    private static SecretKey chatKey;
    private static KeyTransRecipientInfo ktr;

    private static void readKeys() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
        ks.load(new FileInputStream("certificate-info.p12"),
                "pass".toCharArray());
        String alias = (String) ks.aliases().nextElement();
        privateKey = (RSAPrivateKey) ks.getKey(alias,
                "pass".toCharArray());


        KeyStore ks2 = KeyStore.getInstance("PKCS12", "BC");
        ks2.load(new FileInputStream("mycert.p12"),
                "kanomk".toCharArray());
        String alias2 = (String) ks2.aliases().nextElement();
        otherCert = (X509Certificate) ks2.getCertificate(alias2);
    }

    public static void main(String[] args) {
        try {
            Class bcp = Class.forName(
                    "org.bouncycastle.jce.provider.BouncyCastleProvider");
            Security.addProvider((Provider) bcp.newInstance());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            readKeys();
            parseChatKey(ByteBlock.createByteBlock(new FileWritable("tmpinvblock")));
            String msg = parseChatMessage(ByteBlock.createByteBlock(
                    new FileWritable("tmpencryptedchatmsg")));
            System.out.println(msg);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CMSException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parseChatKey(ByteBlock data) throws IOException,
            CMSException, NoSuchProviderException, NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException,
            CertificateNotYetValidException, CertificateExpiredException {
        CMSSignedData csd = new CMSSignedData(ByteBlock.createInputStream(data));
        Collection signers = csd.getSignerInfos().getSigners();
        for (Iterator sit = signers.iterator(); sit.hasNext();) {
            SignerInformation si = (SignerInformation) sit.next();
            boolean verified = si.verify(otherCert, "BC");
            System.out.println("key verified: " + verified);
        }
        CMSProcessableByteArray cpb
                = (CMSProcessableByteArray) csd.getSignedContent();
        ByteBlock signedContent = ByteBlock.wrap((byte[]) cpb.getContent());
        MiniRoomInfo mri = MiniRoomInfo.readMiniRoomInfo(signedContent);

        ByteBlock rest = signedContent.subBlock(mri.getTotalSize());
        int kdlen = BinaryTools.getUShort(rest, 0);
        ByteBlock keyData = rest.subBlock(2, kdlen);

        FileOutputStream fout = new FileOutputStream("customasn1thing");
        keyData.write(fout);
        fout.close();

        InputStream kdin = ByteBlock.createInputStream(keyData);
        ASN1InputStream ain = new ASN1InputStream(kdin);
        ASN1Sequence root = (ASN1Sequence) ain.readObject();
        ASN1Sequence seq = (ASN1Sequence) root.getObjectAt(0);
        ktr = KeyTransRecipientInfo.getInstance(seq);
        IssuerAndSerialNumber iasn = ((IssuerAndSerialNumber) ktr.getRecipientIdentifier().getId());
        System.out.println(iasn.getName());
        System.out.println(iasn.getSerialNumber().getValue());
        DERObjectIdentifier keyoid = (DERObjectIdentifier) root.getObjectAt(1);

        System.out.println("key for: " + keyoid.getId());

        String encoid = ktr.getKeyEncryptionAlgorithm().getObjectId().getId();
        System.out.println("encrypted with: " + encoid);
        Cipher cipher = Cipher.getInstance(encoid, "BC");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] result = cipher.doFinal(ktr.getEncryptedKey().getOctets());
        chatKey = new SecretKeySpec(result, keyoid.getId());
    }

    private static String parseChatMessage(ByteBlock data) throws IOException,
            NoSuchAlgorithmException, NoSuchPaddingException,
            NoSuchProviderException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, CMSException, CertificateNotYetValidException,
            CertificateExpiredException {
        InputStream in = ByteBlock.createInputStream(data);
        ASN1InputStream ain = new ASN1InputStream(in);

        ASN1Sequence seq = (ASN1Sequence) ain.readObject();
        BERTaggedObject bert = (BERTaggedObject) seq.getObjectAt(1);
        ASN1Sequence seq2 = (ASN1Sequence) bert.getObject();
        EncryptedData encd = new EncryptedData(seq2);
        System.out.println(encd.getUnprotectedAttrs());
//        Enumeration objs = encd.getUnprotectedAttrs().getObjects();
//        while (objs.hasMoreElements()) {
//            System.out.println("- " + objs.nextElement());
//        }
        EncryptedContentInfo enci = encd.getEncryptedContentInfo();
        byte[] encryptedData = enci.getEncryptedContent().getOctets();

        AlgorithmIdentifier alg = enci.getContentEncryptionAlgorithm();

        byte[] iv = ((ASN1OctetString) alg.getParameters()).getOctets();

        Cipher c = Cipher.getInstance(alg.getObjectId().getId(), "BC");
        c.init(Cipher.DECRYPT_MODE, chatKey, new IvParameterSpec(iv));

        ByteBlock result = ByteBlock.wrap(c.doFinal(encryptedData));


        OscarTools.HttpHeaderInfo hinfo = OscarTools.parseHttpHeader(result);

//        FileOutputStream fout = new FileOutputStream("decryptedchatdata");
//        hinfo.getData().write(fout);
//        fout.close();
        InputStream csdin = ByteBlock.createInputStream(hinfo.getData());
        CMSSignedData csd = new CMSSignedData(csdin);
        Collection signers = csd.getSignerInfos().getSigners();
        for (Iterator sit = signers.iterator(); sit.hasNext();) {
            SignerInformation si = (SignerInformation) sit.next();
            boolean verified = si.verify(otherCert, "BC");
            if (!verified) System.out.println("NOTE: message not verified");
        }
        byte[] scBytes = (byte[]) csd.getSignedContent().getContent();
        ByteBlock signedContent = ByteBlock.wrap(scBytes);
        OscarTools.HttpHeaderInfo hinfo2
                = OscarTools.parseHttpHeader(signedContent);
        return OscarTools.getInfoString(hinfo2.getData(),
                    (String) hinfo2.getHeaders().get("content-type"));
    }
}