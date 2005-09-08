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

import net.kano.joscar.snaccmd.ssi.SsiItem;
import net.kano.joscar.ssiitem.BuddyItem;
import net.kano.joscar.ssiitem.SsiItemObjectWithId;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public final class SsiTools {
    private SsiTools() { }

    public static List<SsiItem> getBuddiesToDelete(List<Buddy> ingroup) {
        List<SsiItem> items = new ArrayList<SsiItem>();
        for (Buddy buddy : ingroup) {
            if (!(buddy instanceof SimpleBuddy)) {
                throw new IllegalArgumentException("can't delete buddy " + buddy
                        + " : wrong type");
            }
            SimpleBuddy simpleBuddy = (SimpleBuddy) buddy;
            BuddyItem item = simpleBuddy.getItem();
            SsiItem ssiItem = item.toSsiItem();
            items.add(ssiItem);
        }
        return items;
    }

    public static List<Integer> getIdsForItems(List<SsiItem> items) {
        List<Integer> ids = new ArrayList<Integer>();
        for (SsiItem ssiItem : items) ids.add(ssiItem.getId());
        return ids;
    }

    public static <E extends SsiItemObjectWithId> void removeItemsWithId(Collection<E> items,
            int id) {
        for (Iterator<E> it = items.iterator();
                it.hasNext();) {
            SsiItemObjectWithId otherItem = it.next();
            if (otherItem.getId() == id) it.remove();
        }
    }

    public static boolean isOnlyBuddies(List<SsiItem> items) {
        for (SsiItem item : items) {
            if (item.getItemType() != SsiItem.TYPE_BUDDY) return false;
        }
        return true;
    }
}
