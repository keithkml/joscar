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
import net.kano.joscar.snaccmd.ssi.ModifyItemsCmd;

class SsiBuddy extends SimpleBuddy implements MutableBuddy {
    public SsiBuddy(SsiBuddyList list, BuddyItem item) {
        super(list, item);
    }

    public void changeAlias(String alias) {
        BuddyItem item = getItemCopy();
        item.setAlias(alias);
        sendItemModification(item);
    }

    public void changeBuddyComment(String comment) {
        BuddyItem item = getItemCopy();
        item.setComment(comment);
        sendItemModification(item);
    }

    public void changeAlertEventMask(int alertEventMask) {
        BuddyItem item = getItemCopy();
        item.setAlertWhenMask(alertEventMask);
        sendItemModification(item);
    }

    public void changeAlertActionMask(int alertActionMask) {
        BuddyItem item = getItemCopy();
        item.setAlertActionMask(alertActionMask);
        sendItemModification(item);
    }

    public void changeAlertSound(String alertSound) {
        BuddyItem item = getItemCopy();
        item.setAlertSound(alertSound);
        sendItemModification(item);
    }

    private BuddyItem getItemCopy() {
        return new BuddyItem(getItem());
    }

    private void sendItemModification(BuddyItem item) {
        SsiService ssiService = getBuddyList().getSsiService();
        ssiService.sendSsiModification(new ModifyItemsCmd(item.toSsiItem()));
    }

    public SsiBuddyList getBuddyList() {
        return (SsiBuddyList) super.getBuddyList();
    }
}
