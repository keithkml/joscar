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

package net.kano.aimcrypto;

import net.kano.aimcrypto.exceptions.BadKeyPrefsException;
import net.kano.aimcrypto.exceptions.BadKeysException;
import net.kano.aimcrypto.exceptions.InsufficientKeysException;
import net.kano.aimcrypto.exceptions.NoSuchAliasException;
import net.kano.aimcrypto.exceptions.WrongKeyTypesException;
import net.kano.joscar.DefensiveTools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

public final class PrivateSecurityInfo extends FileBasedResource {
    private static final String[] NAMES_EMPTY = new String[0];

    private final Screenname screenname;
    private File baseDir = null;

    private String certificateFilename = null;
    private File certificateFile = null;
    private String signingAlias = null;
    private String encryptionAlias = null;
    private String password = null;
    private final File certsDir;

    private PossibleCertificateList possibleCerts;
    private KeysLoader keysLoader = new KeysLoader();
    private boolean savePassword = false;

    public PrivateSecurityInfo(Screenname screenname, File baseDir) {
        DefensiveTools.checkNull(screenname, "screenname");
        DefensiveTools.checkNull(baseDir, "baseDir");

        this.screenname = screenname;
        this.baseDir = baseDir;
        this.certsDir = new File(baseDir, "certs");
        this.possibleCerts = new PossibleCertificateList();
    }

    public Screenname getScreenname() { return screenname; }

    public synchronized boolean isUpToDate() {
        return possibleCerts.isUpToDate() && keysLoader.isUpToDate();
    }

    public synchronized boolean reloadIfNecessary() throws LoadingException {
        boolean a = possibleCerts.reloadIfNecessary();
        System.out.println("reloaded possible certs: " + a);
        boolean b = keysLoader.reloadIfNecessary();
        System.out.println("reloaded current keys: " + b);
        return a || b;
    }

    public synchronized void reload() throws LoadingException {
        possibleCerts.reload();
        keysLoader.reload();
    }

    public synchronized void loadPrefs() throws IOException {
        File prefs = new File(baseDir, "key-prefs.properties");

        if (!prefs.exists()) {
            throw new FileNotFoundException(prefs.getAbsolutePath());
        }

        Properties props = new Properties();
        props.load(new FileInputStream(prefs));
        String fn = props.getProperty("certificate-file");
        setCertificateFilename(fn);
        signingAlias = props.getProperty("signing-alias");
        encryptionAlias = props.getProperty("encryption-alias");
        password = props.getProperty("password");
        savePassword = (password != null);
    }

    public void savePrefs() {

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
        if (!certsDir.exists()) certsDir.mkdirs();
        String newName = file.getName();
        File dest = new File(certsDir, newName);

        FileChannel sourceChannel = new FileInputStream(file).getChannel();
        FileChannel destinationChannel = new
                FileOutputStream(dest).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
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

    private class PossibleCertificateList extends FileBasedResource {
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

    private class KeysLoader extends FileBasedResource {
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
            System.out.println("reloading...");
            try {
                loadKeys();
            } catch (Exception e) {
                throw new LoadingException(e);
            }
        }

        public synchronized void loadKeys()
                throws BadKeysException, BadKeyPrefsException {
            clearStuff();
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

        private synchronized void loadKeysImpl() throws BadKeysException,
                NoSuchProviderException, KeyStoreException, IOException,
                NoSuchAlgorithmException, CertificateException,
                UnrecoverableKeyException, BadKeyPrefsException {

            String encAlias = encryptionAlias;
            String signAlias = signingAlias;
            File certFile = certificateFile;
            String pass = password;
            if (pass == null) {
                throw new BadKeyPrefsException("password");
            } else if (certFile == null) {
                throw new BadKeyPrefsException("certificate-file");
            }

            char[] passChars = pass.toCharArray();

            KeyStore ks = KeyStore.getInstance("PKCS12", "BC");

            File file = certFile;
            keysModified = file.lastModified();
            ks.load(new FileInputStream(file), passChars);

            loadPossibleAliases(ks);
            passwordValid = true;

            if (encAlias == null) {
                throw new BadKeyPrefsException("encryption-alias");
            } else if (signAlias == null) {
                throw new BadKeyPrefsException("signing-alias");
            }

            KeyPair signingKeys = loadKeys(ks, signAlias, passChars);
            KeyPair encryptionKeys;
            boolean same = encAlias.equals(signAlias);
            if (same) {
                encryptionKeys = signingKeys;
            } else {
                encryptionKeys = loadKeys(ks, encAlias, passChars);
            }

            keysInfo = new PrivateKeysInfo(signingKeys, encryptionKeys);
            loadedFromFile = file;
            loadedSigningAlias = signAlias;
            loadedEncAlias = encAlias;
        }

        private void loadPossibleAliases(KeyStore ks) throws KeyStoreException {
            List aliases = new ArrayList();
            Enumeration alenum = ks.aliases();
            while (alenum.hasMoreElements()) {
                String alias = (String) alenum.nextElement();
                aliases.add(alias);
            }
            possibleAliases = (String[]) aliases.toArray(new String[aliases.size()]);
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
