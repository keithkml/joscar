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

package net.kano.aimcrypto.forms;

import net.kano.aimcrypto.AppSession;
import net.kano.aimcrypto.DistinguishedName;
import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.config.CantSavePrefsException;
import net.kano.aimcrypto.config.CertificateTrustManager;
import net.kano.aimcrypto.config.TrustChangeListener;
import net.kano.aimcrypto.config.TrustException;
import net.kano.joscar.DefensiveTools;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.ViewportLayout;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.AncestorListener;
import javax.swing.event.AncestorEvent;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CertificatesPrefsPanel extends JPanel {
    private JPanel mainPanel;

    private JList trustedCertsList;
    private JButton importCertButton;
    private JButton removeCertButton;

    private JLabel certErrorIconLabel;
    private JPanel certErrorPanel;
    private JTextPane certErrorText;
    private JLabel infoMessage;

    private final AppSession appSession;
    private final Screenname sn;
    private final CertificateTrustManager certTrustMgr;

    private final Icon errorIcon = UIManager.getIcon("OptionPane.errorIcon");

    private RemoveCertAction removeCertAction = new RemoveCertAction();
    private ImportCertAction importCertAction = new ImportCertAction();


    private Comparator certComparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            X509Certificate c1 = (X509Certificate) o1;
            X509Certificate c2 = (X509Certificate) o2;
            DistinguishedName dn1 = DistinguishedName.getSubjectInstance(c1);
            DistinguishedName dn2 = DistinguishedName.getSubjectInstance(c2);
            int names = dn1.getName().compareToIgnoreCase(dn2.getName());
            if (names != 0) return names;
            int companies = dn1.getOrganization().compareToIgnoreCase(
                    dn2.getOrganization());
            return companies;
        }
    };
    private SortedListModel trustedCertsModel
            = new SortedListModel(certComparator);

    private JFileChooser fileChooser = null;

    private static final EmptyBorder EMPTY_BORDER = new EmptyBorder(2, 5, 2, 5);
    private final TrustChangeListener trustChangeListener = new TrustChangeListener() {
        public void trustAdded(CertificateTrustManager manager,
                final X509Certificate cert) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    trustedCertsModel.addIfAbsent(cert);
                }
            });
        }

        public void trustRemoved(CertificateTrustManager manager,
                final X509Certificate cert) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    trustedCertsModel.remove(cert);
                }
            });
        }
    };

    {
        setLayout(new BorderLayout());
        add(mainPanel);
        importCertButton.setAction(importCertAction);
        removeCertButton.setAction(removeCertAction);

        trustedCertsList.setModel(trustedCertsModel);
        trustedCertsList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                String mod;
                if (value instanceof X509Certificate) {
                    X509Certificate cert = (X509Certificate) value;
                    DistinguishedName dn
                            = DistinguishedName.getSubjectInstance(cert);
                    mod = dn.getName() + ", " + dn.getOrganization();
                } else {
                    mod = value.toString();
                }
                super.getListCellRendererComponent(list, mod, index, isSelected,
                        cellHasFocus);
                Border border = new CompoundBorder(getBorder(), EMPTY_BORDER);
                int height = getPreferredSize().height;
                Dimension dimension = new Dimension(175, height);
                setMaximumSize(dimension);
                setPreferredSize(dimension);
                setBorder(border);
                return this;
            }
        });
        ListSelectionModel selmodel = trustedCertsList.getSelectionModel();
        selmodel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateCertButtons();
            }
        });

        certErrorIconLabel.setIcon(errorIcon);
        certErrorIconLabel.setText(null);
        addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                System.out.println("starting sync");
                clearCertErrorLabel();
                startSync();
            }

            public void componentHidden(ComponentEvent e) {
                System.out.println("stopping sync");
                stopSync();
            }
        });

        setInfoMessage(null);

        updateCertButtons();
    }

    private void setInfoMessage(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                infoMessage.setText(msg);
                infoMessage.setVisible(msg != null);
            }
        });
    }

    public CertificatesPrefsPanel(AppSession appSession, Screenname sn,
            CertificateTrustManager certTrustMgr) {
        DefensiveTools.checkNull(appSession, "appSession");
        DefensiveTools.checkNull(sn, "sn");
        DefensiveTools.checkNull(certTrustMgr, "mgr");

        this.appSession = appSession;
        this.sn = sn;
        this.certTrustMgr = certTrustMgr;
        startSync();
    }

    private void startSync() {
        this.certTrustMgr.addTrustListener(trustChangeListener);
        trustedCertsModel.replaceContents(Arrays.asList(
                this.certTrustMgr.getTrustedCertificates()));
    }

    private void stopSync() {
        this.certTrustMgr.removeTrustChangeListener(trustChangeListener);
        trustedCertsModel.clear();
    }

    private void importCert() {
        initFileChooser();
        fileChooser.showOpenDialog(this);

        File[] files = fileChooser.getSelectedFiles();

        if (files == null || files.length == 0) return;

        List failed = new ArrayList();

        for (int i = 0; i < files.length; i++) {
            try {
                if (!certTrustMgr.importCert(files[i])) {
                    failed.add(new ImportFailureInfo(files[i]));
                }
            } catch (Exception e) {
                failed.add(new ImportFailureInfo(files[i], e));
            }
        }

        if (!failed.isEmpty()) {
            String text;
            if (files.length == 1) {
                ImportFailureInfo info = (ImportFailureInfo) failed.get(0);
                Exception e = info.getException();
                String details;
                details = getImportFailedDetails(e,
                        "Try importing the file again.");
                text = "The certificate you selected could not be imported.\n\n"
                        + details;

            } else {
                StringBuffer detailsBuffer = new StringBuffer(100);
                if (failed.size() == files.length) {
                    detailsBuffer.append("None of the files you selected could "
                            + "be imported.");

                } else {
                    String word = (failed.size() == 1 ? "One" : "Some");
                    detailsBuffer.append(word + " of the files you selected "
                            + "could not be imported.");
                }
                Map errors = new HashMap();
                for (Iterator it = failed.iterator(); it.hasNext();) {
                    ImportFailureInfo info = (ImportFailureInfo) it.next();
                    String details = getImportFailedDetails(info.getException(),
                            null);
                    List siblings = (List) errors.get(details);
                    if (siblings == null) {
                        siblings = new ArrayList();
                        errors.put(details, siblings);
                    }
                    siblings.add(info.getFile());
                }

                for (Iterator eit = errors.entrySet().iterator(); eit.hasNext();) {
                    Map.Entry entry = (Map.Entry) eit.next();
                    String details = (String) entry.getKey();
                    List fileList = (List) entry.getValue();
                    StringBuffer buf = new StringBuffer(50);
                    for (Iterator fit = fileList.iterator(); fit.hasNext();) {
                        File file = (File) fit.next();
                        buf.append(file.getName());
                        if (fit.hasNext()) buf.append(", ");
                    }
                    buf.append(": ");
                    buf.append(details);
                    detailsBuffer.append("\n\n");
                    detailsBuffer.append(buf);
                }
                text = detailsBuffer.toString();
            }
            certErrorText.setText(text);
            certErrorPanel.setVisible(true);
        }
    }

    private String getImportFailedDetails(Exception e, String tryAgainText) {
        String details;
        String tryAgainMod = tryAgainText == null ? "" : tryAgainText;
        if (e instanceof IOException) {
            details = "An error occurred while loading the file. "
                    + tryAgainMod;

        } else if (e instanceof TrustException) {
            Throwable cause = e.getCause();
            if (cause instanceof CantSavePrefsException) {
                details = "Your list of trusted certificates could not be "
                        + "saved. " + tryAgainMod;
            } else {
                details = "An error occurred while loading the file. "
                        + tryAgainMod;
            }

        } else if (e != null) {
            details = "The certificate file may be corrupt or in the "
                    + "wrong format. Only certificate files in PEM, "
                    + "BER, or DER format can be imported. " + tryAgainMod;

        } else {
            details = "The certificate is already on your list.";
        }
        return details;
    }

    private static class ImportFailureInfo {
        private final File file;
        private final Exception e;

        public ImportFailureInfo(File file) {
            this(file, null);
        }

        public ImportFailureInfo(File file, Exception e) {
            this.file = file;
            this.e = e;
        }

        public File getFile() {
            return file;
        }

        public Exception getException() {
            return e;
        }
    }

    private void removeSelectedCerts() {
        Object[] selected = trustedCertsList.getSelectedValues();
        for (int i = 0; i < selected.length; i++) {
            Object obj = selected[i];
            if (!(obj instanceof X509Certificate)) {
                continue;
            }

            X509Certificate value = (X509Certificate) obj;
            certTrustMgr.revokeTrust(value);
        }
    }

    private synchronized void initFileChooser() {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(true);
            fileChooser.setAccessory(new CertificateDetailsAccessory(fileChooser));
            fileChooser.setApproveButtonText("Import");
            fileChooser.setDialogTitle("Import Certificate");
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.addChoosableFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    if (f.isDirectory()) return true;
                    String root = f.getName().toLowerCase();
                    return root.endsWith(".cacert") || root.endsWith(".der")
                            || root.endsWith(".crt") || root.endsWith(".cer")
                            || root.endsWith(".pem");
                }

                public String getDescription() {
                    return "X.509 Certificate Files";
                }
            });
        }
    }

    private void clearCertErrorLabel() {
        certErrorPanel.setVisible(false);
    }

    private void updateCertButtons() {
        removeCertAction.setEnabled(!trustedCertsList.getSelectionModel()
                .isSelectionEmpty());
    }

    private class ImportCertAction extends AbstractAction {
        public ImportCertAction() {
            super("Import...");

            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
        }

        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    importCert();
                }
            });
        }

    }

    private class RemoveCertAction extends AbstractAction {
        public RemoveCertAction() {
            super("Remove");

            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
        }

        public void actionPerformed(ActionEvent e) {
            removeSelectedCerts();
        }
    }
}
