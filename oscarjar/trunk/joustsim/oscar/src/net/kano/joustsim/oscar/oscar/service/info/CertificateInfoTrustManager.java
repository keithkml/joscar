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

package net.kano.joustsim.oscar.oscar.service.info;

import net.kano.joustsim.trust.TrustedCertificateInfo;
import net.kano.joustsim.trust.TrustedCertificatesListener;
import net.kano.joustsim.trust.TrustedCertificatesTracker;
import net.kano.joustsim.trust.BuddyCertificateInfo;
import net.kano.joustsim.trust.CertificateHolder;
import net.kano.joustsim.trust.DefaultCertificateHolder;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class CertificateInfoTrustManager {
  private static final Logger LOGGER = Logger
      .getLogger(CertificateInfoTrustManager.class.getName());

  private final TrustedCertificatesTracker certTrustMgr;

  private Map<CertificateHolder, Set<BuddyCertificateInfoHolder>> certInfoTrust
      = new HashMap<CertificateHolder, Set<BuddyCertificateInfoHolder>>();
  private Map<BuddyHashHolder, BuddyCertificateInfoHolder> certHolders
      = new HashMap<BuddyHashHolder, BuddyCertificateInfoHolder>();
  private CopyOnWriteArrayList<CertificateInfoTrustListener> listeners
      = new CopyOnWriteArrayList<CertificateInfoTrustListener>();

  public CertificateInfoTrustManager(TrustedCertificatesTracker certTrustMgr) {
    DefensiveTools.checkNull(certTrustMgr, "certTrustMgr");

    this.certTrustMgr = certTrustMgr;

    certTrustMgr.addTrustedCertsListener(new TrustedCertificatesListener() {
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
    List<BuddyCertificateInfoHolder> updated
        = new ArrayList<BuddyCertificateInfoHolder>();
    synchronized (this) {
      Set<BuddyCertificateInfoHolder> certinfos = certInfoTrust.get(holder);
      for (BuddyCertificateInfoHolder infoHolder : certinfos) {
        if (updateTrusted(infoHolder)) {
          updated.add(infoHolder);
        }
      }
    }

    for (BuddyCertificateInfoHolder infoHolder : updated) {
      if (infoHolder.isTrusted()) {
        fireCertInfoTrustedEvent(infoHolder);
      } else {
        fireCertInfoNoLongerTrustedEvent(infoHolder);
      }
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
    for (CertificateInfoTrustListener listener : listeners) {
      listener.certificateInfoTrusted(this, buddyCertInfo);
    }
  }

  private void fireCertInfoNoLongerTrustedEvent(
      BuddyCertificateInfoHolder infoHolder) {
    assert !Thread.holdsLock(this);

    BuddyCertificateInfo buddyCertInfo = infoHolder.getInfo();
    for (CertificateInfoTrustListener listener : listeners) {
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

    assert encCert != null && signingCert != null;

    BuddyHashHolder hashHolder = new BuddyHashHolder(certInfo.getBuddy(),
        certInfo.getCertificateInfoHash());
    BuddyCertificateInfoHolder holder
        = new BuddyCertificateInfoHolder(certInfo);

    synchronized (this) {
      certHolders.put(hashHolder, holder);

      associateCert(encCert, holder);
      associateCert(signingCert, holder);
    }

    certTrustMgr.addTrackedCertificate(encCert);
    if (signingCert != encCert) {
      certTrustMgr.addTrackedCertificate(signingCert);
    }
    boolean trusted = updateTrusted(holder);
    if (trusted) {
      LOGGER.fine("certificate for " + certInfo.getBuddy() + " is trusted");
      fireCertInfoTrustedEvent(holder);
    } else {
      LOGGER.fine("certificate for " + certInfo.getBuddy() + " is NOT trusted");
    }
  }

  public void removeTrackedCertificateInfo(BuddyCertificateInfo certInfo) {
    DefensiveTools.checkNull(certInfo, "certInfo");

    X509Certificate encCert = certInfo.getEncryptionCertificate();
    X509Certificate signingCert = certInfo.getSigningCertificate();

    BuddyCertificateInfoHolder holder;
    boolean wasTrusted;
    synchronized (this) {
      certTrustMgr.removeTrackedCertificate(encCert);
      certTrustMgr.removeTrackedCertificate(signingCert);

      disassociateCert(signingCert, certInfo);
      disassociateCert(encCert, certInfo);

      BuddyHashHolder hashHolder = new BuddyHashHolder(certInfo.getBuddy(),
          certInfo.getCertificateInfoHash());
      holder = certHolders.remove(hashHolder);
      wasTrusted = holder != null && holder.isTrusted();
    }

    if (wasTrusted) fireCertInfoNoLongerTrustedEvent(holder);
  }

  private synchronized void disassociateCert(X509Certificate cert,
      BuddyCertificateInfo certInfo) {
    CertificateHolder certHolder = new DefaultCertificateHolder(cert);
    Set<BuddyCertificateInfoHolder> associated = certInfoTrust.get(certHolder);
    if (associated == null) return;
    for (Iterator<BuddyCertificateInfoHolder> it = associated.iterator();
        it.hasNext();) {
      BuddyCertificateInfoHolder holder
          = it.next();
      if (holder.getInfo().equals(certInfo)) it.remove();
    }
  }

  private synchronized boolean associateCert(X509Certificate cert,
      BuddyCertificateInfoHolder infoHolder) {
    CertificateHolder certHolder = new DefaultCertificateHolder(cert);
    Set<BuddyCertificateInfoHolder> associated = certInfoTrust.get(certHolder);
    if (associated == null) {
      associated = new HashSet<BuddyCertificateInfoHolder>();
      certInfoTrust.put(certHolder, associated);
    }
    return associated.add(infoHolder);
  }

  public synchronized boolean isTrusted(BuddyCertificateInfo certInfo) {
    BuddyHashHolder hashHolder = new BuddyHashHolder(certInfo.getBuddy(),
        certInfo.getCertificateInfoHash());
    BuddyCertificateInfoHolder holder
        = certHolders.get(hashHolder);
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
