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

import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.app.GuiResources;
import net.kano.joustsim.app.GuiSession;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.BuddyInfoManager;
import net.kano.joustsim.oscar.oscar.service.icbm.Conversation;
import net.kano.joustsim.oscar.oscar.service.icbm.ConversationAdapter;
import net.kano.joustsim.oscar.oscar.service.icbm.ConversationException;
import net.kano.joustsim.oscar.oscar.service.icbm.ConversationListener;
import net.kano.joustsim.oscar.oscar.service.icbm.secureim.DecryptableAimMessageInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.secureim.DecryptedAimMessageInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ImConversation;
import net.kano.joustsim.oscar.oscar.service.icbm.Message;
import net.kano.joustsim.oscar.oscar.service.icbm.MessageInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.secureim.MessageWithCertificateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.secureim.OutgoingSecureAimMessageInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.secureim.SecureAimConversation;
import net.kano.joustsim.oscar.oscar.service.icbm.SimpleMessage;
import net.kano.joustsim.oscar.oscar.service.info.BuddyTrustAdapter;
import net.kano.joustsim.oscar.oscar.service.info.BuddyTrustEvent;
import net.kano.joustsim.oscar.oscar.service.info.BuddyTrustManager;
import net.kano.joustsim.oscar.oscar.service.info.CertificateInfoTrustListener;
import net.kano.joustsim.oscar.oscar.service.info.CertificateInfoTrustManager;
import net.kano.joustsim.text.AolRtfString;
import net.kano.joustsim.text.aolrtfbox.AolRtfDocument;
import net.kano.joustsim.text.aolrtfbox.AolRtfEditorKit;
import net.kano.joustsim.text.convbox.ConversationBox;
import net.kano.joustsim.text.convbox.ConversationDocument;
import net.kano.joustsim.text.convbox.ConversationLine;
import net.kano.joustsim.text.convbox.IconID;
import net.kano.joustsim.trust.BuddyCertificateInfo;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImBox extends JFrame {
    private JSplitPane splitPane;
    private JTextPane inputBox;
    private JComboBox convTypeBox;
    private JButton sendButton;
    private JToolBar formatToolbar;
    private JPanel miniDialogStackHolder;
    private JPanel convBoxHolder;

    private final ImageIcon boldIcon = new ImageIcon(getClass().getClassLoader()
            .getResource("icons/temp/bold.gif"));
    private final ImageIcon italicIcon = new ImageIcon(getClass().getClassLoader()
            .getResource("icons/temp/italic.gif"));
    private final ImageIcon underlineIcon = new ImageIcon(getClass().getClassLoader()
            .getResource("icons/temp/underline.gif"));

    private ConversationBox convBox = new ConversationBox();

    private DefaultComboBoxModel typesModel = new DefaultComboBoxModel();

    private final GuiSession guiSession;
    private final AimConnection conn;
    private final Screenname buddy;

    private Set convs = new HashSet();
    private SendAction sendAction = new SendAction();

    private Set alreadyAskedCerts = new HashSet();
    private Map certInfoIds = new HashMap();

    private final ConversationListener conversationListener
            = new ConversationAdapter() {
        public void conversationOpened(Conversation c) {
            BuddyInfoManager bim = conn.getBuddyInfoManager();
            BuddyCertificateInfo certs = bim.getBuddyInfo(buddy).getCertificateInfo();
            if (certs != null) askAboutCertificates(certs);

            updateButtons();
        }

        public void gotMessage(Conversation c, final MessageInfo minfo) {
            if (minfo instanceof DecryptableAimMessageInfo) {
                DecryptableAimMessageInfo dinfo = (DecryptableAimMessageInfo) minfo;
                BuddyCertificateInfo certInfo = dinfo.getMessageCertificateInfo();

                askAboutCertificates(certInfo);
            }
            Message message = minfo.getMessage();
            String body = message.getMessageBody();
            if (body != null) {
                final AolRtfString parsed = AolRtfString.readLine(body);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        convDoc.addConversationLine(new ConversationLine(
                                minfo.getFrom(), parsed,
                                minfo.getTimestamp(), getIconIdsForMessage(minfo)));
                    }
                });
            }
        }

        public void sentMessage(final Conversation c, final MessageInfo minfo) {
            Message msg = minfo.getMessage();
            final AolRtfString parsed = AolRtfString.readLine(msg.getMessageBody());
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    convDoc.addConversationLine(new ConversationLine(
                            minfo.getFrom(), parsed, minfo.getTimestamp(),
                            getIconIdsForOutgoingMessage(minfo)));
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

    private final Icon[] secureMessageIcons = new Icon[] {
        GuiResources.getTinyGrayLockIcon(),
    };
    private final Icon[] insecureMessageIcons = new Icon[] {
        GuiResources.getTinyGrayInsecureLockIcon(),
    };
    private final CertificateInfoTrustManager certInfoTrustManager;
    private final BuddyTrustManager buddyTrustManager;

    private IconID outgoingSecureIconID = new IconID();
    private IconID insecureIconID = new IconID();
    private boolean validBox = true;

    private final MiniDialogStack miniDialogStack = new MiniDialogStack();

    private Conversation currentConversation = null;
    private ConversationDocument convDoc = convBox.getDocument();

    private final Action boldAction = new BoldAction();
    private final Action italicAction = new ItalicAction();
    private final Action underlineAction = new UnderlineAction();
    private final Action undoAction = new UndoAction();
    private final Action redoAction = new RedoAction();
    private final Action pageDownAction = new PageDownAction();
    private final Action pageUpAction = new PageUpAction();

    private final JToggleButton boldButton;
    private final JToggleButton italicButton;
    private final JToggleButton underlineButton;

    private final AolRtfDocument inputDocument;
    private final UndoManager inputUndoManager = new UndoManager();

    {
        getContentPane().add(splitPane);
        splitPane.setResizeWeight(1.0);

        convBoxHolder.setLayout(new BorderLayout());
        convBoxHolder.add(convBox);

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


        sendButton.setAction(sendAction);
        splitPane.setBorder(null);

        convTypeBox.setModel(typesModel);
        convTypeBox.setRenderer(new TypesBoxRenderer());

        formatToolbar.setBorderPainted(false);
        boldButton = addToolbarAction(boldAction);
        italicButton = addToolbarAction(italicAction);
        underlineButton = addToolbarAction(underlineAction);

        inputBox.setEditorKit(new AolRtfEditorKit());
        inputDocument = (AolRtfDocument) inputBox.getDocument();

        InputMap inputmap = inputBox.getInputMap();
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B,
                KeyEvent.CTRL_MASK), "bold");
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I,
                KeyEvent.CTRL_MASK), "italic");
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U,
                KeyEvent.CTRL_MASK), "underline");
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                KeyEvent.CTRL_MASK), "undo");
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK), "redo");
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                0), "send");
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP,
                0), "pageUp");
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN,
                0), "pageDown");
        ActionMap actionMap = inputBox.getActionMap();
        actionMap.put("bold", boldAction);
        actionMap.put("italic", italicAction);
        actionMap.put("underline", underlineAction);
        actionMap.put("undo", undoAction);
        actionMap.put("redo", redoAction);
        actionMap.put("send", sendAction);
        actionMap.put("pageUp", pageUpAction);
        actionMap.put("pageDown", pageDownAction);

        inputDocument.addUndoableEditListener(inputUndoManager);

        inputBox.addCaretListener(new CaretListener() {
            public void caretUpdate(CaretEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updateToolbarButtons();
                    }
                });
            }
        });

        convTypeBox.addItemListener(new ItemListener() {
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
            public void windowOpened(WindowEvent e) {
                inputBox.requestFocusInWindow();
            }

            public void windowClosed(WindowEvent e) {
                closeAllConversations();
            }
        });

        convDoc.setIconsForID(outgoingSecureIconID, secureMessageIcons);

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
        buddyTrustManager = conn.getBuddyTrustManager();
        buddyTrustManager.addBuddyTrustListener(new BuddyTrustAdapter() {
            public void gotUntrustedCertificateChange(BuddyTrustEvent event) {
                if (!event.isFor(ImBox.this.buddy)) return;
                askAboutCertificates(event.getCertInfo());
            }

            public void gotUnknownCertificateChange(BuddyTrustEvent event) {
                if (!event.isFor(ImBox.this.buddy)) return;
                askAboutCertificates(event.getCertInfo());
            }
        });
        certInfoTrustManager = conn.getCertificateInfoTrustManager();
        certInfoTrustManager.addTrustListener(new CertificateInfoTrustListener() {
            public void certificateInfoTrusted(CertificateInfoTrustManager manager,
                    BuddyCertificateInfo certInfo) {
                if (!certInfo.getBuddy().equals(ImBox.this.buddy)) return;

                updateIcons(certInfo);
            }

            public void certificateInfoNoLongerTrusted(
                    CertificateInfoTrustManager manager, BuddyCertificateInfo certInfo) {
                if (!certInfo.getBuddy().equals(ImBox.this.buddy)) return;

                updateIcons(certInfo);
            }
        });

        ConversationDocument convDoc = convBox.getDocument();
        convDoc.registerScreenname(conn.getScreenname(), true);
        convDoc.registerScreenname(this.buddy, false);
        convBox.requestFocusInWindow();
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

    public synchronized void setInvalidBox() {
        if (!validBox) return;

        validBox = false;
        //TODO: "this window is no longer valid" won't be true with directim
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                convDoc.addInfoMessage(AolRtfString.readLine("This window is no longer "
                        + "active because your connection to AIM was closed."));
                Color gray = UIManager.getColor("Panel.background");
                convBox.getTextPane().setBackground(gray);
                inputBox.setBackground(gray);
                convTypeBox.setEnabled(false);
                inputBox.setEnabled(false);
                formatToolbar.setEnabled(false);
                Component[] buttons = formatToolbar.getComponents();
                for (int i = 0; i < buttons.length; i++) {
                    Component button = buttons[i];
                    button.setEnabled(false);
                }
            }
        });
        updateButtons();
    }

    private IconID[] getIconIdsForMessage(MessageInfo minfo) {
        List ids = new ArrayList(5);
        if (minfo instanceof DecryptableAimMessageInfo
                || minfo instanceof DecryptedAimMessageInfo) {
            MessageWithCertificateInfo info = (MessageWithCertificateInfo) minfo;

            BuddyCertificateInfo certInfo = info.getMessageCertificateInfo();
            updateIcons(certInfo);
            IconID icon = getIconIdForCertInfo(certInfo);
            if (icon != null) {
                ids.add(icon);
                markInsecureMessages();
            }
        } else {
            ids.add(insecureIconID);
        }
        return (IconID[]) ids.toArray(new IconID[ids.size()]);
    }

    public synchronized boolean isValidBox() { return validBox; }

    private IconID[] getIconIdsForOutgoingMessage(MessageInfo minfo) {
        List ids = new ArrayList(5);
        if (minfo instanceof OutgoingSecureAimMessageInfo) {
            ids.add(outgoingSecureIconID);
            markInsecureMessages();
        } else {
            ids.add(insecureIconID);
        }
        return (IconID[]) ids.toArray(new IconID[ids.size()]);
    }

    private void markInsecureMessages() {
        //TODO: mark partially secure icon id's too
        convDoc.setIconsForID(insecureIconID, insecureMessageIcons);
    }

    private synchronized IconID getIconIdForCertInfo(BuddyCertificateInfo certInfo) {
        if (certInfo == null) return null;

        ByteBlock hash = certInfo.getCertificateInfoHash();
        IconID id = (IconID) certInfoIds.get(hash);
        if (id == null) {
            id = new IconID();
            certInfoIds.put(hash, id);
        }
        return id;
    }

    private void updateIcons(BuddyCertificateInfo certInfo) {
        System.out.println("- updating icons for " + certInfo.getBuddy()
                + " (" + certInfo.isUpToDate() + ")");
        IconID id = getIconIdForCertInfo(certInfo);
        if (id != null) {
            Icon[] icons;
            if (certInfoTrustManager.isTrusted(certInfo)) {
                System.out.println("- info is trusted");
                icons = secureMessageIcons;
            } else {
                System.out.println("- info is not trusted");
                icons = null;
            }
            convDoc.setIconsForID(id, icons);
        }
    }

    private JToggleButton addToolbarAction(Action action) {
        JToggleButton button = new JToggleButton(action);
        button.setText("");
        button.setBorder(null);
        button.setMargin(null);
        formatToolbar.add(button);
        return button;
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

        if (certInfoTrustManager.isTrusted(certInfo)) return;

        miniDialogStack.popup(new BuddyTrustMiniDialog(conn, buddy, certInfo));
    }

    private void updateButtons() {
        Conversation conversation = getCurrentConversation();
        final boolean enabled = validBox && conversation != null
                && conversation.isOpen() && conversation.canSendMessage();
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

    private void updateToolbarButtons() {
        MutableAttributeSet attr = inputBox.getInputAttributes();
        boldButton.setSelected(StyleConstants.isBold(attr));
        italicButton.setSelected(StyleConstants.isItalic(attr));
        underlineButton.setSelected(StyleConstants.isUnderline(attr));
    }

    private void beepInputBox() {
        UIManager.getLookAndFeel().provideErrorFeedback(inputBox);
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
            Icon icon = null;
            if (conv instanceof ImConversation) {
                string = "Insecure";
            } else if (conv instanceof SecureAimConversation) {
                icon = GuiResources.getTinyColoredLockIcon();
                string = "Secure";
            }
            super.getListCellRendererComponent(list,
                    string, index, isSelected, cellHasFocus);
            setForeground(conv.isOpen() ? null : Color.LIGHT_GRAY);
            setIcon(icon);
            return this;
        }
    }

    private class UndoAction extends AbstractAction {
        public UndoAction() {
            super("Undo");
        }

        public void actionPerformed(ActionEvent e) {
            try {
                inputUndoManager.undo();
            } catch (CannotUndoException er) {
                beepInputBox();
            }
        }
    }
    private class RedoAction extends AbstractAction {
        public RedoAction() {
            super("Redo");
        }

        public void actionPerformed(ActionEvent e) {
            try {
                inputUndoManager.redo();
            } catch (CannotRedoException er) {
                beepInputBox();
            }
        }
    }

    public abstract class PageAction extends AbstractAction {
        public PageAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            JScrollBar bar = convBox.getScrollPane().getVerticalScrollBar();
            BoundedRangeModel model = bar.getModel();
            int old = model.getValue();
            int dir = getDirection();
            int newval = dir * bar.getBlockIncrement(dir);
            model.setValue(old + newval);
        }

        protected abstract int getDirection();
    }

    public class PageUpAction extends PageAction {
        public PageUpAction() {
            super("Page Up");
        }

        protected int getDirection() { return -1; }
    }

    public class PageDownAction extends PageAction {
        public PageDownAction() {
            super("Page Down");
        }

        protected int getDirection() { return 1; }
    }

    public abstract class ToggleAction extends AbstractAction {
        protected ToggleAction() {
        }

        protected ToggleAction(String name) {
            super(name);
        }

        protected ToggleAction(String name, Icon icon) {
            super(name, icon);
        }

        public void actionPerformed(ActionEvent e) {
            AttributeSet old = inputBox.getCharacterAttributes();
            boolean newon = !isOn(old);
            MutableAttributeSet attr = new SimpleAttributeSet();
            setOn(attr, newon);
            inputBox.setCharacterAttributes(attr, false);
            inputBox.requestFocusInWindow();
        }

        protected abstract void setOn(MutableAttributeSet attr, boolean bold);

        protected abstract boolean isOn(AttributeSet old);
    }

    public class BoldAction extends ToggleAction {
        public BoldAction() {
            super("Bold", boldIcon);
        }

        protected void setOn(MutableAttributeSet attr, boolean bold) {
            StyleConstants.setBold(attr, bold);
        }

        protected boolean isOn(AttributeSet old) {
            return StyleConstants.isBold(old);
        }
    }
    public class ItalicAction extends ToggleAction {
        public ItalicAction() {
            super("Italic", italicIcon);
        }

        protected void setOn(MutableAttributeSet attr, boolean bold) {
            StyleConstants.setItalic(attr, bold);
        }

        protected boolean isOn(AttributeSet old) {
            return StyleConstants.isItalic(old);
        }
    }
    public class UnderlineAction extends ToggleAction {
        public UnderlineAction() {
            super("Underline", underlineIcon);
        }

        protected void setOn(MutableAttributeSet attr, boolean bold) {
            StyleConstants.setUnderline(attr, bold);
        }

        protected boolean isOn(AttributeSet old) {
            return StyleConstants.isUnderline(old);
        }
    }

    public class SendAction extends AbstractAction {
        private SendAction() {
            super("Send");
        }

        public void actionPerformed(ActionEvent e) {
            Conversation conversation = getCurrentConversation();
            try {
                conversation.sendMessage(new SimpleMessage(inputBox.getText()));
                inputBox.setText("");
//                inputUndoManager.discardAllEdits();
                inputBox.requestFocusInWindow();

            } catch (ConversationException ex) {
                //TODO: print error message when message can't be sent
                ex.printStackTrace();
            }
        }
    }
}
