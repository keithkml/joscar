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
 *  File created by keith @ Jan 30, 2004
 *
 */

package net.kano.aimcrypto.forms.prefs;

import net.kano.aimcrypto.AppSession;
import net.kano.aimcrypto.DistinguishedName;
import net.kano.aimcrypto.GuiResources;
import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.config.LoadingException;
import net.kano.aimcrypto.config.LocalKeysManager;
import net.kano.aimcrypto.config.PrivateKeysInfo;
import net.kano.aimcrypto.forms.ListComboBoxModel;
import net.kano.joscar.DefensiveTools;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//TODO: this class modifies the model outside the event thread
public class LocalCertificatesPrefsPane extends JPanel implements PrefsPane {
    private static final Object VALUE_BROWSE = new Object();
    private static final Object VALUE_SEPARATOR = new Object();
    private static final Object VALUE_NONE = new Object();

    private static final Object VALUE_CHOOSE = new Object();

    private JPanel mainPanel;
    private JComboBox certFileBox;
    private JPanel chooseCertsPanel;
    private JComboBox signWithBox;
    private JComboBox encryptWithBox;
    private JTextPane currentCertsPane;
    private JLabel aliasesInfoLabel;
    private JPasswordField passwordBox;
    private JCheckBox savePasswordBox;
    private JLabel signWithLabel;
    private JLabel encryptWithLabel;
    private JLabel passwordLabel;
    private JLabel certInfoIconLabel;
    private JLabel myCertLabel;

    private JFileChooser fc = null;

    private final AppSession appSession;
    private final Screenname sn;
    private final LocalKeysManager securityInfo;

    private ListComboBoxModel certificateFileList = new ListComboBoxModel();
    private ListComboBoxModel signingCertificateList = new ListComboBoxModel();
    private ListComboBoxModel encryptingCertificateList = new ListComboBoxModel();

    private String currentCertFilename = null;
    private boolean currentCertFileValid = false;
    private boolean signingAliasValid = false;
    private boolean encAliasValid = false;

    private LoadingException lastException = null;

    private final Icon warningIcon = GuiResources.getWarningIcon();
    private final Icon lockedIcon = GuiResources.getMediumLockIcon();

    private final JComponent[] possiblyBold = new JComponent[] {
        myCertLabel, passwordLabel, signWithLabel, encryptWithLabel
    };

    private CertReloaderThread reloader = null;

    {
        setLayout(new BorderLayout());
        add(mainPanel);

        currentCertsPane.setFont(UIManager.getFont("Label.font"));
        certFileBox.setModel(certificateFileList);
        signWithBox.setModel(signingCertificateList);
        encryptWithBox.setModel(encryptingCertificateList);

        certFileBox.setRenderer(new DefaultListCellRenderer() {
            private final JSeparator separator = new JSeparator();

            public Component getListCellRendererComponent(JList list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                Object modvalue = value;

                if (value == VALUE_NONE) {
                    modvalue = "<None>";
                } else if (value == VALUE_BROWSE) {
                    modvalue = "Import...";
                } else if (value == VALUE_SEPARATOR) {
                    return separator;
                } else if (value instanceof String) {
                    if (loadedYet
                            && value.equals(currentCertFilename)
                            && !currentCertFileValid) {
                        modvalue = "<HTML><STRIKE>" + value;
                    }
                }
                Component textComp = super.getListCellRendererComponent(list,
                        modvalue, index, isSelected, cellHasFocus);

                return textComp;
            }
        });
        certFileBox.addItemListener(new ItemListener() {
            private Object lastItem = certFileBox.getSelectedItem();

            public void itemStateChanged(ItemEvent e) {
                int type = e.getStateChange();
                Object item = e.getItem();
                if (type == ItemEvent.SELECTED) {
                    if (item == VALUE_SEPARATOR) {
                        certFileBox.setSelectedItem(lastItem);
                    } else if (item == VALUE_BROWSE) {
                        certFileBox.setSelectedItem(lastItem);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                browseForCertificate();
                                updateThings(true);
                                passwordBox.requestFocus();
                            }
                        });
                    } else if (item instanceof String || item == VALUE_NONE) {
                        Object realItem = item == VALUE_NONE ? null : item;
                        securityInfo.switchToCertificateFile((String) realItem);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                updateThings(true);
                                passwordBox.requestFocus();
                            }
                        });
                    }
                } else if (type == ItemEvent.DESELECTED) {
                    lastItem = item;
                }
            }
        });

        signWithBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object item = e.getItem();
                    if (item instanceof String) {
                        securityInfo.setSigningAlias((String) item);
                        updateThings(true);
                    }
                }
            }
        });
        encryptWithBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object item = e.getItem();
                    if (item instanceof String) {
                        securityInfo.setEncryptionAlias((String) item);
                        updateThings(true);
                    }
                }
            }
        });

        DefaultListCellRenderer aliasRenderer = new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                Object realval = value;
                if (value == VALUE_CHOOSE) realval = "Choose...";
                return super.getListCellRendererComponent(list, realval, index,
                        isSelected, cellHasFocus);
            }
        };
        signWithBox.setRenderer(aliasRenderer);
        encryptWithBox.setRenderer(aliasRenderer);

        addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                updateThings(true);
            }

            public void componentHidden(ComponentEvent e) {
                stopReloader();
            }
        });

        savePasswordBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                securityInfo.setSavePassword(savePasswordBox.isSelected());
            }
        });
        passwordBox.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}

            public void focusLost(FocusEvent e) {
                String newpass = new String(passwordBox.getPassword());
                String oldpass = securityInfo.getPassword();
                if (newpass.equals(oldpass)) return;
                securityInfo.setPassword(newpass);
                updateThings(false);
            }
        });

        currentCertsPane.setEditorKit(new StyledEditorKit());

        certInfoIconLabel.setText(null);
        certInfoIconLabel.setVisible(false);
    }

    public LocalCertificatesPrefsPane(AppSession appSession, Screenname sn) {
        DefensiveTools.checkNull(appSession, "appSession");
        DefensiveTools.checkNull(sn, "sn");

        this.appSession = appSession;
        this.sn = sn;
        this.securityInfo = appSession.getLocalPrefs(sn).getLocalKeysManager();
    }

    public String getPlainPrefsName() {
        return "Personal Certificates";
    }

    public boolean isGlobalPrefs() {
        return false;
    }

    public Icon getSmallPrefsIcon() {
        return null;
    }

    public String getPrefsName() {
        return getPlainPrefsName();
    }

    public String getPrefsDescription() {
        return "Select your AIM security certificates";
    }

    public Component getPrefsComponent() {
        return this;
    }

    public void prefsWindowFocused() {
        updateThings(false);
    }

    public void prefsWindowFocusLost() {
    }

    public void prefsPaneShown() {
        updateThings(true);
    }

    public void prefsPaneHidden() {
    }

    private boolean loadedYet = false;

    private synchronized void updateThings(boolean force) {
        loadedYet = false;
        ensureReloaderUp();
        reloader.reload(force);

        if (force) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    populateUI();
                }
            });
            repaint();
        }
    }

    private void populateUI() {
        ListComboBoxModel cfl = certificateFileList;
        cfl.clear();

        LocalKeysManager securityInfo = this.securityInfo;
        String[] possibleCertNames;
        String currentName;
        String pass;
        boolean savePassword;
        boolean haveKeys;
        boolean canChangePass;
        String[] aliasesArray;
        String signingAlias;
        String encryptionAlias;
        synchronized(securityInfo) {
            possibleCertNames = securityInfo.getPossibleCertificateNames();
            currentName = securityInfo.getCertificateFilename();
            pass = securityInfo.getPassword();
            savePassword = securityInfo.getSavePassword();
            haveKeys = securityInfo.getKeysInfo() != null;
            canChangePass = !haveKeys && !securityInfo.isPasswordValid();
            aliasesArray = securityInfo.getPossibleAliases();
            signingAlias = securityInfo.getSigningAlias();
            encryptionAlias = securityInfo.getEncryptionAlias();
        }

        List names = Arrays.asList(possibleCertNames);
        Collections.sort(names);
        cfl.clear();
        cfl.addElement(VALUE_NONE);
        cfl.addAll(names);
        cfl.addElement(VALUE_SEPARATOR);
        cfl.addElement(VALUE_BROWSE);

        currentCertFilename = currentName;
        cfl.setSelectedItem(currentName == null ? VALUE_NONE : currentName);
        boolean valid = names.contains(currentName);
        currentCertFileValid = valid;

        passwordBox.setText(pass == null ? "" : pass);
        savePasswordBox.setSelected(savePassword);
        passwordLabel.setEnabled(valid);
        passwordBox.setEnabled(valid && canChangePass);
        if (valid && !canChangePass) {
            passwordBox.setToolTipText("The password for this certificate file "
                    + "has already been entered");
        } else {
            passwordBox.setToolTipText(null);
        }
        savePasswordBox.setEnabled(valid);

        List aliases;
        boolean haveAliases = aliasesArray != null;
        boolean aliasesEmpty;
        if (haveAliases) {
            aliases = Arrays.asList(aliasesArray);
            Collections.sort(aliases);
            aliasesEmpty = aliases.isEmpty();
        } else {
            aliases = Collections.EMPTY_LIST;
            aliasesEmpty = true;
        }

        encryptWithBox.setEnabled(valid && !aliasesEmpty);
        signWithBox.setEnabled(valid && !aliasesEmpty);

        ListComboBoxModel scl = signingCertificateList;
        scl.clear();
        scl.addAll(aliases);
        scl.setSelectedItem(signingAlias == null && haveAliases
                ? VALUE_CHOOSE : signingAlias);
        signingAliasValid = aliases.contains(signingAlias);

        ListComboBoxModel ecl = encryptingCertificateList;
        ecl.clear();
        ecl.addAll(aliases);
        ecl.setSelectedItem(encryptionAlias == null && haveAliases
                ? VALUE_CHOOSE : encryptionAlias);
        encAliasValid = aliases.contains(encryptionAlias);

        if (valid && haveAliases) {
            int aliasCount = aliases.size();
            String certs;
            if (aliasCount == 1) {
                certs = "one certificate";
            } else {
                certs = aliasCount + " certificates";
            }
            aliasesInfoLabel.setText(
                    "This certificate file contains " + certs + ".");
        } else {
            aliasesInfoLabel.setText("No certificate file is loaded.");
        }

        signWithLabel.setEnabled(valid && haveAliases);
        encryptWithLabel.setEnabled(valid && haveAliases);

        updateSecurityDescription();

        JComponent next = null;
        if (!valid) {
            next = myCertLabel;
        } else if (canChangePass) {
            next = passwordLabel;
        } else if (!signingAliasValid) {
            next = signWithLabel;
        } else if (!encAliasValid) {
            next = encryptWithLabel;
        }
        for (int i = 0; i < possiblyBold.length; i++) {
            JComponent component = possiblyBold[i];
            Font font;
            if (component == next) {
                font = component.getFont().deriveFont(Font.BOLD);
            } else {
                font = null;
            }
            component.setFont(font);
        }

//        Dimension pref = getPreferredSize();
//        int width = getWidth();
//        int height = getHeight();
//        if (pref.width > width) width = pref.width;
//        if (pref.height > height) height = pref.height;
//        setSize(width, height);
    }

    private void updateSecurityDescription() {
        PrivateKeysInfo keysInfo = securityInfo.getKeysInfo();
        X509Certificate signingCert = null;
        X509Certificate encryptingCert = null;
        if (keysInfo != null) {
            signingCert = keysInfo.getSigningCertificate();
            encryptingCert = keysInfo.getEncryptionCertificate();
        }

        Icon icon = null;
        boolean leaveAlone = false;
        if (keysInfo == null || signingCert == null || encryptingCert == null) {
            if (loadedYet) {
                String details;
                if (currentCertFileValid) {
                    if (!securityInfo.isPasswordValid()) {
                        String pass = securityInfo.getPassword();
                        String details2;
                        if (pass == null || pass.trim().length() == 0) {
                            details2 = "Try entering the certificate file's "
                                    + "security password above.";
                        } else {
                            details2 = "Try another security password, or "
                                    + "import the certificate file again.";
                        }
                        details = "An error occurred while loading the "
                                + "certificate file. " + details2;
                    } else {
                        boolean needsEnc = !encAliasValid;
                        boolean needsSigning = !signingAliasValid;
                        if (needsSigning && needsEnc) {
                            details = "Select certificates for signing and "
                                    + "encrypting your messages above to enable "
                                    + "security features.";
                        } else if (needsSigning) {
                            details = "Choose a certificate for signing your "
                                    + "messages above to enable security features.";
                        } else if (needsEnc) {
                            details = "Choose a certificate for encrypting your "
                                    + "messages above to enable security features.";
                        } else {
                            details = "An error occurred while loading the "
                                    + "certificate file.";
                        }
                    }
                } else {
                    int pcn = securityInfo.getPossibleCertificateNames().length;
                    if (securityInfo.getCertificateFilename() != null) {
                        String details2;
                        if (pcn == 0) {
                            details2 = "importing a different certificate file";
                        } else {
                            details2 = "selecting a different certificate file "
                                    + "above";
                        }
                        details = "The certificate file you selected could not "
                                + "be found. Try importing the file again or "
                                + details2 + ".";
                    } else {
                        String word;
                        if (pcn == 0) word = "Import";
                        else word = "Select";

                        details = word + " a certificate file above to enable "
                                + "security features.";
                    }
                }

                currentCertsPane.setText("Your outgoing messages are not "
                        + "secure.\n\n" + details);
                icon = warningIcon;
            } else if (currentCertsPane.getText().trim().length() == 0) {
                currentCertsPane.setText("Loading certificates...");
            } else {
                leaveAlone = true;
            }

        } else {
            DistinguishedName signer = DistinguishedName.getSubjectInstance(
                    signingCert);
            DistinguishedName encryptor = DistinguishedName.getSubjectInstance(
                    encryptingCert);

            String signerName = signer.getName();
            String encryptorName = encryptor.getName();

            if (signerName == null) signerName = "(Unknown)";
            if (encryptorName == null) encryptorName = "(Unknown)";

            if (signerName.equals(encryptorName)) {
                currentCertsPane.setText("Your secure messages are "
                        + "signed and encrypted by " + signerName + ".");
            } else {
                currentCertsPane.setText("Outgoing messages are signed by "
                        + signerName + ", and encrypted by "
                        + encryptorName + ".");

            }

            SimpleAttributeSet red = new SimpleAttributeSet();
            red.addAttribute(StyleConstants.FontFamily,
                    mainPanel.getFont().getName());
            currentCertsPane.setParagraphAttributes(red, true);
            icon = lockedIcon;
        }
//        if (lastException != null) {
//            StringWriter stringWriter = new StringWriter();
//            lastException.printStackTrace(new PrintWriter(stringWriter));
//            currentCertsPane.setText(stringWriter.toString());
//        }
        if (!leaveAlone) {
            certInfoIconLabel.setIcon(icon);
            certInfoIconLabel.setVisible(icon != null);
        }
    }

    private boolean browseForCertificate() {
        initFileChooser();
        int result = fc.showDialog(this, null);
        if (result != JFileChooser.APPROVE_OPTION) return false;

        File file = fc.getSelectedFile();
        if (file == null) return false;

        try {
            securityInfo.importCertFile(file);
            securityInfo.switchToCertificateFile(file.getName());
            return true;
        } catch (IOException e) {
            String msg;
            if (e instanceof FileNotFoundException) {
                msg = "The file does not exist.";
            } else {
                msg = "An error occurred while reading the file. You may "
                        + "want to try importing the file again.";
            }
            JOptionPane.showMessageDialog(this, "<HTML><B>The file you "
                    + "selected could not be imported.</B><BR><BR>" + msg,
                    "Could not import file", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private synchronized void initFileChooser() {
        if (fc != null) return;
        fc = new JFileChooser();
        fc.setDialogTitle("Import Certificate File");
        fc.setAcceptAllFileFilterUsed(true);
        fc.setApproveButtonText("Import");
        fc.setApproveButtonToolTipText("Import selected certificate file");
        fc.setMultiSelectionEnabled(false);
        fc.addChoosableFileFilter(new FileFilter() {
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".p12")
                        || f.isDirectory();
            }

            public String getDescription() {
                return "PKCS12 Certificate Files";
            }
        });
    }

    private synchronized void ensureReloaderUp() {
        if (reloader == null) {
            CertReloaderThread rel = new CertReloaderThread();
            rel.start();
            reloader = rel;
        }
    }

    private synchronized void stopReloader() {
        CertReloaderThread rel;
        synchronized(this) {
            rel = reloader;
        }
        if (rel != null) {
            rel.shutdown();
            synchronized(this) {
                if (reloader == rel) reloader = null;
            }
        }
    }

    public void setLastException(LoadingException lastException) {
        this.lastException = lastException;
    }

    private class CertReloaderThread extends Thread {
        private final Object reloadLock = new Object();
        private boolean shouldReload = false;
        private boolean shouldShutdown = false;

        private Runnable populator = new Runnable() {
            public void run() {
                populateUI();
            }
        };
        private boolean forceRepaint = false;

        public void run() {
            while (!Thread.interrupted()) {
                boolean force;
                try {
                    force = waitForReloadRequest();
                } catch (InterruptedException e) {
                    break;
                }
                reloadIfNecessary(force);
                if (shouldShutdown) break;
            }
        }

        public void reload(boolean force) {
            synchronized(reloadLock) {
                shouldReload = true;
                forceRepaint = force;
                reloadLock.notifyAll();
            }
        }

        private boolean waitForReloadRequest() throws InterruptedException {
            while (true) {
                synchronized(reloadLock) {
                    if (shouldReload || shouldShutdown) {
                        shouldReload = false;
                        return forceRepaint;
                    }
                    reloadLock.wait();
                }
            }
        }

        private void reloadIfNecessary(boolean force) {
            boolean repaint = false;
            try {
                LocalCertificatesPrefsPane.this.setLastException(null);
                repaint = securityInfo.reloadIfNecessary();
            } catch (LoadingException e) {
                setLastException(e);
                repaint = true;
            } finally {
                loadedYet = true;
                if (repaint || force) SwingUtilities.invokeLater(populator);
            }
        }

        public void shutdown() {
            synchronized(reloadLock) {
                shouldShutdown = true;
                reloadLock.notifyAll();
            }
        }
    }
}
