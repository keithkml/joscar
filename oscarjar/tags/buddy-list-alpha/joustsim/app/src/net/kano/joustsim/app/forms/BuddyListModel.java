/*
 *  Copyright (c) 2005, The Joust Project
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
 */

package net.kano.joustsim.app.forms;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joustsim.oscar.oscar.service.ssi.Buddy;
import net.kano.joustsim.oscar.oscar.service.ssi.BuddyList;
import net.kano.joustsim.oscar.oscar.service.ssi.BuddyListLayoutListener;
import net.kano.joustsim.oscar.oscar.service.ssi.Group;
import net.kano.joustsim.oscar.oscar.service.ssi.MutableGroup;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

public class BuddyListModel implements TreeModel {
    private BuddyList list;
    private Object root = new Object();
    private List<GroupHolder> groups = new ArrayList<GroupHolder>();
    private CopyOnWriteArrayList<TreeModelListener> listeners
            = new CopyOnWriteArrayList<TreeModelListener>();

    public BuddyListModel(BuddyList list) {
        this.list = list;
        list.addRetroactiveLayoutListener(new BuddyListLayoutListener() {
            public void groupsReordered(BuddyList list, List<? extends Group> oldOrder,
                    List<? extends Group> newOrder) {
                List<GroupHolder> newGroups = new ArrayList<GroupHolder>();
                for (Group group : newOrder) {
                    BuddyListModel.GroupHolder holder = getGroupHolder(group);
                    if (holder == null) continue;
                    newGroups.add(holder);
                }
                groups = newGroups;
                for (TreeModelListener listener : listeners) {
                    listener.treeStructureChanged(new TreeModelEvent(
                            BuddyListModel.this,
                            new Object[] { root }));
                }
            }

            public void groupAdded(BuddyList list, List<? extends Group> oldItems,
                    List<? extends Group> newItems, Group group,
                    List<? extends Buddy> buddies) {
                int index = newItems.indexOf(group);
                assert index != -1 : newItems + " - " + group;

                int toinsert = 0;
                for (; index >= 0; index--) {
                    Group prevGroup = newItems.get(index);
                    BuddyListModel.GroupHolder holder = getGroupHolder(prevGroup);
                    if (holder != null) {
                        toinsert = groups.indexOf(holder) + 1;
                        break;
                    }
                }
                // if the loop completes without setting toinsert, it's okay,
                // we insert it at the beginning, that's where it must go

                GroupHolder newHolder = new GroupHolder(group);
                groups.add(toinsert, newHolder);
                for (Buddy buddy : buddies) {
                    newHolder.addBuddy(buddy);
                }
                for (TreeModelListener listener : listeners) {
                    listener.treeNodesInserted(new TreeModelEvent(
                            BuddyListModel.this, new Object[] { root },
                            new int[] { toinsert }, new Object[] { newHolder }));
                }
            }

            public void groupRemoved(BuddyList list, List<? extends Group> oldItems,
                    List<? extends Group> newItems, Group group) {
                BuddyListModel.GroupHolder holder = getGroupHolder(group);
                int index = groups.indexOf(holder);
                groups.remove(index);
                for (TreeModelListener listener : listeners) {
                    listener.treeNodesRemoved(new TreeModelEvent(
                            BuddyListModel.this, new Object[] { root },
                            new int[] { index }, new Object[] { holder }));
                }
            }

            public void buddyAdded(BuddyList list, Group group,
                    List<? extends Buddy> oldItems, List<? extends Buddy> newItems,
                    Buddy buddy) {
                int index = newItems.indexOf(buddy);
                assert index != -1 : newItems + " - " + buddy;
                GroupHolder groupHolder = getGroupHolder(group);

                int toinsert = 0;
                for (; index >= 0; index--) {
                    Buddy prevBuddy = newItems.get(index);
                    BuddyHolder buddyHolder = groupHolder.getBuddyHolder(prevBuddy);
                    if (buddyHolder != null) {
                        toinsert = groupHolder.getIndexOfBuddy(buddyHolder) + 1;
                        break;
                    }
                }
                // if the loop completes without setting toinsert, it's okay,
                // we insert it at the beginning, that's where it must go

                BuddyHolder newHolder = new BuddyHolder(buddy);
                groupHolder.addBuddy(toinsert, newHolder);
                for (TreeModelListener listener : listeners) {
                    listener.treeNodesInserted(new TreeModelEvent(
                            BuddyListModel.this, new Object[] { getRoot(), groupHolder },
                            new int[] { toinsert }, new Object[] { newHolder }));
                }
            }


            public void buddyRemoved(BuddyList list, Group group,
                    List<? extends Buddy> oldItems, List<? extends Buddy> newItems,
                    Buddy buddy) {
                BuddyListModel.GroupHolder groupHolder = getGroupHolder(group);
                BuddyHolder holder = groupHolder.getBuddyHolder(buddy);
                int index = groupHolder.getIndexOfBuddy(holder);
                groupHolder.removeBuddy(index);
                for (TreeModelListener listener : listeners) {
                    listener.treeNodesRemoved(new TreeModelEvent(
                            BuddyListModel.this, new Object[] { getRoot(), groupHolder },
                            new int[] { index }, new Object[] { holder }));
                }
            }

            public void buddiesReordered(BuddyList list, Group group,
                    List<? extends Buddy> oldBuddies,
                    List<? extends Buddy> newBuddies) {
                BuddyListModel.GroupHolder holder = getGroupHolder(group);
                List<BuddyHolder> newHolders = new ArrayList<BuddyHolder>();
                for (Buddy buddy : newBuddies) {
                    BuddyListModel.BuddyHolder buddyHolder = holder.getBuddyHolder(buddy);
                    if (buddyHolder == null) continue;
                    newHolders.add(buddyHolder);
                }
                holder.setBuddies(newHolders);
                for (TreeModelListener listener : listeners) {
                    listener.treeStructureChanged(new TreeModelEvent(
                            BuddyListModel.this,
                            new Object[] { getRoot(), holder }));
                }
            }
        });
    }

    private GroupHolder getGroupHolder(Group group) {
        for (GroupHolder groupHolder : groups) {
            if (groupHolder.getGroup() == group) return groupHolder;
        }
        return null;
    }

    public Object getRoot() {
        return root;
    }

    public Object getChild(Object parent, int index) {
        if (parent == root) {
            return groups.get(index);
        } else if (parent instanceof GroupHolder) {
            BuddyHolder child = ((GroupHolder) parent)
                    .getBuddyHolder(index);
            return child;
        }
        return null;
    }

    public int getChildCount(Object parent) {
        if (parent == root) {
            return groups.size();
        } else if (parent instanceof GroupHolder) {
            int count = ((GroupHolder) parent).getBuddyCount();
            return count;
        } else {
            return 0;
        }
    }

    public boolean isLeaf(Object node) {
        return node != root && !(node instanceof GroupHolder);
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        if (!(newValue instanceof String)) return;
        String newName = (String) newValue;

        Object[] objs = path.getPath();
        int len = objs.length;
        if (len == 2) {
            GroupHolder holder = (GroupHolder) objs[1];
            Group group = holder.getGroup();
            if (group instanceof MutableGroup) {
                MutableGroup mutableGroup = (MutableGroup) group;
                mutableGroup.rename(newName);
            }
        }
    }

    public int getIndexOfChild(Object parent, Object child) {
        if (parent == root && child instanceof GroupHolder) {
            return groups.indexOf(child);
        } else if (parent instanceof GroupHolder && child instanceof BuddyHolder) {
            return ((GroupHolder) parent).getIndexOfBuddy((BuddyHolder) child);
        } else {
            return -1;
        }
    }

    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    public TreePath getPathToGroup(final MutableGroup mutableGroup) {
        return new TreePath(new Object[] { getRoot(), getGroupHolder(mutableGroup) });
    }

    public static class GroupHolder {
        private final Group group;
        private List<BuddyHolder> buddies = new ArrayList<BuddyHolder>();

        private GroupHolder(Group group) {
            this.group = group;
        }

        public int getBuddyCount() {
            return buddies.size();
        }

        public int getIndexOfBuddy(BuddyHolder child) {
            return buddies.indexOf(child);
        }

        public Group getGroup() {
            return group;
        }

        public BuddyHolder getBuddyHolder(Buddy buddy) {
            for (BuddyHolder holder : buddies) {
                if (holder.getBuddy() == buddy) return holder;
            }
            return null;
        }

        public void addBuddy(int toinsert, BuddyHolder newHolder) {
            buddies.add(toinsert, newHolder);
        }

        public void removeBuddy(int index) {
            buddies.remove(index);
        }

        public BuddyHolder getBuddyHolder(int index) {
            return buddies.get(index);
        }

        public String toString() {
            return group.getName();
        }

        public void addBuddy(Buddy buddy) {
            buddies.add(new BuddyHolder(buddy));
        }

        public void setBuddies(List<BuddyHolder> newBuddies) {
            buddies = newBuddies;
        }
    }

    public static class BuddyHolder {
        private final Buddy buddy;

        private BuddyHolder(Buddy buddy) {
            this.buddy = buddy;
        }

        public Buddy getBuddy() {
            return buddy;
        }

        public String toString() {
            return buddy.getScreenname().getFormatted() + " (" + buddy.getAlias() + ")";
        }
    }
}
