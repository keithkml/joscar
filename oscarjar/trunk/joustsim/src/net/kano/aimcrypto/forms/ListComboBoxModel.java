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
 *  File created by keith @ Jan 31, 2004
 *
 */

package net.kano.aimcrypto.forms;

import javax.swing.AbstractListModel;
import javax.swing.MutableComboBoxModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

public class ListComboBoxModel
        extends AbstractListModel implements MutableComboBoxModel {
    private final List list = new ArrayList();
    private Object selected = null;

    public int getSize() {
        return list.size();
    }

    public void addAll(Collection c) {
        if (c.isEmpty()) return;
        
        int start = list.size();
        list.addAll(c);
        fireIntervalAdded(this, start, list.size()-1);
    }

    public Object getElementAt(int index) {
        if (index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    public void removeElementAt(int index) {
        if (index < 0 || index >= list.size()) return;
        list.remove(index);
        fireIntervalRemoved(this, index, index);
    }

    public void addElement(Object obj) {
        list.add(obj);
        int index = list.size()-1;
        fireIntervalAdded(this, index, index);
    }

    public void removeElement(Object obj) {
        int index = list.indexOf(obj);
        if (index != -1) {
            list.remove(index);
            fireIntervalRemoved(this, index, index);
        }
    }

    public void insertElementAt(Object obj, int index) {
        list.add(index, obj);
        fireIntervalAdded(this, index, index);
    }

    public Object getSelectedItem() {
        return selected;
    }

    public void setSelectedItem(Object anObject) {
        if ((selected != null && !selected.equals(anObject)) ||
                selected == null && anObject != null) {
            selected = anObject;
            fireContentsChanged(this, -1, -1);
        }
    }

    public void clear() {
        int last = list.size()-1;
        if (last == -1) return;
        list.clear();
        fireIntervalRemoved(this, 0, last);
    }
}
