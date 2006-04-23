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

import net.kano.joscar.snac.SnacRequestAdapter;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snaccmd.ssi.CreateItemsCmd;
import net.kano.joscar.snaccmd.ssi.DeleteItemsCmd;
import net.kano.joscar.snaccmd.ssi.ModifyItemsCmd;
import net.kano.joscar.snaccmd.ssi.SsiDataModResponse;
import net.kano.joscar.snaccmd.ssi.SsiItem;
import net.kano.joscar.ssiitem.BuddyItem;
import net.kano.joscar.ssiitem.GroupItem;
import net.kano.joscar.ssiitem.RootItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

class SsiBuddyList extends SimpleBuddyList implements MutableBuddyList {
    private final SsiServiceImpl service;
    private final SsiSyntheticGroup syntheticGroup;

    protected SsiBuddyList(SsiServiceImpl service) {
        this.service = service;
        syntheticGroup = new SsiSyntheticGroup(service, this);
        service.addItemChangeListener(this);
    }

    protected SyntheticGroup getSyntheticGroup() {
        return syntheticGroup;
    }

    protected SimpleBuddy createBuddy(BuddyItem buddyItem) {
        return new SsiBuddy(this, buddyItem);
    }

    protected SimpleBuddyGroup createBuddyGroup(GroupItem groupItem) {
        return new SsiBuddyGroup(this, groupItem);
    }

    public void addGroup(String name) {
        final int id = service.getUniqueGroupId();
        CreateItemsCmd cmd = new CreateItemsCmd(new GroupItem(name, id).toSsiItem());
        service.sendSsiModification(cmd, new SnacRequestAdapter() {
            public void handleResponse(SnacResponseEvent e) {
                if (e.getSnacCommand() instanceof SsiDataModResponse) {
                    SsiDataModResponse dataModResponse
                            = (SsiDataModResponse) e.getSnacCommand();
                    int result = dataModResponse.getResults()[0];
                    if (result == SsiDataModResponse.RESULT_SUCCESS) {
                        RootItem rootItem = getRootItem();
                        if (rootItem != null) {
                            addGroupToRootItem(rootItem, id);
                        }
                    }
                }
            }

            private boolean addGroupToRootItem(RootItem rootItem, int id) {
                int[] oldIds = rootItem.getGroupids();
                int[] newIds;
                if (oldIds == null) {
                    newIds = new int[1];
                } else {
                    for (int oldId : oldIds) {
                        if (oldId == id) return false;
                    }
                    newIds = new int[oldIds.length + 1];
                    System.arraycopy(oldIds, 0, newIds, 0, oldIds.length);
                }
                newIds[newIds.length-1] = id;

                RootItem newRootItem = new RootItem(rootItem);
                newRootItem.setGroupids(newIds);
                ModifyItemsCmd cmd2 = new ModifyItemsCmd(newRootItem.toSsiItem());
                service.sendSsiModification(cmd2);
                return true;
            }
        });
    }

    public void moveBuddies(Collection<? extends Buddy> buddies, AddMutableGroup group) {
        Map<Group,List<Buddy>> group2buddy = new HashMap<Group, List<Buddy>>();
        for (Buddy buddy : buddies) {
            for (Group parent : getGroups()) {
                if (parent == group) continue;

                if (parent.getBuddiesCopy().contains(buddy)) {
                    List<Buddy> ingroup = group2buddy.get(parent);
                    if (ingroup == null) {
                        ingroup = new ArrayList<Buddy>();
                        group2buddy.put(parent, ingroup);
                    }
                    ingroup.add(buddy);
                }
            }
        }


        // this list might be different than the passed buddies because some of
        // the buddies might already be in the right group
        Set<Buddy> toAdd = new LinkedHashSet<Buddy>();
        for (Map.Entry<Group, List<Buddy>> entry : group2buddy.entrySet()) {
            Group parent = entry.getKey();
            List<Buddy> ingroup = entry.getValue();
            if (parent instanceof MutableGroup) {
                MutableGroup mutableGroup = (MutableGroup) parent;
                mutableGroup.deleteBuddies(ingroup);
            }
            toAdd.addAll(ingroup);
        }
        group.copyBuddies(toAdd);
    }

    public void deleteGroupAndBuddies(Group group) {
        List<SsiItem> items = new ArrayList<SsiItem>();
        for (Buddy buddy : group.getBuddiesCopy()) {
            if (buddy instanceof SimpleBuddy) {
                SimpleBuddy simpleBuddy = (SimpleBuddy) buddy;
                items.add(simpleBuddy.getItem().toSsiItem());
            }
        }
        assert SsiTools.isOnlyBuddies(items);

        final Integer groupid;
        if (group instanceof SimpleBuddyGroup) {
            SimpleBuddyGroup buddyGroup = (SimpleBuddyGroup) group;
            SsiItem groupItem = buddyGroup.getItem().toSsiItem();
            assert groupItem.getId() == 0
                    && groupItem.getItemType() == SsiItem.TYPE_GROUP;
            items.add(groupItem);
            groupid = groupItem.getParentId();
        } else {
            groupid = null;
        }
        DeleteItemsCmd deleteCmd = new DeleteItemsCmd(items);
        service.sendSsiModification(deleteCmd, new SnacRequestAdapter() {
            public void handleResponse(SnacResponseEvent e) {
                if (e.getSnacCommand() instanceof SsiDataModResponse) {
                    SsiDataModResponse dataModResponse = (SsiDataModResponse) e.getSnacCommand();
                    int result = dataModResponse.getResults()[0];
                    if (result == SsiDataModResponse.RESULT_SUCCESS) {
                        RootItem rootItem = getRootItem();
                        if (rootItem != null && groupid != null) {
                            removeGroupFromRoot(rootItem, groupid);
                        }
                    }
                }
            }

            private boolean removeGroupFromRoot(RootItem rootItem, int groupid) {
                // figure out how long to make the new array (there might be
                // duplicate copies of groupid in the list)
                int[] oldIds = rootItem.getGroupids();
                int[] newIds;
                if (oldIds != null) {
                    int total = 0;
                    for (int oldId : oldIds) {
                        if (oldId == groupid) total++;
                    }
                    if (total == 0) return false;

                    // copy the ID's over
                    newIds = new int[oldIds.length - total];
                    int i = 0;
                    for (int id : oldIds) {
                        if (id == groupid) continue;
                        newIds[i] = id;
                        i++;
                    }
                    assert i == newIds.length;

                } else {
                    newIds = new int[0];
                }

                // modify the root item
                RootItem newRootItem = new RootItem(rootItem);
                newRootItem.setGroupids(newIds);
                service.sendSsiModification(
                        new ModifyItemsCmd(newRootItem.toSsiItem()));
                return true;
            }
        });
    }

    public SsiServiceImpl getSsiService() {
        return service;
    }
}