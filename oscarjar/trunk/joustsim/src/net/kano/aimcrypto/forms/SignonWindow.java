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

import net.kano.aimcrypto.GuiSession;
import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.connection.ConnectionFailedStateInfo;
import net.kano.aimcrypto.connection.LoginFailureStateInfo;
import net.kano.aimcrypto.connection.StateInfo;
import net.kano.joscar.OscarTools;

import javax.swing.AbstractAction;
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Arrays;

public class SignonWindow extends JFrame {
    private static final Object ITEM_DELETE = new Object();
    private static final Object ITEM_SEPARATOR = new Object();

    private JPanel mainPanel;
    private JLabel screennameLabel;
    private JLabel passwordLabel;
    private JComboBox screennameBox;
    private JPasswordField passwordBox;
    private JButton signonButton;
    private JButton prefsButton;
    private JLabel signonFailedLabel;
    private JCheckBox rememberPassBox;
    private JButton closeButton;
//    private JButton clearButton;

    private final ImageIcon programIcon;
    {
        URL iconUrl = getClass().getClassLoader().getResource("icons/program-small.gif");
        programIcon = iconUrl == null ? null : new ImageIcon(iconUrl);
    }

    private SignonAction signonAction = new SignonAction();
    private ShowPrefsAction showPrefsAction = new ShowPrefsAction();
    private ClearAction clearAction = new ClearAction();
    private CloseAction closeAction = new CloseAction();

    private GuiSession guiSession;

    private boolean disabled = false;

    private JTextComponent snTextEditor = null;
    private final DefaultComboBoxModel knownScreennamesModel
            = new DefaultComboBoxModel();

    private Object previousSelectedItem = null;
    private final ComboBoxEditor screennameBoxEditor = screennameBox.getEditor();

    {
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
        screennameBoxEditor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("action");
                updateButtons();
                previousSelectedItem = screennameBox.getSelectedItem();
            }
        });
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

        getContentPane().add(mainPanel);
        passwordBox.getDocument().addDocumentListener(new InputFieldChangeListener());

        signonButton.setAction(signonAction);
        prefsButton.setAction(showPrefsAction);
        closeButton.setAction(closeAction);
//        clearButton.setAction(clearAction);

        passwordBox.addFocusListener(new OnFocusSelector());

        updateButtons();

        setResizable(false);

        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                setEnabled(true);
                reloadScreennames();
                updateButtons();
                pack();
                setSize(getPreferredSize());
            }
            public void windowClosing(WindowEvent e) {
                guiSession.close();
            }
        });
        addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
            }
        });
        ImageIcon icon = programIcon;
        Image image = null;
        if (icon != null) {
            image = icon.getImage();
        }
        setIconImage(image);
    }

    public SignonWindow(GuiSession session) {
        super("Sign On");

        this.guiSession = session;
    }

    private void updateButtons() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                boolean snempty = getScreenname().length() == 0;
                boolean passempty = passwordBox.getDocument().getLength() == 0;

                screennameLabel.setEnabled(!disabled);
                screennameBox.setEnabled(!disabled);
                passwordLabel.setEnabled(!disabled && !snempty);
                passwordBox.setEnabled(!disabled && !snempty);
                rememberPassBox.setEnabled(!disabled && !snempty && !passempty);

                signonAction.setEnabled(!disabled && !snempty && !passempty);
                clearAction.setEnabled(!disabled && !snempty || !passempty);
                showPrefsAction.setEnabled(!disabled && !snempty);
            }
        });
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
                msg = sinfo.toString();
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    signonFailedLabel.setText("<HTML><b>Could not sign on:</b> "
                            + msg);
                    signonFailedLabel.setVisible(true);
                }
            });
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    signonFailedLabel.setVisible(false);
                }
            });
        }
//        pack();
//        int width = getWidth();
//        setSize(width, getPreferredSize().height);
    }

    private void updatePassword() {
        passwordBox.setText("");
    }

    private void reloadScreennames() {
        final String[] sns = guiSession.getAppSession().getGlobalPrefs().getKnownScreennames();
        Arrays.sort(sns);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Object sel = knownScreennamesModel.getSelectedItem();
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

    }

    private class SignonAction extends AbstractAction {
        public SignonAction() {
            super("Sign On");

            putValue(SHORT_DESCRIPTION, "Sign onto AIM");
            putValue(ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
        }

        public void actionPerformed(ActionEvent e) {
            setEnabled(false);
            guiSession.signon(new Screenname(getScreenname()),
                    new String(passwordBox.getPassword()));
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
            guiSession.showPrefsWindow();
        }
    }

    private class ClearAction extends AbstractAction {
        public ClearAction() {
            super("Clear");

            putValue(SHORT_DESCRIPTION,
                    "Clear the screenname and password boxes");
            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
        }

        public void actionPerformed(ActionEvent e) {
            screennameBox.setSelectedItem("");
            passwordBox.setText("");
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
