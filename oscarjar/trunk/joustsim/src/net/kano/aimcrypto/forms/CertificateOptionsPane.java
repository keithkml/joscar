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

package net.kano.aimcrypto.forms;

import net.kano.aimcrypto.AppSession;
import net.kano.aimcrypto.DistinguishedName;
import net.kano.aimcrypto.LoadingException;
import net.kano.aimcrypto.PrivateKeysInfo;
import net.kano.aimcrypto.PrivateSecurityInfo;
import net.kano.aimcrypto.Screenname;
import net.kano.joscar.DefensiveTools;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.ListCellRenderer;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.net.URL;

import junit.runner.ReloadingTestSuiteLoader;
import com.sun.corba.se.internal.orbutil.Condition;

public class CertificateOptionsPane extends JFrame {
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
    private final PrivateSecurityInfo securityInfo;

    private ListComboBoxModel certificateFileList = new ListComboBoxModel();
    private ListComboBoxModel signingCertificateList = new ListComboBoxModel();
    private ListComboBoxModel encryptingCertificateList = new ListComboBoxModel();

    private String currentCertFilename = null;
    private boolean currentCertFileValid = false;
    private boolean signingAliasValid = false;
    private boolean encAliasValid = false;

    private LoadingException lastException = null;

    private Icon warningIcon = UIManager.getIcon("OptionPane.warningIcon");
    private Icon lockedIcon;
    {
        URL resource = getClass().getClassLoader().getResource("icons/lock-medium.png");
        if (resource == null) {
            lockedIcon = null;
        } else {
            lockedIcon = new ImageIcon(resource);
        }
    }

    private final JComponent[] possiblyBold = new JComponent[] {
        myCertLabel, passwordLabel, signWithLabel, encryptWithLabel
    };

    private CertReloaderThread reloader = null;

    {
        getContentPane().add(mainPanel);
        currentCertsPane.setFont(mainPanel.getFont());
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
                                updateThings();
                                passwordBox.requestFocus();
                            }
                        });
                    } else if (item instanceof String || item == VALUE_NONE) {
                        Object realItem = item == VALUE_NONE ? null : item;
                        securityInfo.switchToCertificateFile((String) realItem);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                updateThings();
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
                        updateThings();
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
                        updateThings();
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

        addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent e) {
                updateThings();
            }

            public void windowLostFocus(WindowEvent e) { }
        });
        addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                ensureReloaderUp();
                updateThings();
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
                updateThings();
            }
        });

        currentCertsPane.setEditorKit(new StyledEditorKit());

        certInfoIconLabel.setText(null);
        certInfoIconLabel.setVisible(false);
    }

    public CertificateOptionsPane(AppSession appSession, Screenname sn) {
        DefensiveTools.checkNull(appSession, "appSession");

        this.appSession = appSession;
        this.sn = sn;
        this.securityInfo = appSession.getPrivateSecurityInfo(sn);
    }

    private boolean loadedYet = false;

    private synchronized void updateThings() {
        loadedYet = false;
        ensureReloaderUp();
        reloader.reload();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                populateUI();
            }
        });
    }

    private void populateUI() {
        ListComboBoxModel cfl = certificateFileList;
        cfl.clear();

        PrivateSecurityInfo securityInfo = this.securityInfo;
        List names = Arrays.asList(securityInfo.getPossibleCertificateNames());
        Collections.sort(names);
        cfl.clear();
        cfl.addElement(VALUE_NONE);
        cfl.addAll(names);
        cfl.addElement(VALUE_SEPARATOR);
        cfl.addElement(VALUE_BROWSE);

        String currentName = securityInfo.getCertificateFilename();
        currentCertFilename = currentName;
        cfl.setSelectedItem(currentName == null ? VALUE_NONE : currentName);
        boolean valid = names.contains(currentName);
        currentCertFileValid = valid;

        String pass = securityInfo.getPassword();
        passwordBox.setText(pass == null ? "" : pass);
        savePasswordBox.setSelected(securityInfo.getSavePassword());
        passwordLabel.setEnabled(valid);
        boolean haveKeys = securityInfo.getKeysInfo() != null;
        boolean canChangePass = !haveKeys && !securityInfo.isPasswordValid();
        passwordBox.setEnabled(valid && canChangePass);
        if (valid && !canChangePass) {
            passwordBox.setToolTipText("The password for this certificate file "
                    + "has already been entered");
        } else {
            passwordBox.setToolTipText(null);
        }
        savePasswordBox.setEnabled(valid);

        String[] aliasesArray = securityInfo.getPossibleAliases();
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
        String signingAlias = securityInfo.getSigningAlias();
        scl.setSelectedItem(signingAlias == null && haveAliases
                ? VALUE_CHOOSE : signingAlias);
        signingAliasValid = aliases.contains(signingAlias);

        ListComboBoxModel ecl = encryptingCertificateList;
        ecl.clear();
        ecl.addAll(aliases);
        String encryptionAlias = securityInfo.getEncryptionAlias();
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

                    String word;
                    if (pcn == 0) word = "Import";
                    else word = "Select";

                    details = word + " a certificate file above to enable "
                            + "security features.";
                }
                currentCertsPane.setText("Your outgoing messages are not "
                        + "secure.\n\n" + details);
                icon = warningIcon;
            } else if (currentCertsPane.getText().trim().length() == 0) {
                currentCertsPane.setText("Loading certificates...");
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
                currentCertsPane.setText("Outgoing messages are signed and "
                        + "encrypted by " + signerName + ".");
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
        certInfoIconLabel.setIcon(icon);
        certInfoIconLabel.setVisible(icon != null);
    }

    private boolean browseForCertificate() {
        initFileChooser();
        fc.showDialog(this, null);
        File file = fc.getSelectedFile();
        if (file == null) {
            return false;
        } else {
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
    }

    private synchronized void initFileChooser() {
        if (fc != null) return;
        fc = new JFileChooser();
        fc.setDialogTitle("Import Certificate File");
        fc.setAcceptAllFileFilterUsed(true);
        fc.setApproveButtonText("Import");
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

    private class CertReloaderThread extends Thread {
        private final Object reloadLock = new Object();
        private boolean shouldReload = false;
        private boolean shouldShutdown = false;

        private Runnable populator = new Runnable() {
            public void run() {
                populateUI();
            }
        };

        public void run() {
            while (!Thread.interrupted()) {
                try {
                    waitForReloadRequest();
                } catch (InterruptedException e) {
                    break;
                }
                reloadIfNecessary();
                if (shouldShutdown) break;
            }
        }

        public void reload() {
            synchronized(reloadLock) {
                shouldReload = true;
                reloadLock.notifyAll();
            }
        }

        private void waitForReloadRequest() throws InterruptedException {
            while (true) {
                synchronized(reloadLock) {
                    reloadLock.wait();
                    if (shouldReload || shouldShutdown) {
                        shouldReload = false;
                        return;
                    }
                }
            }
        }

        private void reloadIfNecessary() {
            try {
                securityInfo.reloadIfNecessary();
            } catch (LoadingException e) {
                lastException = e;
            } finally {
                loadedYet = true;
                SwingUtilities.invokeLater(populator);
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
