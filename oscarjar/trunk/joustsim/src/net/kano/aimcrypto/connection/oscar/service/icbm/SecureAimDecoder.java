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
 *  File created by keith @ Jan 29, 2004
 *
 */

package net.kano.aimcrypto.connection.oscar.service.icbm;

import net.kano.aimcrypto.AimCertificateHolder;
import net.kano.aimcrypto.KeyPair;
import net.kano.aimcrypto.config.PrivateKeysInfo;
import net.kano.aimcrypto.config.PrivateKeysInfo;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.OscarTools;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.KeyTransRecipientInformation;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Collection;
import java.util.Iterator;
import java.util.Collections;

public class SecureAimDecoder extends SecureAimCodec {
    public synchronized DecryptedMessageInfo decryptMessage(
            ByteBlock encryptedData)
            throws CMSException, NoSuchProviderException,
            NoSuchAlgorithmException, InvalidSignatureException,
            NoBuddyKeysException, NoLocalKeysException {

        AimCertificateHolder buddyCerts = getBuddyCerts();
        if (buddyCerts == null) throw new NoBuddyKeysException();
        X509Certificate signingCert = buddyCerts.getSigningCertificate();
        if (signingCert == null) throw new NoBuddyKeysException();

        PrivateKeysInfo localKeys = getLocalKeys();
        if (localKeys == null) throw new NoLocalKeysException();
        KeyPair signingKeys = localKeys.getSigningKeys();
        if (signingKeys == null) throw new NoLocalKeysException();
        RSAPrivateKey signingKey = signingKeys.getPrivateKey();
        if (signingKey == null) throw new NoLocalKeysException();

        InputStream encin = ByteBlock.createInputStream(encryptedData);
        CMSEnvelopedData ced = new CMSEnvelopedData(encin);
        Collection recip = ced.getRecipientInfos().getRecipients();

        if (recip.isEmpty()) {
            //TODO: throw new NoRecipientsException
            return null;
        }

        KeyTransRecipientInformation rinfo
                = (KeyTransRecipientInformation) recip.iterator().next();

        byte[] content = rinfo.getContent(signingKey, "BC");

        OscarTools.HttpHeaderInfo hdrInfo
                = OscarTools.parseHttpHeader(ByteBlock.wrap(content));

        InputStream in = ByteBlock.createInputStream(hdrInfo.getData());
        CMSSignedData csd = new CMSSignedData(in);

        SignerInformationStore signerInfos = csd.getSignerInfos();
        Collection signers = signerInfos.getSigners();

        for (Iterator sit = signers.iterator(); sit.hasNext();) {
            SignerInformation si = (SignerInformation) sit.next();
            boolean verified;
            try {
                verified = si.verify(signingCert, "BC");
            } catch (CertificateExpiredException e) {
                throw new InvalidSignatureException(e, si);
            } catch (CertificateNotYetValidException e) {
                throw new InvalidSignatureException(e, si);
            }
            if (!verified) {
                throw new InvalidSignatureException(si);
            }
        }

        CMSProcessable signedContent = csd.getSignedContent();
        ByteBlock data = ByteBlock.wrap((byte[]) signedContent.getContent());

        OscarTools.HttpHeaderInfo bodyInfo
                = OscarTools.parseHttpHeader(data);

        String msg = OscarTools.getInfoString(bodyInfo.getData(),
                (String) bodyInfo.getHeaders().get("content-type"));

        return new DecryptedMessageInfo(msg, buddyCerts);
    }

    public final class DecryptedMessageInfo {
        private final String str;
        private final AimCertificateHolder securityInfo;

        private DecryptedMessageInfo(String str,
                AimCertificateHolder securityInfo) {
            DefensiveTools.checkNull(str, "str");
            DefensiveTools.checkNull(securityInfo, "securityInfo");

            this.str = str;
            this.securityInfo = securityInfo;
        }

        public String getMessage() { return str; }

        public AimCertificateHolder getSecurityInfo() { return securityInfo; }
    }
}
