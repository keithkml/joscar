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

package net.kano.aimcrypto.forms;

import net.kano.aimcrypto.DistinguishedName;
import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.config.BuddyCertificateInfo;
import net.kano.aimcrypto.config.CertificateTrustManager;
import net.kano.aimcrypto.config.TrustException;
import net.kano.aimcrypto.connection.AimConnection;
import net.kano.aimcrypto.connection.oscar.service.info.BuddyTrustAdapter;
import net.kano.aimcrypto.connection.oscar.service.info.BuddyTrustManager;
import net.kano.joscar.DefensiveTools;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class BuddyTrustedMiniDialog extends JPanel implements MiniDialog {
    private JPanel mainPanel;
    private JButton trustButton;
    private JScrollPane textScrollPane;
    private JTextPane textPane;
    private JLabel iconLabel;

    private final CertificateTrustManager trustMgr;
    private final BuddyTrustManager buddyTrustMgr;
    private final Screenname buddy;
    private final BuddyCertificateInfo origCertificateInfo;

    private BuddyCertificateInfo certificateInfo = null;

    private TrustAction trustAction = new TrustAction();
    private final ImageIcon certIcon
            = new ImageIcon(getClass().getClassLoader().getResource(
                    "icons/certificate-small.png"));
    private final ImageIcon grayedCertIcon
            = new ImageIcon(getClass().getClassLoader().getResource(
                    "icons/certificate-small-gray.png"));
    private final BuddyTrustAdapter trustListener = new BuddyTrustAdapter() {
        public void gotTrustedCertificateChange(BuddyTrustManager manager,
                Screenname buddy, BuddyCertificateInfo info) {
            if (!buddy.equals(BuddyTrustedMiniDialog.this.buddy)) return;

            hideMe();
        }

        public void gotUntrustedCertificateChange(BuddyTrustManager manager,
                Screenname buddy, final BuddyCertificateInfo info) {
            if (!buddy.equals(BuddyTrustedMiniDialog.this.buddy)) return;

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateCertInfo(info);
                }
            });
        }

        public void gotUnknownCertificateChange(BuddyTrustManager manager,
                Screenname buddy, BuddyCertificateInfo certInfo) {
            setLoadingCertInfo();
        }
    };

    {
        setLayout(new BorderLayout());
        add(mainPanel);
        textScrollPane.setBorder(null);

        iconLabel.setText(null);

        trustButton.setAction(trustAction);
    }

    public BuddyTrustedMiniDialog(AimConnection conn, Screenname buddy,
            BuddyCertificateInfo certInfo) {
        DefensiveTools.checkNull(conn, "conn");
        DefensiveTools.checkNull(buddy, "buddy");

        this.trustMgr = conn.getLocalPrefs().getStoredCertificateTrustManager();
        this.buddyTrustMgr = conn.getBuddyTrustManager();
        this.buddy = buddy;
        this.origCertificateInfo = certInfo;

        setLoadingCertInfo();
        buddyTrustMgr.addBuddyTrustListener(trustListener);
        if (certInfo != null && certInfo.isUpToDate()) {
            System.out.println("cert info is ok!");
            updateCertInfo(certInfo);
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
        System.out.println("updating cert info: " + certInfo);
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
        return orig != null && !orig.getCertificateInfoHash().equals(
                certInfo.getCertificateInfoHash());
    }

    private synchronized void setCertInfo(BuddyCertificateInfo certInfo) {
        this.certificateInfo = certInfo;
    }

    private class TrustAction extends AbstractAction {
        public TrustAction() {
            super("Trust");

            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_T));
        }

        public void actionPerformed(ActionEvent e) {
            try {
                trustMgr.trustCertificate(certificateInfo.getEncryptionCertificate());
            } catch (TrustException e1) {
                e1.printStackTrace();
            }
            try {
                trustMgr.trustCertificate(certificateInfo.getSigningCertificate());
            } catch (TrustException e1) {
                e1.printStackTrace();
            }
        }
    }
}
