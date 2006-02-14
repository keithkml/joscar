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

package net.kano.joustsim.oscar.oscar.service.ssi;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.ssi.SsiItem;
import net.kano.joscar.ssiitem.BuddyItem;
import net.kano.joscar.ssiitem.DefaultSsiItemObjFactory;
import net.kano.joscar.ssiitem.GroupItem;
import net.kano.joscar.ssiitem.RootItem;
import net.kano.joscar.ssiitem.SsiItemObj;
import net.kano.joscar.ssiitem.SsiItemObjectFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;

/*
 * SEMANTICS:
 * no root item - groups are sorted alphabetically
 * buddies but no group item - buddies are added to synthetic "Other Buddies" group, sorted alphabetically
 * group not listed in root item - orphan groups are sorted alphabetically at the end of the group list
 * buddy not listed in any group item - orphan buddies are sorted alphabetically at end of group's buddies
 *
 */

class SimpleBuddyList implements BuddyList, SsiItemChangeListener {
    public static final Comparator<SimpleBuddy> COMPARATOR_SN
            = new Comparator<SimpleBuddy>() {
        public int compare(SimpleBuddy o1, SimpleBuddy o2) {
            String sn1 = o1.getItem().getScreenname();
            String sn2 = o2.getItem().getScreenname();
            return sn1.compareToIgnoreCase(sn2);
        }
    };
    private static final Comparator<Group> COMPARATOR_GROUPNAME
            = new Comparator<Group>() {
        public int compare(Group o1, Group o2) {
            if (o1 instanceof SsiSyntheticGroup) return 1;
            if (o2 instanceof SsiSyntheticGroup) return -1;
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    };

    private List<AbstractGroup> groups = new ArrayList<AbstractGroup>();
    private final SyntheticGroup syntheticGroup = new SyntheticGroup(this);

    private final SsiItemObjectFactory factory = new DefaultSsiItemObjFactory();
    private RootItem rootItem = null;

    private CopyOnWriteArrayList<BuddyListLayoutListener> listeners
            = new CopyOnWriteArrayList<BuddyListLayoutListener>();

    protected SyntheticGroup getSyntheticGroup() {
        return syntheticGroup;
    }

    protected synchronized RootItem getRootItem() {
        return rootItem;
    }

    protected CopyOnWriteArrayList<BuddyListLayoutListener> getListeners() {
        return listeners;
    }

    public void handleItemCreated(final SsiItem item) {
        runAndRecordChanges(new Runnable() {
            public void run() {
                SsiItemObj iobj = factory.getItemObj(item);
                handleItemCreated(iobj);
            }
        });
    }

    public void handleItemModified(final SsiItem item) {
        runAndRecordChanges(new Runnable() {
            public void run() {
                SsiItemObj iobj = factory.getItemObj(item);
                handleItemModified(iobj);
            }
        });
    }

    public void handleItemDeleted(final SsiItem item) {
        runAndRecordChanges(new Runnable() {
            public void run() {
                handleItemActuallyDeleted(item);
            }
        });
    }

    private void runAndRecordChanges(Runnable runnable) {
        ListState saved;
        ListState newShot;
        synchronized (this) {
            saved = takeSnapshot();
            runnable.run();
            newShot = takeSnapshot();
        }
        detectChanges(saved, newShot);
    }

    private synchronized ListState takeSnapshot() {
        return new ListState(false);
    }

    private void detectChanges(ListState oldState, ListState newState) {
        List<AbstractGroup> oldGroups = new ArrayList<AbstractGroup>(
                oldState.getBuddies().keySet());
        List<AbstractGroup> newGroups = new ArrayList<AbstractGroup>(
                newState.getBuddies().keySet());
        ChangeTools.detectChanges(oldGroups, newGroups, new GroupChangeListener(oldState, newState));
        for (AbstractGroup group : newGroups) {
            detectChangesInGroup(oldState, newState, group);
        }
    }

    private void detectChangesInGroup(ListState oldState, ListState newState,
            final AbstractGroup group) {
        List<SimpleBuddy> oldBuddies = oldState.getBuddies(group);
        if (oldBuddies == null) return;
        List<SimpleBuddy> newBuddies = newState.getBuddies(group);
        if (newBuddies == null) return;

        ChangeTools.detectChanges(oldBuddies, newBuddies,
                new BuddyChangeListener(group));
        group.detectChanges(oldState.getGroupState(group),
                newState.getGroupState(group));
        for (SimpleBuddy buddy : newBuddies) {
            SimpleBuddy.BuddyState oldBuddyState = oldState.getBuddyState(buddy);
            SimpleBuddy.BuddyState newBuddyState = newState.getBuddyState(buddy);
            if (oldBuddyState == null || newBuddyState == null) continue;
            buddy.detectChanges(oldBuddyState, newBuddyState);
        }
    }

    private synchronized void handleItemCreated(SsiItemObj item) {
        if (item instanceof RootItem) {
            RootItem rootItem = (RootItem) item;
            // a root item was added.
            // X maybe groups order changed.
            this.rootItem = rootItem;
            sortGroups();

        } else if (item instanceof GroupItem) {
            GroupItem groupItem = (GroupItem) item;
            // a group was added.
            SimpleBuddyGroup newGroup = createBuddyGroup(groupItem);
            groups.add(newGroup);


            // maybe its buddies were moved to this group from the "other buddies" group.
            List<SimpleBuddy> moveBuddies = new ArrayList<SimpleBuddy>();
            for (SimpleBuddy buddy : syntheticGroup.getBuddiesCopy()) {
                if (buddy.getItem().getGroupId() == groupItem.getId()) {
                    moveBuddies.add(buddy);
                }
            }
            syntheticGroup.removeBuddies(moveBuddies);
            syntheticGroup.sortBuddies();

            newGroup.addBuddies(moveBuddies);
            newGroup.sortBuddies();

            // maybe the "other buddies" group is removed because all orphans are in this group.
            if (syntheticGroup.getBuddiesCopy().isEmpty()) {
                groups.remove(syntheticGroup);
            }

            // (maybe group order changes if it's not listed in the root item,
            // or the "other buddies" group was removed.)
            sortGroups();

        } else if (item instanceof BuddyItem) {
            BuddyItem buddyItem = (BuddyItem) item;
            // a buddy was added.
            SimpleBuddy buddy = createBuddy(buddyItem);
            AbstractGroup group = getGroup(buddyItem.getGroupId());
            if (group == null) {
                group = syntheticGroup;
                // maybe the "other buddies" group is added.
                if (!groups.contains(group)) {
                    groups.add(group);
                    sortGroups();
                }
            }
            group.addBuddy(buddy);
            // maybe "other buddies" order changes.
            group.sortBuddies();
        }
    }

    protected SimpleBuddy createBuddy(BuddyItem buddyItem) {
        return new SimpleBuddy(this, buddyItem);
    }

    protected SimpleBuddyGroup createBuddyGroup(GroupItem groupItem) {
        return new SimpleBuddyGroup(this, groupItem);
    }

    private synchronized Runnable handleItemModified(SsiItemObj newItem) {
        if (newItem instanceof RootItem) {
            RootItem rootItem = (RootItem) newItem;
            // root item changed.
            this.rootItem = rootItem;
            // maybe groups order changed.
            sortGroups();

        } else if (newItem instanceof GroupItem) {
            GroupItem groupItem = (GroupItem) newItem;
            // group item changed.
            final SimpleBuddyGroup group = getGroup(groupItem.getId());
            if (group == null) {
                throw new IllegalStateException("group " + groupItem
                        + " was modified but it's not present in group list");
            }
            final AbstractGroup.GroupState state = group.saveState();
            group.setItem(groupItem);
            final AbstractGroup.GroupState newState = group.saveState();

            // maybe buddy order in the group changed.
            group.sortBuddies();

            return new Runnable() {
                public void run() {
                    group.detectChanges(state, newState);
                }
            };

        } else if (newItem instanceof BuddyItem) {
            BuddyItem buddyItem = (BuddyItem) newItem;
            // buddy item changed.
            AbstractGroup group = getActualGroup(buddyItem.getGroupId());
            SimpleBuddy buddy = group.getBuddy(buddyItem.getId());
            buddy.setItem(buddyItem);
            // TODO: if buddy was renamed, we should remove and add? what happens when we send a rename?
        }
        return null;
    }

    private synchronized void handleItemActuallyDeleted(SsiItem item) {
        int type = item.getItemType();
        if (type == SsiItem.TYPE_GROUP) {
            if (DefaultSsiItemObjFactory.isRootItem(item)) {
                // root item deleted.
                this.rootItem = null;

                // maybe groups order changed.
                sortGroups();

            } else {
                // group item deleted.
                // maybe "other buddies" group is added because buddies are now orphans.
                // maybe "other buddies" order changed.
                SimpleBuddyGroup group = getGroup(item.getParentId());
                if (group == null) {
                    throw new IllegalStateException("group " + item + " was "
                            + "supposedly deleted but we have no record of it");
                }
                List<SimpleBuddy> buddies = group.getBuddiesCopy();
                if (!buddies.isEmpty()) {
                    if (!groups.contains(syntheticGroup)) {
                        groups.add(syntheticGroup);
                    }
                    syntheticGroup.addBuddies(buddies);
                    syntheticGroup.sortBuddies();
                }
                groups.remove(group);
                sortGroups();
                group.setActive(false);

            }

        } else if (type == SsiItem.TYPE_BUDDY) {
            // buddy item deleted.
            AbstractGroup group = getActualGroup(item.getParentId());
            SimpleBuddy buddy = group.getBuddy(item.getId());
            group.removeBuddy(buddy);
            group.sortBuddies();

            // maybe "other buddies" group is deleted because this was the only item.
            if (group == syntheticGroup) {
                if (group.getBuddiesCopy().isEmpty()) {
                    groups.remove(group);
                    sortGroups();
                }
            }
            buddy.setActive(false);
        }
    }

    private synchronized void sortGroups() {
        List<AbstractGroup> oldGroups = groups;
        List<AbstractGroup> newGroups = new ArrayList<AbstractGroup>();
        RootItem rootItem = this.rootItem;
        List<AbstractGroup> leftover;
        if (rootItem != null) {
            Map<Integer, AbstractGroup> id2group = new HashMap<Integer, AbstractGroup>();
            for (AbstractGroup group : oldGroups) {
                if (group instanceof SimpleBuddyGroup) {
                    SimpleBuddyGroup buddyGroup = (SimpleBuddyGroup) group;
                    id2group.put(buddyGroup.getItem().getId(), buddyGroup);
                }
            }

            int[] groupids = rootItem.getGroupids();
            if (groupids != null) {
                for (int groupId : groupids) {
                    AbstractGroup group = id2group.remove(groupId);
                    if (group == null) continue;

                    newGroups.add(group);
                }
            }
            leftover = new ArrayList<AbstractGroup>(id2group.values());

        } else {
            leftover = new ArrayList<AbstractGroup>(oldGroups);
        }

        Collections.sort(leftover, COMPARATOR_GROUPNAME);
        newGroups.addAll(leftover);

        assert !newGroups.contains(syntheticGroup)
                || newGroups.indexOf(syntheticGroup) == newGroups.size() - 1;
        groups = newGroups;
    }

    private synchronized SimpleBuddyGroup getGroup(int groupId) {
        for (AbstractGroup group : groups) {
            if (group instanceof SimpleBuddyGroup) {
                SimpleBuddyGroup buddyGroup = (SimpleBuddyGroup) group;
                if (buddyGroup.getItem().getId() == groupId) return buddyGroup;
            }
        }
        return null;
    }

    private AbstractGroup getActualGroup(int groupId) {
        AbstractGroup group = getGroup(groupId);
        if (group == null) {
            group = syntheticGroup;
        }
        return group;
    }

    Object getLock() {
        return this;
    }

    public void addLayoutListener(BuddyListLayoutListener listener) {
        listeners.add(listener);
    }

    public void removeLayoutListener(BuddyListLayoutListener listener) {
        listeners.remove(listener);
    }

    public synchronized List<? extends Group> getGroups() {
        return DefensiveTools.getUnmodifiableCopy(groups);
    }

    public void addRetroactiveLayoutListener(
            BuddyListLayoutListener listener) {
        ListState empty = new ListState(true);
        ListState state;
        synchronized (this) {
            addLayoutListener(listener);
            state = takeSnapshot();
        }
        detectChanges(empty, state);
    }

    private class ListState {
        private final Map<AbstractGroup, List<SimpleBuddy>> buddies;
        private final Map<AbstractGroup, AbstractGroup.GroupState> groupStates;
        private final Map<SimpleBuddy, SimpleBuddy.BuddyState> buddyStates;

        private ListState(boolean empty) {
            if (empty) {
                buddies = Collections.EMPTY_MAP;
                groupStates = Collections.EMPTY_MAP;
                buddyStates = Collections.EMPTY_MAP;

            } else {
                synchronized (SimpleBuddyList.this) {
                    Map<AbstractGroup, List<SimpleBuddy>> buddies
                            = new LinkedHashMap<AbstractGroup, List<SimpleBuddy>>();
                    Map<AbstractGroup, AbstractGroup.GroupState> groupStates
                            = new HashMap<AbstractGroup, AbstractGroup.GroupState>();
                    Map<SimpleBuddy, SimpleBuddy.BuddyState> buddyStates
                            = new HashMap<SimpleBuddy, SimpleBuddy.BuddyState>();
                    for (AbstractGroup group : groups) {
                        buddies.put(group, group.getBuddiesCopy());
                        groupStates.put(group, group.saveState());
                        for (SimpleBuddy buddy : group.getBuddies()) {
                            buddyStates.put(buddy, buddy.saveState());
                        }
                    }
                    this.buddies = Collections.unmodifiableMap(buddies);
                    this.groupStates = Collections.unmodifiableMap(groupStates);
                    this.buddyStates = Collections.unmodifiableMap(buddyStates);
                }

            }
        }

        private Map<AbstractGroup,List<SimpleBuddy>> getBuddies() {
            return buddies;
        }

        public Map<AbstractGroup,AbstractGroup.GroupState> getGroupStates() {
            return groupStates;
        }

        public List<SimpleBuddy> getBuddies(Group group) {
            return buddies.get(group);
        }

        public AbstractGroup.GroupState getGroupState(Group group) {
            return groupStates.get(group);
        }

        public Map<SimpleBuddy,SimpleBuddy.BuddyState> getBuddyStates() {
            return buddyStates;
        }


        public SimpleBuddy.BuddyState getBuddyState(Buddy buddy) {
            return buddyStates.get(buddy);
        }
    }

    private class GroupChangeListener implements DetectedChangeListener<Group> {
        private ListState oldState;
        private ListState newState;

        public GroupChangeListener(ListState oldState, ListState newState) {
            this.oldState = oldState;
            this.newState = newState;
        }

        public void itemAdded(Collection<? extends Group> oldItems,
                Collection<? extends Group> newItems,
                Group item) {
            assert !Thread.holdsLock(SimpleBuddyList.this);

            List<Group> oldItemsCopy = DefensiveTools.getUnmodifiableCopy(oldItems);
            List<Group> newItemsCopy = DefensiveTools.getUnmodifiableCopy(newItems);
            for (BuddyListLayoutListener listener : listeners) {
                listener.groupAdded(SimpleBuddyList.this, oldItemsCopy, newItemsCopy, item,
                        newState.getBuddies(item));
            }
        }

        public void itemRemoved(Collection<? extends Group> oldItems,
                Collection<? extends Group> newItems,
                Group item) {
            assert !Thread.holdsLock(SimpleBuddyList.this);

            List<Group> oldItemsCopy = DefensiveTools.getUnmodifiableCopy(oldItems);
            List<Group> newItemsCopy = DefensiveTools.getUnmodifiableCopy(newItems);
            for (BuddyListLayoutListener listener : listeners) {
                listener.groupRemoved(SimpleBuddyList.this, oldItemsCopy, newItemsCopy, item);
            }
        }

        public void itemsReordered(Collection<? extends Group> oldItems,
                Collection<? extends Group> newItems) {
            assert !Thread.holdsLock(SimpleBuddyList.this);

            List<Group> oldItemsCopy = DefensiveTools.getUnmodifiableCopy(oldItems);
            List<Group> newItemsCopy = DefensiveTools.getUnmodifiableCopy(newItems);
            for (BuddyListLayoutListener listener : listeners) {
                listener.groupsReordered(SimpleBuddyList.this, oldItemsCopy, newItemsCopy);
            }
        }
    }

    private class BuddyChangeListener implements DetectedChangeListener<SimpleBuddy> {
        private final Group group;

        public BuddyChangeListener(Group group) {
            this.group = group;
        }

        public void itemAdded(Collection<? extends SimpleBuddy> oldItems,
                Collection<? extends SimpleBuddy> newItems,
                SimpleBuddy item) {
            assert !Thread.holdsLock(SimpleBuddyList.this);

            List<SimpleBuddy> oldItemsCopy = DefensiveTools.getUnmodifiableCopy(oldItems);
            List<SimpleBuddy> newItemsCopy = DefensiveTools.getUnmodifiableCopy(newItems);

            for (BuddyListLayoutListener listener : listeners) {
                listener.buddyAdded(SimpleBuddyList.this, group, oldItemsCopy, newItemsCopy,
                        item);
            }
        }

        public void itemRemoved(Collection<? extends SimpleBuddy> oldItems,
                Collection<? extends SimpleBuddy> newItems,
                SimpleBuddy item) {
            assert !Thread.holdsLock(SimpleBuddyList.this);

            List<SimpleBuddy> oldItemsCopy = DefensiveTools.getUnmodifiableCopy(oldItems);
            List<SimpleBuddy> newItemsCopy = DefensiveTools.getUnmodifiableCopy(newItems);

            for (BuddyListLayoutListener listener : listeners) {
                listener.buddyRemoved(SimpleBuddyList.this, group, oldItemsCopy,
                        newItemsCopy, item);
            }
        }

        public void itemsReordered(Collection<? extends SimpleBuddy> oldItems,
                Collection<? extends SimpleBuddy> newItems) {
            assert !Thread.holdsLock(SimpleBuddyList.this);

            List<SimpleBuddy> oldItemsCopy = DefensiveTools.getUnmodifiableCopy(oldItems);
            List<SimpleBuddy> newItemsCopy = DefensiveTools.getUnmodifiableCopy(newItems);

            for (BuddyListLayoutListener listener : listeners) {
                listener.buddiesReordered(SimpleBuddyList.this, group, oldItemsCopy,
                        newItemsCopy);
            }
        }
    }
}
