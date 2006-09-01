/*
 * Copyright (c) 2005, The Joust Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 * - Neither the name of the Joust Project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * File created by keithkml
 */

package net.kano.joustsim.oscar.oscar.service.chatrooms;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.snaccmd.MiniRoomInfo;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.BuddyInfoManager;
import net.kano.joustsim.oscar.oscar.NoBuddyKeysException;
import net.kano.joustsim.trust.BuddyCertificateInfo;
import net.kano.joustsim.trust.KeyPair;
import net.kano.joustsim.trust.PrivateKeys;
import net.kano.joustsim.trust.PrivateKeysPreferences;
import net.kano.joustsim.trust.TrustPreferences;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.cms.KeyTransRecipientInfo;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.jetbrains.annotations.NotNull;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collection;

public class KeyExtractionTools {
  @NotNull public static SecretKey extractChatKey(ByteBlock data,
      X509Certificate buddySigningCert, TrustPreferences trustPrefs)
      throws NoPrivateKeyException, CertificateNotYetValidException,
      CertificateExpiredException, BadKeyException {

    KeyPair myEncryptingKeys = getMyEncryptingKeys(trustPrefs);

    try {
      CMSSignedData csd
          = new CMSSignedData(ByteBlock.createInputStream(data));
      Collection<SignerInformation> signers = csd.getSignerInfos()
          .getSigners();
      for (SignerInformation signer : signers) {
        boolean verified;
        try {
          verified = signer.verify(buddySigningCert, "BC");
        } catch (CertificateExpiredException e) {
          throw e;
        } catch (CertificateNotYetValidException e) {
          throw e;
        }
        if (!verified) throw new BadKeyException();
      }
      CMSProcessableByteArray cpb
          = (CMSProcessableByteArray) csd.getSignedContent();
      ByteBlock signedContent = ByteBlock
          .wrap((byte[]) cpb.getContent());
      MiniRoomInfo mri = MiniRoomInfo.readMiniRoomInfo(signedContent);

      ByteBlock rest = signedContent.subBlock(mri.getTotalSize());
      int kdlen = BinaryTools.getUShort(rest, 0);
      ByteBlock keyData = rest.subBlock(2, kdlen);

      InputStream kdin = ByteBlock.createInputStream(keyData);
      ASN1InputStream ain = new ASN1InputStream(kdin);
      ASN1Sequence root = (ASN1Sequence) ain.readObject();
      ASN1Sequence seq = (ASN1Sequence) root.getObjectAt(0);
      KeyTransRecipientInfo ktr = KeyTransRecipientInfo
          .getInstance(seq);
      DERObjectIdentifier keyoid
          = (DERObjectIdentifier) root.getObjectAt(1);

      String encoid = ktr.getKeyEncryptionAlgorithm().getObjectId()
          .getId();
      Cipher cipher = Cipher.getInstance(encoid, "BC");
      cipher.init(Cipher.DECRYPT_MODE,
          myEncryptingKeys.getPrivateKey());

      byte[] result = cipher.doFinal(ktr.getEncryptedKey().getOctets());
      return new SecretKeySpec(result, keyoid.getId());

    } catch (NoSuchProviderException e) {
    } catch (BadPaddingException e) {
    } catch (NoSuchAlgorithmException e) {
    } catch (IOException e) {
    } catch (IllegalBlockSizeException e) {
    } catch (InvalidKeyException e) {
    } catch (NoSuchPaddingException e) {
    } catch (CMSException e) {
    }
    throw new BadKeyException();
  }

  private @NotNull static KeyPair getMyEncryptingKeys(TrustPreferences prefs)
      throws NoPrivateKeyException {
    PrivateKeysPreferences pkPrefs = prefs.getPrivateKeysPreferences();
    PrivateKeys keysInfo = pkPrefs.getKeysInfo();
    if (keysInfo == null) throw new NoPrivateKeyException();
    return keysInfo.getEncryptingKeys();
  }

  @NotNull public static X509Certificate getBuddySigningCert(Screenname sn,
      BuddyInfoManager infoMgr) throws NoBuddyKeysException {
    BuddyCertificateInfo certInfo = infoMgr.getBuddyInfo(sn)
        .getCertificateInfo();
    if (certInfo == null) throw new NoBuddyKeysException();
    X509Certificate buddySigningCert = certInfo.getSigningCertificate();
    if (buddySigningCert == null) throw new NoBuddyKeysException();
    return buddySigningCert;
  }
}
