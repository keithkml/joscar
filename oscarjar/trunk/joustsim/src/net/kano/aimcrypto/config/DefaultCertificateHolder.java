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
 *  File created by keith @ Feb 2, 2004
 *
 */

package net.kano.aimcrypto.config;

import net.kano.joscar.DefensiveTools;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

public class DefaultCertificateHolder implements CertificateHolder {
    private final X509Certificate certificate;
    private final BigInteger mod;
    private final BigInteger exp;

    public DefaultCertificateHolder(X509Certificate cert) {
        DefensiveTools.checkNull(cert, "cert");

        PublicKey pubkey = cert.getPublicKey();
        if (!(pubkey instanceof RSAPublicKey)) {
            throw new IllegalArgumentException();
        }

        this.certificate = cert;
        RSAPublicKey rsaKey = (RSAPublicKey) pubkey;
        mod = rsaKey.getModulus();
        if (mod == null) {
            throw new IllegalArgumentException("modulus of key is null");
        }
        exp = rsaKey.getPublicExponent();
        if (exp == null) {
            throw new IllegalArgumentException("exponent of key is null");
        }
    }

    public final X509Certificate getCertificate() { return certificate; }

    public final int hashCode() {
        return mod.hashCode() ^ exp.hashCode();
    }

    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CertificateHolder)) return false;

        DefaultCertificateHolder holder = (DefaultCertificateHolder) obj;

        return holder.mod.equals(mod) && holder.exp.equals(exp);
    }
}
