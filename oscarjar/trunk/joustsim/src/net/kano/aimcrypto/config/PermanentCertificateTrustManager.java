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
import net.kano.joscar.DefensiveTools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
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

public class PermanentCertificateTrustManager
        extends CertificateTrustManager implements FileBasedResource {
    private static TrustedCertInfo loadCertificatePermanently(File loadedFromFile)
            throws NoSuchProviderException, CertificateException,
            IOException, IllegalArgumentException {
        DefensiveTools.checkNull(loadedFromFile, "loadedFromFile");

        long lastmod = loadedFromFile.lastModified();
        if (lastmod == 0) {
            throw new IllegalArgumentException("invalid last modification "
                    + "date: " + lastmod);
        }

        X509Certificate xcert = loadX509Certificate(loadedFromFile);

        return new TrustedCertInfo(loadedFromFile, lastmod, xcert);
    }

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

    private static void writeCert(X509Certificate cert, File file)
            throws IOException, CertificateEncodingException {
        DefensiveTools.checkNull(cert, "cert");
        DefensiveTools.checkNull(file, "file");

        FileOutputStream fout = new FileOutputStream(file);
        try {
            fout.getChannel().lock();
            fout.write(cert.getEncoded());
        } finally {
            fout.close();
        }
    }

    private final File trustedCertsDir;

    private final Map file2info = new HashMap();
    private final Map cert2info = new HashMap();

    private final CertificateLoader certLoader;

    public PermanentCertificateTrustManager(Screenname buddy,
            File trustedCertsDir) {
        super(buddy);

        DefensiveTools.checkNull(trustedCertsDir, "trustedCertsDir");

        this.trustedCertsDir = trustedCertsDir;
        this.certLoader = new CertificateLoader();
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

    public boolean trustCertificate(X509Certificate cert)
            throws TrustException {
        DefensiveTools.checkNull(cert, "cert");

        if (isTrusted(cert)) return false;
        checkCanBeAdded(cert);

        File file;
        try {
            file = createFileForCert(cert);
        } catch (Exception e) {
            throw new TrustException(e);
        }
        if (file == null) {
            throw new TrustException(new CantSavePrefsException(
                    "Couldn't create file to certificate to disk"));
        }

        try {
            writeCert(cert, file);
        } catch (Exception e) {
            throw new TrustException(e);
        }

        long lastmod = file.lastModified();
        TrustedCertInfo info = new TrustedCertInfo(file, lastmod, cert);

        boolean isnew = addTrust(info);
        if (isnew) fireTrustedEvent(cert);

        return isnew;
    }

    public boolean revokeTrust(X509Certificate cert) {
        boolean removed;
        synchronized(this) {
            deleteTrustFile(cert);
            removed = removeTrustCompletely(cert);
        }
        if (removed) fireNoLongerTrustedEvent(cert);
        return removed;
    }

    private synchronized boolean removeTrustCompletely(X509Certificate cert) {
        CertificateHolder holder = new CertificateHolder(cert);
        TrustedCertInfo info = (TrustedCertInfo) cert2info.remove(holder);
        if (info != null) {
            file2info.remove(info.getLoadedFromFile());
        }
        boolean removed = removeTrust(cert);
        return removed;
    }

    private synchronized boolean deleteTrustFile(X509Certificate cert) {
        CertificateHolder holder = new CertificateHolder(cert);
        TrustedCertInfo info = (TrustedCertInfo) cert2info.get(holder);
        if (info == null) return false;

        File file = info.getLoadedFromFile();
        boolean deleted = file.delete();
        return deleted;
    }

    private File createFileForCert(X509Certificate cert)
            throws IOException, CantSavePrefsException {
        trustedCertsDir.mkdir();
        if (!trustedCertsDir.canWrite()) return null;

        String fixed = getPossibleFilenameRoot(cert);
        String fn = fixed;
        File file;
        int n = 1;
        do {
            file = new File(trustedCertsDir, fn + ".der");
            fn = fixed + "-" + n;
            n++;
            try {
                if (file.createNewFile()) break;
                else throw new CantSavePrefsException("couldn't create file");
            } catch (IOException e) {
                if (n == 99) throw e;
            }
        } while (n < 100);

        return file;
    }

    private synchronized boolean addTrust(TrustedCertInfo info)
            throws CantBeAddedException {
        DefensiveTools.checkNull(info, "info");

        File file = info.getLoadedFromFile();
        X509Certificate cert = info.getCertificate();
        CertificateHolder holder = new CertificateHolder(cert);
        cert2info.put(holder, info);
        file2info.put(file, info);

        boolean added = addTrust(cert);
        return added;
    }

    private synchronized boolean removeTrust(TrustedCertInfo info) {
        DefensiveTools.checkNull(info, "info");

        File file = info.getLoadedFromFile();

        if (info.equals(file2info.get(file))) {
            file2info.remove(file);
        }
        X509Certificate cert = info.getCertificate();
        CertificateHolder holder = new CertificateHolder(cert);
        if (info.equals(cert2info.get(holder))) {
            cert2info.remove(holder);
        }

        boolean removed = removeTrust(cert);
        return removed;
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
            synchronized(PermanentCertificateTrustManager.this) {
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
                            removeTrust(info);
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
                        TrustedCertInfo info = loadCertificatePermanently(file);
                        addTrust(info);
                        addedInfos.add(info);
                    } catch (NoSuchProviderException e) {
                        //TODO: handle cert loading exception
                        e.printStackTrace();
                    } catch (CertificateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (CantBeAddedException e) {
                        e.printStackTrace();
                    }
                }
            }
            // here we leave the lock so we can call listeners

            // fire all of the removed events
            for (Iterator it = removedInfos.iterator(); it.hasNext();) {
                TrustedCertInfo info = (TrustedCertInfo) it.next();
                fireNoLongerTrustedEvent(info.getCertificate());
            }

            // fire all of the added events
            for (Iterator it = addedInfos.iterator(); it.hasNext();) {
                TrustedCertInfo info = (TrustedCertInfo) it.next();
                fireTrustedEvent(info.getCertificate());
            }
        }
    }

    private static final class TrustedCertInfo {
        private final File loadedFromFile;
        private final long lastModWhenLoaded;
        private final X509Certificate cert;

        protected TrustedCertInfo(File loadedFromFile, long lastModWhenLoaded,
                X509Certificate cert) {
            DefensiveTools.checkNull(loadedFromFile, "loadedFromFile");
            DefensiveTools.checkRange(lastModWhenLoaded, "lastModWhenLoaded", 1);
            DefensiveTools.checkNull(cert, "cert");

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
}
