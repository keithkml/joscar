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
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.rvcmd.InvitationMessage;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.app.GuiSession;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.BuddyInfo;
import net.kano.joustsim.oscar.BuddyInfoManager;
import net.kano.joustsim.oscar.oscar.service.icbm.ImConversation;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransferManager;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.OutgoingFileTransfer;
import net.kano.joustsim.oscar.oscar.service.ssi.AddMutableGroup;
import net.kano.joustsim.oscar.oscar.service.ssi.Buddy;
import net.kano.joustsim.oscar.oscar.service.ssi.DeleteMutableGroup;
import net.kano.joustsim.oscar.oscar.service.ssi.Group;
import net.kano.joustsim.oscar.oscar.service.ssi.MutableBuddy;
import net.kano.joustsim.oscar.oscar.service.ssi.MutableBuddyList;
import net.kano.joustsim.oscar.oscar.service.ssi.RenameMutableGroup;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public class BuddyListBox extends JPanel {
    private JButton infoButton;
    private JButton chatButton;
    private JButton imButton;
    private JTree buddyTree;
    private JPanel mainPanel;
    private BuddyListModel model;
    private JPopupMenu menu = new JPopupMenu();
    private MutableBuddyList buddyList;
    private GuiSession guiSession;
    private JButton sendFileButton;
    private Map<ExtraInfoBlock, BufferedImage> imagesCache
            = new HashMap<ExtraInfoBlock, BufferedImage>();

    {
        setLayout(new BorderLayout());
        add(mainPanel);

        menu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                menu.removeAll();
                TreePath selectionPath = buddyTree.getSelectionPath();
                if (selectionPath == null) return;

                Object item = selectionPath.getLastPathComponent();
                if (item == model.getRoot()) {
                    menu.add(new AddGroupAction());

                } else if (item instanceof BuddyListModel.GroupHolder) {
                    BuddyListModel.GroupHolder holder = (BuddyListModel.GroupHolder) item;
                    Group group = holder.getGroup();
                    if (group instanceof AddMutableGroup) {
                        AddMutableGroup mutableGroup = (AddMutableGroup) group;
                        menu.add(new AddBuddyAction(mutableGroup));
                    }
                    menu.addSeparator();

                    menu.add(new AddGroupAction());
                    if (group instanceof RenameMutableGroup) {
                        final RenameMutableGroup mutableGroup = (RenameMutableGroup) group;

                        menu.add(new RenameGroupAction(mutableGroup));
                    }
                    menu.add(new RemoveGroupAction(group));


                } else if (item instanceof BuddyListModel.BuddyHolder) {
                    BuddyListModel.BuddyHolder holder = (BuddyListModel.BuddyHolder) item;
                    Group mgroup = null;
                    Buddy buddy = holder.getBuddy();
                    for (Group group : buddyList.getGroups()) {
                        if (group.getBuddiesCopy().contains(buddy)) {
                            if (group instanceof AddMutableGroup
                                    || group instanceof RenameMutableGroup
                                    || group instanceof DeleteMutableGroup) {
                                mgroup = group;
                                break;
                            }
                        }
                    }
                    if (mgroup != null) {
                        if (mgroup instanceof AddMutableGroup) {
                            AddMutableGroup addMutableGroup = (AddMutableGroup) mgroup;
                            menu.add(new AddBuddyAction(addMutableGroup));
                        }
                        if (mgroup instanceof DeleteMutableGroup) {
                            DeleteMutableGroup deleteMutableGroup = (DeleteMutableGroup) mgroup;

                            menu.add(new RemoveBuddyAction(deleteMutableGroup, buddy));
                        }
                        menu.addSeparator();
                        if (buddy instanceof MutableBuddy) {
                            MutableBuddy mutableBuddy = (MutableBuddy) buddy;
                            menu.add(new ChangeAliasAction(mutableBuddy));
                            menu.add(new ChangeCommentAction(mutableBuddy));
                        }
                    }
                }
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        buddyTree.setComponentPopupMenu(menu);
        buddyTree.setEditable(true);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                    boolean sel, boolean expanded, boolean leaf, int row,
                    boolean hasFocus) {
                assert super.getTreeCellRendererComponent(tree, value, sel, expanded,
                        leaf, row, hasFocus) == this;
                if (value instanceof BuddyListModel.BuddyHolder) {
                    BuddyListModel.BuddyHolder buddyHolder = (BuddyListModel.BuddyHolder) value;
                    AimConnection conn = guiSession.getAimConnection();
                    Screenname sn = buddyHolder.getBuddy().getScreenname();
                    BuddyInfoManager infoManager = conn.getBuddyInfoManager();
                    BuddyInfo buddyInfo = infoManager.getBuddyInfo(sn);
                    ExtraInfoBlock hash = buddyInfo.getIconHash();
                    BufferedImage img = imagesCache.get(hash);
                    if (img == null) {
                        ByteBlock iconData = buddyInfo.getIconData();
                        if (iconData != null) {

                            try {
                                img = ImageIO.read(
                                        ByteBlock.createInputStream(iconData));
                                imagesCache.put(hash, img);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (img != null) {
                        setIcon(new ImageIcon(img));
                    } else {
                        setIcon(null);
                    }
                }
                return this;
            }
        };
        buddyTree.setCellRenderer(renderer);
        buddyTree.setCellEditor(new DefaultTreeCellEditor(buddyTree, renderer) {
            public boolean isCellEditable(EventObject event) {
                TreePath path = buddyTree.getSelectionPath();
                if (path == null) return false;
                Object comp = path.getLastPathComponent();
                if (comp instanceof BuddyListModel.BuddyHolder) {
                    return true;
                } else {
                    return super.isCellEditable(event);
                }
            }
        });
        buddyTree.setInvokesStopCellEditing(true);
        imButton.setAction(new ImAction());
        sendFileButton.setAction(new AbstractAction() {
            {
                putValue(NAME, "Send File");
            }

            public void actionPerformed(ActionEvent e) {
                Screenname sn = getSelectedScreenname();
                if (sn != null) {
                    AimConnection conn = guiSession.getAimConnection();
                    FileTransferManager mgr = conn.getIcbmService()
                            .getFileTransferManager();
                    OutgoingFileTransfer transfer = mgr.createOutgoingFileTransfer(sn);
                    JFileChooser chooser = new JFileChooser();
                    chooser.setApproveButtonText("Send");
                    chooser.setDialogTitle("Send Files");
                    chooser.setMultiSelectionEnabled(true);
                    int result = chooser.showOpenDialog(BuddyListBox.this);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File[] files = chooser.getSelectedFiles();
                        if (files.length > 1) {
                            transfer.setFiles("Folder", Arrays.asList(files));
                        } else {
                            transfer.setFile(files[0]);
                        }
                        guiSession.getDummyOnlineWindow().watchTransfer(transfer);
                        transfer.makeRequest(new InvitationMessage("Here's a file!"));
                    }
                }
            }
        });
    }

    private Screenname getSelectedScreenname() {
        Screenname sn = null;
        TreePath path = buddyTree.getSelectionPath();
        if (path != null) {
            Object selected = path.getLastPathComponent();
            if (selected instanceof BuddyListModel.BuddyHolder) {
                BuddyListModel.BuddyHolder buddyHolder = (BuddyListModel.BuddyHolder) selected;
                sn = buddyHolder.getBuddy().getScreenname();
            }
        }
        return sn;
    }

    public void updateSession(GuiSession guiSession) {
        this.guiSession = guiSession;
        buddyList = guiSession.getAimConnection().getSsiService().getBuddyList();
        model = new BuddyListModel(buddyList);
        buddyTree.setModel(model);
    }

    private class ImAction extends AbstractAction {
        {
            putValue(NAME, "IM");
        }

        public void actionPerformed(ActionEvent e) {
            Screenname sn = getSelectedScreenname();
            if (sn != null) {
                ImConversation conv = guiSession.getAimConnection()
                        .getIcbmService()
                        .getImConversation(sn);
                conv.open();
            }
        }
    }

    private class AddGroupAction extends AbstractAction {
        {
            putValue(NAME, "Add Group");
        }

        public void actionPerformed(ActionEvent e) {
            String name = JOptionPane
                    .showInputDialog(BuddyListBox.this, "Group name:", "New Group");
            if (name != null) buddyList.addGroup(name);
        }
    }
    private class RemoveGroupAction extends AbstractAction {
        private final Group group;

        {
            putValue(NAME, "Remove Group");
        }

        public RemoveGroupAction(Group group) {
            this.group = group;
        }

        public void actionPerformed(ActionEvent e) {
            buddyList.deleteGroupAndBuddies(group);
        }
    }
    private class AddBuddyAction extends AbstractAction {
        private final AddMutableGroup group;

        {
            putValue(NAME, "Add Buddy");
        }

        public AddBuddyAction(AddMutableGroup group) {
            this.group = group;
        }

        public void actionPerformed(ActionEvent e) {
            String name = JOptionPane
                    .showInputDialog(BuddyListBox.this, "Screenname:", "New Buddy");
            if (name != null) group.addBuddy(name);
        }
    }
    private class RemoveBuddyAction extends AbstractAction {
        private DeleteMutableGroup group;
        private Buddy buddy;

        public RemoveBuddyAction(DeleteMutableGroup group, Buddy buddy) {
            this.group = group;
            this.buddy = buddy;
        }

        {
            putValue(NAME, "Remove Buddy");
        }

        public void actionPerformed(ActionEvent e) {
            group.deleteBuddy(buddy);
        }
    }
    private class ChangeAliasAction extends AbstractAction {
        private MutableBuddy buddy;

        {
            putValue(NAME, "Change Alias");
        }

        public ChangeAliasAction(MutableBuddy buddy) {
            this.buddy = buddy;
        }

        public void actionPerformed(ActionEvent e) {
            String name = JOptionPane
                    .showInputDialog(BuddyListBox.this, "Alias:", buddy.getAlias());
            if (name != null) buddy.changeAlias(name);
        }
    }
    private class ChangeCommentAction extends AbstractAction {
        private MutableBuddy buddy;

        {
            putValue(NAME, "Change Buddy Comment");
        }

        public ChangeCommentAction(MutableBuddy buddy) {
            this.buddy = buddy;
        }

        public void actionPerformed(ActionEvent e) {
            String name = JOptionPane
                    .showInputDialog(BuddyListBox.this, "Buddy Comment:", buddy.getBuddyComment());
            if (name != null) buddy.changeBuddyComment(name);
        }
    }

    private class RenameGroupAction extends AbstractAction {
        private final RenameMutableGroup mutableGroup;

        public RenameGroupAction(RenameMutableGroup mutableGroup) {
            this.mutableGroup = mutableGroup;
            putValue(NAME, "Rename group");
        }

        public void actionPerformed(ActionEvent e) {
            buddyTree.startEditingAtPath(model.getPathToGroup(mutableGroup));
        }
    }
}