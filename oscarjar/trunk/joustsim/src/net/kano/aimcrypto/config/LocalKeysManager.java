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
 *  File created by keith @ Jan 14, 2004
 *
 */

package net.kano.aimcrypto.config;

import net.kano.aimcrypto.exceptions.BadKeyPrefsException;
import net.kano.aimcrypto.exceptions.BadKeysException;
import net.kano.aimcrypto.exceptions.InsufficientKeysException;
import net.kano.aimcrypto.exceptions.NoSuchAliasException;
import net.kano.aimcrypto.exceptions.WrongKeyTypesException;
import net.kano.aimcrypto.config.DefaultFileBasedResource;
import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.KeyPair;
import net.kano.joscar.DefensiveTools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public final class LocalKeysManager
        extends DefaultFileBasedResource implements Preferences {
    private static final String PROP_CERTFILE = "certificate-file";
    private static final String PROP_SIGNALIAS = "signing-alias";
    private static final String PROP_ENCALIAS = "encryption-alias";
    private static final String PROP_PASSWORD = "password";
    private static final String[] NAMES_EMPTY = new String[0];

    private final Screenname screenname;
    private File keysDir = null;

    private String certificateFilename = null;
    private File certificateFile = null;
    private String signingAlias = null;
    private String encryptionAlias = null;
    private String password = null;
    private final File certsDir;

    private PossibleCertificateList possibleCerts;
    private KeysLoader keysLoader = new KeysLoader();
    private boolean savePassword = false;
    private File prefsFile;

    public LocalKeysManager(Screenname screenname, File keysDir) {
        DefensiveTools.checkNull(screenname, "screenname");
        DefensiveTools.checkNull(keysDir, "baseDir");

        this.screenname = screenname;
        this.keysDir = keysDir;
        this.certsDir = new File(keysDir, "certs");
        this.prefsFile = new File(keysDir, "key-prefs.properties");
        this.possibleCerts = new PossibleCertificateList();
    }

    public Screenname getScreenname() { return screenname; }

    public boolean isUpToDate() {
        return possibleCerts.isUpToDate() && keysLoader.isUpToDate();
    }

    public boolean reloadIfNecessary() throws LoadingException {
        boolean a = possibleCerts.reloadIfNecessary();
        boolean b = keysLoader.reloadIfNecessary();
        return a || b;
    }

    public void reload() throws LoadingException {
        possibleCerts.reload();
        keysLoader.reload();
    }

    public void loadPrefs() throws IOException {
        Properties props = PrefTools.loadProperties(prefsFile);
        loadPrefs(props);
    }

    private synchronized void loadPrefs(Properties props) {
        String fn = props.getProperty(PROP_CERTFILE);
        setCertificateFilename(fn);
        signingAlias = props.getProperty(PROP_SIGNALIAS);
        encryptionAlias = props.getProperty(PROP_ENCALIAS);
        password = props.getProperty(PROP_PASSWORD);
        savePassword = (password != null);
    }

    public void savePrefs() throws IOException {
        File prefsFile = this.prefsFile;
        // make sure it exists
        keysDir.mkdir();
        prefsFile.createNewFile();
        if (!prefsFile.canWrite()) {
            throw new FileNotFoundException(prefsFile.getAbsolutePath());
        }

        Properties props = generatePrefsProperties();

        PrefTools.writeProperties(prefsFile, props,
                "Local certificate preferences for " + getScreenname());
    }

    private synchronized Properties generatePrefsProperties() {
        Properties props = new Properties();

        String certfile = certificateFilename;
        String signAlias = signingAlias;
        String encAlias = encryptionAlias;
        String pass = savePassword ? password : null;

        if (certfile != null) props.setProperty(PROP_CERTFILE, certfile);
        if (signAlias != null) props.setProperty(PROP_SIGNALIAS, signAlias);
        if (encAlias != null) props.setProperty(PROP_ENCALIAS, encAlias);
        if (pass != null) props.setProperty(PROP_PASSWORD, pass);

        return props;
    }

    public PrivateKeysInfo getKeysInfo() { return keysLoader.getKeysInfo(); }

    public synchronized void setCertificateFilename(String fn) {
        this.certificateFilename = fn;
        certificateFile = fn == null ? null : new File(certsDir, fn);
    }

    public synchronized void setSigningAlias(String signingAlias) {
        this.signingAlias = signingAlias;
    }

    public synchronized void setEncryptionAlias(String encryptionAlias) {
        this.encryptionAlias = encryptionAlias;
    }

    public synchronized void setPassword(String password) {
        this.password = password;
    }

    public synchronized File getCertificateFile() {
        return certificateFile;
    }

    public synchronized String getCertificateFilename() {
        return certificateFilename;
    }

    public synchronized String getSigningAlias() {
        return signingAlias;
    }

    public synchronized String getEncryptionAlias() {
        return encryptionAlias;
    }

    public synchronized String getPassword() {
        return password;
    }

    public String[] getPossibleCertificateNames() {
        return possibleCerts.getPossibleCertNames();
    }

    public String[] getPossibleAliases() {
        return keysLoader.getPossibleAliases();
    }

    public void importCertFile(File file) throws IOException {
        File kd;
        File cd;
        synchronized(this) {
            kd = keysDir;
            cd = certsDir;
        }
        if (!cd.isDirectory()) {
            kd.mkdir();
            cd.mkdir();
        }
        String newName = file.getName();
        File dest = new File(cd, newName);

        FileChannel sourceChannel = null;
        FileChannel destinationChannel = null;
        try {
            sourceChannel = new FileInputStream(file).getChannel();
            destinationChannel = new FileOutputStream(dest).getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        } finally {
            if (sourceChannel != null) {
                try { sourceChannel.close(); } catch (IOException e) { }
            }
            if (destinationChannel != null) {
                try { destinationChannel.close(); } catch (IOException e) { }
            }
        }
    }

    public synchronized void switchToCertificateFile(String name) {
        String old = getCertificateFilename();
        if ((old == null && name == null)
                || (old != null && old.equals(name))) {
            return;
        }
        setCertificateFilename(name);
        setEncryptionAlias(null);
        setSigningAlias(null);
        setPassword(null);
    }

    public synchronized void setSavePassword(boolean savePassword) {
        this.savePassword = savePassword;
    }

    public synchronized boolean getSavePassword() {
        return savePassword;
    }

    private class PossibleCertificateList extends DefaultFileBasedResource {
        private String[] possibleCertNames = NAMES_EMPTY;

        public PossibleCertificateList() {
            super(certsDir);
        }

        public synchronized String[] getPossibleCertNames() {
            return (String[]) possibleCertNames.clone();
        }

        public synchronized boolean reloadIfNecessary() {
            try {
                return super.reloadIfNecessary();
            } catch (LoadingException e) {
                return false;
            }
        }

        public synchronized void reload() {
            File[] files = certsDir.listFiles();
            String[] names = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                names[i] = files[i].getName();
            }
            possibleCertNames = names;
        }
    }

    public boolean isPasswordValid() {
        return keysLoader.isPasswordValid();
    }

    private class KeysLoader extends DefaultFileBasedResource {
        private PrivateKeysInfo keysInfo = null;
        private File loadedFromFile = null;
        private long keysModified = 0;

        private String[] possibleAliases = null;
        private String loadedSigningAlias = null;
        private String loadedEncAlias = null;
        private boolean passwordValid = false;

        public synchronized PrivateKeysInfo getKeysInfo() { return keysInfo; }

        public synchronized String[] getPossibleAliases() {
            return possibleAliases == null
                    ? null : (String[]) possibleAliases.clone();
        }

        public synchronized boolean isUpToDate() {
            File loadedFrom = loadedFromFile;
            File newCertFile = certificateFile;

            if (loadedFrom == null && newCertFile == null) {
                return true;

            } else {
                if (loadedFrom == null || newCertFile == null
                        || !loadedFrom.equals(newCertFile)) {
                    return false;

                } else {
                    return keysModified == newCertFile.lastModified()
                            && equal(loadedSigningAlias, signingAlias)
                            && equal(loadedEncAlias, encryptionAlias);
                }
            }
        }

        private boolean equal(String lsa, String sa) {
            return lsa == sa || (lsa != null && lsa.equals(sa));
        }

        protected long getLastModified() {
            return loadedFromFile == null ? 0 : loadedFromFile.lastModified();
        }

        public void reload() throws LoadingException {
            try {
                loadKeys();
            } catch (Exception e) {
                throw new LoadingException(e);
            }
        }

        public void loadKeys()
                throws BadKeysException, BadKeyPrefsException {
            try {
                loadKeysImpl();
            } catch (BadKeyPrefsException ants) {
                throw ants;
            } catch (BadKeysException up) {
                throw up;
            } catch (Exception e) {
                throw new BadKeysException(e);
            }
        }

        private synchronized void clearStuff() {
            keysInfo = null;
            loadedFromFile = null;
            loadedSigningAlias = null;
            loadedEncAlias = null;
            possibleAliases = null;
            passwordValid = false;
        }

        private void loadKeysImpl() throws BadKeysException,
                NoSuchProviderException, KeyStoreException, IOException,
                NoSuchAlgorithmException, CertificateException,
                UnrecoverableKeyException, BadKeyPrefsException {

            String encAlias;
            String signAlias;
            File certFile;
            String pass;
            synchronized(this) {
                clearStuff();

                encAlias = encryptionAlias;
                signAlias = signingAlias;
                certFile = certificateFile;
                pass = password;
                if (pass == null) {
                    throw new BadKeyPrefsException(PROP_PASSWORD);
                } else if (certFile == null) {
                    throw new BadKeyPrefsException(PROP_CERTFILE);
                }
                keysModified = certFile.lastModified();
            }

            char[] passChars = pass.toCharArray();

            KeyStore ks = KeyStore.getInstance("PKCS12", "BC");

            FileInputStream fin = new FileInputStream(certFile);
            try {
                ks.load(fin, passChars);
            } finally {
                try { fin.close(); } catch (IOException ignored) { }
            }

            String[] posAliases = loadPossibleAliases(ks);
            synchronized(this) {
                possibleAliases = posAliases;
                passwordValid = true;
            }

            if (encAlias == null) {
                throw new BadKeyPrefsException(PROP_ENCALIAS);
            } else if (signAlias == null) {
                throw new BadKeyPrefsException(PROP_SIGNALIAS);
            }

            KeyPair signingKeys = loadKeys(ks, signAlias, passChars);
            KeyPair encryptionKeys;
            boolean same = encAlias.equals(signAlias);
            if (same) {
                encryptionKeys = signingKeys;
            } else {
                encryptionKeys = loadKeys(ks, encAlias, passChars);
            }

            PrivateKeysInfo newkeys = new PrivateKeysInfo(signingKeys,
                    encryptionKeys);
            synchronized(this) {
                keysInfo = newkeys;
                loadedFromFile = certFile;
                loadedSigningAlias = signAlias;
                loadedEncAlias = encAlias;
            }
        }

        private String[] loadPossibleAliases(KeyStore ks) throws KeyStoreException {
            List aliases = new ArrayList();
            Enumeration alenum = ks.aliases();
            while (alenum.hasMoreElements()) {
                String alias = (String) alenum.nextElement();
                aliases.add(alias);
            }
            return (String[]) aliases.toArray(new String[aliases.size()]);
        }

        private KeyPair loadKeys(KeyStore ks, String alias, char[] passChars)
                throws KeyStoreException, NoSuchAliasException,
                NoSuchAlgorithmException, UnrecoverableKeyException,
                InsufficientKeysException, WrongKeyTypesException {

            if (!ks.containsAlias(alias)) {
                throw new NoSuchAliasException(alias);
            }

            Key privKey = ks.getKey(alias, passChars);
            Certificate pubCert = ks.getCertificate(alias);

            if (privKey == null || pubCert == null) {
                throw new InsufficientKeysException(
                        privKey != null,
                        pubCert != null);
            }

            boolean isrsa = privKey instanceof RSAPrivateKey;
            boolean isx509 = pubCert instanceof X509Certificate;
            if (!isrsa || !isx509) {
                throw new WrongKeyTypesException(isrsa ? null : privKey.getClass(),
                        isx509 ? null : pubCert.getClass());
            }

            return new KeyPair((RSAPrivateKey) privKey, (X509Certificate) pubCert);
        }

        public synchronized boolean isPasswordValid() {
            return passwordValid;
        }
    }
}
