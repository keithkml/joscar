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

package net.kano.joustsim.app.forms;

import net.kano.joscar.OscarTools;
import net.kano.joscar.snaccmd.auth.AuthResponse;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.app.GuiResources;
import net.kano.joustsim.app.GuiSession;
import net.kano.joustsim.app.JoustsimSession;
import net.kano.joustsim.app.config.GeneralLocalPrefs;
import net.kano.joustsim.app.config.GlobalPrefs;
import net.kano.joustsim.app.config.LocalPreferencesManager;
import net.kano.joustsim.oscar.ConnectionFailedStateInfo;
import net.kano.joustsim.oscar.DisconnectedStateInfo;
import net.kano.joustsim.oscar.LoginFailureStateInfo;
import net.kano.joustsim.oscar.StateInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.AuthFailureInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.DisconnectedFailureInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.FlapErrorFailureInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.LoginFailureInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.SnacErrorFailureInfo;
import net.kano.joustsim.oscar.oscar.loginstatus.TimeoutFailureInfo;

import javax.swing.AbstractAction;
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLDocument;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;

public class SignonBox extends JPanel implements SignonWindowBox {
    private static final Object ITEM_DELETE = new Object();
    private static final Object ITEM_SEPARATOR = new Object();

    private JPanel mainPanel;
    private JLabel screennameLabel;
    private JLabel passwordLabel;
    private JComboBox screennameBox;
    private JPasswordField passwordBox;
    private JButton signonButton;
    private JButton prefsButton;
    private JCheckBox rememberPassBox;
    private JButton closeButton;
    private JTextPane errorText;
    private JLabel errorIconLabel;
    private JPanel errorPanel;
    private JScrollPane errorScrollPane;
    private JPanel localPanel;

    private SignonAction signonAction = new SignonAction();
    private ShowPrefsAction showPrefsAction = new ShowPrefsAction();
    private CloseAction closeAction = new CloseAction();

    private GuiSession guiSession;

    private boolean disabled = false;

    private JTextComponent snTextEditor = null;
    private final WellBehavedComboBoxModel knownScreennamesModel
            = new WellBehavedComboBoxModel();

    private final ComboBoxEditor screennameBoxEditor = screennameBox.getEditor();
    private final JoustsimSession appSession;
    private int maxWidth = -1;
    private final AttributeSet defaultErrorStyles;
    private final Icon errorIcon = GuiResources.getErrorIcon();
    private final Icon infoIcon = GuiResources.getInformationIcon();

    private final TitledBorder errorPanelBorder
            = new TitledBorder((String) null);
    private boolean loadedScreennames = false;
    private SignonWindow signonWindow = null;

    {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        Font font = UIManager.getFont("Label.font");
        StyleConstants.setFontFamily(attrs, font.getName());
        StyleConstants.setFontSize(attrs, font.getSize());
        defaultErrorStyles = attrs.copyAttributes();
    }

    {
        rememberPassBox.setMnemonic('r');
        localPanel.setBorder(new TitledBorder((String) null));
        screennameLabel.setLabelFor(screennameBox);
        screennameLabel.setDisplayedMnemonic('N');
        screennameBox.setModel(knownScreennamesModel);
        Component snEditor = screennameBoxEditor.getEditorComponent();
        if (snEditor instanceof JTextComponent) {
            snTextEditor = (JTextComponent) snEditor;
            snTextEditor.addFocusListener(new OnFocusSelector());
            Document doc = snTextEditor.getDocument();
            doc.addDocumentListener(new InputFieldChangeListener());
            doc.addDocumentListener(new InputFieldChangeListener() {
                private String lastNormal = "";
                private String realLastNormal = "";

                protected void changed() {
                    String newnormal = OscarTools.normalize(getScreenname());

                    if (!newnormal.equals(realLastNormal)) {
                        if (!newnormal.equals(lastNormal)
                                && newnormal.length() > 0
                                && lastNormal.length() > 0) {
                            clearErrorText();
                        }
                        updatePassword();
                        realLastNormal = newnormal;
                        if (newnormal.length() > 0) lastNormal = newnormal;
                    }
                }
            });
        }
        screennameBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                int change = e.getStateChange();
                if (change == ItemEvent.SELECTED) {
                    String current = getScreenname();
                    Screenname sn = new Screenname(current);
                    LocalPreferencesManager prefs;
                    prefs = appSession.getLocalPrefsIfExist(sn);
                    if (prefs != null) {
                        GeneralLocalPrefs genPrefs = prefs.getGeneralPrefs();
                        String format = genPrefs.getScreennameFormat();
                        if (format != null && !format.equals(current)) {
                            screennameBox.setSelectedItem(format);
                        }
                    }
                    updatePassword();
                }
            }
        });
        screennameBox.setRenderer(new DefaultListCellRenderer() {
            private JSeparator separator = new JSeparator();

            public Component getListCellRendererComponent(JList list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                Object mod;
                if (value == ITEM_SEPARATOR) return separator;
                if (value == ITEM_DELETE) mod = "Delete...";
                else mod = value;
                return super.getListCellRendererComponent(list, mod, index,
                        isSelected, cellHasFocus);
            }
        });

        setLayout(new BorderLayout());
        add(mainPanel);
        passwordLabel.setDisplayedMnemonic('W');
        passwordLabel.setLabelFor(passwordBox);
        Document passDoc = passwordBox.getDocument();
        passDoc.addDocumentListener(new InputFieldChangeListener());
        passwordBox.addFocusListener(new OnFocusSelector());

        signonButton.setAction(signonAction);
        prefsButton.setAction(showPrefsAction);
        closeButton.setAction(closeAction);
        closeButton.setVerifyInputWhenFocusTarget(false);

        rememberPassBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Screenname sn = new Screenname(getScreenname());
                LocalPreferencesManager prefs
                        = appSession.getLocalPrefsIfExist(sn);
                if (prefs != null) {
                    String pass;
                    if (!rememberPassBox.isSelected()) pass = null;
                    else pass = new String(passwordBox.getPassword());

                    prefs.getGeneralPrefs().setSavedPassword(pass);
                }
            }
        });

        errorPanel.setBorder(errorPanelBorder);
        errorIconLabel.setText(null);
        errorScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

        updateButtons();
    }

    public void signonWindowBoxShown(SignonWindow window) {
        this.signonWindow = window;

        setDisabled(false);
        getRootPane().setDefaultButton(signonButton);
        reloadScreennames();
        updateButtons();
        updateSize();
    }

    public Component getSignonWindowBoxComponent() {
        return this;
    }

    public void signonWindowBoxShown() {
    }

    private void updateSize() {
        validate();
        final Dimension pref = super.getPreferredSize();
        Dimension npref;
        if (maxWidth == -1) {
            maxWidth = pref.width;
            npref = pref;
        } else {
            int origHeight = pref.height;
            npref = new Dimension(maxWidth, origHeight);
        }
        setPreferredSize(npref);
        if (!getSize().equals(npref)) {
            signonWindow.updateSize(this);
        }
    }

    private Dimension preferredSize = null;

    public Dimension getPreferredSize() {
        if (preferredSize == null) return super.getPreferredSize();
        else return preferredSize;
    }

    public void setPreferredSize(Dimension preferredSize) {
        this.preferredSize = preferredSize;
    }

    public SignonBox(GuiSession session) {
        this.guiSession = session;
        appSession = guiSession.getAppSession();
    }

    private void updateButtons() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String normalized = OscarTools.normalize(getScreenname());
                boolean snvalid = normalized.length() != 0
                        && isScreennameValid();
                boolean passempty = passwordBox.getDocument().getLength() == 0;

                screennameLabel.setEnabled(!disabled);
                screennameBox.setEnabled(!disabled);
                passwordLabel.setEnabled(!disabled && snvalid);
                passwordBox.setEnabled(!disabled && snvalid);
                rememberPassBox.setEnabled(!disabled && snvalid && !passempty);

                signonAction.setEnabled(!disabled && snvalid && !passempty);
                showPrefsAction.setEnabled(!disabled && snvalid);
            }
        });
    }

    private boolean isScreennameValid() {
        String sn = getScreenname();
        for (int i = 0; i < sn.length(); i++) {
            char ch = sn.charAt(i);
            boolean valid = (ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == ' ';
            if (!valid) return false;
        }
        return true;
    }

    public String getScreenname() {
        if (snTextEditor != null) {
            String text = snTextEditor.getText();
            return text == null ? "" : text;
        } else {
            Object sel = screennameBox.getSelectedItem();
            if (sel == null) return "";
            return sel.toString();
        }
    }

    public String getPassword() {
        return String.copyValueOf(passwordBox.getPassword());
    }

    public void setFailureInfo(StateInfo sinfo) {
        String title= null;
        String errorText = null;
        if (sinfo != null) {
            final String msg;
            if (sinfo instanceof LoginFailureStateInfo) {
                LoginFailureStateInfo lfsi
                        = (LoginFailureStateInfo) sinfo;
                LoginFailureInfo lfi = lfsi.getLoginFailureInfo();
                if (lfi instanceof TimeoutFailureInfo) {
                    msg = "The AOL server is not responding. Try again in a "
                            + "few minutes.";

                } else if (lfi instanceof FlapErrorFailureInfo
                        || lfi instanceof SnacErrorFailureInfo) {
                    String errcode;
                    if (lfi instanceof FlapErrorFailureInfo) {
                        FlapErrorFailureInfo fi = (FlapErrorFailureInfo) lfi;
                        errcode = "Red-" + fi.getFlapError();
                    } else {
                        SnacErrorFailureInfo si = (SnacErrorFailureInfo) lfi;
                        errcode = "Green-" + si;
                    }
                    msg = "An unknown error occurred while signing in. The "
                            + "error was " + errcode + ".";

                } else if (lfi instanceof AuthFailureInfo) {
                    AuthFailureInfo afi = (AuthFailureInfo) lfi;
                    int ec = afi.getErrorCode();

                    if (ec == AuthResponse.ERROR_ACCOUNT_DELETED) {
                        msg = "Your account has been deleted.";
                    } else if (ec == AuthResponse.ERROR_BAD_INPUT) {
                        msg = "The connection was corrupted while signing on. "
                                + "Try signing on again.";
                    } else if (ec == AuthResponse.ERROR_BAD_PASSWORD) {
                        msg = "The password you entered is not correct.";
                    } else if (ec == AuthResponse.ERROR_CLIENT_TOO_OLD) {
                        msg = "AOL said that this version of "
                                + GuiResources.getFullProgramName() + " is too "
                                + "old to connect. Try visiting the "
                                + GuiResources.getProjectName() + " website at "
                                + GuiResources.getProgramWebsiteUrl() + ", or "
                                + "try to sign on again later.";
                    } else if (ec == AuthResponse.ERROR_CONNECTING_TOO_MUCH_A
                            || ec == AuthResponse.ERROR_CONNECTING_TOO_MUCH_B) {
                        msg = "You are connecting too frequently. Wait 5 or 10 "
                                + "minutes and try again.";
                    } else if (ec == AuthResponse.ERROR_INVALID_SECURID) {
                        msg = "The SecurID you entered is wrong. If you did "
                                + "not enter a SecurID, some other error "
                                + "occurred. Try signing on again.";
                    } else if (ec == AuthResponse.ERROR_INVALID_SN_OR_PASS_A
                            || ec == AuthResponse.ERROR_INVALID_SN_OR_PASS_B) {
                        msg = "The screenname or password you entered is not "
                                + "correct. Try retyping your password and "
                                + "signing on again.";
                    } else if (ec == AuthResponse.ERROR_SIGNON_BLOCKED) {
                        msg = "Your account has been temporarily blocked.";
                    } else if (ec == AuthResponse.ERROR_TEMP_UNAVAILABLE_A
                            || ec == AuthResponse.ERROR_TEMP_UNAVAILABLE_B
                            || ec == AuthResponse.ERROR_TEMP_UNAVAILABLE_C
                            || ec == AuthResponse.ERROR_TEMP_UNAVAILABLE_D
                            || ec == AuthResponse.ERROR_TEMP_UNAVAILABLE_E
                            || ec == AuthResponse.ERROR_TEMP_UNAVAILABLE_F
                            || ec == AuthResponse.ERROR_TEMP_UNAVAILABLE_G) {
                        msg = "AIM is temporarily unavailable. Try signing on "
                                + "again, and if this error continues to "
                                + "occur, you can try again later.";
                    } else if (ec == AuthResponse.ERROR_UNDER_13) {
                        msg = "Your account is marked as being owned by "
                                + "someone under the age of 13. You must be "
                                + "13 years of age to use the AIM service. If "
                                + "this is not correct, visit the AIM website.";
                    } else {
                        msg = "An unknown error occurred while signing in. The "
                                + "error was Blue-" + ec + ".";
                    }
                } else if (lfi instanceof DisconnectedFailureInfo) {
                    DisconnectedFailureInfo di = (DisconnectedFailureInfo) lfi;
                    if (!di.isOnPurpose()) {
                        msg = "The connection to the AIM service was lost while "
                                + "signing in.";
                    } else {
                        // the user did it on purpose, it looks like, so we
                        // don't need to tell him that he did it
                        msg = null;
                    }
                } else {
                    msg = "An unknown error occurred while signing in.";
                }

            } else if (sinfo instanceof ConnectionFailedStateInfo) {
                ConnectionFailedStateInfo cfsi
                        = (ConnectionFailedStateInfo) sinfo;
                msg = "A connection could not be made to the AIM service.<br>"
                        + "(Connection to " + cfsi.getHost() + ":"
                        + cfsi.getPort() + " failed.)";

            } else if (sinfo instanceof DisconnectedStateInfo) {
                DisconnectedStateInfo di = (DisconnectedStateInfo) sinfo;
                if (!di.isOnPurpose()) {
                    title = "Disconnected";
                    msg = null;
                    errorText = "<b>Disconnected.</b><br>"
                            + "The connection to the AIM service was lost.";
                } else {
                    // the user wanted it, so we don't need to tell him
                    msg = null;
                }
            } else {
                // nothing interesting happened
                msg = null;
            }
            if (msg != null) {
                title = "Sign-on failed";
                errorText = "<b>Could not sign on to AIM.</b><br> " + msg;
            }
        }
        if (errorText != null) {
            setErrorText(title, errorText);
        } else {
            clearErrorText();
        }
    }

    public void clearErrorText() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (errorPanel.isVisible()) {
                    errorPanel.setVisible(false);
                    updateSize();
                }
            }
        });
    }

    private void setErrorText(String title, String text) {
        final Icon icon = errorIcon;
        setErrorText(icon, title, text);
    }

    private void setErrorText(final Icon icon, final String title,
            final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                errorPanelBorder.setTitle(title);
                errorIconLabel.setIcon(icon);
                errorText.setText(text);
                errorText.setCaretPosition(0);
                HTMLDocument doc = (HTMLDocument) errorText.getDocument();
                doc.setCharacterAttributes(0, doc.getLength(),
                        defaultErrorStyles, false);
                errorPanel.setVisible(true);
                updateSize();
            }
        });
    }

    private void setInfoText(String title, String text) {
        setErrorText(infoIcon, title, text);
    }

    private void updatePassword() {
        Screenname sn = new Screenname(getScreenname());
        LocalPreferencesManager prefs = appSession.getLocalPrefsIfExist(sn);
        String toSet = "";
        boolean remember = false;
        if (prefs != null) {
            String pass = prefs.getGeneralPrefs().getSavedPassword();
            toSet = pass == null ? "" : pass;
            remember = (pass != null);
        }

        passwordBox.setText(toSet);
        rememberPassBox.setSelected(remember);
    }

    private void reloadScreennames() {
        GlobalPrefs globalPrefs = appSession.getGlobalPrefs();
        boolean reloaded = globalPrefs.reloadIfNecessary();
        if (!reloaded && loadedScreennames) return;
        loadedScreennames = true;
        final Screenname[] known = appSession.getKnownScreennames();
        Arrays.sort(known);
        final String[] sns = new String[known.length];
        for (int i = 0; i < known.length; i++) {
            sns[i] = known[i].getFormatted();
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Object sel = knownScreennamesModel.getSelectedItem();
                if (sel == null) {
                    sel = "";
                    knownScreennamesModel.setSelectedItem(sel);
                }
                knownScreennamesModel.removeAllElements();
                for (int i = 0; i < sns.length; i++) {
                    knownScreennamesModel.addElement(sns[i]);
                }
                knownScreennamesModel.addElement(ITEM_SEPARATOR);
                knownScreennamesModel.addElement(ITEM_DELETE);
            }
        });
    }

    private void deleteScreenname(Object item) {
        if (item == null) return;

        String snText = item.toString().trim();
        if (OscarTools.normalize(snText).length() == 0) return;

        Screenname sn = new Screenname(snText);
        LocalPreferencesManager prefs = appSession.getLocalPrefsIfExist(sn);
        if (prefs == null) {
            setErrorText("Could not delete preferences",
                    "<B>Account preferences for " + snText.trim()
                    + " could not be deleted.</b><br><br>No preferences are "
                    + "stored for this screen name.");

        } else {
            String deleteOption = "Delete";
            String cancelOption = "Cancel";
            String[] options = new String[] { cancelOption, deleteOption };
            int selected = JOptionPane.showOptionDialog(this,
                    "<HTML><B>Delete all account preferences stored for<BR>"
                    + snText + "?</B><BR><BR>This "
                    + "will delete all preferences stored for this<BR>"
                    + "screen name, as well as all Personal Certificates<BR>"
                    + "and trusted certificates.",
                    "Delete Account Preferences",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, options, cancelOption);

            if (selected == JOptionPane.CLOSED_OPTION) return;

            if (options[selected] == deleteOption) {
                if (!appSession.deleteLocalPrefs(sn)) {
                    setErrorText("Could not delete preferences",
                            "<B>Some account preferences for " + snText
                            + " could not be deleted.</b><br><br>Check that "
                            + "you have permission to modify your "
                            + "preferences folder and try again.");
                } else {
                    reloadScreennames();
                    setInfoText("Deleted preferences",
                            "Account preferences for " + snText
                            + " were deleted successfully.");
                }
            }
        }
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        updateButtons();
    }

    private class SignonAction extends AbstractAction {
        public SignonAction() {
            super("Sign On");

            putValue(SHORT_DESCRIPTION, "Sign onto AIM");
            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
        }

        public void actionPerformed(ActionEvent e) {
            final Screenname sn = new Screenname(getScreenname());
            final LocalPreferencesManager prefs = appSession.getLocalPrefs(sn);
            if (prefs == null) return;

            setDisabled(true);
            repaint();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    String pass = new String(passwordBox.getPassword());
                    GeneralLocalPrefs generalPrefs = prefs.getGeneralPrefs();
                    if (rememberPassBox.isSelected()) {
                        generalPrefs.setSavedPassword(pass);
                    } else {
                        generalPrefs.setSavedPassword(null);
                    }
                    guiSession.signon(sn, pass);
                }
            });
        }
    }

    private class ShowPrefsAction extends AbstractAction {
        public ShowPrefsAction() {
            super("Account Preferences");

            putValue(SHORT_DESCRIPTION, "Edit account preferences for this "
                    + "screenname before signing on");
            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
        }

        public void actionPerformed(ActionEvent e) {
            guiSession.openPrefsWindow(new Screenname(getScreenname()));
        }
    }

    private class CloseAction extends AbstractAction {
        public CloseAction() {
            super("Close");

            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
            putValue(SHORT_DESCRIPTION, "Close this window");
        }

        public void actionPerformed(ActionEvent e) {
            guiSession.close();
        }
    }

    private class InputFieldChangeListener implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
            changed();
        }

        public void insertUpdate(DocumentEvent e) {
            changed();
        }

        public void removeUpdate(DocumentEvent e) {
            changed();
        }

        protected void changed() {
            updateButtons();
        }
    }

    private class WellBehavedComboBoxModel extends DefaultComboBoxModel {
        private Object selected = null;
        private int ignore = 0;

        public void setSelectedItem(Object anObject) {
            if (ignore > 0) return;

            if (anObject == ITEM_DELETE) {
                final Object item = getSelectedItem();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        deleteScreenname(item);
                    }
                });
            } else if (anObject == ITEM_SEPARATOR) {
                // do nothing
            } else {
                selected = anObject;
                super.setSelectedItem(anObject);
            }
        }

        public Object getSelectedItem() { return selected; }

        public void removeAllElements() {
            try {
                ignore++;
                super.removeAllElements();
            } finally {
                ignore--;
            }
        }

        // implements javax.swing.MutableComboBoxModel
        public void removeElementAt(int index) {
            try {
                ignore++;
                super.removeElementAt(index);
            } finally {
                ignore--;
            }
        }

        // implements javax.swing.MutableComboBoxModel
        public void addElement(Object anObject) {
            try {
                ignore++;
                super.addElement(anObject);
            } finally {
                ignore--;
            }
        }

        // implements javax.swing.MutableComboBoxModel
        public void removeElement(Object anObject) {
            try {
                ignore++;
                super.removeElement(anObject);
            } finally {
                ignore--;
            }
        }

        // implements javax.swing.MutableComboBoxModel
        public void insertElementAt(Object anObject, int index) {
            try {
                ignore++;
                super.insertElementAt(anObject, index);
            } finally {
                ignore--;
            }
        }
    }
}
