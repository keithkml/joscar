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
import net.kano.joscar.snaccmd.ssi.SsiItem;
import net.kano.joscar.ssiitem.DefaultSsiItemObjFactory;
import net.kano.joscar.ssiitem.DenyItem;
import net.kano.joscar.ssiitem.PrivacyItem;
import net.kano.joscar.ssiitem.SsiItemObj;
import net.kano.joscar.ssiitem.SsiItemObjectFactory;
import net.kano.joustsim.Screenname;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Collection;
import java.util.HashSet;

public class PermissionList {
    private SsiItemObjectFactory factory = new DefaultSsiItemObjFactory();
    private SortedSet<PrivacyItem> privacyItems
            = new TreeSet<PrivacyItem>(new ItemIdComparator());
    private CopyOnWriteArrayList<PermissionListListener> listeners
            = new CopyOnWriteArrayList<PermissionListListener>();
    private List<DenyItem> denyItems = new ArrayList<DenyItem>();

    public void handleItemCreated(SsiItem item) {
        handleItemObjCreated(factory.getItemObj(item));
    }

    private void handleItemObjCreated(SsiItemObj item) {
        if (item instanceof PrivacyItem) {
            PrivacyItem pitem = (PrivacyItem) item;
            PrivacyItem old;
            PrivacyItem newTopItem;
            synchronized (this) {
                old = getPrivacyItem();
                privacyItems.add(pitem);
                newTopItem = getPrivacyItem();
            }
            updatePrivacyItem(old, newTopItem);

        } else if (item instanceof DenyItem) {
            DenyItem denyItem = (DenyItem) item;
            Set<Screenname> oldBlocked;
            Set<Screenname> newBlocked;
            synchronized (this) {
                oldBlocked = getBlockedBuddies();
                denyItems.add(denyItem);
                newBlocked = getBlockedBuddies();
            }
            updateBlockedBuddies(oldBlocked, newBlocked);
        }
    }

    public void handleItemModified(SsiItem item) {
        SsiItemObj itemObj = factory.getItemObj(item);
        if (itemObj instanceof PrivacyItem) {
            PrivacyItem pitem = (PrivacyItem) itemObj;
            PrivacyItem oldItem;
            PrivacyItem newTopItem;
            synchronized (this) {
                oldItem = getPrivacyItem();
                removePrivacyItemWithId(pitem.getId());
                privacyItems.add(pitem);
                newTopItem = getPrivacyItem();
            }
            updatePrivacyItem(oldItem, newTopItem);

        } else if (itemObj instanceof DenyItem) {
            DenyItem denyItem = (DenyItem) itemObj;
            Set<Screenname> oldBlocked;
            Set<Screenname> newBlocked;
            synchronized (this) {
                oldBlocked = getBlockedBuddies();
                removeDenyItemWithId(denyItem.getId());
                denyItems.add(denyItem);
                newBlocked = getBlockedBuddies();
            }
            updateBlockedBuddies(oldBlocked, newBlocked);
        }
    }

    private void removeDenyItemWithId(int id) {
        for (Iterator<DenyItem> it = denyItems.iterator();
                it.hasNext();) {
            DenyItem denyItem = it.next();
            if (denyItem.getId() == id) it.remove();
        }
    }

    public void handleItemDeleted(SsiItem item) {
        SsiItemObj itemObj = factory.getItemObj(item);
        if (itemObj instanceof PrivacyItem) {
            PrivacyItem privacyItem = (PrivacyItem) itemObj;
            PrivacyItem oldItem;
            PrivacyItem newItem;
            synchronized (this) {
                oldItem = getPrivacyItem();
                removePrivacyItemWithId(privacyItem.getId());
                newItem = getPrivacyItem();
            }
            updatePrivacyItem(oldItem, newItem);
        } else if (itemObj instanceof DenyItem) {
            DenyItem denyItem = (DenyItem) itemObj;

            Set<Screenname> oldBlocked;
            Set<Screenname> newBlocked;
            synchronized (this) {
                oldBlocked = getBlockedBuddies();
                removeDenyItemWithId(denyItem.getId());
                newBlocked = getBlockedBuddies();
            }
            updateBlockedBuddies(oldBlocked, newBlocked);
        }
    }

    private void updateBlockedBuddies(Set<Screenname> oldBlocked,
            Set<Screenname> newBlocked) {
        ChangeTools.detectChanges(oldBlocked, newBlocked,
                new DetectedChangeListener<Screenname>() {
            public void itemAdded(Collection<? extends Screenname> oldItems,
                    Collection<? extends Screenname> newItems, Screenname item) {
                fireBuddyBlocked(item);
            }

            public void itemRemoved(Collection<? extends Screenname> oldItems,
                    Collection<? extends Screenname> newItems, Screenname item) {
                fireBuddyUnblocked(item);
            }

            public void itemsReordered(Collection<? extends Screenname> oldItems,
                    Collection<? extends Screenname> newItems) {
                // who cares
            }
        });
    }

    private void fireBuddyBlocked(Screenname item) {
        assert !Thread.holdsLock(this);

        for (PermissionListListener listener : listeners) {
            listener.handleBuddyBlocked(PermissionList.this, item);
        }
    }

    private void fireBuddyUnblocked(Screenname item) {
        assert !Thread.holdsLock(this);

        for (PermissionListListener listener : listeners) {
            listener.handleBuddyUnblocked(PermissionList.this, item);
        }
    }

    public synchronized @Nullable PrivacyMode getPrivacyMode() {
        return getPrivacyModeFromItem(getPrivacyItem());
    }

    private synchronized @Nullable PrivacyItem getPrivacyItem() {
        return privacyItems.isEmpty() ? null : privacyItems.last();
    }

    private void updatePrivacyItem(@Nullable PrivacyItem oldItem,
            @Nullable PrivacyItem newItem) {
        PrivacyMode oldMode = getPrivacyModeFromItem(oldItem);
        PrivacyMode newMode = getPrivacyModeFromItem(newItem);
        if (oldMode != newMode) firePrivacyModeChanged(oldMode, newMode);
    }

    private PrivacyMode getPrivacyModeFromItem(@Nullable PrivacyItem item) {
        return item == null ? null : getPrivacyModeFromCode(
                item.getPrivacyMode());
    }

    private void firePrivacyModeChanged(PrivacyMode oldMode,
            PrivacyMode newMode) {
        assert !Thread.holdsLock(this);

        for (PermissionListListener listener : listeners) {
            listener.handlePrivacyModeChange(this, oldMode, newMode);
        }
    }

    private PrivacyMode getPrivacyModeFromCode(int code) {
        if (code == PrivacyItem.MODE_ALLOW_ALL) return PrivacyMode.ALLOW_ALL;
        else if (code == PrivacyItem.MODE_ALLOW_PERMITS) return PrivacyMode.ALLOW_ALLOWED;
        else if (code == PrivacyItem.MODE_BLOCK_ALL) return PrivacyMode.BLOCK_ALL;
        else if (code == PrivacyItem.MODE_BLOCK_DENIES) return PrivacyMode.BLOCK_BLOCKED;
        else if (code == PrivacyItem.MODE_ALLOW_BUDDIES) return PrivacyMode.ALLOW_BUDDIES;
        else return null;
    }

    public synchronized Set<Screenname> getBlockedBuddies() {
        Set<Screenname> blocked = new HashSet<Screenname>();
        for (DenyItem denyItem : denyItems) {
            blocked.add(new Screenname(denyItem.getScreenname()));
        }
        return blocked;
    }

    public Set<Screenname> getAllowedBuddies() { return null; }


    private void removePrivacyItemWithId(int id) {
        for (Iterator<PrivacyItem> it = privacyItems.iterator(); it.hasNext();) {
            PrivacyItem otherItem = it.next();
            if (otherItem.getId() == id) it.remove();
        }
    }

    public enum PrivacyMode {
        ALLOW_ALL,
        ALLOW_ALLOWED,
        BLOCK_ALL,
        BLOCK_BLOCKED,
        ALLOW_BUDDIES
    }

    private static class ItemIdComparator implements Comparator<PrivacyItem> {
        public int compare(PrivacyItem o1, PrivacyItem o2) {
            int id2 = o2.getId();
            int id1 = o1.getId();
            if (id1 < id2) return -1;
            if (id1 > id2) return 1;
            return 0;
        }
    }
}
