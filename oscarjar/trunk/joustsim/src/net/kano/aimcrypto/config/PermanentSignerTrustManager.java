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
 *  File created by keith @ Feb 3, 2004
 *
 */

package net.kano.aimcrypto.config;

import net.kano.aimcrypto.Screenname;
import net.kano.joscar.DefensiveTools;

import java.io.File;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class PermanentSignerTrustManager
        extends PermanentCertificateTrustManager {
    public PermanentSignerTrustManager(Screenname buddy, File trustedCertsDir) {
        super(buddy, trustedCertsDir);
    }

    protected boolean canBeAdded(X509Certificate certificate) {
        DefensiveTools.checkNull(certificate, "certificate");

        return certificate.getBasicConstraints() != -1;
    }

    public synchronized boolean isSignedByTrustedSigner(Certificate cert) {
        X509Certificate[] certs = getTrustedCertificates();
        for (int i = 0; i < certs.length; i++) {
            X509Certificate signer = certs[i];
            try {
                cert.verify(signer.getPublicKey());

                // if no exception was thrown, this certificate is verified
                return true;
            } catch (Exception ignored) { }
        }
        return false;
    }
}
