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

import net.kano.joscar.ssiitem.GroupItem;
import net.kano.joscar.DefensiveTools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SimpleBuddyGroup extends AbstractGroup {
    private GroupItem item;

    private final int itemId;
    private boolean active = true;

    public SimpleBuddyGroup(SimpleBuddyList list, GroupItem item) {
        super(list);

        itemId = item.getId();
        setItem(item);
    }

    public void setItem(GroupItem item) {
        DefensiveTools.checkNull(item, "item");

        synchronized(getBuddyListLock()) {
            GroupItem oldItem = this.item;
            if (item == oldItem) return;

            if (item.getId() != itemId) {
                throw new IllegalArgumentException("item ID " + item.getId()
                        + " does not match this group's ID " + itemId);
            }

            this.item = item;
        }
    }

    public GroupItem getItem() {
        synchronized(getBuddyListLock()) {
            return item;
        }
    }

    protected List<SimpleBuddy> getSortedBuddies() {
        Map<Integer,SimpleBuddy> id2buddy = new HashMap<Integer, SimpleBuddy>();
        for (SimpleBuddy buddy : getBuddies()) {
            id2buddy.put(buddy.getItem().getId(), buddy);
        }

        List<SimpleBuddy> newBuddies = new ArrayList<SimpleBuddy>();
        GroupItem item = getItem();
        int[] buddies = item.getBuddies();
        if (buddies != null) {
            for (int buddyId : buddies) {
                SimpleBuddy buddy = id2buddy.remove(buddyId);
                if (buddy == null) continue;
                newBuddies.add(buddy);
            }
        }
        List<SimpleBuddy> leftover = new ArrayList<SimpleBuddy>(id2buddy.values());
        Collections.sort(leftover, SimpleBuddyList.COMPARATOR_SN);
        newBuddies.addAll(leftover);
        return newBuddies;
    }

    protected boolean isGroupValid() {
        if (!super.isGroupValid()) return false;

        for (SimpleBuddy buddy : getBuddies()) {
            if (buddy.getItem().getGroupId() != itemId) return false;
        }
        return true;
    }

    public String getName() {
        return getItem().getGroupName();
    }

    public boolean isActive() {
        synchronized(getBuddyListLock()) {
            return active;
        }
    }

    public void setActive(boolean active) {
        synchronized(getBuddyListLock()) {
            this.active = active;
        }
    }
}