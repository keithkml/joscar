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
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;

public final class PrivateSecurityInfo {
    private final Screenname screenname;
    private KeysInfo keysInfo;

    public PrivateSecurityInfo(Screenname screenname) {
        this(screenname, null);
    }

    public PrivateSecurityInfo(Screenname screenname, KeysInfo keysInfo) {
        DefensiveTools.checkNull(screenname, "screenname");

        this.screenname = screenname;
        this.keysInfo = keysInfo;
    }

    public void loadKeysFromP12(URL p12url, String alias, String pass)
            throws BadKeysException {
        try {
            loadKeysImpl(p12url, alias, pass);
        } catch (BadKeysException bke) {
            throw bke;
        } catch (Exception e) {
            throw new BadKeysException(e);
        }
    }

    private void loadKeysImpl(URL url, String alias, String pass)
            throws BadKeysException, Exception {
        char[] passChars = pass.toCharArray();

        KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
        ks.load(url.openStream(), passChars);

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

        synchronized(this) {
            keysInfo = new KeysInfo((RSAPrivateKey) privKey,
                    (X509Certificate) pubCert);
        }
    }

    public synchronized void forgetKeys() {
        keysInfo = null;
    }

    public Screenname getScreenname() { return screenname; }

    public KeysInfo getKeysInfo() { return keysInfo; }
}
