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

package net.kano.aimcrypto.forms;

import net.kano.aimcrypto.AppSession;
import net.kano.aimcrypto.GuiResources;
import net.kano.aimcrypto.GuiSession;
import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.config.GeneralLocalPrefs;
import net.kano.aimcrypto.config.GlobalPrefs;
import net.kano.aimcrypto.config.LocalPreferencesManager;
import net.kano.aimcrypto.connection.ConnectionFailedStateInfo;
import net.kano.aimcrypto.connection.LoginFailureStateInfo;
import net.kano.aimcrypto.connection.StateInfo;
import net.kano.joscar.OscarTools;

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
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
    private final DefaultComboBoxModel knownScreennamesModel
            = new DefaultComboBoxModel();

    private Object previousSelectedItem = null;
    private final ComboBoxEditor screennameBoxEditor = screennameBox.getEditor();
    private final AppSession appSession;
    private int maxWidth = -1;
    private final AttributeSet defaultErrorStyles;
    private final Icon errorIcon = GuiResources.getErrorIcon();
    private final Icon infoIcon = GuiResources.getInformationIcon();

    private final TitledBorder errorPanelBorder
            = new TitledBorder((String) null);
    private boolean loadedScreennames = false;

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
                private String lastNormal = null;

                protected void changed() {
                    String newnormal = OscarTools.normalize(getScreenname());
                    if (!newnormal.equals(lastNormal)) {
                        updatePassword();
                        lastNormal = newnormal;
                    }
                }
            });
        }
        screennameBox.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent e) {
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                previousSelectedItem = screennameBoxEditor.getItem();
            }
        });
        screennameBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object item = e.getItem();
                    if (item == ITEM_SEPARATOR) {
                        screennameBox.setSelectedItem(previousSelectedItem);
                    } else if (item == ITEM_DELETE) {
                        screennameBox.setSelectedItem(previousSelectedItem);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                deleteScreenname(previousSelectedItem);
                            }
                        });
                    } else {
                        clearErrorText();
                        String current = getScreenname();
                        Screenname sn = new Screenname(current);
                        LocalPreferencesManager prefs
                                = appSession.getLocalPrefsIfExist(sn);
                        if (prefs != null) {
                            GeneralLocalPrefs genPrefs = prefs.getGeneralPrefs();
                            String format = genPrefs.getScreennameFormat();
                            if (format != null && !format.equals(current)) {
                                screennameBox.setSelectedItem(format);
                            }
                        }
                    }
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

    public Component getSignonWindowBoxComponent() {
        return this;
    }

    public void signonWindowBoxShown() {
        setDisabled(false);
        getRootPane().setDefaultButton(signonButton);
        reloadScreennames();
        updateButtons();
        updateSize();
    }

    private void updateSize() {
        Dimension pref = getPreferredSize();
        if (maxWidth == -1) {
            maxWidth = pref.width;
            setSize(pref);
        } else {
            setSize(new Dimension(maxWidth, pref.height));
        }
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
        System.out.println("failure info: " + sinfo);

        String errorText = null;
        if (sinfo != null) {
            final String msg;
            if (sinfo instanceof LoginFailureStateInfo) {
                LoginFailureStateInfo lfsi
                        = (LoginFailureStateInfo) sinfo;
                msg = lfsi.getLoginFailureInfo().toString();

            } else if (sinfo instanceof ConnectionFailedStateInfo) {
                ConnectionFailedStateInfo cfsi
                        = (ConnectionFailedStateInfo) sinfo;
                msg = "couldn't connect to " + cfsi.getHost() + ":"
                        + cfsi.getPort();

            } else {
                System.err.println("unknown error: "
                        + sinfo.getClass().getName());
                return;
            }
            if (msg != null) errorText = "<b>Could not sign on:</b> " + msg;
        }
        if (errorText != null) {
            setErrorText("Sign-on failed", errorText);
        } else {
            clearErrorText();
        }
    }

    private void clearErrorText() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                errorPanel.setVisible(false);
                updateSize();
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
                knownScreennamesModel.setSelectedItem(sel);
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
            String[] options = new String[] { deleteOption, cancelOption };
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
                    setInfoText("Deleted preferences",
                            "Account preferences for " + snText
                            + " were deleted successfully.");
                    reloadScreennames();
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
            final long start = System.currentTimeMillis();
            final Screenname sn = new Screenname(getScreenname());
            final LocalPreferencesManager prefs = appSession.getLocalPrefs(sn);
            if (prefs == null) return;

            setDisabled(true);
            repaint();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    long start2 = System.currentTimeMillis();
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
}
