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
 *  File created by keith @ Jan 18, 2004
 *
 */

package net.kano.aimcrypto.forms;

import net.kano.aimcrypto.GuiSession;
import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.connection.AimConnection;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DummyOnlineWindow extends JFrame {
    private JPanel mainPanel;
    private JButton disconnectButton;
    private JTextField snBox;
    private JButton openButton;
    private JLabel onlineLabel;

    private final GuiSession guiSession;
    private AimConnection conn = null;
    private OpenImAction openAction = new OpenImAction();

    {
        getContentPane().add(mainPanel);
        openButton.setAction(openAction);
        disconnectButton.setAction(new DisconnectAction());
        snBox.getDocument().addDocumentListener(new DocumentListener() {
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
        });
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                conn.disconnect();
            }
        });

    }

    public DummyOnlineWindow(GuiSession session) {
        this.guiSession = session;
        updateSession();
    }

    public void updateSession() {
        this.conn = guiSession.getAimConnection();

        String sn = conn.getScreenname().getFormatted();
        setTitle("Buddy List");
        onlineLabel.setText("You are online as " + sn);
        snBox.setText("");
        updateButtons();
    }

    private void updateButtons() {
        openAction.setEnabled(snBox.getDocument().getLength() != 0);
    }

    private class OpenImAction extends AbstractAction {
        public OpenImAction() {
            super("IM");
        }

        public void actionPerformed(ActionEvent e) {
            Screenname sn = new Screenname(snBox.getText());
            snBox.setText("");
            guiSession.openImBox(sn);
        }
    }

    private class DisconnectAction extends AbstractAction {
        public DisconnectAction() {
            super("Disconnect");
        }

        public void actionPerformed(ActionEvent e) {
            guiSession.disconnect();
        }
    }
}
