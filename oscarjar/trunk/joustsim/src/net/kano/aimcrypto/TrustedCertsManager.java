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

package net.kano.aimcrypto;

import net.kano.aimcrypto.config.CertificateHolder;
import net.kano.aimcrypto.config.SignerTrustManager;
import net.kano.aimcrypto.config.TrustChangeListener;
import net.kano.aimcrypto.config.CertificateTrustManager;
import net.kano.aimcrypto.config.TrustTools;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

//TODO: call TrustedCertsManager listeners on trust changes
public class TrustedCertsManager {
    private final CertificateTrustManager certTrustMgr;
    private final SignerTrustManager signerTrustMgr;

    private Map trackedCerts = new HashMap();
    private Map signers = new HashMap();

    private CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();

    public TrustedCertsManager(CertificateTrustManager certTrustMgr,
            SignerTrustManager signerTrustMgr) {
        DefensiveTools.checkNull(certTrustMgr, "certTrustMgr");
        DefensiveTools.checkNull(signerTrustMgr, "signerTrustMgr");

        this.certTrustMgr = certTrustMgr;
        this.signerTrustMgr = signerTrustMgr;

        certTrustMgr.addTrustListener(new TrustChangeListener() {
            public void trustAdded(CertificateTrustManager manager,
                    X509Certificate cert) {
                certTrustAdded(cert);
            }

            public void trustRemoved(CertificateTrustManager manager,
                    X509Certificate cert) {
                certTrustRemoved(cert);
            }
        });
        signerTrustMgr.addTrustListener(new TrustChangeListener() {
            public void trustAdded(CertificateTrustManager manager,
                    X509Certificate cert) {
                signerTrustAdded(cert);
            }

            public void trustRemoved(CertificateTrustManager manager,
                    X509Certificate cert) {
                signerTrustRemoved(cert);
            }
        });
    }

    private synchronized void signerTrustAdded(X509Certificate signer) {
        for (Iterator it = trackedCerts.values().iterator(); it.hasNext();) {
            TrustInfo info = (TrustInfo) it.next();
            X509Certificate cert = info.getCertificate();
            if (TrustTools.isSigned(signer, cert)) {
                registerSignerSignee(info, signer);
            }
        }
    }

    private synchronized void signerTrustRemoved(X509Certificate cert) {
        SignerInfo info = (SignerInfo) signers.get(cert);
        if (info == null) return;

        TrustInfo[] signedCerts = info.getSignedCerts();
        for (int i = 0; i < signedCerts.length; i++) {
            TrustInfo signedCert = signedCerts[i];
            signedCert.removeSigner(info);
        }
        signers.remove(cert);
    }

    private synchronized void registerSignerSignee(TrustInfo info,
            X509Certificate signer) {
        info.addSigner(registerSigner(signer, info));
    }

    private synchronized void certTrustAdded(X509Certificate cert) {
        TrustInfo info = (TrustInfo) trackedCerts.get(cert);
        if (info == null) return;

        info.setExplicitlyTrusted(false);
    }

    private synchronized void certTrustRemoved(X509Certificate cert) {
        TrustInfo info = (TrustInfo) trackedCerts.get(cert);
        if (info == null) return;

        info.setExplicitlyTrusted(true);
    }

    public void addTrustedCertsListener(TrustedCertsListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeTrustedCertsListener(TrustedCertsListener l) {
        listeners.remove(l);
    }

    public synchronized boolean addTrackedCertificate(X509Certificate cert) {
        DefensiveTools.checkNull(cert, "cert");

        CertificateHolder holder = new CertificateHolder(cert);
        if (trackedCerts.containsKey(holder)) return false;

        DefensiveTools.checkNull(cert, "cert");

        TrustInfo info = new TrustInfo(cert);
        info.setExplicitlyTrusted(certTrustMgr.isTrusted(cert));
        X509Certificate[] ts = signerTrustMgr.getTrustedSigners(cert);
        for (int i = 0; i < ts.length; i++) {
            X509Certificate signer = ts[i];

            if (signer == null) continue;

            registerSignerSignee(info, signer);
        }
        trackedCerts.put(holder, info);

        return true;
    }

    private synchronized SignerInfo registerSigner(X509Certificate signer,
            TrustInfo signee) {
        DefensiveTools.checkNull(signer, "signer");
        DefensiveTools.checkNull(signee, "signee");

        SignerInfo info = getSignerInfoInstance(signer);
        info.addSignee(signee);
        return info;
    }

    private SignerInfo getSignerInfoInstance(X509Certificate signer) {
        SignerInfo info = (SignerInfo) signers.get(signer);
        if (info == null) {
            info = new SignerInfo(signer);
            signers.put(signer, info);
        }
        return info;
    }

    public synchronized boolean removeTrackedCertificate(X509Certificate cert) {
        DefensiveTools.checkNull(cert, "cert");

        Object removed = trackedCerts.remove(new CertificateHolder(cert));
        return removed != null;
    }

    private static final class TrustInfo extends CertificateHolder {
        private boolean explicitlyTrusted = false;
        private final Set signers = new HashSet(5);

        public TrustInfo(X509Certificate cert) {
            super(cert);
        }

        public synchronized boolean isSomehowTrusted() {
            return explicitlyTrusted || !signers.isEmpty();
        }

        public synchronized boolean isExplicitlyTrusted() {
            return explicitlyTrusted;
        }

        public synchronized void setExplicitlyTrusted(boolean trusted) {
            this.explicitlyTrusted = trusted;
        }

        public synchronized void addSigner(SignerInfo signer) {
            DefensiveTools.checkNull(signer, "signer");

            signers.add(signer);
        }

        public synchronized SignerInfo[] getSigners() {
            return (SignerInfo[]) signers.toArray(new SignerInfo[signers.size()]);
        }

        public synchronized boolean isSignedBy(SignerInfo signer) {
            DefensiveTools.checkNull(signer, "signer");

            return signers.contains(signer);
        }

        public synchronized void removeSigner(SignerInfo signer) {
            DefensiveTools.checkNull(signer, "signer");

            signers.remove(signer);
        }

        public synchronized void addSigners(SignerInfo[] trusted) {
            DefensiveTools.checkNull(trusted, "trusted");
            X509Certificate[] safe = (X509Certificate[])
                    DefensiveTools.getNonnullArray(trusted, "trusted");

            signers.addAll(Arrays.asList(safe));
        }
    }

    private static final class SignerInfo extends CertificateHolder {
        private final Set signees = new HashSet(10);

        public SignerInfo(X509Certificate cert) {
            super(cert);
        }

        public synchronized void addSignee(TrustInfo signee) {
            DefensiveTools.checkNull(signee, "signee");

            assert TrustTools.isSigned(getCertificate(), signee.getCertificate());

            signees.add(signee);
        }

        public synchronized TrustInfo[] getSignedCerts() {
            return (TrustInfo[]) signees.toArray(new TrustInfo[signees.size()]);
        }

        public synchronized void removeSignee(TrustInfo signee) {
            DefensiveTools.checkNull(signee, "signee");

            signees.remove(signee);
        }
    }
}
