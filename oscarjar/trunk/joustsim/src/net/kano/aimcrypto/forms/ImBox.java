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
import net.kano.aimcrypto.config.BuddyCertificateInfo;
import net.kano.aimcrypto.connection.AimConnection;
import net.kano.aimcrypto.connection.oscar.service.icbm.Conversation;
import net.kano.aimcrypto.connection.oscar.service.icbm.ConversationAdapter;
import net.kano.aimcrypto.connection.oscar.service.icbm.ConversationException;
import net.kano.aimcrypto.connection.oscar.service.icbm.ConversationListener;
import net.kano.aimcrypto.connection.oscar.service.icbm.DecryptableAimMessageInfo;
import net.kano.aimcrypto.connection.oscar.service.icbm.ImConversation;
import net.kano.aimcrypto.connection.oscar.service.icbm.Message;
import net.kano.aimcrypto.connection.oscar.service.icbm.MessageInfo;
import net.kano.aimcrypto.connection.oscar.service.icbm.SecureAimConversation;
import net.kano.aimcrypto.connection.oscar.service.icbm.SimpleMessage;
import net.kano.aimcrypto.connection.oscar.service.info.BuddyTrustAdapter;
import net.kano.aimcrypto.connection.oscar.service.info.BuddyTrustEvent;
import net.kano.aimcrypto.conv.AolRtfString;
import net.kano.aimcrypto.conv.ConversationDocument;
import net.kano.aimcrypto.conv.ConversationEditorKit;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.MiscTools;
import net.kano.joscar.ByteBlock;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ImBox extends JFrame {
    private JSplitPane splitPane;
    private JScrollPane textScrollPane;
    private JScrollPane inputScrollPane;
    private JTextPane inputBox;
    private JTextPane convBox;
    private JComboBox typeBox;
    private JButton sendButton;
    private JPanel inputSidePanel;
    private JToolBar formatToolbar;
    private JPanel miniDialogStackHolder;

    private final ConversationEditorKit inputKit = new ConversationEditorKit();
    private final ConversationEditorKit convKit = new ConversationEditorKit();
    private final ConversationDocument inputDoc;
    private final ConversationDocument convDoc;

    private DefaultComboBoxModel typesModel = new DefaultComboBoxModel();

    private final GuiSession guiSession;
    private final AimConnection conn;
    private final Screenname buddy;

    private Set convs = new HashSet();
    private SendAction sendAction = new SendAction();

    private Set alreadyAskedCerts = new HashSet();

    private final ConversationListener conversationListener
            = new ConversationAdapter() {
        public void conversationOpened(Conversation c) {
            updateButtons();
        }

        public void gotMessage(Conversation c, MessageInfo minfo) {
            if (minfo instanceof DecryptableAimMessageInfo) {
                DecryptableAimMessageInfo dinfo = (DecryptableAimMessageInfo) minfo;
                BuddyCertificateInfo certInfo = dinfo.getMessageCertificateInfo();
                System.out.println("info=" + certInfo);

                askAboutCertificates(certInfo);
            }
            Message message = minfo.getMessage();
            String body = message.getMessageBody();
            System.out.println("got message: " + MiscTools.getClassName(minfo)
                    + " - " + body);
            if (body != null) {
                final AolRtfString parsed = AolRtfString.readLine(body);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        convDoc.addLine(parsed);
                    }
                });
            }
        }

        public void sentMessage(Conversation c, MessageInfo minfo) {
            Message msg = minfo.getMessage();
            final AolRtfString parsed = AolRtfString.readLine(msg.getMessageBody());
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    convDoc.addLine(parsed);
                }
            });
        }

        public void conversationClosed(Conversation c) {
            updateButtons();
        }

        public void canSendMessageChanged(Conversation c, boolean canSend) {
            updateButtons();
        }
    };
    private final MiniDialogStack miniDialogStack = new MiniDialogStack();

    private Conversation currentConversation = null;

    {
        getContentPane().add(splitPane);
        splitPane.setResizeWeight(1.0);

        miniDialogStackHolder.setLayout(new BorderLayout());
        miniDialogStackHolder.add(miniDialogStack);
        miniDialogStack.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                System.out.println("mini dialog was shown...");
                if (!miniDialogStackHolder.isVisible()) {
                    System.out.println("- we're invisible! making visible..");
                    miniDialogStackHolder.setVisible(true);
                }
            }

            public void componentHidden(ComponentEvent e) {
                System.out.println("mini dialog is hidden..");
                if (miniDialogStackHolder.isVisible()) {
                    System.out.println("- we're visible! hiding..");
                    miniDialogStackHolder.setVisible(false);
                }
            }
        });

        textScrollPane.setBorder(null);

        inputBox.setEditorKit(inputKit);
        inputDoc = (ConversationDocument) inputBox.getDocument();
        inputKit.setDefaultCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        convBox.setEditorKit(convKit);
        convDoc = (ConversationDocument) convBox.getDocument();

        sendButton.setAction(sendAction);
        splitPane.setBorder(null);

        typeBox.setModel(typesModel);
        typeBox.setRenderer(new TypesBoxRenderer());

        formatToolbar.setBorderPainted(false);
        JButton boldButton = formatToolbar.add(new BoldAction());
        boldButton.setBorder(null);
        boldButton.setBorderPainted(false);
        boldButton.setMargin(null);
//        formatToolbar.add(new ItalicAction());
//        formatToolbar.add(new UnderlineAction());

        typeBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Object selectedObj = typesModel.getSelectedItem();
                if (!(selectedObj instanceof ConversationInfo)) return;

                ConversationInfo selected = (ConversationInfo) selectedObj;
                Conversation conv = selected.getConversation();
                setCurrentConversation(conv);
                conv.open();
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                closeAllConversations();
            }
        });

        setIconImage(new ImageIcon(getClass().getClassLoader().getResource(
                "icons/im-window-tiny.png")).getImage());
    }

    public ImBox(GuiSession guiSession, AimConnection conn, Screenname buddy) {
        DefensiveTools.checkNull(guiSession, "guiSession");
        DefensiveTools.checkNull(conn, "conn");
        DefensiveTools.checkNull(buddy, "buddy");

        setTitle(buddy.getFormatted());
        this.guiSession = guiSession;
        this.conn = conn;
        this.buddy = buddy;
        conn.getBuddyTrustManager().addBuddyTrustListener(new BuddyTrustAdapter() {
            public void gotUntrustedCertificateChange(BuddyTrustEvent event) {
                if (!event.isFor(ImBox.this.buddy)) return;
                askAboutCertificates(event.getCertInfo());
            }

            public void gotUnknownCertificateChange(BuddyTrustEvent event) {
                if (!event.isFor(ImBox.this.buddy)) return;
                askAboutCertificates(event.getCertInfo());
            }
        });
    }

    public void handleConversation(Conversation conversation) {
        if (convs.contains(conversation)) return;

        conversation.addConversationListener(conversationListener);

        convs.add(conversation);
        final ConversationInfo convInfo = new ConversationInfo(conversation);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                typesModel.addElement(convInfo);
                updateButtons();
            }
        });
    }

    private synchronized void closeAllConversations() {
        for (Iterator it = convs.iterator(); it.hasNext();) {
            Conversation conversation = (Conversation) it.next();
            conversation.close();
        }
    }

    private void askAboutCertificates(BuddyCertificateInfo certInfo) {
        ByteBlock hash = certInfo.getCertificateInfoHash();
        boolean wasnew = alreadyAskedCerts.add(hash);
        if (!wasnew) return;

        System.out.println("asking for certificates for " + buddy + ": " + certInfo);
        miniDialogStack.popup(new BuddyTrustMiniDialog(conn, buddy, certInfo));
    }

    private void updateButtons() {
        Conversation conversation = getCurrentConversation();
        System.out.println("updating buttons for " + MiscTools.getClassName(conversation));
        System.out.println((conversation != null) + "-"
                + (conversation != null && conversation.isOpen()) + "-"
                + (conversation != null && conversation.canSendMessage()));
        final boolean enabled = conversation != null && conversation.isOpen()
                && conversation.canSendMessage();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                sendAction.setEnabled(enabled);
            }
        });
    }

    public synchronized Conversation getCurrentConversation() {
        return currentConversation;
    }

    public synchronized void setCurrentConversation(Conversation conv) {
        this.currentConversation = conv;
        updateButtons();
    }

    public void setValidBox(boolean valid) {
        //TODO: disable things in the box
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
            super.getListCellRendererComponent(list,
                    string, index, isSelected, cellHasFocus);
            setForeground(conv.isOpen() ? null : Color.LIGHT_GRAY);
            return this;
        }
    }

    private class BoldAction extends AbstractAction {
        public BoldAction() {
            putValue(SMALL_ICON, new ImageIcon(getClass().getClassLoader()
                    .getResource("icons/temp/bold.gif")));
        }

        public void actionPerformed(ActionEvent e) {
            AttributeSet old = inputBox.getCharacterAttributes();
            boolean bold = !StyleConstants.isBold(old);
            MutableAttributeSet attr = new SimpleAttributeSet();
            StyleConstants.setBold(attr, bold);
            inputBox.setCharacterAttributes(attr, false);
        }
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
                //TODO: return focus to whomever had it

            } catch (ConversationException ex) {
                //TODO: print error message when message can't be sent
                ex.printStackTrace();
            }
        }
    }
}
