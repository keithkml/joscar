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
 *  File created by keith @ Feb 1, 2004
 *
 */

package net.kano.aimcrypto.config;

import net.kano.aimcrypto.DistinguishedName;
import net.kano.aimcrypto.Screenname;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
/*

public class OldCertificateTrustManager implements FileBasedResource {
    private static String getPossibleFilenameRoot(X509Certificate cert) {
        DistinguishedName dn = DistinguishedName.getSubjectInstance(cert);
        String name = dn.getName();
        StringBuffer fixedbuf = new StringBuffer(name.length());
        fix(name, fixedbuf);
        if (fixedbuf.length() == 0) {
            fix(dn.getOrganization(), fixedbuf);
        }
        if (fixedbuf.length() == 0) {
            fixedbuf.append("unknown");
        }
        String fixed = fixedbuf.toString();
        return fixed;
    }

    private static void fix(String name, StringBuffer fixedbuf) {
        for (int i = 0; i < name.length(); i++) {
            char c = Character.toLowerCase(name.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                fixedbuf.append(c);
            }
        }
    }

    private final Screenname buddy;
    private final File trustedCertsDir;

    private final Map file2info = new HashMap();
    private final Set permTrusted = new HashSet();
    private final Set tempTrusted = new HashSet();
    private final Set totalTrusted = new HashSet();
    private final Set visibleTempTrusted = new HashSet();
    private final Map cert2info = new HashMap();

    private CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();
    private CertificateLoader certLoader = new CertificateLoader();

    OldCertificateTrustManager(Screenname buddy, File trustedCertsDir) {
        DefensiveTools.checkNull(buddy, "buddy");
        DefensiveTools.checkNull(trustedCertsDir, "trustedCertsDir");

        this.buddy = buddy;
        this.trustedCertsDir = trustedCertsDir;
    }

    public final Screenname getBuddy() { return buddy; }

    public void addTrustListener(TrustChangeListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeTrustChangeListener(TrustChangeListener l) {
        listeners.remove(l);
    }

    public boolean isUpToDate() {
        return certLoader.isUpToDate();
    }

    public boolean reloadIfNecessary() throws LoadingException {
        return certLoader.reloadIfNecessary();
    }

    public void reload() throws LoadingException {
        certLoader.reload();
    }

    public boolean trustCertificateTemporarily(X509Certificate cert) {
        boolean added = addTempTrust(cert);
        if (added) nowTrusted(cert, false);
    }

    public boolean trustCertificatePermanently(X509Certificate cert) {
        DefensiveTools.checkNull(cert, "cert");

        if (isTrustedPermanently(cert)) return false;

        boolean saved = false;
        File file = createFileForCert(cert);
        if (file != null) {
            long lastmod = file.lastModified();
            try {
                saveCert(cert, file);
                saved = true;
            } catch (IOException e) {
                //TODO: couldn't save certificate
                e.printStackTrace();
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }

            if (saved) {
                TrustedCertInfo info = new TrustedCertInfo(file, lastmod, cert);
                boolean isnew = addPermTrust(info);
                if (isnew) nowTrusted(cert, true);
            }
        }

        if (!saved) {
            addTempTrust(cert);
        }

        return saved;
    }

    public synchronized boolean isTrustedPermanently(X509Certificate cert) {
        DefensiveTools.checkNull(cert, "cert");

        return permTrusted.contains(new CertificateHolder(cert));
    }

    public synchronized boolean isOnlyTrustedTemporarily(X509Certificate cert) {
        DefensiveTools.checkNull(cert, "cert");

        return visibleTempTrusted.contains(new CertificateHolder(cert));
    }

    public synchronized boolean isTrusted(X509Certificate cert) {
        return totalTrusted.contains(new CertificateHolder(cert));
    }

    public boolean revokeTrust(X509Certificate cert) {
        boolean removedTemp;
        boolean removedPerm;
        synchronized(this) {
            removedTemp = removeTempTrust(cert);
            deleteTrustFile(cert);
            removedPerm = removePermTrust(cert);
        }
        boolean changed = removedPerm || removedTemp;
        if (changed) noLongerTrusted(cert);
        return changed;
    }

    public boolean revokeOnlyPermanentTrust(X509Certificate cert) {
        boolean removed = removeTempTrust(cert);
        if (removed) noLongerTrusted(cert);
        return removed;
    }

    private synchronized boolean removePermTrust(X509Certificate cert) {
        CertificateHolder holder = new CertificateHolder(cert);
        TrustedCertInfo info = (TrustedCertInfo) cert2info.remove(holder);
        if (info != null) {
            file2info.remove(info.getLoadedFromFile());
        }
        permTrusted.remove(cert);
        return updateTrust(holder);
    }

    private synchronized boolean deleteTrustFile(X509Certificate cert) {
        TrustedCertInfo info = (TrustedCertInfo) cert2info.get(cert);
        if (info == null) return false;
        File file = info.getLoadedFromFile();
        boolean deleted = file.delete();
        return deleted;
    }

    private static void saveCert(X509Certificate cert, File file)
            throws IOException, CertificateEncodingException {
        file.getParentFile().mkdirs();
        FileOutputStream fout = new FileOutputStream(file);
        try {
            fout.getChannel().lock();
            fout.write(cert.getEncoded());
        } finally {
            fout.close();
        }
    }

    private File createFileForCert(X509Certificate cert) {
        String fixed = getPossibleFilenameRoot(cert);
        String fn = fixed;
        File file;
        int n = 1;
        boolean success;
        do {
            file = new File(trustedCertsDir, fn + ".der");
            fn = fixed + "-" + n;
            n++;
            try {
                success = file.createNewFile();
            } catch (IOException e) {
                success = false;
            }
        } while (!success && n < 100);

        if (!success) file = null;

        return file;
    }

    private synchronized boolean addTempTrust(X509Certificate cert) {
        CertificateHolder holder = new CertificateHolder(cert);

        tempTrusted.add(holder);
        return updateTrust(holder);
    }

    private synchronized boolean removeTempTrust(X509Certificate cert) {
        CertificateHolder holder = new CertificateHolder(cert);

        tempTrusted.remove(holder);
        return updateTrust(holder);
    }

    private synchronized boolean addPermTrust(TrustedCertInfo info) {
        DefensiveTools.checkNull(info, "info");

        File file = info.getLoadedFromFile();
        X509Certificate cert = info.getCertificate();
        cert2info.put(new CertificateHolder(cert), info);
        file2info.put(file, info);

        return addPermTrust(cert);
    }

    private synchronized boolean addPermTrust(X509Certificate cert) {
        CertificateHolder holder = new CertificateHolder(cert);
        permTrusted.add(holder);
        return updateTrust(holder);
    }

    private synchronized boolean removePermTrust(File file, TrustedCertInfo info) {
        DefensiveTools.checkNull(file, "file");
        DefensiveTools.checkNull(info, "info");

        if (info.equals(file2info.get(file))) {
            file2info.remove(file);
        }
        CertificateHolder holder = new CertificateHolder(info.getCertificate());
        if (info.equals(cert2info.get(holder))) {
            cert2info.remove(holder);
        }

        return removePermTrustEntry(info.getCertificate());
    }

    private synchronized boolean removePermTrustEntry(X509Certificate cert) {
        CertificateHolder holder = new CertificateHolder(cert);
        permTrusted.remove(holder);
        return updateTrust(holder);
    }

    private synchronized boolean updateTrust(CertificateHolder holder) {
        boolean perm = permTrusted.contains(holder);
        boolean temp = tempTrusted.contains(holder);

        boolean totalsChanged;
        if (perm || temp) totalsChanged = totalTrusted.add(holder);
        else totalsChanged = totalTrusted.remove(holder);

        if (perm) visibleTempTrusted.remove(holder);
        else if (temp) visibleTempTrusted.add(holder);

        return totalsChanged;
    }

    private void nowTrusted(X509Certificate cert, boolean perm) {
        assert !Thread.holdsLock(this);

        DefensiveTools.checkNull(cert, "cert");

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            TrustChangeListener listener = (TrustChangeListener) it.next();
            listener.trustAdded(this, cert, perm);
        }
    }

    private void noLongerTrusted(X509Certificate cert) {
        assert !Thread.holdsLock(this);

        DefensiveTools.checkNull(cert, "cert");

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            TrustChangeListener listener = (TrustChangeListener) it.next();
            listener.trustRemoved(this, cert);
        }
    }

    private static class TrustedCertInfo {
        private static TrustedCertInfo loadCertificate(File loadedFromFile)
                throws NoSuchProviderException, CertificateException,
                IOException, IllegalArgumentException {
            DefensiveTools.checkNull(loadedFromFile, "loadedFromFile");

            long lastmod = loadedFromFile.lastModified();
            if (lastmod == 0) {
                throw new IllegalArgumentException("invalid last modification "
                        + "date: " + lastmod);
            }

            CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
            FileInputStream fin = new FileInputStream(loadedFromFile);
            Certificate cert;
            try {
                fin.getChannel().lock(0, Long.MAX_VALUE, true);
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

            return new TrustedCertInfo(loadedFromFile, lastmod, xcert);
        }

        private final File loadedFromFile;
        private final long lastModWhenLoaded;
        private final X509Certificate cert;

        public TrustedCertInfo(File loadedFromFile, long lastModWhenLoaded,
                X509Certificate cert) {
            this.loadedFromFile = loadedFromFile;
            this.lastModWhenLoaded = lastModWhenLoaded;
            this.cert = cert;
        }

        public boolean isUpToDate(File file) {
            DefensiveTools.checkNull(file, "file");

            return file.equals(loadedFromFile)
                    && file.lastModified() == lastModWhenLoaded;
        }

        public File getLoadedFromFile() { return loadedFromFile; }

        public long getLastModWhenLoaded() { return lastModWhenLoaded; }

        public X509Certificate getCertificate() { return cert; }
    }

    private class CertificateLoader extends DefaultFileBasedResource {
        public CertificateLoader() {
            super(trustedCertsDir);
        }

        public void reload() throws LoadingException {
            reloadImpl();
        }

        private void reloadImpl() {
            File[] files = trustedCertsDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".der");
                }
            });
            if (files == null) return;

            for (int i = 0; i < files.length; i++) {
                try {
                    files[i] = files[i].getCanonicalFile();
                } catch (IOException ignored) { }

            }

            Set newfiles = new HashSet(Arrays.asList(files));
            List addedInfos = new ArrayList();
            List removedInfos = new ArrayList();
            synchronized(OldCertificateTrustManager.this) {
                List filesToAdd = new ArrayList();
                // clear keys that are no longer trusted, and see if old trusted
                // and new trusted keys are up to date
                Set oldTrusted = file2info.entrySet();
                for (Iterator it = oldTrusted.iterator(); it.hasNext();) {
                    Entry entry = (Entry) it.next();

                    File file = (File) entry.getKey();
                    if (!newfiles.contains(file)) {
                        // this file has been deleted, so the corresponding
                        // certificate isn't trusted
                        removedInfos.add(entry.getValue());
                        it.remove();

                    } else {
                        // this file is still there; check to see if it's
                        // changed
                        TrustedCertInfo info = (TrustedCertInfo) entry.getValue();

                        if (!info.isUpToDate(file)) {
                            // this file has changed; reload it
                            removedInfos.add(info);
                            filesToAdd.add(file);
                            removePermTrust(file, info);
                        }
                    }
                }

                // prepare to load the new files
                newfiles.removeAll(oldTrusted);
                for (Iterator it = newfiles.iterator(); it.hasNext();) {
                    File file = (File) it.next();
                    filesToAdd.add(file);
                }

                // load the new files
                for (Iterator it = filesToAdd.iterator(); it.hasNext();) {
                    File file = (File) it.next();
                    try {
                        TrustedCertInfo info = TrustedCertInfo.loadCertificate(file);
                        addedInfos.add(info);
                        addPermTrust(info);
                    } catch (NoSuchProviderException e) {
                        //TODO: handle cert loading exception
                        e.printStackTrace();
                    } catch (CertificateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            // leave the lock so we can call listeners

            // fire all of the removed events
            for (Iterator it = removedInfos.iterator(); it.hasNext();) {
                TrustedCertInfo info = (TrustedCertInfo) it.next();
                noLongerTrusted(info.getCertificate());
            }

            // fire all of the added events
            for (Iterator it = addedInfos.iterator(); it.hasNext();) {
                TrustedCertInfo info = (TrustedCertInfo) it.next();
                nowTrusted(info.getCertificate(), true);
            }
        }
    }
}
*/
