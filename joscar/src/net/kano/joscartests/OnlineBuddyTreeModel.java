/*
 *  Copyright (c) 2002-2003, The Joust Project
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
 *  File created by keith @ Apr 12, 2003
 *
 */

package net.kano.joscartests;

import net.kano.joscar.OscarTools;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class OnlineBuddyTreeModel extends DefaultTreeModel {
    private JoscarTester tester;
    private LinkedHashMap onlineBuddies = new LinkedHashMap();

    private UserInfoListener userInfoListener = new UserInfoListener() {
        public void userOnline(String sn, OnlineUserInfo info) {
            register(info);
            fireTreeNodesInserted(getRoot(), new Object[] { getRoot() },
                new int[] { onlineBuddies.size() - 1 }, new Object[] { info });
        }

        public void userOffline(String sn) {
            onlineBuddies.remove(sn);
        }
    };

    public OnlineBuddyTreeModel(JoscarTester tester) {
        super(new DefaultMutableTreeNode());
        this.tester = tester;
        tester.addUserInfoListener(userInfoListener);
        initUsers();
    }

    private void initUsers() {
        OnlineUserInfo[] users = tester.getOnlineUsers();

        int[] indices = new int[users.length];
        for (int i = 0; i < users.length; i++) {
            onlineBuddies.put(users[i].getScreenname(), users[i]);
            register(users[i]);
            indices[i] = i;
        }

        fireTreeNodesInserted(getRoot(), new Object[] { getRoot() },
                indices, users);
    }

    private PropertyChangeListener pclistener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            OnlineUserInfo info = (OnlineUserInfo) evt.getSource();

            Object source = getRoot();
            Object[] path = new Object[] { getRoot() };
            int[] indices = new int[] { getIndexOfChild(getRoot(), info) };
            Object[] objs = new Object[] { info };

            if (!info.isOnline()) {
                unregister(info);
                fireTreeNodesRemoved(source, path, indices, objs);
            } else {
                fireTreeNodesChanged(source, path, indices, objs);
            }
        }
    };

    private synchronized void register(OnlineUserInfo info) {
        info.addPropertyChangeListener(pclistener);
        onlineBuddies.put(OscarTools.normalize(info.getScreenname()), info);
    }

    private synchronized void unregister(OnlineUserInfo info) {
        info.removePropertyChangeListener(pclistener);
        onlineBuddies.remove(OscarTools.normalize(info.getScreenname()));
    }

    public synchronized int getIndexOfChild(Object parent, Object child) {
        if (parent == null || child == null) return -1;

        Collection values = onlineBuddies.values();
        int i = 0;
        for (Iterator it = values.iterator(); it.hasNext(); i++) {
            if (it.next() == child) return i;
        }
        return -1;
    }

    public synchronized Object getChild(Object parent, int index) {
        return new ArrayList(onlineBuddies.values()).get(index);
    }

    public synchronized int getChildCount(Object parent) {
        return onlineBuddies.size();
    }

    public synchronized boolean isLeaf(Object node) {
        return node != getRoot();
    }
}
