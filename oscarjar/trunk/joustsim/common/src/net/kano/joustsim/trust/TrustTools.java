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
 *  File created by keith @ Feb 5, 2004
 *
 */

package net.kano.joustsim.trust;

import net.kano.joscar.ByteBlock;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public final class TrustTools {
    private TrustTools() { }

    public static boolean isSigned(X509Certificate signer, X509Certificate cert) {
        try {
            cert.verify(signer.getPublicKey());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isCertificateAuthority(X509Certificate certificate) {
        return certificate.getBasicConstraints() != -1;
    }

    public static X509Certificate loadX509Certificate(File file)
            throws CertificateException, NoSuchProviderException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
        FileInputStream fin = new FileInputStream(file);
        Certificate cert;
        try {
            fin.getChannel().lock(0L, Long.MAX_VALUE, true);
            cert = cf.generateCertificate(fin);
        } finally {
            fin.close();
        }
        if (cert == null) {
            throw new NullPointerException("Unknown error: Certificate was "
                    + "null");
        }
        if (!(cert instanceof X509Certificate)) {
            throw new IllegalArgumentException("this file is not an X.509 "
                    + "certificate, it's a " + cert.getClass().getName());
        }
        X509Certificate xcert = (X509Certificate) cert;
        return xcert;
    }

    public static X509Certificate decodeCertificate(ByteBlock certData)
            throws NoSuchProviderException, CertificateException {

        CertificateFactory factory
                = CertificateFactory.getInstance("X.509", "BC");
        InputStream is = ByteBlock.createInputStream(certData);
        return (X509Certificate) factory.generateCertificate(is);
    }
}
