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
 *  File created by keith @ Sep 29, 2003
 *
 */

package net.kano.joscardemo.security;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.MiscTools;
import net.kano.joscar.OscarTools;
import net.kano.joscar.snaccmd.FullRoomInfo;
import net.kano.joscar.snaccmd.MiniRoomInfo;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.BERConstructedOctetString;
import org.bouncycastle.asn1.BERSequence;
import org.bouncycastle.asn1.BERTaggedObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.cms.EncryptedContentInfo;
import org.bouncycastle.asn1.cms.EncryptedData;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.cms.KeyTransRecipientInfo;
import org.bouncycastle.asn1.cms.RecipientIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.KeyTransRecipientInformation;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class BCSecureSession extends SecureSession {

    private Random random = new Random();

    private KeyStore keystore;
    private PrivateKey privateKey;
    private X509Certificate pubCert;
    private Map chatKeys = new HashMap();
    private Map certs = new HashMap();

    {
        try {
            Security.addProvider(new BouncyCastleProvider());
        } catch (SecurityException e) {
            System.err.println("[couldn't load Bouncy Castle JCE provider]");
        }

        try {
            loadKeys();
        } catch (Exception e) {
            System.err.println("couldn't load private key: "
                    + MiscTools.getClassName(e) + ": " + e.getMessage());
        }
    }

    BCSecureSession() { }

    // generic

    private void loadKeys() throws KeyStoreException,
            NoSuchProviderException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException {
        keystore = KeyStore.getInstance("PKCS12", "BC");
        keystore.load(new FileInputStream("certificate-info.p12"),
                "pass".toCharArray());
        String alias = (String) keystore.aliases().nextElement();
        pubCert = (X509Certificate) keystore.getCertificate(alias);
        privateKey = (PrivateKey) keystore.getKey(alias,
                "pass".toCharArray());
    }

    public X509Certificate getMyCertificate() { return pubCert; }

    public void setCert(String sn, X509Certificate cert) {
        certs.put(OscarTools.normalize(sn), cert);
    }

    public X509Certificate getCert(String sn) {
        return (X509Certificate) certs.get(OscarTools.normalize(sn));
    }

    public boolean hasCert(String sn) { return getCert(sn) != null; }

    private byte[] signData(byte[] dataToSign) throws SecureSessionException {
        try {
            byte[] signedData;
            CMSSignedDataGenerator sgen = new CMSSignedDataGenerator();
            sgen.addSigner(privateKey, pubCert,
                    CMSSignedDataGenerator.DIGEST_MD5);
            CMSSignedData csd = sgen.generate(
                    new CMSProcessableByteArray(dataToSign),
                    true, "BC");
            signedData = csd.getEncoded();
            return signedData;
        } catch (Exception e) {
            throw new SecureSessionException(e);
        }
    }

    private byte[] getCmsSignedBlock(String msg)
            throws IOException, SecureSessionException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(bout, "US-ASCII");
        osw.write("Content-Transfer-Encoding: binary\r\n"
                + "Content-Type: text/x-aolrtf; charset=us-ascii\r\n"
                + "Content-Language: en\r\n"
                + "\r\n");
        osw.flush();
        bout.write(msg.getBytes());

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
        byte[] dataToEncrypt = bout.toByteArray();
        return dataToEncrypt;
    }

    // IM's

    public String decodeEncryptedIM(String sn, ByteBlock encData)
            throws SecureSessionException {
        try {
            InputStream encin = ByteBlock.createInputStream(encData);
            CMSEnvelopedData ced = new CMSEnvelopedData(encin);
            Collection recip = ced.getRecipientInfos().getRecipients();

            if (recip.isEmpty()) return null;

            KeyTransRecipientInformation rinfo
                    = (KeyTransRecipientInformation) recip.iterator().next();

            byte[] content = rinfo.getContent(privateKey, "BC");

            OscarTools.HttpHeaderInfo hdrInfo
                    = OscarTools.parseHttpHeader(ByteBlock.wrap(content));

            InputStream in = ByteBlock.createInputStream(hdrInfo.getData());
            CMSSignedData csd = new CMSSignedData(in);
            SignerInformationStore signerInfos = csd.getSignerInfos();
            Collection signers = signerInfos.getSigners();
            for (Iterator sit = signers.iterator(); sit.hasNext();) {
                SignerInformation si = (SignerInformation) sit.next();
                boolean verified = si.verify(getCert(sn), "BC");
                System.out.println("verified: " + verified);
            }
            CMSProcessable signedContent = csd.getSignedContent();
            ByteBlock data = ByteBlock.wrap((byte[]) signedContent.getContent());

            OscarTools.HttpHeaderInfo bodyInfo
                    = OscarTools.parseHttpHeader(data);

            String msg = OscarTools.getInfoString(bodyInfo.getData(),
                    (String) bodyInfo.getHeaders().get("content-type"));

            return OscarTools.stripHtml(msg);
        } catch (Exception e) {
            throw new SecureSessionException(e);
        }
    }

    public ByteBlock encryptIM(String sn, String msg)
            throws SecureSessionException {
        try {
            X509Certificate cert = getCert(sn);

            if (cert == null) return null;

            byte[] signedDataBlock = getCmsSignedBlock(msg);

            CMSEnvelopedDataGenerator gen = new CMSEnvelopedDataGenerator();
            gen.addKeyTransRecipient(cert);
            CMSEnvelopedData envData = gen.generate(
                    new CMSProcessableByteArray(signedDataBlock),
                    "2.16.840.1.101.3.4.1.2", "BC");

            return ByteBlock.wrap(envData.getEncoded());
        } catch (Exception e) {
            throw new SecureSessionException(e);
        }
    }

    // SSL

    public ServerSocket createSSLServerSocket(final String sn)
            throws SecureSessionException {
        try {
            ServerSocket socket;
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keystore, "pass".toCharArray());
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(kmf.getKeyManagers(),
                    new TrustManager[] { new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs,
                                String string) throws CertificateException {
                            checkTrusted(certs);
                        }

                        public void checkServerTrusted(X509Certificate[] certs,
                                String string) throws CertificateException {
                            checkTrusted(certs);
                        }

                        private void checkTrusted(X509Certificate[] certs)
                                throws CertificateException {
                            System.out.println("checking trust for " + Arrays.asList(certs));
                            X509Certificate usercert = getCert(sn);
                            for (int i = 0; i < certs.length; i++) {
                                if (certs[i].equals(usercert)) return;
                            }
                            throw new CertificateException();
                        }
                    } },
                    new SecureRandom());

            SSLServerSocketFactory factory = context.getServerSocketFactory();
            SSLServerSocket sss = (SSLServerSocket)
                    factory.createServerSocket(7050);
            sss.setNeedClientAuth(true);
            socket = sss;
            return socket;
        } catch (Exception e) {
            throw new SecureSessionException(e);
        }
    }

    public Socket createSecureSocket(InetAddress address, int port)
            throws SecureSessionException {
        try {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keystore, "pass".toCharArray());
            SSLContext context = SSLContext.getInstance("SSL");
            KeyManager[] kms = kmf.getKeyManagers();
            X509KeyManager xkm = null;
            for (int i = 0; i < kms.length; i++) {
                if (kms[i] instanceof X509KeyManager) {
                    System.out.println("found x509keymgr");
                    if (xkm == null) xkm = (X509KeyManager) kms[i];
                }
            }
            final X509KeyManager xkm1 = xkm;
            context.init(
                    new KeyManager[] {new X509KeyManager() {
                        public PrivateKey getPrivateKey(String string) {
                            return xkm1.getPrivateKey(string);
                        }

                        public X509Certificate[] getCertificateChain(String string) {
                            return xkm1.getCertificateChain(string);
                        }

                        public String[] getClientAliases(String string, Principal[] principals) {
                            return xkm1.getClientAliases(string, principals);
                        }

                        public String[] getServerAliases(String string, Principal[] principals) {
                            return xkm1.getServerAliases(string, principals);
                        }

                        public String chooseServerAlias(String string, Principal[] principals,
                                Socket socket) {
                            return xkm1.chooseServerAlias(string, principals, socket);
                        }

                        public String chooseClientAlias(String[] strings, Principal[] principals,
                                Socket socket) {
                            String alias = xkm1.chooseClientAlias(strings, null, socket);
                            return alias;
                        }
                    }},
                    new TrustManager[]{new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs,
                                String string) throws CertificateException {
                            return;
                        }

                        public void checkServerTrusted(X509Certificate[] certs,
                                String string) throws CertificateException {
                            return;
                        }

//            private void checkTrusted(X509Certificate[] certs) {
//                X509Certificate usercert = tester.getCert(session.getScreenname());
//                for (int i = 0; i < certs.length; i++) {
//                    System.out.println("*** checking trust for " + certs[i]
//                            + " ***");
//                    if (certs[i].equals(usercert)) {
//                        System.out.println("trusted!!");
//                        return;
//                    }
//                }
//                System.out.println("couldn't find cert matching " + usercert);
//                throw new CertificateException();
//            }
                    }},
                    new SecureRandom());

            SSLSocketFactory fact = context.getSocketFactory();
            SSLSocket socket = (SSLSocket) fact.createSocket(address, port);
            socket.startHandshake();
            return socket;
        } catch (Exception e) {
            throw new SecureSessionException(e);
        }
    }

    // chat


    public void generateKey(String chat) throws SecureSessionException {
        try {
            KeyGenerator kg
                    = KeyGenerator.getInstance("2.16.840.1.101.3.4.1.42");
            kg.init(new SecureRandom());
            setChatKey(chat, kg.generateKey());
        } catch (Exception e) {
            throw new SecureSessionException(e);
        }
    }

    public void setChatKey(String roomName, SecretKey chatKey) {
        chatKeys.put(roomName, chatKey);
    }

    public SecretKey getChatKey(String chat) {
        return (SecretKey) chatKeys.get(chat);
    }

    public ByteBlock genChatSecurityInfo(FullRoomInfo chatInfo, String sn)
            throws SecureSessionException {
        try {
            SecretKey key = getChatKey(chatInfo.getRoomName());
            byte[] keyData = key.getEncoded();

            Cipher c = Cipher.getInstance("1.2.840.113549.1.1.1", "BC");
            X509Certificate cert = getCert(sn);
            c.init(Cipher.ENCRYPT_MODE, cert);

            byte[] encryptedKey = c.doFinal(keyData);

            X509Name xname = new X509Name(cert.getSubjectDN().getName());
            IssuerAndSerialNumber ias
                    = new IssuerAndSerialNumber(xname, cert.getSerialNumber());
            KeyTransRecipientInfo ktr = new KeyTransRecipientInfo(
                    new RecipientIdentifier(ias),
                    new AlgorithmIdentifier("1.2.840.113549.1.1.1"),
                    new DEROctetString(encryptedKey));

            ASN1EncodableVector vec = new ASN1EncodableVector();
            vec.add(ktr);
            vec.add(new DERObjectIdentifier("2.16.840.1.101.3.4.1.42"));

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ASN1OutputStream aout = new ASN1OutputStream(bout);
            aout.writeObject(new DERSequence(vec));
            aout.close();

            ByteArrayOutputStream bout2 = new ByteArrayOutputStream();
            new MiniRoomInfo(chatInfo).write(bout2);
            BinaryTools.writeUShort(bout2, bout.size());
            bout.writeTo(bout2);

            return ByteBlock.wrap(signData(bout2.toByteArray()));
        } catch (Exception e) {
            throw new SecureSessionException(e);
        }
    }

    public SecretKey extractChatKey(String sn, ByteBlock data)
            throws SecureSessionException {
        try {
            CMSSignedData csd
                    = new CMSSignedData(ByteBlock.createInputStream(data));
            Collection signers = csd.getSignerInfos().getSigners();
            for (Iterator sit = signers.iterator(); sit.hasNext();) {
                SignerInformation si = (SignerInformation) sit.next();
                boolean verified = si.verify(getCert(sn), "BC");
                if (!verified) System.err.println("NOTE: key not verified!");
            }
            CMSProcessableByteArray cpb
                    = (CMSProcessableByteArray) csd.getSignedContent();
            ByteBlock signedContent = ByteBlock.wrap((byte[]) cpb.getContent());
            MiniRoomInfo mri = MiniRoomInfo.readMiniRoomInfo(signedContent);

            ByteBlock rest = signedContent.subBlock(mri.getTotalSize());
            int kdlen = BinaryTools.getUShort(rest, 0);
            ByteBlock keyData = rest.subBlock(2, kdlen);

            InputStream kdin = ByteBlock.createInputStream(keyData);
            ASN1InputStream ain = new ASN1InputStream(kdin);
            ASN1Sequence root = (ASN1Sequence) ain.readObject();
            ASN1Sequence seq = (ASN1Sequence) root.getObjectAt(0);
            KeyTransRecipientInfo ktr = KeyTransRecipientInfo.getInstance(seq);
            DERObjectIdentifier keyoid
                    = (DERObjectIdentifier) root.getObjectAt(1);

            String encoid = ktr.getKeyEncryptionAlgorithm().getObjectId().getId();
            Cipher cipher = Cipher.getInstance(encoid, "BC");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] result = cipher.doFinal(ktr.getEncryptedKey().getOctets());
            return new SecretKeySpec(result, keyoid.getId());
        } catch (Exception e) {
            throw new SecureSessionException(e);
        }
    }

    public String parseChatMessage(String chat, String sn, ByteBlock data)
            throws SecureSessionException {
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
                for (Iterator sit = signers.iterator(); sit.hasNext();) {
                    SignerInformation si = (SignerInformation) sit.next();
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
                    (String) hinfo2.getHeaders().get("content-type"));
        } catch (Exception e) {
            throw new SecureSessionException(e);
        }
    }

    public byte[] encryptChatMsg(String chat, String msg)
            throws SecureSessionException {
        try {
            byte[] dataToEncrypt = getCmsSignedBlock(msg);

            byte[] iv = new byte[16];
            random.nextBytes(iv);

            Cipher c = Cipher.getInstance("2.16.840.1.101.3.4.1.42", "BC");
            c.init(Cipher.ENCRYPT_MODE, getChatKey(chat), new IvParameterSpec(iv));

            byte[] encrypted = c.doFinal(dataToEncrypt);

            EncryptedContentInfo eci = new EncryptedContentInfo(
                    new DERObjectIdentifier("1.2.840.113549.1.7.1"),
                    new AlgorithmIdentifier(
                            new DERObjectIdentifier("2.16.840.1.101.3.4.1.42"),
                            new DEROctetString(iv)),
                    new BERConstructedOctetString(encrypted));
            EncryptedData ed = new EncryptedData(eci, null);

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
        } catch (Exception e) {
            throw new SecureSessionException(e);
        }
    }
}
