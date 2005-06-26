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

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class TrustedCertificatesTracker {
    private static final Logger logger
            = Logger.getLogger(TrustedCertificatesTracker.class.getName());

    private final CertificateTrustManager certTrustMgr;
    private final SignerTrustManager signerTrustMgr;

    private Map<CertificateHolder,TrustedCertificateInfoImpl> trackedCerts
            = new HashMap<CertificateHolder, TrustedCertificateInfoImpl>();
    private Map<X509Certificate,SignerInfoImpl> signers
            = new HashMap<X509Certificate, SignerInfoImpl>();

    private CopyOnWriteArrayList<TrustedCertificatesListener> listeners
            = new CopyOnWriteArrayList<TrustedCertificatesListener>();

    public TrustedCertificatesTracker(CertificateTrustManager certTrustMgr,
            SignerTrustManager signerTrustMgr) {
        this.certTrustMgr = certTrustMgr;
        this.signerTrustMgr = signerTrustMgr;

        if (certTrustMgr != null) {
            certTrustMgr.addTrustListener(new CertificateTrustListener() {
                public void trustAdded(CertificateTrustManager manager,
                        X509Certificate cert) {
                    System.out.println("TrustedCertificatesTracker: now trusted: "
                            + cert.getSubjectDN());
                    certTrustAdded(cert);
                }

                public void trustRemoved(CertificateTrustManager manager,
                        X509Certificate cert) {
                    certTrustRemoved(cert);
                }
            });
        } else {
            logger.fine("Warning: Trusted certificates tracker will not track "
                    + "explicitly trusted certificates since the certificate "
                    + "trust manager is null");
        }
        if (signerTrustMgr != null) {
            signerTrustMgr.addTrustListener(new CertificateTrustListener() {
                public void trustAdded(CertificateTrustManager manager,
                        X509Certificate cert) {
                    signerTrustAdded(cert);
                }

                public void trustRemoved(CertificateTrustManager manager,
                        X509Certificate cert) {
                    signerTrustRemoved(cert);
                }
            });
        } else {
            logger.fine("Warning: Trusted certificates tracker will not track "
                    + "signer-trusted certificates since the signer trust "
                    + "manager is null");
        }
    }

    private void signerTrustAdded(X509Certificate signer) {
        List<TrustedCertificateInfoImpl> newTrusted = new ArrayList<TrustedCertificateInfoImpl>();
        synchronized(this) {
            for (TrustedCertificateInfoImpl info : trackedCerts.values()) {
                boolean wasTrusted = info.isSomehowTrusted();
                X509Certificate cert = info.getCertificate();
                if (TrustTools.isSigned(signer, cert)) {
                    registerSignerSignee(info, signer);
                    if (!wasTrusted && info.isSomehowTrusted()) {
                        newTrusted.add(info);
                    }
                }
            }
        }
        for (TrustedCertificateInfoImpl aNewTrusted : newTrusted) {
            fireNowTrustedEvent(aNewTrusted);
        }
    }

    private void signerTrustRemoved(X509Certificate cert) {
        List<TrustedCertificateInfoImpl> noLongerTrusted = new ArrayList<TrustedCertificateInfoImpl>();
        synchronized(this) {
            SignerInfo signerInfo = signers.get(cert);
            if (signerInfo == null) return;

            List<TrustedCertificateInfo> signedCerts = signerInfo.getSignedCerts();
            for (TrustedCertificateInfo signedCert1 : signedCerts) {
                TrustedCertificateInfoImpl signedCert
                        = (TrustedCertificateInfoImpl) signedCert1;

                boolean wasTrusted = signedCert.isSomehowTrusted();
                signedCert.removeSigner(signerInfo);

                if (wasTrusted && !signedCert.isSomehowTrusted()) {
                    noLongerTrusted.add(signedCert);
                }
            }
            signers.remove(cert);
        }

        for (TrustedCertificateInfoImpl aNoLongerTrusted : noLongerTrusted) {
            fireNoLongerTrustedEvent(aNoLongerTrusted);
        }
    }

    private void fireNowTrustedEvent(TrustedCertificateInfo info) {
        assert !Thread.holdsLock(this);

        for (TrustedCertificatesListener listener : listeners) {
            listener.certificateTrusted(this, info);
        }
    }

    private void fireNoLongerTrustedEvent(TrustedCertificateInfo info) {
        assert !Thread.holdsLock(this);

        for (TrustedCertificatesListener listener : listeners) {
            listener.certificateNoLongerTrusted(this, info);
        }
    }

    private synchronized void registerSignerSignee(TrustedCertificateInfoImpl info,
            X509Certificate signer) {
        info.addSigner(registerSigner(signer, info));
    }

    private void certTrustAdded(X509Certificate cert) {
        TrustedCertificateInfoImpl info;
        boolean newTrusted;
        synchronized(this) {
            DefaultCertificateHolder holder = new DefaultCertificateHolder(cert);
            info = trackedCerts.get(holder);
            if (info == null) return;

            boolean wasTrusted = info.isSomehowTrusted();
            info.setExplicitlyTrusted(true);
            newTrusted = !wasTrusted && info.isSomehowTrusted();
        }
        if (newTrusted) {
            System.out.println("TrustedCertificatesTracker: cert is now "
                    + "trusted, firing events: " + cert.getSubjectDN());
            fireNowTrustedEvent(info);
        }
    }

    private void certTrustRemoved(X509Certificate cert) {
        TrustedCertificateInfoImpl info;
        boolean noLongerTrusted;
        synchronized(this) {
            DefaultCertificateHolder holder = new DefaultCertificateHolder(cert);
            info = trackedCerts.get(holder);
            if (info == null) return;

            boolean wasTrusted = info.isSomehowTrusted();
            info.setExplicitlyTrusted(false);
            noLongerTrusted = wasTrusted && !info.isSomehowTrusted();
        }
        if (noLongerTrusted) fireNoLongerTrustedEvent(info);
    }

    public void addTrustedCertsListener(TrustedCertificatesListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeTrustedCertsListener(TrustedCertificatesListener l) {
        listeners.remove(l);
    }

    public boolean addTrackedCertificate(X509Certificate cert) {
        DefensiveTools.checkNull(cert, "cert");

        TrustedCertificateInfoImpl info;
        boolean newTrusted;
        synchronized(this) {
            CertificateHolder holder = new DefaultCertificateHolder(cert);
            if (trackedCerts.containsKey(holder)) return false;

            info = new TrustedCertificateInfoImpl(cert);
            CertificateTrustManager certTrustMgr = this.certTrustMgr;
            boolean explicitlyTrusted;
            if (certTrustMgr != null) {
                explicitlyTrusted = certTrustMgr.isTrusted(cert);
            } else {
                explicitlyTrusted = false;
            }
            info.setExplicitlyTrusted(explicitlyTrusted);
            SignerTrustManager signerTrustMgr = this.signerTrustMgr;
            if (signerTrustMgr != null) {
                List<X509Certificate> ts = signerTrustMgr.getTrustedSigners(cert);
                for (X509Certificate signer : ts) {
                    if (signer == null) continue;

                    registerSignerSignee(info, signer);
                }
            }
            trackedCerts.put(holder, info);
            newTrusted = info.isSomehowTrusted();
        }

        if (newTrusted) {
            System.out.println("TrustedCertificatesTracker: cert "
                    + cert.getSubjectDN() + " is now trusted");
            fireNowTrustedEvent(info);
        }

        return true;
    }

    private synchronized SignerInfo registerSigner(X509Certificate signer,
            TrustedCertificateInfoImpl signee) {
        DefensiveTools.checkNull(signer, "signer");
        DefensiveTools.checkNull(signee, "signee");

        SignerInfoImpl info = getSignerInfoInstance(signer);
        info.addSignee(signee);
        return info;
    }

    private SignerInfoImpl getSignerInfoInstance(X509Certificate signer) {
        assert Thread.holdsLock(this);

        SignerInfoImpl info = signers.get(signer);
        if (info == null) {
            info = new SignerInfoImpl(signer);
            signers.put(signer, info);
        }
        return info;
    }

    public boolean removeTrackedCertificate(X509Certificate cert) {
        DefensiveTools.checkNull(cert, "cert");

        boolean noLongerTrusted;
        TrustedCertificateInfo info;
        synchronized(this) {
            info = trackedCerts.remove(new DefaultCertificateHolder(cert));
            if (info == null) return false;

            noLongerTrusted = info.isSomehowTrusted();
            List<SignerInfo> signers = info.getSigners();
            for (SignerInfo signer1 : signers) {
                SignerInfoImpl signer = (SignerInfoImpl) signer1;
                signer.removeSignee(info);
            }
        }

        if (noLongerTrusted) fireNoLongerTrustedEvent(info);

        return true;
    }

    public synchronized boolean isTrusted(X509Certificate cert) {
        CertificateHolder holder = new DefaultCertificateHolder(cert);
        TrustedCertificateInfo info = trackedCerts.get(holder);
        if (info == null) return false;
        return info.isSomehowTrusted();
    }

    public static final class TrustedCertificateInfoImpl
            extends DefaultCertificateHolder implements TrustedCertificateInfo {
        private boolean explicitlyTrusted = false;
        private final Set<SignerInfo> signers = new HashSet<SignerInfo>(5);

        private TrustedCertificateInfoImpl(X509Certificate cert) {
            super(cert);
        }

        public synchronized boolean isSomehowTrusted() {
            return explicitlyTrusted || !signers.isEmpty();
        }

        public synchronized boolean isExplicitlyTrusted() {
            return explicitlyTrusted;
        }

        private synchronized void setExplicitlyTrusted(boolean trusted) {
            this.explicitlyTrusted = trusted;
        }

        private synchronized boolean addSigner(SignerInfo signer) {
            DefensiveTools.checkNull(signer, "signer");

            return signers.add(signer);
        }

        public synchronized List<SignerInfo> getSigners() {
            return DefensiveTools.getUnmodifiableCopy(signers);
        }

        public synchronized boolean isSignedBy(SignerInfo signer) {
            DefensiveTools.checkNull(signer, "signer");

            return signers.contains(signer);
        }

        private synchronized boolean removeSigner(SignerInfo signer) {
            DefensiveTools.checkNull(signer, "signer");

            return signers.remove(signer);
        }
    }

    private static final class SignerInfoImpl
            extends DefaultCertificateHolder implements SignerInfo {
        private final Set<TrustedCertificateInfo> signees
                = new HashSet<TrustedCertificateInfo>(10);

        private SignerInfoImpl(X509Certificate cert) {
            super(cert);
        }

        private synchronized void addSignee(TrustedCertificateInfoImpl signee) {
            DefensiveTools.checkNull(signee, "signee");

            assert TrustTools.isSigned(getCertificate(), signee.getCertificate());

            signees.add(signee);
        }

        public synchronized List<TrustedCertificateInfo> getSignedCerts() {
            return DefensiveTools.getUnmodifiableCopy(signees);
        }

        private synchronized void removeSignee(TrustedCertificateInfo signee) {
            DefensiveTools.checkNull(signee, "signee");

            signees.remove(signee);
        }
    }
}

