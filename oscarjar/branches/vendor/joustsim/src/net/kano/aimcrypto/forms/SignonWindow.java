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

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SignonWindow extends JFrame {
    private JPanel mainPanel;
    private JLabel screennameLabel;
    private JLabel passwordLabel;
    private JTextField screennameBox;
    private JPasswordField passwordBox;
    private JButton signonButton;
    private JButton optionsButton;
    private JButton clearButton;

    private SignonAction signonAction = new SignonAction();
    private ClearAction clearAction = new ClearAction();
    private ShowOptionsAction showOptionsAction = new ShowOptionsAction();

    private GuiSession guiSession;

    private boolean disabled = false;
    private JLabel signonFailedLabel;

    {
        getContentPane().add(mainPanel);
        screennameBox.getDocument().addDocumentListener(new InputFieldListener());
        passwordBox.getDocument().addDocumentListener(new InputFieldListener());

        signonButton.setAction(signonAction);
        optionsButton.setAction(showOptionsAction);
        clearButton.setAction(clearAction);

        screennameBox.addFocusListener(new OnFocusSelector(screennameBox));
        passwordBox.addFocusListener(new OnFocusSelector(passwordBox));

        updateButtons();

        addWindowStateListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                setEnabled(true);
                updateButtons();
            }
        });
    }

    public SignonWindow(GuiSession session) {
        super("Sign On - AimCrypto");

        this.guiSession = session;
    }

    private void updateButtons() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                screennameLabel.setEnabled(!disabled);
                screennameBox.setEnabled(!disabled);
                passwordLabel.setEnabled(!disabled);
                passwordBox.setEnabled(!disabled);

                boolean snempty = screennameBox.getDocument().getLength() == 0;
                boolean passempty = passwordBox.getDocument().getLength() == 0;
                signonAction.setEnabled(!disabled && !snempty && !passempty);
                clearAction.setEnabled(!disabled && !snempty || !passempty);
                showOptionsAction.setEnabled(!disabled && !snempty);
            }
        });
    }

    public String getScreenname() { return screennameBox.getText(); }

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

    private class SignonAction extends AbstractAction {
        public SignonAction() {
            super("Sign On");

            putValue(SHORT_DESCRIPTION, "Sign onto AIM");
        }

        public void actionPerformed(ActionEvent e) {
            setEnabled(false);
            guiSession.signon(new Screenname(screennameBox.getText()),
                    new String(passwordBox.getPassword()));
        }
    }

    private class ShowOptionsAction extends AbstractAction {
        public ShowOptionsAction() {
            super("Options");

            putValue(SHORT_DESCRIPTION, "Edit options before signing on");
        }

        public void actionPerformed(ActionEvent e) {
            guiSession.showOptionsWindow();
        }
    }

    private class ClearAction extends AbstractAction {
        public ClearAction() {
            super("Clear");

            putValue(SHORT_DESCRIPTION,
                    "Clear the screenname and password boxes");
        }

        public void actionPerformed(ActionEvent e) {
            screennameBox.setText("");
            passwordBox.setText("");
        }
    }

    private class InputFieldListener implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
            changed();
        }

        public void insertUpdate(DocumentEvent e) {
            changed();
        }

        public void removeUpdate(DocumentEvent e) {
            changed();
        }

        private void changed() {
            updateButtons();
        }
    }
}
