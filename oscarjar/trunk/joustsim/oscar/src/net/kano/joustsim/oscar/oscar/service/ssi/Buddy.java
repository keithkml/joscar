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

import net.kano.joscar.ssiitem.BuddyItem;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joustsim.Screenname;

//TODO: item objects are mutable. we shouldn't make them public, or we should clone them
public class Buddy {
    private final SimpleBuddyList buddyList;

    private CopyOnWriteArrayList<BuddyListener> listeners
            = new CopyOnWriteArrayList<BuddyListener>();

    private final int itemId;
    private BuddyItem item;
    private boolean active = true;

    private Screenname screenname;
    private String alias;
    private String buddyComment;

    private int alertActionMask;
    private String alertSound;
    private int alertEventMask;

    public Buddy(SimpleBuddyList list, BuddyItem item) {
        this.buddyList = list;

        this.itemId = item.getId();
        setItem(item);
    }

    public BuddyItem getItem() {
        synchronized(getBuddyListLock()) {
            return item;
        }
    }

    public void setItem(BuddyItem item) {
        synchronized(getBuddyListLock()) {
            if (item.getId() != itemId) {
                throw new IllegalArgumentException("item " + item + " does not "
                        + "match ID " + itemId);
            }
            this.item = item;
            screenname = new Screenname(item.getScreenname());
            alias = item.getAlias();
            alertActionMask = item.getAlertActionMask();
            alertSound = item.getAlertSound();
            alertEventMask = item.getAlertWhenMask();
            buddyComment = item.getBuddyComment();
        }
    }

    protected BuddyState saveState() {
        synchronized(getBuddyListLock()) {
            return new BuddyState();
        }
    }

    public void detectChanges(BuddyState oldState, BuddyState newState) {
        Screenname oldSn = oldState.getScreenname();
        Screenname newSn = newState.getScreenname();
        if (!Group.areEqual(oldSn.getFormatted(), newSn.getFormatted())) {
            for (BuddyListener listener : listeners) {
                listener.screennameChanged(this, oldSn, newSn);
            }
        }
        String oldAlias = oldState.getAlias();
        String newAlias = newState.getAlias();
        if (!Group.areEqual(oldAlias, newAlias)) {
            for (BuddyListener listener : listeners) {
                listener.aliasChanged(this, oldAlias, newAlias);
            }
        }
        String oldComment = oldState.getBuddyComment();
        String newComment = newState.getBuddyComment();
        if (!Group.areEqual(oldComment, newComment)) {
            for (BuddyListener listener : listeners) {
                listener.buddyCommentChanged(this, oldComment, newComment);
            }
        }
        int oldAlertAction = oldState.getAlertActionMask();
        int newAlertAction = newState.getAlertActionMask();
        if (oldAlertAction != newAlertAction) {
            for (BuddyListener listener : listeners) {
                listener.alertActionChanged(this, oldAlertAction, newAlertAction);
            }
        }
        String oldAlertSound = oldState.getAlertSound();
        String newAlertSound = newState.getAlertSound();
        if (!Group.areEqual(oldAlertSound, newAlertSound)) {
            for (BuddyListener listener : listeners) {
                listener.alertSoundChanged(this, oldAlertSound, newAlertSound);
            }
        }
        int oldAlertEvent = oldState.getAlertEventMask();
        int newAlertEvent = newState.getAlertEventMask();
        if (oldAlertEvent != newAlertEvent) {
            for (BuddyListener listener : listeners) {
                listener.alertTimeChanged(this, oldAlertEvent, newAlertEvent);
            }
        }
    }

    protected class BuddyState {
        private Screenname screenname;
        private String alias;
        private String buddyComment;

        private int alertActionMask;
        private String alertSound;
        private int alertEventMask;

        public BuddyState() {
            this.screenname = Buddy.this.screenname;
            this.alias = Buddy.this.alias;
            this.buddyComment = Buddy.this.buddyComment;
            this.alertActionMask = Buddy.this.alertActionMask;
            this.alertSound = Buddy.this.alertSound;
            this.alertEventMask = Buddy.this.alertEventMask;
        }

        public Screenname getScreenname() {
            return screenname;
        }

        public String getAlias() {
            return alias;
        }

        public String getBuddyComment() {
            return buddyComment;
        }

        public int getAlertActionMask() {
            return alertActionMask;
        }

        public String getAlertSound() {
            return alertSound;
        }

        public int getAlertEventMask() {
            return alertEventMask;
        }
    }

    private Object getBuddyListLock() {
        return buddyList.getLock();
    }

    public void setActive(boolean active) {
        synchronized(getBuddyListLock()) {
            this.active = active;
        }
    }

    public boolean isActive() {
        synchronized(getBuddyListLock()) {
            return active;
        }
    }

    public Screenname getScreenname() {
        synchronized(getBuddyListLock()) {
            return screenname;
        }
    }

    public String getAlias() {
        synchronized(getBuddyListLock()) {
            return alias;
        }
    }

    public int getAlertActionMask() {
        synchronized(getBuddyListLock()) {
            return alertActionMask;
        }
    }

    public String getAlertSound() {
        synchronized(getBuddyListLock()) {
            return alertSound;
        }
    }

    public int getAlertEventMask() {
        synchronized(getBuddyListLock()) {
            return alertEventMask;
        }
    }

    public String getBuddyComment() {
        synchronized(getBuddyListLock()) {
            return buddyComment;
        }
    }

    public String toString() {
        return "Buddy " + getScreenname() + " (alias " + getAlias() + ")";
    }
}
