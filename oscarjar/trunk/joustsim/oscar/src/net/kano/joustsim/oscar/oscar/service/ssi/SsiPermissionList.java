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
import net.kano.joscar.snaccmd.ssi.CreateItemsCmd;
import net.kano.joscar.snaccmd.ssi.DeleteItemsCmd;
import net.kano.joscar.snaccmd.ssi.ModifyItemsCmd;
import net.kano.joscar.snaccmd.ssi.ItemsCmd;
import net.kano.joscar.ssiitem.DefaultSsiItemObjFactory;
import net.kano.joscar.ssiitem.DenyItem;
import net.kano.joscar.ssiitem.PrivacyItem;
import net.kano.joscar.ssiitem.SsiItemObj;
import net.kano.joscar.ssiitem.SsiItemObjectFactory;
import net.kano.joscar.ssiitem.PermitItem;
import net.kano.joustsim.Screenname;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Collections;

class SsiPermissionList implements PermissionList {
    private SsiItemObjectFactory factory = new DefaultSsiItemObjFactory();

    private SortedSet<PrivacyItem> privacyItems
            = new TreeSet<PrivacyItem>(new ItemIdComparator());
    private CopyOnWriteArrayList<PermissionListListener> listeners
            = new CopyOnWriteArrayList<PermissionListListener>();
    private List<DenyItem> denyItems = new ArrayList<DenyItem>();
    private List<PermitItem> permitItems = new ArrayList<PermitItem>();

    private AllowedBuddyChangeListener allowedBuddyChangeListener
            = new AllowedBuddyChangeListener();
    private BlockedBuddyChangeListener blockedBuddyChangeListener
            = new BlockedBuddyChangeListener();
    private final SsiService ssiService;

    public SsiPermissionList(SsiService ssiService) {
        this.ssiService = ssiService;
    }

    public void handleItemCreated(final SsiItem item) {
        modifyItems(new Runnable() {
            public void run() {
                handleItemsActuallyCreated(factory.getItemObj(item));
            }
        });
    }

    public void handleItemModified(final SsiItem item) {
        modifyItems(new Runnable() {
            public void run() {
                SsiItemObj itemObj = factory.getItemObj(item);
                handleItemActuallyModified(itemObj);
            }
        });
    }

    public void handleItemDeleted(final SsiItem item) {
        modifyItems(new Runnable() {
            public void run() {
                SsiItemObj itemObj = factory.getItemObj(item);
                handleItemActuallyDeleted(itemObj);
            }
        });
    }

    private synchronized void handleItemsActuallyCreated(SsiItemObj item) {
        if (item instanceof PrivacyItem) {
            final PrivacyItem pitem = (PrivacyItem) item;
            privacyItems.add(pitem);

        } else if (item instanceof DenyItem) {
            final DenyItem denyItem = (DenyItem) item;
            denyItems.add(denyItem);

        } else if (item instanceof PermitItem) {
            PermitItem permitItem = (PermitItem) item;
            permitItems.add(permitItem);
        }
    }

    private synchronized void handleItemActuallyModified(SsiItemObj itemObj) {
        if (itemObj instanceof PrivacyItem) {
            final PrivacyItem pitem = (PrivacyItem) itemObj;
            removePrivacyItemWithId(pitem.getId());
            privacyItems.add(pitem);

        } else if (itemObj instanceof DenyItem) {
            final DenyItem denyItem = (DenyItem) itemObj;
            removeDenyItemWithId(denyItem.getId());
            denyItems.add(denyItem);
        } else if (itemObj instanceof PermitItem) {
            PermitItem permitItem = (PermitItem) itemObj;
            removeAllowItemWithId(permitItem.getId());
            permitItems.add(permitItem);
        }
    }

    private synchronized void handleItemActuallyDeleted(SsiItemObj itemObj) {
        if (itemObj instanceof PrivacyItem) {
            PrivacyItem privacyItem = (PrivacyItem) itemObj;
            removePrivacyItemWithId(privacyItem.getId());

        } else if (itemObj instanceof DenyItem) {
            DenyItem denyItem = (DenyItem) itemObj;

            removeDenyItemWithId(denyItem.getId());
        }
    }

    private void modifyItems(Runnable runnable) {
        assert !Thread.holdsLock(this);

        PrivacyItem oldItem;
        PrivacyItem newTopItem;
        Set<Screenname> oldBlocked;
        Set<Screenname> newBlocked;
        Set<Screenname> oldAllowed;
        Set<Screenname> newAllowed;
        synchronized (this) {
            oldItem = getPrivacyItem();
            oldBlocked = getBlockedBuddies();
            oldAllowed = getAllowedBuddies();
            runnable.run();
            newTopItem = getPrivacyItem();
            newBlocked = getBlockedBuddies();
            newAllowed = getAllowedBuddies();
        }
        updatePrivacyItem(oldItem, newTopItem);
        updateBlockedBuddies(oldBlocked, newBlocked);
        updateAllowedBuddies(oldAllowed, newAllowed);
    }

    private synchronized void removeAllowItemWithId(int id) {
        for (Iterator<PermitItem> it = permitItems.iterator();
                it.hasNext();) {
            PermitItem permitItem = it.next();
            if (permitItem.getId() == id) it.remove();
        }
    }

    private synchronized void removeDenyItemWithId(int id) {
        for (Iterator<DenyItem> it = denyItems.iterator();
                it.hasNext();) {
            DenyItem denyItem = it.next();
            if (denyItem.getId() == id) it.remove();
        }
    }

    private void updateBlockedBuddies(Set<Screenname> oldBlocked,
            Set<Screenname> newBlocked) {
        ChangeTools.detectChanges(oldBlocked, newBlocked,
                blockedBuddyChangeListener);
    }

    private void updateAllowedBuddies(Set<Screenname> oldAllowed,
            Set<Screenname> newAllowed) {
        ChangeTools.detectChanges(oldAllowed, newAllowed,
                allowedBuddyChangeListener);
    }

    public synchronized PrivacyMode getPrivacyMode() {
        return getPrivacyModeFromItem(getPrivacyItem());
    }

    private synchronized @Nullable PrivacyItem getPrivacyItem() {
        return privacyItems.isEmpty() ? null : privacyItems.last();
    }

    private void updatePrivacyItem(@Nullable PrivacyItem oldItem,
            @Nullable PrivacyItem newItem) {
        assert !Thread.holdsLock(this);

        PrivacyMode oldMode = getPrivacyModeFromItem(oldItem);
        PrivacyMode newMode = getPrivacyModeFromItem(newItem);
        if (oldMode != newMode) {
            for (PermissionListListener listener : listeners) {
                listener.handlePrivacyModeChange(this, oldMode, newMode);
            }
        }
    }

    private @NotNull PrivacyMode getPrivacyModeFromItem(@Nullable PrivacyItem item) {
        return item == null ? PrivacyMode.ALLOW_ALL : getPrivacyModeFromCode(
                item.getPrivacyMode());
    }

    private PrivacyMode getPrivacyModeFromCode(int code) {
        if (code == PrivacyItem.MODE_ALLOW_ALL) {
            return PrivacyMode.ALLOW_ALL;
        } else if (code == PrivacyItem
                .MODE_ALLOW_PERMITS) {
            return PrivacyMode.ALLOW_ALLOWED;
        } else if (code == PrivacyItem
                .MODE_BLOCK_ALL) {
            return PrivacyMode.BLOCK_ALL;
        } else if (code == PrivacyItem
                .MODE_BLOCK_DENIES) {
            return PrivacyMode.BLOCK_BLOCKED;
        } else if (code == PrivacyItem
                .MODE_ALLOW_BUDDIES) {
            return PrivacyMode.ALLOW_BUDDIES;
        } else {
            return PrivacyMode.ALLOW_ALL;
        }
    }

    public synchronized Set<Screenname> getBlockedBuddies() {
        Set<Screenname> blocked = new HashSet<Screenname>();
        for (DenyItem denyItem : denyItems) {
            blocked.add(new Screenname(denyItem.getScreenname()));
        }
        return blocked;
    }

    public synchronized Set<Screenname> getAllowedBuddies() {
        Set<Screenname> allowed = new HashSet<Screenname>();
        for (PermitItem permitItem : permitItems) {
            allowed.add(new Screenname(permitItem.getScreenname()));
        }
        return allowed;
    }

    public synchronized Set<Screenname> getEffectiveBlockedBuddies() {
        if (getPrivacyMode() == PrivacyMode.BLOCK_BLOCKED) {
            return getBlockedBuddies();
        } else {
            return Collections.EMPTY_SET;
        }
    }

    public synchronized Set<Screenname> getEffectiveAllowedBuddies() {
        if (getPrivacyMode() == PrivacyMode.ALLOW_ALLOWED) {
            return getAllowedBuddies();
        } else {
            return Collections.EMPTY_SET;
        }
}

    public void addToBlockList(Screenname sn) {
        int uniqueId = ssiService.getUniqueItemId(SsiItem.TYPE_DENY, SsiItem.GROUP_ROOT);
        DenyItem denyItem = new DenyItem(sn.getFormatted(), uniqueId);
        ssiService.sendSsiModification(new CreateItemsCmd(denyItem.toSsiItem()));
    }

    public void addToAllowedList(Screenname sn) {
        int uniqueId = ssiService.getUniqueItemId(SsiItem.TYPE_PERMIT, SsiItem.GROUP_ROOT);
        PermitItem permitItem = new PermitItem(sn.getFormatted(), uniqueId);
        ssiService.sendSsiModification(new CreateItemsCmd(permitItem.toSsiItem()));
    }

    public synchronized void removeFromBlockList(Screenname sn) {
        List<SsiItem> badItems = getDenyItemsForScreenname(sn);
        ssiService.sendSsiModification(new DeleteItemsCmd(badItems));
    }

    public void removeFromAllowedList(Screenname sn) {
        List<SsiItem> badItems = getPermitItemsForScreenname(sn);
        ssiService.sendSsiModification(new DeleteItemsCmd(badItems));
    }

    private synchronized List<SsiItem> getDenyItemsForScreenname(Screenname sn) {
        List<SsiItem> badItems = new ArrayList<SsiItem>();
        for (DenyItem item : denyItems) {
            if (new Screenname(item.getScreenname()).equals(sn)) {
                badItems.add(item.toSsiItem());
            }
        }
        return badItems;
    }

    private synchronized List<SsiItem> getPermitItemsForScreenname(Screenname sn) {
        List<SsiItem> badItems = new ArrayList<SsiItem>();
        for (PermitItem item : permitItems) {
            if (new Screenname(item.getScreenname()).equals(sn)) {
                badItems.add(item.toSsiItem());
            }
        }
        return badItems;
    }

    public void setPrivacyMode(PrivacyMode mode) {
        DefensiveTools.checkNull(mode, "mode");

        int privacyModeCode = getPrivacyModeCode(mode);

        PrivacyItem item = getPrivacyItem();
        ItemsCmd cmd;
        if (item == null) {
            int uniqueId = ssiService
                    .getUniqueItemId(SsiItem.TYPE_PRIVACY, SsiItem.GROUP_ROOT);
            PrivacyItem ours = new PrivacyItem(uniqueId, privacyModeCode, 0);
            cmd = new CreateItemsCmd(ours.toSsiItem());
        } else {
            PrivacyItem ours = new PrivacyItem(item);
            ours.setPrivacyMode(privacyModeCode);
            cmd = new ModifyItemsCmd(ours.toSsiItem());
        }
        ssiService.sendSsiModification(cmd);
    }

    private int getPrivacyModeCode(PrivacyMode mode) {
        if (mode == PrivacyMode.ALLOW_ALL) return PrivacyItem.MODE_ALLOW_ALL;
        if (mode == PrivacyMode.ALLOW_ALLOWED) return PrivacyItem.MODE_ALLOW_PERMITS;
        if (mode == PrivacyMode.ALLOW_BUDDIES) return PrivacyItem.MODE_ALLOW_BUDDIES;
        if (mode == PrivacyMode.BLOCK_ALL) return PrivacyItem.MODE_BLOCK_ALL;
        if (mode == PrivacyMode.BLOCK_BLOCKED) return PrivacyItem.MODE_BLOCK_DENIES;
        throw new IllegalStateException("invalid privacy mode " + mode);
    }

    private void removePrivacyItemWithId(int id) {
        for (Iterator<PrivacyItem> it = privacyItems.iterator();
                it.hasNext();) {
            PrivacyItem otherItem = it.next();
            if (otherItem.getId() == id) it.remove();
        }
    }

    public void addPermissionListListener(PermissionListListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removePermissionListListener(PermissionListListener listener) {
        listeners.remove(listener);
    }

    private class AllowedBuddyChangeListener implements DetectedChangeListener<Screenname> {
        public void itemAdded(Collection<? extends Screenname> oldItems,
                Collection<? extends Screenname> newItems, Screenname item) {
            assert !Thread.holdsLock(SsiPermissionList.this);

            Set<Screenname> oldItemsCopy = DefensiveTools.getUnmodifiableSetCopy(oldItems);
            Set<Screenname> newItemsCopy = DefensiveTools.getUnmodifiableSetCopy(newItems);
            for (PermissionListListener listener : listeners) {
                listener.handleBuddyAllowed(SsiPermissionList.this, oldItemsCopy,
                        newItemsCopy, item);
            }
        }

        public void itemRemoved(Collection<? extends Screenname> oldItems,
                Collection<? extends Screenname> newItems, Screenname item) {
            assert !Thread.holdsLock(SsiPermissionList.this);

            Set<Screenname> oldItemsCopy = DefensiveTools.getUnmodifiableSetCopy(oldItems);
            Set<Screenname> newItemsCopy = DefensiveTools.getUnmodifiableSetCopy(newItems);
            for (PermissionListListener listener : listeners) {
                listener.handleBuddyRemovedFromAllowList(SsiPermissionList.this,
                        oldItemsCopy, newItemsCopy, item);
            }
        }

        public void itemsReordered(Collection<? extends Screenname> oldItems,
                Collection<? extends Screenname> newItems) {
            // who cares??
        }
    }

    private class BlockedBuddyChangeListener implements DetectedChangeListener<Screenname> {
        public void itemAdded(
                Collection<? extends Screenname> oldItems,
                Collection<? extends Screenname> newItems,
                Screenname item) {
            assert !Thread.holdsLock(SsiPermissionList.this);

            Set<Screenname> oldItemsCopy = DefensiveTools.getUnmodifiableSetCopy(oldItems);
            Set<Screenname> newItemsCopy = DefensiveTools.getUnmodifiableSetCopy(newItems);

            for (PermissionListListener listener : listeners) {
                listener.handleBuddyBlocked(SsiPermissionList.this, oldItemsCopy,
                        newItemsCopy, item);
            }
        }

        public void itemRemoved(Collection<? extends Screenname> oldItems,
                Collection<? extends Screenname> newItems,
                Screenname item) {
            assert !Thread.holdsLock(SsiPermissionList.this);

            Set<Screenname> oldItemsCopy = DefensiveTools.getUnmodifiableSetCopy(oldItems);
            Set<Screenname> newItemsCopy = DefensiveTools.getUnmodifiableSetCopy(newItems);

            for (PermissionListListener listener : listeners) {
                listener.handleBuddyUnblocked(SsiPermissionList.this, oldItemsCopy,
                        newItemsCopy, item);
            }
        }

        public void itemsReordered(Collection<? extends Screenname> oldItems,
                Collection<? extends Screenname> newItems) {
            // who cares
        }
    }
}
