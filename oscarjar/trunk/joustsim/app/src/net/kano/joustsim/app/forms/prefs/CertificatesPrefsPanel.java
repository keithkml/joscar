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

package net.kano.joustsim.app.forms.prefs;

import net.kano.joustsim.trust.DistinguishedName;
import net.kano.joustsim.app.GuiResources;
import net.kano.joustsim.oscar.AppSession;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.app.config.CantBeAddedException;
import net.kano.joustsim.app.config.CantSavePrefsException;
import net.kano.joustsim.app.forms.FilterListCellRenderer;
import net.kano.joustsim.app.forms.SortedListModel;
import net.kano.joustsim.trust.CertificateTrustListener;
import net.kano.joustsim.trust.CertificateTrustManager;
import net.kano.joustsim.trust.TrustException;
import net.kano.joustsim.trust.DistinguishedName;
import net.kano.joscar.DefensiveTools;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private static final EmptyBorder EMPTY_BORDER = new EmptyBorder(2, 5, 2, 5);

    private JPanel mainPanel;
    private JLabel infoMessage;

    private JList trustedCertsList;
    private JButton importCertButton;
    private JButton removeCertButton;

    private JLabel certErrorIconLabel;
    private JPanel certErrorPanel;
    private JTextPane certErrorText;

    private final AppSession appSession;
    private final Screenname sn;
    private final CertificateTrustManager certTrustMgr;

    private JFileChooser fileChooser = null;

    private RemoveCertAction removeCertAction = new RemoveCertAction();
    private ImportCertAction importCertAction = new ImportCertAction();

    private final TitledBorder mainBorder = new TitledBorder((String) null);

    private SortedListModel trustedCertsModel
            = new SortedListModel(new Comparator() {
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
    });

    private final CertificateTrustListener trustChangeListener
            = new CertificateTrustListener() {
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

    private final Icon errorIcon = GuiResources.getErrorIcon();
    private final ImageIcon mediumCertificateIcon = GuiResources.getMediumCertificateIcon();
    private final ImageIcon smallCertificateIcon = GuiResources.getTinyCertificateIcon();
    private final JPopupMenu popupMenu = new JPopupMenu();

    {
        setLayout(new BorderLayout());
        add(mainPanel);

        mainPanel.setBorder(mainBorder);

        importCertButton.setAction(importCertAction);
        removeCertButton.setAction(removeCertAction);

        trustedCertsList.setModel(trustedCertsModel);
        ListCellRenderer origRenderer = trustedCertsList.getCellRenderer();
        trustedCertsList.setCellRenderer(new FilterListCellRenderer(origRenderer) {
            public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                String mod;
                if (value instanceof X509Certificate) {
                    X509Certificate cert = (X509Certificate) value;
                    DistinguishedName dn
                            = DistinguishedName.getSubjectInstance(cert);
                    mod = "<HTML><B>" + dn.getName() + "</B><BR>"
                            + dn.getOrganization() + "<BR>" + dn.getCity()
                            + ", " + dn.getState() + ", " + dn.getCountry();
                } else {
                    mod = value.toString();
                }
//                mod = getOriginalRenderer().getClass().getName();
                Component comp = super.getListCellRendererComponent(list,
                        mod, index, isSelected, cellHasFocus);
                if (comp instanceof JLabel) {
                    JLabel label = (JLabel) comp;
                    label.setVerticalTextPosition(JLabel.TOP);
                    label.setIcon(getMediumCertificateIcon());
                    Border border = new CompoundBorder(label.getBorder(), EMPTY_BORDER);
                    label.setBorder(border);
                }
                return comp;
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
                clearCertErrorLabel();
                startSync();
            }

            public void componentHidden(ComponentEvent e) {
                stopSync();
            }
        });

        setPanelDescription(null);

        KeyStroke deleteKey = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        Object removeName = removeCertAction.getValue(Action.NAME);
        trustedCertsList.getInputMap().put(deleteKey, removeName);
        trustedCertsList.getActionMap().put(removeName, removeCertAction);
        popupMenu.add(importCertAction);
        popupMenu.addSeparator();
        popupMenu.add(removeCertAction);
        trustedCertsList.add(popupMenu);
        trustedCertsList.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        updateCertButtons();
    }

    public CertificatesPrefsPanel(AppSession appSession, Screenname sn,
            CertificateTrustManager certTrustMgr) {
        DefensiveTools.checkNull(appSession, "appSession");
        DefensiveTools.checkNull(sn, "sn");
        DefensiveTools.checkNull(certTrustMgr, "certTrustMgr");

        this.appSession = appSession;
        this.sn = sn;
        this.certTrustMgr = certTrustMgr;
        startSync();
    }

    public void setPanelTitle(final String title) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mainBorder.setTitle(title);
            }
        });
    }

    public void setPanelDescription(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                infoMessage.setText(msg);
                infoMessage.setVisible(msg != null);
            }
        });
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
        int selected = fileChooser.showDialog(this, null);
        if (selected != JFileChooser.APPROVE_OPTION) return;

        File[] files = fileChooser.getSelectedFiles();

        if (files == null || files.length == 0) return;

        List failed = new ArrayList();

        for (int i = 0; i < files.length; i++) {
            try {
                if (!certTrustMgr.importCertificate(files[i])) {
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
            certErrorText.setCaretPosition(0);
            certErrorPanel.setVisible(true);
        }
    }

    protected String getImportFailedDetails(Exception e, String tryAgainText) {
        String details;
        String tryAgainMod = tryAgainText == null ? "" : tryAgainText;
        if (e instanceof IOException) {
            details = "An error occurred while loading the file. "
                    + tryAgainMod;

        } else if (e instanceof TrustException) {
            Throwable cause = e.getCause();
            if (cause instanceof CantSavePrefsException) {
                details = "A Your list of certificates could not be "
                        + "saved. " + tryAgainMod;

            } else if (e instanceof CantBeAddedException) {
                details = "The file is in an unsupported format.";

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
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setApproveButtonText("Import");
            fileChooser.setApproveButtonToolTipText("Import selected certificate");
            fileChooser.setDialogTitle("Import Certificate");
            fileChooser.addChoosableFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    if (f.isDirectory()) return true;
                    boolean valid = hasValidCertFilename(f);
                    return valid;
                }

                public String getDescription() {
                    return "X.509 Certificate Files";
                }
            });
            fileChooser.setFileView(new FileView() {
                public Icon getIcon(File f) {
                    if (hasValidCertFilename(f)) {
                        return getSmallCertificateIcon();
                    } else {
                        return super.getIcon(f);
                    }
                }
            });
        }
    }

    private boolean hasValidCertFilename(File f) {
        if (f == null) return false;
        String root = f.getName().toLowerCase();
        return root.endsWith(".cacert") || root.endsWith(".der")
                || root.endsWith(".crt") || root.endsWith(".cer")
                || root.endsWith(".pem");
    }

    public void clearCertErrorLabel() {
        certErrorPanel.setVisible(false);
    }

    private void updateCertButtons() {
        removeCertAction.setEnabled(!trustedCertsList.getSelectionModel()
                .isSelectionEmpty());
    }

    protected ImageIcon getMediumCertificateIcon() {
        return mediumCertificateIcon;
    }

    protected ImageIcon getSmallCertificateIcon() {
        return smallCertificateIcon;
    }

    private class ImportCertAction extends AbstractAction {
        public ImportCertAction() {
            super("Import...");

            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
        }

        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    clearCertErrorLabel();
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
            clearCertErrorLabel();
            removeSelectedCerts();
        }
    }
}
