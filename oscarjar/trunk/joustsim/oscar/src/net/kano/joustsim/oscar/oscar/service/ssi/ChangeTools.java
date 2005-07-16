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

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

public final class ChangeTools {
    private ChangeTools() { }


    public static <E> void detectChanges(Collection<? extends E> oldItems,
            Collection<? extends E> newItems, DetectedChangeListener<? super E> listener) {
//        List<Group> oldGroups = new ArrayList<Group>(
//                saved.getBuddies().keySet());
//        List<Group> newGroups = groups;
        if (oldItems.equals(newItems)) return;
        if (!oldItems.containsAll(newItems)) {
            // there are some added groups
            List<E> addedGroups = new ArrayList<E>(
                    newItems);
            addedGroups.removeAll(oldItems);
            assert !addedGroups.isEmpty();

            for (E group : addedGroups) {
                listener.itemAdded(oldItems, newItems, group);
            }
        }
        if (!newItems.containsAll(oldItems)) {
            // some groups were removed
            List<E> removedGroups = new ArrayList<E>(
                    oldItems);
            removedGroups.removeAll(newItems);
            assert !removedGroups.isEmpty();

            for (E group : removedGroups) {
                listener.itemRemoved(oldItems, newItems, group);
            }
        }

        List<E> oldIntersection = new ArrayList<E>(oldItems);
        oldIntersection.retainAll(newItems);
        List<E> newIntersection = new ArrayList<E>(newItems);
        newIntersection.retainAll(oldItems);
        if (!oldIntersection.equals(newIntersection)) {
            // some buddies were re-ordered
            listener.itemsReordered(oldItems, newItems);
        }
    }

    public static boolean areEqual(Object first, Object newName) {
        return first == null ? newName == null : first.equals(newName);
    }
}