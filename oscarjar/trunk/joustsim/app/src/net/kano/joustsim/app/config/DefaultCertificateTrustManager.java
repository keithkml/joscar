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

package net.kano.joustsim.app.config;

import net.kano.joustsim.Screenname;
import net.kano.joustsim.trust.CertificateHolder;
import net.kano.joustsim.trust.CertificateTrustListener;
import net.kano.joustsim.trust.CertificateTrustManager;
import net.kano.joustsim.trust.DefaultCertificateHolder;
import net.kano.joustsim.trust.TrustException;
import net.kano.joustsim.trust.TrustTools;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class DefaultCertificateTrustManager implements CertificateTrustManager {
    private static final Logger logger
            = Logger.getLogger(DefaultCertificateTrustManager.class.getName());

    private final Screenname buddy;

    private final Set trusted = new HashSet();

    private final CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();

    public DefaultCertificateTrustManager(Screenname buddy) {
        this.buddy = buddy;
    }

    public final Screenname getBuddy() { return buddy; }

    public void addTrustListener(CertificateTrustListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeTrustChangeListener(CertificateTrustListener l) {
        listeners.remove(l);
    }

    public boolean trustCertificate(X509Certificate cert) throws TrustException {
        checkCanBeAdded(cert);
        boolean added = addTrust(cert);
        if (added) fireTrustedEvent(cert);
        return added;
    }

    protected void checkCanBeAdded(X509Certificate cert)
            throws CantBeAddedException {
        if (!canBeAdded(cert)) {
            logger.warning("Can't add certificate to " + this + ": ");
            throw new CantBeAddedException();
        }
    }

    public synchronized boolean isTrusted(X509Certificate cert) {
        return trusted.contains(new DefaultCertificateHolder(cert));
    }

    public boolean revokeTrust(X509Certificate cert) {
        boolean removed = removeTrust(cert);
        if (removed) fireNoLongerTrustedEvent(cert);
        return removed;
    }

    public synchronized X509Certificate[] getTrustedCertificates() {
        X509Certificate[] certs = new X509Certificate[trusted.size()];
        int i = 0;
        for (Iterator it = trusted.iterator(); it.hasNext();) {
            CertificateHolder holder = (CertificateHolder) it.next();
            certs[i] = holder.getCertificate();
            i++;
        }
        return certs;
    }

    protected synchronized boolean addTrust(X509Certificate cert)
            throws CantBeAddedException {
        checkCanBeAdded(cert);
        return trusted.add(new DefaultCertificateHolder(cert));
    }

    protected synchronized boolean removeTrust(X509Certificate cert) {
        return trusted.remove(new DefaultCertificateHolder(cert));
    }

    protected void fireTrustedEvent(X509Certificate cert) {
        assert !Thread.holdsLock(this);

        DefensiveTools.checkNull(cert, "cert");

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            CertificateTrustListener listener = (CertificateTrustListener) it.next();
            listener.trustAdded(this, cert);
        }
    }

    protected void fireNoLongerTrustedEvent(X509Certificate cert) {
        assert !Thread.holdsLock(this);

        DefensiveTools.checkNull(cert, "cert");

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            CertificateTrustListener listener = (CertificateTrustListener) it.next();
            listener.trustRemoved(this, cert);
        }
    }

    public boolean importCertificate(File file) throws TrustException {
        DefensiveTools.checkNull(file, "file");

        X509Certificate cert;
        try {
            cert = TrustTools.loadX509Certificate(file);
        } catch (Exception e) {
            throw new TrustException(e);
        }
        checkCanBeAdded(cert);
        if (cert == null) {
            throw new TrustException("Certificate could not be loaded");
        }

        return trustCertificate(cert);
    }

    protected boolean canBeAdded(X509Certificate certificate) {
        return true;
    }
}
