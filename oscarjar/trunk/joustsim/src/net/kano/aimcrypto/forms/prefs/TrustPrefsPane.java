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
 *  File created by keith @ Feb 4, 2004
 *
 */

package net.kano.aimcrypto.forms.prefs;

import net.kano.aimcrypto.AppSession;
import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.forms.prefs.SignersPrefsPanel;
import net.kano.aimcrypto.config.LocalPreferencesManager;
import net.kano.aimcrypto.config.PermanentCertificateTrustManager;
import net.kano.aimcrypto.config.PermanentSignerTrustManager;
import net.kano.joscar.DefensiveTools;

import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Component;

public class TrustPrefsPane extends JPanel implements PrefsPane {
    private JPanel trustedCertsHolder;
    private JPanel trustedSignersHolder;

    private final AppSession session;
    private final Screenname sn;

    private final PermanentCertificateTrustManager certTrustMgr;
    private final PermanentSignerTrustManager signerTrustMgr;

    private final CertificatesPrefsPanel certsPrefs;
    private final SignersPrefsPanel signersPrefs;
    private JPanel mainPanel;

    {
        setLayout(new BorderLayout());
        add(mainPanel);
    }

    public TrustPrefsPane(AppSession session, Screenname sn) {
        DefensiveTools.checkNull(session, "session");
        DefensiveTools.checkNull(sn, "sn");

        this.session = session;
        this.sn = sn;

        LocalPreferencesManager localPrefs = session.getLocalPrefs(sn);
        certTrustMgr = localPrefs.getStoredCertificateTrustManager();
        certsPrefs = new CertificatesPrefsPanel(session, sn, certTrustMgr);
        certsPrefs.setPanelTitle("Trusted Buddy Certificates");
        certsPrefs.setPanelDescription("<HTML>Buddies who are using one of the "
                + "certificates below will be trusted automatically.");

        signerTrustMgr = localPrefs.getStoredSignerTrustManager();
        signersPrefs = new SignersPrefsPanel(session, sn, signerTrustMgr);
        signersPrefs.setPanelTitle("Trusted Certificate Authorities");
        signersPrefs.setPanelDescription("<HTML>Buddies whose identity is verified "
                + "by one of the Certificate Authorities below will be<BR>"
                + "trusted automatically.");

        trustedCertsHolder.setLayout(new BorderLayout());
        trustedCertsHolder.add(certsPrefs);

        trustedSignersHolder.setLayout(new BorderLayout());
        trustedSignersHolder.add(signersPrefs);
    }

    public String getPlainPrefsName() {
        return "Trusted Buddies";
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
        return "View and change your list of trusted buddies";
    }

    public Component getPrefsComponent() {
        return this;
    }

    public void prefsWindowFocused() {
    }

    public void prefsWindowFocusLost() {
    }

    public void prefsPaneShown() {
    }

    public void prefsPaneHidden() {
    }

}
