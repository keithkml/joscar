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

import net.kano.aimcrypto.DistinguishedName;
import net.kano.joscar.DefensiveTools;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class CertificateDetailsAccessory extends JPanel {
    private final AttributeSet NORMAL;
    private final AttributeSet BOLD;
    private final AttributeSet ITALIC;
    private final AttributeSet BLOCKQUOTE;

    {
        MutableAttributeSet base = new SimpleAttributeSet();
        Font font = UIManager.getFont("Label.font");
//        float indent = 20;
        if (font != null) {
            StyleConstants.FontConstants.setFontFamily(base, font.getFamily());
            StyleConstants.FontConstants.setFontSize(base, font.getSize());
//            indent = font.getSize2D()*2;
        }

        MutableAttributeSet bold = new SimpleAttributeSet(base);
        StyleConstants.FontConstants.setBold(bold, true);

        MutableAttributeSet italic = new SimpleAttributeSet(base);
        StyleConstants.FontConstants.setItalic(italic, true);

        MutableAttributeSet block = new SimpleAttributeSet(base);
//        StyleConstants.FontConstants.setLeftIndent(block, indent);
//        StyleConstants.FontConstants.setFirstLineIndent(block, indent);
        StyleConstants.setAlignment(block, StyleConstants.ALIGN_RIGHT);

        NORMAL = base;
        BOLD = bold;
        ITALIC = italic;
        BLOCKQUOTE = block;
    }

    private JPanel mainPanel;
    private JScrollPane textScrollPane;
    private JTextPane text;

    private final JFileChooser chooser;
    private final StyledDocument document;

    private DateFormat dateFormatter
            = DateFormat.getDateInstance(DateFormat.LONG);

    {
        add(mainPanel);
        text.setEditorKit(new StyledEditorKit());
        document = (StyledDocument) text.getDocument();
        textScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
    }


    public CertificateDetailsAccessory(JFileChooser chooser) {
        DefensiveTools.checkNull(chooser, "chooser");

        this.chooser = chooser;
        chooser.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals(
                        JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                    setLoading();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            updatePreview();
                        }
                    });
                }
            }
        });
    }

    private X509Certificate loadCert(File file)
            throws NoSuchProviderException, CertificateException,
            FileNotFoundException {
        CertificateFactory factory = CertificateFactory.getInstance(
                "X.509", "BC");

        FileInputStream fin = null;
        X509Certificate cert = null;
        try {
            fin = new FileInputStream(file);
            cert = (X509Certificate) factory.generateCertificate(fin);
        } finally {
            if (fin != null) {
                try { fin.close(); } catch (IOException ignored) { }
            }
        }
        return cert;
    }

    private void setLoading() {
        clear();

        File[] files = chooser.getSelectedFiles();
        if (files.length == 0) {
            return;
        } else if (files.length > 1) {
            setNoPreview("No certificate details available");
            return;
        } else {
            setNoPreview("Loading details...");
        }
    }

    private void updatePreview() {
        updatePreviewText();
        text.setCaretPosition(0);
    }

    private void updatePreviewText() {
        clear();

        File[] files = chooser.getSelectedFiles();
        if (files.length == 0) {
            return;
        } else if (files.length > 1) {
            setNoPreview("No certificate details available");
            return;
        }

        File file = files[0];
        X509Certificate cert;
        try {
            cert = loadCert(file);
        } catch (NoSuchProviderException e) {
            setNoPreview("Details for the selected certificate could not "
                    + "be loaded because the certificates is in an "
                    + "unsupported format.");
            return;
        } catch (CertificateException e) {
            setNoPreview("An error prevented details from being loaded for "
                    + "the selected certificate file.\n\nThe error was: "
                    + e.getMessage());
            return;
        } catch (FileNotFoundException e) {
            setNoPreview("The selected file could not be opened. You may not "
                    + "have permission to open this file.");
            return;
        }

        if (cert == null) {
            setNoPreview("An error prevented details from being loaded for "
                    + "the selected certificate file. The file may not be an "
                    + "X.509 certificate file, or it may be corrupt.");
            return;
        }

        int version = cert.getVersion();

        insert("X.509 v" + version + " Certificate\n", BOLD);

        DistinguishedName subdn = DistinguishedName.getSubjectInstance(cert);
        insert("Certifies:\n", ITALIC);
        insert(getDisplayString(subdn) + "\n\n", BLOCKQUOTE);

        DistinguishedName issdn = DistinguishedName.getIssuerInstance(cert);
        insert("Certified by:\n", ITALIC);
        insert(getDisplayString(issdn) + "\n\n", BLOCKQUOTE);

        Date now = new Date();
        Date starts = cert.getNotBefore();
        if (starts.after(now)) {
            insert("Only valid after:\n", ITALIC);
            insert(dateFormatter.format(starts) + "\n\n", BLOCKQUOTE);
        }

        Date expires = cert.getNotAfter();
        String s = expires.before(now) ? "d" : "s";
        insert("Expire" + s + ":\n", ITALIC);
        insert(dateFormatter.format(expires) + "\n\n", BLOCKQUOTE);

        insert("Algorithm:\n", ITALIC);
        insert(cert.getSigAlgName(), BLOCKQUOTE);
    }

    private void clear() {
        try {
            document.remove(0, document.getLength());
        } catch (BadLocationException impossible) { }
    }

    private String getDisplayString(DistinguishedName subdn) {
        String country = subdn.getCountry();
        Locale localy = new Locale("", country);
        String line = subdn.getName() + ", " + subdn.getOrganization() + ", "
                            + subdn.getCity() + ", " + subdn.getState() + ", "
                            + localy.getDisplayCountry();
        return line;
    }

    private void insert(String str, AttributeSet ATTRS_BOLD) {
        try {
            document.insertString(document.getLength(), str, ATTRS_BOLD);
        } catch (BadLocationException impossible) { }
    }

    private void setNoPreview(String text) {
        insert(text, null);
    }
}
