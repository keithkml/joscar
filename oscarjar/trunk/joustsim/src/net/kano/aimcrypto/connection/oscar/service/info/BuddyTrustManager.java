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

import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.config.BuddyCertificateInfo;
import net.kano.aimcrypto.connection.AimConnection;
import net.kano.aimcrypto.connection.BuddyInfo;
import net.kano.aimcrypto.connection.BuddyInfoManager;
import net.kano.aimcrypto.connection.GlobalBuddyInfoListener;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//TODO: find out why paradigmmm and cheshirecatv2 are untrusted instead of unknown
public class BuddyTrustManager {
    private final BuddyInfoManager buddyInfoManager;

    private static boolean isValidCombination(ByteBlock hash,
            BuddyCertificateInfo certInfo) {
        if (hash == null) {
            // if the hash is null, the certificate info has to be null
            return certInfo == null;
        } else {
            if (certInfo == null) {
                // the certificate info is unknown
                return true;
            } else {
                // the certificate info is known, so we have to make sure it
                // corresponds to the given hash
                return certInfo.getCertificateInfoHash().equals(hash);
            }
        }
    }

    private final AimConnection conn;
    private final CertificateInfoTrustManager certTrustMgr;

    private Map trustInfos = new HashMap();
    private Map latestHashes = new HashMap();
    private Map cachedCertInfos = new HashMap();

    private CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();

    public BuddyTrustManager(AimConnection conn) {
        DefensiveTools.checkNull(conn, "conn");

        this.conn = conn;
        this.certTrustMgr = conn.getCertificateInfoTrustManager();

        this.buddyInfoManager = conn.getBuddyInfoManager();

        buddyInfoManager.addGlobalBuddyInfoListener(new GlobalBuddyInfoListener() {
            public void newBuddyInfo(BuddyInfoManager manager, Screenname buddy,
                    BuddyInfo info) {
            }

            public void receivedStatusUpdate(BuddyInfoManager manager,
                    Screenname buddy, BuddyInfo info) {
            }

            public void buddyInfoChanged(BuddyInfoManager manager,
                    Screenname buddy, BuddyInfo buddyInfo,
                    PropertyChangeEvent event) {
                String prop = event.getPropertyName();

                if (prop.equals(BuddyInfo.PROP_CERTIFICATE_INFO)) {
                    BuddyCertificateInfo certInfo
                            = (BuddyCertificateInfo) event.getNewValue();
                    System.out.println("cert info for " + buddy + " changed: " + certInfo);
                    ByteBlock hash;
                    if (certInfo == null) {
                        hash = null;
                    } else {
                        // we want to cache this certificate before we handle
                        // the buddy's new hash
                        hash = certInfo.getCertificateInfoHash();
                        cacheCertInfo(certInfo);
                    }
                    System.out.println("handling hash for " + buddy + ": " + hash);
                    handleBuddyHashChange(buddyInfo, hash);
                }
            }
        });
        certTrustMgr.addTrustListener(new CertificateInfoTrustListener() {
            public void certificateInfoTrusted(
                    CertificateInfoTrustManager manager,
                    BuddyCertificateInfo certInfo) {

                handleCertInfoTrustChange(certInfo, true);
            }

            public void certificateInfoNoLongerTrusted(
                    CertificateInfoTrustManager manager,
                    BuddyCertificateInfo certInfo) {

                handleCertInfoTrustChange(certInfo, false);
            }
        });
    }

    public void addBuddyTrustListener(BuddyTrustListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeBuddyTrustListener(BuddyTrustListener l) {
        listeners.remove(l);
    }

    private void handleCertInfoTrustChange(BuddyCertificateInfo certInfo,
            boolean trusted) {
        assert !Thread.holdsLock(this);

        DefensiveTools.checkNull(certInfo, "certInfo");

        TrustStatus newTrusted = trusted
                ? TrustStatus.TRUSTED
                : TrustStatus.NOT_TRUSTED;

        Screenname sn = certInfo.getBuddy();
        ByteBlock trustedHash = certInfo.getCertificateInfoHash();
        TrustStatus oldTrusted;
        synchronized(this) {
            BuddyTrustInfoHolder trustHolder = getTrustInfoInstance(sn);
            if (trustHolder == null) {
                // this probably means we got a listener callback for a
                // certificate that we aren't monitoring anymore
                return;
            }

            BuddyCertificateInfoHolder certInfoHolder
                    = getCertificateInfoHolder(sn, trustedHash);
            if (certInfoHolder != null) {
                // this should always be executed, but we put it in an if block
                // so it doesn't totally break if something goes wrong
                certInfoHolder.setTrusted(trusted);
            }

            ByteBlock hash = (ByteBlock) latestHashes.get(sn);
            if (hash == null || !hash.equals(trustedHash)) {
                // this isn't the buddy's current certificate, so there's
                // nothing left to do (no listeners need to be called since no
                // buddy could've been trusted or untrusted.
                return;
            }

            oldTrusted = trustHolder.getTrustedStatus();
            if (oldTrusted == newTrusted) {
                // this certificate was already trusted, as far as we know
                return;
            }

            trustHolder.setTrustedStatus(newTrusted);
        }

        // if we weren't supposed to fire an event, we would've returned before
        // now
        fireChangeEvents(sn, certInfo, oldTrusted, newTrusted);
    }

    private void handleBuddyHashChange(BuddyInfo info, ByteBlock hash) {
        Screenname sn = info.getScreenname();
        BuddyCertificateInfo certInfo;
        TrustStatus oldStatus;
        TrustStatus newStatus;
        synchronized(this) {
            BuddyCertificateInfoHolder certInfoHolder
                    = getCertificateInfoHolder(sn, hash);
            certInfo = certInfoHolder == null ? null : certInfoHolder.getInfo();

            // get the trust info for this buddy
            BuddyTrustInfoHolder buddyTrustInfo = getTrustInfoInstance(sn);

            // save the buddy's new certificate hash, and determine whether his
            // trust status changed
            oldStatus = buddyTrustInfo.getTrustedStatus();

            System.out.println("updating buddy hash for " + sn);
            newStatus = updateBuddyHash(sn, hash);
            System.out.println("new status for " + sn + " is " + newStatus);
            if (newStatus == null || newStatus == oldStatus) {
                // the status did not change, so there's nothing left for us to
                // do here
                return;
            }
            // the buddy's trust status changed, so we need to set it here and
            // fire some events to our listeners
            buddyTrustInfo.setTrustedStatus(newStatus);
        }

        fireChangeEvents(sn, certInfo, oldStatus, newStatus);
    }

    private synchronized TrustStatus updateBuddyHash(Screenname sn,
            ByteBlock newHash) {
        DefensiveTools.checkNull(sn, "sn");

//        // store the new hash and get his old hash
//        ByteBlock oldHash = (ByteBlock) latestHashes.put(sn, newHash);
//
//        if (oldHash == newHash || (oldHash != null && oldHash.equals(newHash))) {
//            // the buddy's new hash is the same as his old hash
//            return null;
//        }

        if (newHash == null) {
            // if the hash is null, the buddy can't be trusted
            return TrustStatus.NOT_TRUSTED;

        } else {
            // the hash is not null, so we need to determine whether the buddy
            // is trusted by seeing if we have a copy of the certificate info
            // corresponding to his buddy hash, and then seeing whether it's
            // trusted according to the trust manager
            BuddyCertificateInfoHolder certHolder
                    = getCertificateInfoHolder(sn, newHash);
            if (certHolder == null) {
                // we don't have this certificate, so this buddy has an UNKNOWN
                // certificate
                return TrustStatus.UNKNOWN;

            } else {
                // we have this buddy's certificate, so we return whether it's
                // marked as being trusted or not
                if (certHolder.isTrusted()) return TrustStatus.TRUSTED;
                else return TrustStatus.NOT_TRUSTED;
            }
        }
    }

    private void fireChangeEvents(Screenname sn,
            BuddyCertificateInfo certInfo, TrustStatus oldStatus,
            TrustStatus newStatus) {
        assert !Thread.holdsLock(this);

        if (newStatus == TrustStatus.UNKNOWN) {
            assert certInfo != null && !certInfo.isUpToDate();
            fireUnknownCertificateChangeEvent(sn, certInfo);

        } else if (newStatus == TrustStatus.NOT_TRUSTED) {
            assert certInfo == null || certInfo.isUpToDate();
            fireUntrustedCertificateChangeEvent(sn, certInfo);

        } else if (newStatus == TrustStatus.TRUSTED) {
            assert certInfo != null;
            fireTrustedCertificateChangeEvent(sn, certInfo);
        }

        if ((oldStatus == TrustStatus.UNKNOWN
                || oldStatus == TrustStatus.NOT_TRUSTED)
                && newStatus == TrustStatus.TRUSTED) {
            assert certInfo != null;
            fireBuddyTrustedEvent(sn, certInfo);

        } else if (oldStatus == TrustStatus.TRUSTED
                && (newStatus == TrustStatus.UNKNOWN
                || newStatus == TrustStatus.NOT_TRUSTED)) {
            fireBuddyNoLongerTrustedEvent(sn, certInfo);
        }
    }

    private void fireTrustedCertificateChangeEvent(Screenname sn,
            BuddyCertificateInfo certInfo) {
        assert !Thread.holdsLock(this);

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            BuddyTrustListener listener = (BuddyTrustListener) it.next();
            listener.gotTrustedCertificateChange(this, sn, certInfo);
        }
    }

    private void fireUntrustedCertificateChangeEvent(Screenname sn,
            BuddyCertificateInfo certInfo) {
        assert !Thread.holdsLock(this);

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            BuddyTrustListener listener = (BuddyTrustListener) it.next();
            listener.gotUntrustedCertificateChange(this, sn, certInfo);
        }
    }

    private void fireUnknownCertificateChangeEvent(Screenname sn,
            BuddyCertificateInfo certInfo) {
        assert !Thread.holdsLock(this);

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            BuddyTrustListener listener = (BuddyTrustListener) it.next();
            listener.gotUnknownCertificateChange(this, sn, certInfo);
        }
    }

    private void fireBuddyTrustedEvent(Screenname sn,
            BuddyCertificateInfo certInfo) {
        assert !Thread.holdsLock(this);

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            BuddyTrustListener listener = (BuddyTrustListener) it.next();
            listener.buddyTrusted(this, sn, certInfo);
        }
    }

    private void fireBuddyNoLongerTrustedEvent(Screenname sn,
            BuddyCertificateInfo certInfo) {
        assert !Thread.holdsLock(this);

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            BuddyTrustListener listener = (BuddyTrustListener) it.next();
            listener.buddyTrustRevoked(this, sn, certInfo);
        }
    }

    private synchronized BuddyTrustInfoHolder getTrustInfoInstance(
            Screenname sn) {
        DefensiveTools.checkNull(sn, "sn");

        BuddyTrustInfoHolder inst = (BuddyTrustInfoHolder) trustInfos.get(sn);
        if (inst == null) {
            inst = new BuddyTrustInfoHolder(sn);
            trustInfos.put(sn, inst);
        }
        return inst;
    }

    private void cacheCertInfo(BuddyCertificateInfo certInfo) {
        DefensiveTools.checkNull(certInfo, "certInfo");

        if (!certInfo.hasBothCertificates() || !certInfo.isUpToDate()) return;

        Screenname sn = certInfo.getBuddy();
        ByteBlock hash = certInfo.getCertificateInfoHash();
        BuddyHashHolder hashHolder = new BuddyHashHolder(sn, hash);
        BuddyCertificateInfoHolder certInfoHolder;
        synchronized(this) {
            // if we already cached this certificate info block, we don't need
            // to do anything here
            if (cachedCertInfos.containsKey(hashHolder)) return;

            certInfoHolder = new BuddyCertificateInfoHolder(certInfo);
            cachedCertInfos.put(hashHolder, certInfoHolder);
        }
        // we want to call this outside the lock since it might call our
        // listeners right back, which might cause us to fire our listeners,
        // which shouldn't be done inside a lock
        certTrustMgr.addTrackedCertificateInfo(certInfo);
    }

    private synchronized BuddyCertificateInfoHolder
            getCurrentCertificateInfoHolder(Screenname sn) {
        DefensiveTools.checkNull(sn, "sn");

        ByteBlock hash = getCurrentCertificateInfoHash(sn);
        if (hash == null) return null;

        return getCertificateInfoHolder(sn, hash);
    }

    private synchronized BuddyCertificateInfoHolder getCertificateInfoHolder(
            Screenname sn, ByteBlock hash) {
        DefensiveTools.checkNull(sn, "sn");

        if (hash == null) return null;

        BuddyHashHolder holder = new BuddyHashHolder(sn, hash);
        return (BuddyCertificateInfoHolder) cachedCertInfos.get(holder);
    }

    public synchronized ByteBlock getCurrentCertificateInfoHash(Screenname sn) {
        return (ByteBlock) latestHashes.get(sn);
    }

    public synchronized boolean isTrusted(Screenname buddy) {
        BuddyCertificateInfoHolder holder
                = getCurrentCertificateInfoHolder(buddy);
        return holder != null && holder.isTrusted();
    }

    public synchronized boolean isTrusted(BuddyCertificateInfo info) {
        DefensiveTools.checkNull(info, "info");

        BuddyCertificateInfoHolder holder = getCertificateInfoHolder(
                info.getBuddy(), info.getCertificateInfoHash());
        return holder != null && holder.isTrusted();
    }

    private static class BuddyCertificateInfoHolder {
        private final BuddyCertificateInfo info;
        private boolean trusted = false;

        public BuddyCertificateInfoHolder(BuddyCertificateInfo info) {
            DefensiveTools.checkNull(info, "info");

            this.info = info;
        }

        public BuddyCertificateInfo getInfo() { return info; }

        public synchronized boolean isTrusted() { return trusted; }

        public synchronized void setTrusted(boolean trusted) {
            this.trusted = trusted;
        }
    }

    private static class BuddyTrustInfoHolder {
        private final Screenname buddy;
        private TrustStatus trustedStatus = TrustStatus.UNKNOWN;

        public BuddyTrustInfoHolder(Screenname buddy) {
            DefensiveTools.checkNull(buddy, "buddy");

            this.buddy = buddy;
        }

        public Screenname getBuddy() { return buddy; }

        public synchronized TrustStatus getTrustedStatus() {
            return trustedStatus;
        }

        public synchronized void setTrustedStatus(TrustStatus trustedStatus) {
            DefensiveTools.checkNull(trustedStatus, "trustedStatus");

            this.trustedStatus = trustedStatus;
        }
    };

    private static class TrustStatus {
        public static final TrustStatus UNKNOWN = new TrustStatus("UNKNOWN");
        public static final TrustStatus NOT_TRUSTED = new TrustStatus("NOT_TRUSTED");
        public static final TrustStatus TRUSTED = new TrustStatus("TRUSTED");

        private final String name;

        public TrustStatus(String name) {
            DefensiveTools.checkNull(name, "name");

            this.name = name;
        }

        public String toString() {
            return name;
        }
    }
}
