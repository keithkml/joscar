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
 *  File created by keith @ Jan 25, 2004
 *
 */

package net.kano.aimcrypto.config;

import net.kano.aimcrypto.CertificatePairHolder;
import net.kano.aimcrypto.Screenname;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;

import java.security.cert.X509Certificate;

public final class BuddyCertificateInfo implements CertificatePairHolder {
    private final Screenname buddy;
    private final ByteBlock hash;
    private final X509Certificate encryptionCert;
    private final X509Certificate signingCert;
    private final boolean upToDate;

    public BuddyCertificateInfo(Screenname buddy, ByteBlock hash) {
        this(buddy, hash, null, null, false);
    }

    public BuddyCertificateInfo(Screenname buddy, ByteBlock hash,
            X509Certificate encCert, X509Certificate signingCert) {
        this(buddy, hash, encCert,  signingCert, true);
    }

    public BuddyCertificateInfo(Screenname buddy, ByteBlock hash,
            X509Certificate encCert, X509Certificate signingCert,
            boolean upToDate) {
        this.upToDate = upToDate;
        DefensiveTools.checkNull(buddy, "buddy");
        DefensiveTools.checkNull(hash, "hash");

        this.buddy = buddy;
        this.hash = hash;
        this.encryptionCert = encCert;
        this.signingCert = signingCert;
    }

    public final Screenname getBuddy() { return buddy; }

    public ByteBlock getCertificateInfoHash() { return hash; }

    public boolean hasAnyCertificates() {
        return encryptionCert != null || signingCert != null;
    }

    public boolean hasBothCertificates() {
        return encryptionCert != null && signingCert != null;
    }

    public boolean isUpToDate() { return upToDate; }

    public X509Certificate getEncryptionCertificate() { return encryptionCert; }

    public X509Certificate getSigningCertificate() { return signingCert; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuddyCertificateInfo)) return false;

        final BuddyCertificateInfo buddyCertificateInfo = (BuddyCertificateInfo) o;

        if (upToDate != buddyCertificateInfo.upToDate) return false;
        if (!buddy.equals(buddyCertificateInfo.buddy)) return false;
        if (!hash.equals(buddyCertificateInfo.hash)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = buddy.hashCode();
        result = 29 * result + hash.hashCode();
        result = 29 * result + (upToDate ? 1 : 0);
        return result;
    }
}
