/*
 *  Copyright (c) 2004, The Joust Project
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
 *  File created by keith @ Feb 3, 2004
 *
 */

package net.kano.aimcrypto.forms;

import javax.swing.AbstractListModel;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Collection;
import java.util.Iterator;

public class SortedListModel extends AbstractListModel {
    private final List list = new ArrayList();
    private final Comparator comparator;

    public SortedListModel(Comparator comparator) {
        this.comparator = comparator;
    }

    public boolean addIfAbsent(Object obj) {
        int pos = Collections.binarySearch(list, obj, comparator);
        if (pos >= 0) return false;

        int toinsert = -(pos + 1);
        list.add(toinsert, obj);

        fireIntervalAdded(this, toinsert, toinsert);
        return true;
    }

    public boolean remove(Object obj) {
        int pos = Collections.binarySearch(list, obj, comparator);
        if (pos < 0) return false;
        list.remove(pos);
        fireIntervalRemoved(this, pos, pos);
        return true;
    }

    public void addAllIfAbsent(Collection objs) {
        if (list.isEmpty()) {
            if (objs.isEmpty()) return;
            list.addAll(objs);
            Collections.sort(list, comparator);
            fireIntervalAdded(this, 0, objs.size()-1);
        } else {
            for (Iterator it = objs.iterator(); it.hasNext();) {
                addIfAbsent(it.next());
            }
        }
    }

    public int getSize() {
        return list.size();
    }

    public Object getElementAt(int index) {
        return list.get(index);
    }

    public void replaceContents(List objs) {
        clear();
        addAllIfAbsent(objs);
    }

    public void clear() {
        int last = list.size() - 1;
        if (last == -1) return;
        list.clear();
        fireIntervalRemoved(this, 0, last);
    }
}
