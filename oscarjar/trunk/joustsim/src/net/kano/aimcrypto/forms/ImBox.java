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
import net.kano.aimcrypto.connection.oscar.service.icbm.Conversation;
import net.kano.aimcrypto.connection.oscar.service.icbm.ConversationListener;
import net.kano.aimcrypto.connection.oscar.service.icbm.ConversationNotOpenException;
import net.kano.aimcrypto.connection.oscar.service.icbm.ImConversation;
import net.kano.aimcrypto.connection.oscar.service.icbm.MessageInfo;
import net.kano.aimcrypto.connection.oscar.service.icbm.SimpleMessage;
import net.kano.aimcrypto.connection.oscar.service.icbm.Message;
import net.kano.aimcrypto.connection.oscar.service.icbm.Conversation;
import net.kano.aimcrypto.connection.oscar.service.icbm.ConversationNotOpenException;
import net.kano.aimcrypto.connection.oscar.service.icbm.Message;
import net.kano.aimcrypto.connection.oscar.service.icbm.SecureAimConversation;
import net.kano.aimcrypto.conv.ConversationDocument;
import net.kano.aimcrypto.conv.ConversationEditorKit;
import net.kano.aimcrypto.conv.AolRtfText;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.MiscTools;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

public class ImBox extends JFrame {
    private JSplitPane splitPane;
    private JScrollPane textScrollPane;
    private JScrollPane inputScrollPane;
    private JTextPane inputBox;
    private JTextPane convBox;
    private JComboBox typeBox;
    private JButton sendButton;

    private final ConversationEditorKit inputKit = new ConversationEditorKit();
    private final ConversationEditorKit convKit = new ConversationEditorKit();
    private final ConversationDocument inputDoc;
    private final ConversationDocument convDoc;

    private DefaultComboBoxModel typesModel = new DefaultComboBoxModel();

    private final GuiSession guiSession;
    private final Screenname buddy;

    private Set convs = new HashSet();
    private SendAction sendAction = new SendAction();

    {
        getContentPane().add(splitPane);

        inputBox.setEditorKit(inputKit);
        inputDoc = (ConversationDocument) inputBox.getDocument();
        convBox.setEditorKit(convKit);
        convDoc = (ConversationDocument) convBox.getDocument();

        sendButton.setAction(sendAction);
        splitPane.setBorder(null);

        typeBox.setModel(typesModel);
        typeBox.setRenderer(new TypesBoxRenderer());
    }

    public ImBox(GuiSession guiSession, Screenname buddy) {
        DefensiveTools.checkNull(guiSession, "guiSession");
        DefensiveTools.checkNull(buddy, "buddy");

        setTitle(buddy.getFormatted());
        this.guiSession = guiSession;
        this.buddy = buddy;
    }

    public void handleConversation(Conversation conversation) {
        if (convs.contains(conversation)) return;

        conversation.addConversationListener(new ConversationListener() {
            public void conversationOpened(Conversation c) {
                updateButtons();
            }

            public void gotMessage(Conversation c, MessageInfo minfo) {
                Message message = minfo.getMessage();
                String body = message.getMessageBody();
                System.out.println("got message: " + minfo + " - " + message + " - " + body);
                convDoc.addLine(AolRtfText.readLine(body));
            }

            public void sentMessage(Conversation c, MessageInfo minfo) {
                Message msg = minfo.getMessage();
                convDoc.addLine(AolRtfText.readLine(msg.getMessageBody()));
            }

            public void conversationClosed(Conversation c) {
                updateButtons();
            }
        });
        convs.add(conversation);
        typesModel.addElement(new ConversationInfo(conversation));
        updateButtons();
    }

    private void updateButtons() {
        Conversation conversation = getCurrentConversation();
        final boolean enabled = conversation != null && conversation.isOpen();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                sendAction.setEnabled(enabled);
            }
        });
    }

    public Conversation getCurrentConversation() {
        return ((ConversationInfo) typesModel.getSelectedItem()).getConversation();
    }

    private class SendAction extends AbstractAction {
        private SendAction() {
            super("Send");
        }

        public void actionPerformed(ActionEvent e) {
            Conversation conversation = getCurrentConversation();
            try {
                conversation.sendMessage(new SimpleMessage(inputBox.getText()));
                inputBox.setText("");
            } catch (ConversationNotOpenException ex) {
                //TODO: print error message
            }
        }
    }

    private static class ConversationInfo {
        private final Conversation conv;

        public ConversationInfo(Conversation conv) {
            this.conv = conv;
        }

        public Conversation getConversation() {
            return conv;
        }
    }

    private static class TypesBoxRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            ConversationInfo info = (ConversationInfo) value;
            Conversation conv = info.getConversation();
            String string = "(Unknown)";
            if (conv instanceof ImConversation) {
                string = "Insecure";
            } else if (conv instanceof SecureAimConversation) {
                string = "Secure";
            }
            JLabel comp = (JLabel) super.getListCellRendererComponent(list,
                    string, index, isSelected, cellHasFocus);
            comp.setForeground(conv.isOpen() ? null : Color.LIGHT_GRAY);
            return comp;
        }
    }
}
