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

import net.kano.joscar.DefensiveTools;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;

abstract class AbstractGroup implements Group {
    private final SimpleBuddyList buddyList;
    private CopyOnWriteArrayList<GroupListener> listeners
            = new CopyOnWriteArrayList<GroupListener>();

    private List<SimpleBuddy> buddies = new ArrayList<SimpleBuddy>();

    AbstractGroup(SimpleBuddyList buddyList) {
        this.buddyList = buddyList;
    }

    void sortBuddies() {
        buddies = getSortedBuddies();
    }

    protected abstract List<SimpleBuddy> getSortedBuddies();

    protected List<SimpleBuddy> getBuddies() {
        return buddies;
    }

    public List<SimpleBuddy> getBuddiesCopy() {
        synchronized(getBuddyListLock()) {
            return DefensiveTools.getUnmodifiableCopy(getBuddies());
        }
    }

    protected Object getBuddyListLock() {
        return buddyList.getLock();
    }

    protected BuddyList getBuddyList() {
        return buddyList;
    }

    void addBuddies(Collection<? extends SimpleBuddy> buddies) {
        this.buddies.addAll(buddies);
        assert isGroupValid();
    }

    void removeBuddies(Collection<? extends SimpleBuddy> buddies) {
        this.buddies.removeAll(buddies);
        assert isGroupValid();
    }

    void addBuddy(SimpleBuddy buddy) {
        buddies.add(buddy);
        assert isGroupValid();
    }

    protected boolean isGroupValid() {
        for (SimpleBuddy buddy : buddies) {
            if (buddy == null) return false;
        }
        Set<SimpleBuddy> set = new HashSet<SimpleBuddy>(buddies);
        if (set.size() != buddies.size()) return false;
        return true;
    }

    SimpleBuddy getBuddy(int id) {
        for (SimpleBuddy buddy : getBuddies()) {
            if (buddy.getItem().getId() == id) return buddy;
        }
        return null;
    }

    void removeBuddy(Buddy buddy) {
        boolean removed = buddies.remove(buddy);
        if (!removed) {
            throw new IllegalArgumentException("buddy " + buddy + " is not "
                    + "in group " + this);
        }
    }

    protected CopyOnWriteArrayList<GroupListener> getListeners() {
        return listeners;
    }

    public void addGroupListener(GroupListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeGroupListener(GroupListener listener) {
        listeners.remove(listener);
    }

    protected GroupState saveState() {
        return new GroupState();
    }

    protected void detectChanges(GroupState oldState, GroupState newState) {
        assert !Thread.holdsLock(this);

        String oldName = oldState.getName();
        String newName = newState.getName();
        if (ChangeTools.areEqual(oldName, newName)) {
            for (GroupListener listener : getListeners()) {
                listener.groupNameChanged(this, oldName, newName);
            }
        }
    }

    public String toString() {
        return "Group " + getName() + ": " + buddies.size() + " buddies";
    }

    protected class GroupState {
        private final String name;
        protected GroupState() {
            name = getName();
        }

        public String getName() {
            return name;
        }
    }
}
