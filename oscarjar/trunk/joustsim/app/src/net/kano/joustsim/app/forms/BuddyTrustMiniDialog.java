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
 *  File created by keith @ Feb 7, 2004
 *
 */

package net.kano.joustsim.app.forms;

import net.kano.joustsim.trust.DistinguishedName;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.BuddyInfo;
import net.kano.joustsim.oscar.BuddyInfoManager;
import net.kano.joustsim.oscar.oscar.service.info.BuddyTrustAdapter;
import net.kano.joustsim.oscar.oscar.service.info.BuddyTrustEvent;
import net.kano.joustsim.oscar.oscar.service.info.BuddyTrustManager;
import net.kano.joustsim.trust.BuddyCertificateInfo;
import net.kano.joustsim.trust.CertificateTrustManager;
import net.kano.joustsim.trust.TrustException;
import net.kano.joustsim.trust.DistinguishedName;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

public class BuddyTrustMiniDialog extends JPanel implements MiniDialog {
    private static final String TRUST_STRING = "Trust...";

    private JPanel mainPanel;
    private JScrollPane textScrollPane;
    private JTextPane textPane;
    private JLabel iconLabel;
    private JComboBox trustMenu;

    private final CertificateTrustManager trustMgr;
    private final BuddyTrustManager buddyTrustMgr;
    private final net.kano.joustsim.Screenname buddy;
    private final BuddyCertificateInfo origCertificateInfo;
    private final BuddyInfoManager buddyInfoMgr;

    private BuddyCertificateInfo certificateInfo = null;

    private TrustAction trustAction = new TrustAction();
    private DontTrustAction dontTrustAction = new DontTrustAction();
    private ViewDetailsAction viewDetailsAction = new ViewDetailsAction();

    private final ImageIcon certIcon
            = new ImageIcon(getClass().getClassLoader().getResource(
                    "icons/certificate-small.png"));
    private final ImageIcon grayedCertIcon
            = new ImageIcon(getClass().getClassLoader().getResource(
                    "icons/certificate-small-gray.png"));

    private final BuddyTrustAdapter trustListener = new BuddyTrustAdapter() {
        public void gotTrustedCertificateChange(BuddyTrustEvent event) {
            if (!event.isFor(buddy)) return;
            System.out.println("trusted cert for " + buddy);

            hideMe();
        }

        public void gotUntrustedCertificateChange(BuddyTrustEvent event) {
            if (!event.isFor(buddy)) return;
            System.out.println("untrusted cert for " + buddy);

            final BuddyCertificateInfo info = event.getCertInfo();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateCertInfo(info);
                }
            });
        }

        public void gotUnknownCertificateChange(BuddyTrustEvent event) {
            if (!event.isFor(buddy)) return;
            System.out.println("unknown cert for " + buddy);

            setLoadingCertInfo();
        }
    };
    private final DefaultComboBoxModel trustOptionsModel
            = new DefaultComboBoxModel();


    {
        setLayout(new BorderLayout());
        add(mainPanel);
        textScrollPane.setBorder(null);

        iconLabel.setText(null);

        trustMenu.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object item = e.getItem();
                    if (item instanceof Action) {
                        Action a = (Action) item;
                        KeyEvent ke = new KeyEvent(BuddyTrustMiniDialog.this,
                                0, 0, 0, 0, KeyEvent.CHAR_UNDEFINED);
                        SwingUtilities.notifyAction(a, null, ke,
                                BuddyTrustMiniDialog.this, 0);
                    }
                    if (item != TRUST_STRING) {
                        trustMenu.setSelectedItem(TRUST_STRING);
                    }
                }
            }
        });
        trustOptionsModel.addElement(trustAction);
        trustOptionsModel.addElement(dontTrustAction);
        trustOptionsModel.addElement(new JSeparator());
        trustOptionsModel.addElement(viewDetailsAction);
        trustOptionsModel.setSelectedItem(TRUST_STRING);
        trustMenu.setModel(trustOptionsModel);
        trustMenu.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                Object use;
                if (value instanceof Action) {
                    Action a = (Action) value;
                    use = a.getValue(Action.NAME);

                } else if (value instanceof JSeparator) {
                    return (JSeparator) value;
                } else {
                    use = value;
                }
                super.getListCellRendererComponent(list, use, index,
                        isSelected, cellHasFocus);
                return this;
            }
        });

        textPane.setFont(UIManager.getFont("Label.font"));
    }

    public BuddyTrustMiniDialog(AimConnection conn, net.kano.joustsim.Screenname buddy,
            BuddyCertificateInfo certInfo) {
        DefensiveTools.checkNull(conn, "conn");
        DefensiveTools.checkNull(buddy, "buddy");

        this.trustMgr = conn.getLocalPrefs().getCertificateTrustManager();
        this.buddyTrustMgr = conn.getBuddyTrustManager();
        this.buddy = buddy;
        this.origCertificateInfo = certInfo;

        setLoadingCertInfo();
        buddyTrustMgr.addBuddyTrustListener(trustListener);
        buddyInfoMgr = conn.getBuddyInfoManager();
        BuddyInfo buddyInfo = buddyInfoMgr.getBuddyInfo(buddy);
        BuddyCertificateInfo newCertInfo = buddyInfo.getCertificateInfo();
        if (newCertInfo != null && newCertInfo.isUpToDate()) {
            System.out.println("cert info is ok!");
            updateCertInfo(newCertInfo);
        }
    }

    private void cleanUp() {
        buddyTrustMgr.removeBuddyTrustListener(trustListener);
    }

    public Component getComponent() {
        return this;
    }

    private void setLoadingCertInfo() {
        trustAction.setEnabled(false);
        textPane.setText("Downloading security information for "
                + buddy.getFormatted() + "...");
        iconLabel.setIcon(grayedCertIcon);
    }

    private void updateCertInfo(BuddyCertificateInfo certInfo) {
        setCertInfo(certInfo);
        if (certInfo == null || hasInvalidHash(certInfo)) {
            textPane.setText("Security information for " + buddy.getFormatted()
                    + " could not be downloaded.");
            trustAction.setEnabled(false);
            iconLabel.setIcon(grayedCertIcon);

        } else if (!certInfo.hasBothCertificates()) {
            textPane.setText("Security information that was downloaded for "
                    + buddy.getFormatted() + " seems to be corrupt.");
            trustAction.setEnabled(false);
            iconLabel.setIcon(grayedCertIcon);

        } else {
            DistinguishedName sn = DistinguishedName.getSubjectInstance(
                    certInfo.getSigningCertificate());
            String signedby = sn.getName() + ", " + sn.getOrganization();

            textPane.setText("Messages from " + buddy.getFormatted()
                    + " are signed by " + signedby + ".");
            trustAction.setEnabled(true);
            iconLabel.setIcon(certIcon);
        }
    }

    private void hideMe() {
        cleanUp();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                textPane.setText("(Trusted)");
                setVisible(false);
            }
        });
    }

    private boolean hasInvalidHash(BuddyCertificateInfo certInfo) {
        DefensiveTools.checkNull(certInfo, "certInfo");

        BuddyCertificateInfo orig = origCertificateInfo;
        if (orig == null || orig == certInfo) return false;

        ByteBlock origHash = orig.getCertificateInfoHash();
        ByteBlock retrievedHash = certInfo.getCertificateInfoHash();
        System.out.println("- orig hash: " + origHash);
        System.out.println("- new hash: " + retrievedHash);
        boolean equal = origHash.equals(retrievedHash);
        System.out.println("- equal: " + equal);
        return !equal;
    }

    private BuddyCertificateInfo getCertificateInfo() {
        return certificateInfo;
    }

    private synchronized void setCertInfo(BuddyCertificateInfo certInfo) {
        this.certificateInfo = certInfo;
    }

    private class TrustAction extends AbstractAction {
        public TrustAction() {
            super("Trust Certificates");

            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_T));
        }

        public void actionPerformed(ActionEvent e) {
            BuddyCertificateInfo certInfo = getCertificateInfo();
            try {
                trustMgr.trustCertificate(certInfo.getEncryptionCertificate());
            } catch (TrustException e1) {
                //TODO: couldn't trust
                e1.printStackTrace();
            }
            try {
                trustMgr.trustCertificate(certInfo.getSigningCertificate());
            } catch (TrustException e1) {
                //TODO: couldn't trust
                e1.printStackTrace();
            }
            hideMe();
        }
    }
    private class DontTrustAction extends AbstractAction {
        public DontTrustAction() {
            super("Do Not Trust");

            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
        }

        public void actionPerformed(ActionEvent e) {
            hideMe();
        }
    }
    private class ViewDetailsAction extends AbstractAction {
        public ViewDetailsAction() {
            super("View Certificates...");

            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_V));
        }

        public void actionPerformed(ActionEvent e) {
            //TODO: view certificate details
        }
    }
}
