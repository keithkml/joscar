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
 *  File created by keith @ Jan 28, 2004
 *
 */

package net.kano.aimcrypto.connection.oscar.service.info;

import net.kano.aimcrypto.config.BuddySecurityInfo;
import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.config.BuddySecurityInfo;
import net.kano.aimcrypto.connection.AimConnection;
import net.kano.aimcrypto.connection.BuddyInfo;
import net.kano.aimcrypto.connection.BuddyInfoManager;
import net.kano.aimcrypto.connection.GlobalBuddyInfoListener;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class BuddyCertificateManager {
    private final BuddyCertificateManager parent;

    private AimConnection conn = null;

    private Set trusted = new HashSet();
    private Map hashes = new HashMap();
    private Map securityInfos = new HashMap();
    private CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();

    public BuddyCertificateManager() {
        this(null, null);
    }

    public BuddyCertificateManager(BuddyCertificateManager parent) {
        this.parent = parent;
    }

    public BuddyCertificateManager(AimConnection conn, BuddyCertificateManager parent) {
        if (conn != null) bind(conn);
        this.parent = parent;
    }

    public boolean trust(BuddySecurityInfo info) {
        DefensiveTools.checkNull(info, "info");

        ByteBlock trustedhash = info.getCertificateInfoHash();
        if (trustedhash == null) {
            throw new IllegalArgumentException("attempted to trust a "
                    + "nonexistent certificate info hash");
        }
        Screenname buddy = info.getBuddy();
        HashKey key = new HashKey(buddy, trustedhash);

        boolean wasnew;
        boolean newlytrusted = false;
        synchronized(this) {
            securityInfos.put(key, info);
            wasnew = trusted.add(key);
            if (wasnew) {
                ByteBlock curhash = getCurrentCertificateHash(buddy);
                newlytrusted = trustedhash.equals(curhash);
            }
        }
        if (wasnew) {
            fireTrustAddedEvent(info);
            if (newlytrusted) {
                fireBuddyTrustedEvent(buddy, trustedhash, info);
            }
        }
        return wasnew;
    }

    private void fireBuddyTrustedEvent(Screenname buddy, ByteBlock trustedhash,
            BuddySecurityInfo info) {
        assert !Thread.holdsLock(this);
        System.out.println("firing buddy trusted event");

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            CertificateListener listener = (CertificateListener) it.next();
            listener.buddyTrusted(this, buddy, trustedhash, info);
        }
    }

    private void fireBuddyTrustRevokedEvent(Screenname buddy, ByteBlock hash,
            BuddySecurityInfo info) {
        assert !Thread.holdsLock(this);

        System.out.println("firing buddy trust revoked event");

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            CertificateListener listener = (CertificateListener) it.next();
            listener.buddyTrustRevoked(this, buddy, hash, info);
        }
    }

    public synchronized boolean revokeTrust(BuddySecurityInfo info) {
        DefensiveTools.checkNull(info, "info");

        return revokeTrust(info.getBuddy(), info.getCertificateInfoHash());
    }

    public boolean revokeTrust(Screenname buddy, ByteBlock hash) {
        DefensiveTools.checkNull(buddy, "buddy");
        DefensiveTools.checkNull(hash, "hash");

        HashKey key = new HashKey(buddy, hash);
        boolean removed;
        boolean untrusted = false;
        synchronized(this) {
            removed = trusted.remove(key);
            if (removed) {
                untrusted = hash.equals(getCurrentCertificateHash(buddy));
            }
        }
        if (removed) {
            fireTrustRevokedEvent(buddy, hash);
            if (untrusted) {
                BuddySecurityInfo info = getSecurityInfo(buddy, hash);
                fireBuddyTrustRevokedEvent(buddy, hash, info);
            }
        }
        return removed;
    }

    public void bind(AimConnection conn) {
        DefensiveTools.checkNull(conn, "conn");

        synchronized(this) {
            if (this.conn != null) {
                throw new IllegalStateException("already connected to " + conn);
            }
            this.conn = conn;
        }
        BuddyInfoManager bim = conn.getBuddyInfoManager();
        bim.addGlobalBuddyInfoListener(new GlobalBuddyInfoListener() {
            public void newBuddyInfo(BuddyInfoManager manager, Screenname buddy,
                    BuddyInfo info) {
            }

            public void buddyInfoChanged(BuddyInfoManager manager, Screenname buddy,
                    BuddyInfo info, PropertyChangeEvent event) {
                String name = event.getPropertyName();
                System.out.println("thing changed: " + name);

                if (name.equals("certificateInfoHash")) {
                    System.out.println("hash changed: " + buddy);
                    ByteBlock newHash = (ByteBlock) event.getNewValue();
                    updateHash(buddy, newHash);

                } else if (name.equals("securityInfo")) {
                    System.out.println("certmanager: info changed: " + buddy);
                    BuddySecurityInfo newi = (BuddySecurityInfo) event.getNewValue();
                    ByteBlock newHash = newi.getCertificateInfoHash();
                    System.out.println("new hash: " + newHash);
                    storeSecurityInfo(buddy, newHash, newi);
                    System.out.println("stored security info, updating hash");
                    updateHash(buddy, newHash);
                    System.out.println("done updating");
                }
            }

        });
    }

    public synchronized boolean isTrusted(Screenname buddy) {
        return isTrusted(new HashKey(buddy, getCurrentCertificateHash(buddy)));
    }

    public boolean isTrusted(Screenname buddy, ByteBlock hash) {
        return isTrusted(new HashKey(buddy, hash));
    }

    public boolean isTrusted(BuddySecurityInfo info) {
        return isTrusted(new HashKey(info.getBuddy(),
                info.getCertificateInfoHash()));
    }

    private synchronized boolean isTrusted(HashKey key) {
        return this.trusted.contains(key)
                || (parent == null || parent.isTrusted(key));
    }

    public BuddyCertificateManager getParent() { return parent; }

    public synchronized BuddySecurityInfo getSecurityInfo(Screenname buddy,
            ByteBlock hash) {
        DefensiveTools.checkNull(buddy, "buddy");

        return (BuddySecurityInfo) securityInfos.get(new HashKey(buddy, hash));
    }

    public synchronized ByteBlock getCurrentCertificateHash(Screenname buddy) {
        return (ByteBlock) hashes.get(buddy);
    }

    public synchronized BuddySecurityInfo getSecurityInfo(Screenname buddy) {
        return getSecurityInfo(buddy, getCurrentCertificateHash(buddy));
    }

    public void addListener(CertificateListener listener) {
        DefensiveTools.checkNull(listener, "listener");
        
        listeners.addIfAbsent(listener);
    }

    public void removeListener(CertificateListener listener) {
        listeners.remove(listener);
    }

    private synchronized void storeSecurityInfo(Screenname buddy,
            ByteBlock newHash, BuddySecurityInfo newi) {
        DefensiveTools.checkNull(newHash, "newHash");
        HashKey key = new HashKey(buddy, newHash);
        securityInfos.put(key, newi);
    }

    private boolean updateHash(Screenname buddy, ByteBlock newHash) {
        assert !Thread.holdsLock(this);

        System.out.println("updating hash for " + buddy + ": " + newHash);
        boolean wastrusted;
        boolean istrusted;
        BuddySecurityInfo info;
        synchronized(this) {
            // check to see if the given hash is the same as the old hash; if it
            // is, there's nothing to do
            ByteBlock old = getCurrentCertificateHash(buddy);
            if ((old == null && newHash == null)
                    || (newHash != null && newHash.equals(old))) {
                // the new hash and the old hash are the same
                System.out.println("new and old are the same: " + old);
                return false;
            }

            // check if the buddy's previous hash was trusted
            wastrusted = trusted.contains(new HashKey(buddy, old));

            if (newHash == null) {
                // the buddy removed his certificate, so we just clear some
                // stuff out
                hashes.remove(buddy);
                istrusted = false;
                info = null;

            } else {
                // store the new hash as the buddy's current hash
                hashes.put(buddy, newHash);

                // check if this new hash is trusted, and see if we already have a
                // certificate stored for the hash
                HashKey key = new HashKey(buddy, newHash);
                istrusted = trusted.contains(key);
                info = (BuddySecurityInfo) securityInfos.get(key);
            }
        }

        // something is wrong if we trust the hash but we don't have a
        // certificate for it
        assert istrusted ? info != null : true;

        if (istrusted) {
            System.out.println("is trusted!");
            fireTrustedChangeEvent(buddy, info);
            if (!wastrusted) {
                System.out.println("wasn't trusted!");
                fireBuddyTrustedEvent(buddy, newHash, info);
            }
        } else {
            System.out.println("is not trusted!");
            if (info == null) fireUnknownChangeEvent(buddy, newHash);
            else fireUntrustedChangeEvent(buddy, info);

            if (wastrusted) {
                System.out.println("used to be trusted!");
                fireBuddyTrustRevokedEvent(buddy, newHash, info);
            }
        }

        return true;
    }

    private void fireTrustedChangeEvent(Screenname buddy,
            BuddySecurityInfo info) {
        assert !Thread.holdsLock(this);

        System.out.println("firing trusted change event");

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            CertificateListener listener = (CertificateListener) it.next();
            listener.gotTrustedCertificateChange(this, buddy, info);
        }
    }

    private void fireUntrustedChangeEvent(Screenname buddy,
            BuddySecurityInfo info) {
        assert !Thread.holdsLock(this);

        System.out.println("firing untrusted change event");

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            CertificateListener listener = (CertificateListener) it.next();
            listener.gotUntrustedCertificateChange(this, buddy, info);
        }
    }

    private void fireUnknownChangeEvent(Screenname buddy, ByteBlock newHash) {
        assert !Thread.holdsLock(this);

        System.out.println("firing unknown change event");

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            CertificateListener listener = (CertificateListener) it.next();
            listener.gotUnknownCertificateChange(this, buddy, newHash);
        }
    }

    private void fireTrustAddedEvent(BuddySecurityInfo info) {
        assert !Thread.holdsLock(this);

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            CertificateListener listener = (CertificateListener) it.next();
            listener.trustAdded(this, info.getBuddy(), info);
        }
    }

    private void fireTrustRevokedEvent(Screenname buddy, ByteBlock hash) {
        assert !Thread.holdsLock(this);

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            CertificateListener listener = (CertificateListener) it.next();
            listener.trustRevoked(this, buddy, hash);
        }
    }

    private static final class HashKey {
        private final Screenname buddy;
        private final ByteBlock hash;

        public HashKey(Screenname buddy, ByteBlock hash) {
            DefensiveTools.checkNull(buddy, "buddy");

            this.buddy = buddy;
            this.hash = hash;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof HashKey)) return false;

            final HashKey key = (HashKey) o;

            if (!buddy.equals(key.buddy)) return false;
            if (hash != null ? !hash.equals(key.hash) : key.hash != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = buddy.hashCode();
            result = 29 * result + (hash != null ? hash.hashCode() : 0);
            return result;
        }
    }
}
