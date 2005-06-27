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

import net.kano.joustsim.app.GuiSession;
import net.kano.joustsim.oscar.oscar.service.ssi.Buddy;
import net.kano.joustsim.oscar.oscar.service.ssi.Group;
import net.kano.joustsim.oscar.oscar.service.ssi.MutableBuddy;
import net.kano.joustsim.oscar.oscar.service.ssi.MutableBuddyList;
import net.kano.joustsim.oscar.oscar.service.ssi.MutableGroup;

import javax.swing.AbstractAction;
import javax.swing.JButton;
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
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

public class BuddyListBox extends JPanel {
    private JButton infoButton;
    private JButton chatButton;
    private JButton imButton;
    private JTree buddyTree;
    private JPanel mainPanel;
    private BuddyListModel model;
    private JPopupMenu menu = new JPopupMenu();
    private MutableBuddyList buddyList;
    private static final DataFlavor FLAVOR_GROUPS = new DataFlavor(List.class, "Buddy Group");
    private static final DataFlavor FLAVOR_BUDDIES = new DataFlavor(List.class, "Buddy");


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
                    if (group instanceof MutableGroup) {
                        MutableGroup mutableGroup = (MutableGroup) group;
                        menu.add(new AddBuddyAction(mutableGroup));
                    }
                    menu.addSeparator();

                    menu.add(new AddGroupAction());
                    if (group instanceof MutableGroup) {
                        final MutableGroup mutableGroup = (MutableGroup) group;

                        menu.add(new AbstractAction() {
                            {
                                putValue(NAME, "Rename group");
                            }
                            public void actionPerformed(ActionEvent e) {
                                buddyTree.startEditingAtPath(model.getPathToGroup(mutableGroup));
                            }
                        });
                    }
                    menu.add(new RemoveGroupAction(group));


                } else if (item instanceof BuddyListModel.BuddyHolder) {
                    BuddyListModel.BuddyHolder holder = (BuddyListModel.BuddyHolder) item;
                    MutableGroup mgroup = null;
                    Buddy buddy = holder.getBuddy();
                    for (Group group : buddyList.getGroups()) {
                        if (group.getBuddiesCopy().contains(buddy)) {
                            if (group instanceof MutableGroup) {
                                mgroup = (MutableGroup) group;
                                break;
                            }
                        }
                    }
                    if (mgroup != null) {
                        menu.add(new AddBuddyAction(mgroup));
                        menu.add(new RemoveBuddyAction(mgroup, buddy));
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
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
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
    }

    private List<Group> getSelectedGroups() {
        List<Group> holders = new ArrayList<Group>();
        for (TreePath path : buddyTree.getSelectionPaths()) {
            Object obj = path.getLastPathComponent();
            if (obj instanceof BuddyListModel.GroupHolder) {
                BuddyListModel.GroupHolder holder = (BuddyListModel.GroupHolder) obj;
                holders.add(holder.getGroup());
            }
        }
        return holders;
    }

    public void updateSession(GuiSession guiSession) {
        buddyList = guiSession.getAimConnection().getSsiService().getBuddyList();
        model = new BuddyListModel(buddyList);
        buddyTree.setModel(model);
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
        private final MutableGroup group;

        {
            putValue(NAME, "Add Buddy");
        }

        public AddBuddyAction(MutableGroup group) {
            this.group = group;
        }

        public void actionPerformed(ActionEvent e) {
            String name = JOptionPane
                    .showInputDialog(BuddyListBox.this, "Screenname:", "New Buddy");
            if (name != null) group.addBuddy(name);
        }
    }
    private class RemoveBuddyAction extends AbstractAction {
        private MutableGroup group;
        private Buddy buddy;

        public RemoveBuddyAction(MutableGroup group, Buddy buddy) {
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
}