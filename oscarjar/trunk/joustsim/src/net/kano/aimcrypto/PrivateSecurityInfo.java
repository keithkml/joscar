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
 *  File created by keith @ Jan 14, 2004
 *
 */

package net.kano.aimcrypto;

import net.kano.joscar.DefensiveTools;
import net.kano.aimcrypto.exceptions.BadKeysException;
import net.kano.aimcrypto.exceptions.InsufficientKeysException;
import net.kano.aimcrypto.exceptions.NoSuchAliasException;
import net.kano.aimcrypto.exceptions.WrongKeyTypesException;

import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.io.IOException;

public final class PrivateSecurityInfo {
    private final Screenname screenname;
    private PrivateKeysInfo keysInfo;

    public PrivateSecurityInfo(Screenname screenname) {
        this(screenname, null);
    }

    public PrivateSecurityInfo(Screenname screenname, PrivateKeysInfo keysInfo) {
        DefensiveTools.checkNull(screenname, "screenname");

        this.screenname = screenname;
        this.keysInfo = keysInfo;
    }

    public Screenname getScreenname() { return screenname; }

    public void loadKeysFromP12(URL p12url, String signingAlias,
            String encryptionAlias, String pass)
            throws BadKeysException {
        try {
            loadKeysImpl(p12url, signingAlias, encryptionAlias, pass);
        } catch (BadKeysException bke) {
            throw bke;
        } catch (Exception e) {
            throw new BadKeysException(e);
        }
    }

    private void loadKeysImpl(URL url, String signingAlias,
            String encryptionAlias, String pass)
            throws BadKeysException, NoSuchProviderException,
            KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException {
        boolean same = encryptionAlias.equals(signingAlias);
        char[] passChars = pass.toCharArray();

        KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
        ks.load(url.openStream(), passChars);

        KeyPair signingKeys = loadKeys(ks, signingAlias, passChars);
        KeyPair encryptionKeys;
        if (same) encryptionKeys = signingKeys;
        else encryptionKeys = loadKeys(ks, encryptionAlias, passChars);

        synchronized(this) {
            keysInfo = new PrivateKeysInfo(signingKeys, encryptionKeys);
        }
    }

    private static KeyPair loadKeys(KeyStore ks, String alias, char[] passChars)
            throws KeyStoreException, NoSuchAliasException,
            NoSuchAlgorithmException, UnrecoverableKeyException,
            InsufficientKeysException, WrongKeyTypesException {

        if (!ks.containsAlias(alias)) {
            throw new NoSuchAliasException(alias);
        }

        Key privKey = ks.getKey(alias, passChars);
        Certificate pubCert = ks.getCertificate(alias);

        if (privKey == null || pubCert == null) {
            throw new InsufficientKeysException(privKey != null,
                    pubCert != null);
        }

        boolean isrsa = privKey instanceof RSAPrivateKey;
        boolean isx509 = pubCert instanceof X509Certificate;
        if (!isrsa || !isx509) {
            throw new WrongKeyTypesException(isrsa ? null : privKey.getClass(),
                    isx509 ? null : pubCert.getClass());
        }

        return new KeyPair((RSAPrivateKey) privKey, (X509Certificate) pubCert);
    }

    public PrivateKeysInfo getKeysInfo() { return keysInfo; }

    public synchronized void forgetKeys() { keysInfo = null; }
}
