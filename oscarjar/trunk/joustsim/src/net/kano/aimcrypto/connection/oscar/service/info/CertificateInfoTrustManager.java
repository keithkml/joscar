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
 *  File created by keith @ Feb 6, 2004
 *
 */

package net.kano.aimcrypto.connection.oscar.service.info;

import net.kano.aimcrypto.TrustedCertificatesTracker;
import net.kano.aimcrypto.TrustedCertsListener;
import net.kano.aimcrypto.TrustedCertificateInfo;
import net.kano.aimcrypto.config.BuddyCertificateInfo;
import net.kano.aimcrypto.config.DefaultCertificateHolder;
import net.kano.aimcrypto.config.CertificateHolder;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.CopyOnWriteArrayList;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public class CertificateInfoTrustManager {
    private final TrustedCertificatesTracker certTrustMgr;

    private Map certInfoTrust = new HashMap();
    private Map certHolders = new HashMap();
    private CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();

    public CertificateInfoTrustManager(TrustedCertificatesTracker certTrustMgr) {
        DefensiveTools.checkNull(certTrustMgr, "certTrustMgr");

        this.certTrustMgr = certTrustMgr;

        certTrustMgr.addTrustedCertsListener(new TrustedCertsListener() {
            public void certificateTrusted(TrustedCertificatesTracker manager,
                    TrustedCertificateInfo info) {
                handleCertChange(info.getCertificate());
            }

            public void certificateNoLongerTrusted(
                    TrustedCertificatesTracker manager,
                    TrustedCertificateInfo info) {
                handleCertChange(info.getCertificate());
            }
        });
    }

    public void addTrustListener(CertificateInfoTrustListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeTrustListener(CertificateInfoTrustListener l) {
        listeners.remove(l);
    }

    private void handleCertChange(X509Certificate cert) {
        assert !Thread.holdsLock(this);

        CertificateHolder holder = new DefaultCertificateHolder(cert);
        List updated = new ArrayList();
        synchronized(this) {
            Set certinfos = (Set) certInfoTrust.get(holder);
            for (Iterator it = certinfos.iterator(); it.hasNext();) {
                BuddyCertificateInfoHolder infoHolder
                        = (BuddyCertificateInfoHolder) it.next();

                if (updateTrusted(infoHolder)) {
                    updated.add(infoHolder);
                }
            }
        }

        for (Iterator it = updated.iterator(); it.hasNext();) {
            BuddyCertificateInfoHolder infoHolder
                    = (BuddyCertificateInfoHolder) it.next();

            if (infoHolder.isTrusted()) fireCertInfoTrustedEvent(infoHolder);
            else fireCertInfoNoLongerTrustedEvent(infoHolder);
        }
    }

    private synchronized boolean updateTrusted(
            BuddyCertificateInfoHolder infoHolder) {
        DefensiveTools.checkNull(infoHolder, "infoHolder");

        boolean wasTrusted = infoHolder.isTrusted();

        BuddyCertificateInfo info = infoHolder.getInfo();
        X509Certificate encCert = info.getEncryptionCertificate();
        X509Certificate signingCert = info.getSigningCertificate();

        boolean encTrusted = certTrustMgr.isTrusted(encCert);
        boolean signTrusted = certTrustMgr.isTrusted(signingCert);
        boolean isTrusted = encTrusted && signTrusted;

        if (isTrusted != wasTrusted) {
            infoHolder.setTrusted(isTrusted);
            return true;
        } else {
            return false;
        }
    }

    private void fireCertInfoTrustedEvent(
            BuddyCertificateInfoHolder infoHolder) {
        assert !Thread.holdsLock(this);

        BuddyCertificateInfo buddyCertInfo = infoHolder.getInfo();
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            CertificateInfoTrustListener listener
                    = (CertificateInfoTrustListener) it.next();
            listener.certificateInfoTrusted(this, buddyCertInfo);
        }
    }

    private void fireCertInfoNoLongerTrustedEvent(
            BuddyCertificateInfoHolder infoHolder) {
        assert !Thread.holdsLock(this);

        BuddyCertificateInfo buddyCertInfo = infoHolder.getInfo();
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            CertificateInfoTrustListener listener
                    = (CertificateInfoTrustListener) it.next();
            listener.certificateInfoNoLongerTrusted(this, buddyCertInfo);
        }
    }

    public void addTrackedCertificateInfo(BuddyCertificateInfo certInfo) {
        DefensiveTools.checkNull(certInfo, "certInfo");

        if (!certInfo.hasBothCertificates()) {
            throw new IllegalArgumentException("certificate information "
                    + "object does not contain both encryption and signing "
                    + "certificates");
        }

        X509Certificate encCert = certInfo.getEncryptionCertificate();
        X509Certificate signingCert = certInfo.getSigningCertificate();

        BuddyHashHolder hashHolder = new BuddyHashHolder(certInfo.getBuddy(),
                certInfo.getCertificateInfoHash());
        BuddyCertificateInfoHolder holder
                = new BuddyCertificateInfoHolder(certInfo);

        synchronized(this) {
            certHolders.put(hashHolder, holder);

            associateCert(encCert, holder);
            associateCert(signingCert, holder);
        }

        certTrustMgr.addTrackedCertificate(encCert);
        certTrustMgr.addTrackedCertificate(signingCert);
        if (updateTrusted(holder)) {
            fireCertInfoTrustedEvent(holder);
        }
    }

    public void removeTrackedCertificateInfo(BuddyCertificateInfo certInfo) {
        DefensiveTools.checkNull(certInfo, "certInfo");

        X509Certificate encCert = certInfo.getEncryptionCertificate();
        X509Certificate signingCert = certInfo.getSigningCertificate();

        BuddyCertificateInfoHolder holder;
        boolean wasTrusted;
        synchronized(this) {
            certTrustMgr.removeTrackedCertificate(encCert);
            certTrustMgr.removeTrackedCertificate(signingCert);

            disassociateCert(signingCert, certInfo);
            disassociateCert(encCert, certInfo);

            holder = (BuddyCertificateInfoHolder) certHolders.remove(certInfo);
            wasTrusted = holder != null && holder.isTrusted();
        }

        if (wasTrusted) fireCertInfoNoLongerTrustedEvent(holder);
    }

    private synchronized void disassociateCert(X509Certificate cert,
            BuddyCertificateInfo certInfo) {
        CertificateHolder certHolder = new DefaultCertificateHolder(cert);
        Set associated = (Set) certInfoTrust.get(certHolder);
        if (associated == null)return;
        for (Iterator it = associated.iterator(); it.hasNext();) {
            BuddyCertificateInfoHolder holder
                    = (BuddyCertificateInfoHolder) it.next();
            if (holder.getInfo().equals(certInfo)) it.remove();
        }
    }

    private synchronized boolean associateCert(X509Certificate cert,
            BuddyCertificateInfoHolder infoHolder) {
        CertificateHolder certHolder = new DefaultCertificateHolder(cert);
        Set associated = (Set) certInfoTrust.get(certHolder);
        if (associated == null) {
            associated = new HashSet();
            certInfoTrust.put(certHolder, associated);
        }
        return associated.add(infoHolder);
    }

    public synchronized boolean isTrusted(BuddyCertificateInfo certInfo) {
        BuddyCertificateInfoHolder holder
                = (BuddyCertificateInfoHolder) certHolders.get(certInfo);
        return holder != null && holder.isTrusted();
    }

    private static class BuddyCertificateInfoHolder {
        private final BuddyCertificateInfo info;
        private boolean trusted = false;

        public BuddyCertificateInfoHolder(BuddyCertificateInfo info) {
            this.info = info;
        }

        public BuddyCertificateInfo getInfo() {
            return info;
        }

        public synchronized boolean isTrusted() {
            return trusted;
        }

        public synchronized void setTrusted(boolean trusted) {
            this.trusted = trusted;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BuddyCertificateInfoHolder)) return false;

            BuddyCertificateInfoHolder buddyCertificateInfoHolder
                    = (BuddyCertificateInfoHolder) o;

            if (!info.equals(buddyCertificateInfoHolder.info)) return false;

            return true;
        }

        public int hashCode() {
            return info.hashCode();
        }
    }
}
