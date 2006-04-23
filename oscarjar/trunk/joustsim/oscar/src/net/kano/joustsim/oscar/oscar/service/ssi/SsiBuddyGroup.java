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

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

class SsiBuddyGroup extends SimpleBuddyGroup implements MutableGroup {
    private static final Logger LOGGER = Logger
            .getLogger(SsiBuddyGroup.class.getName());

    public SsiBuddyGroup(SsiBuddyList list, GroupItem item) {
        super(list, item);
    }

    protected SsiBuddyList getBuddyList() {
        return (SsiBuddyList) super.getBuddyList();
    }

    public void rename(String newName) {
        GroupItem item = new GroupItem(getItem());
        item.setGroupName(newName);
        SsiServiceImpl ssiService = getSsiService();
        ssiService.sendSsiModification(new ModifyItemsCmd(item.toSsiItem()));
    }

    public void addBuddy(String screenname) {
        SsiServiceImpl service = getSsiService();
        int parentid = getItem().getId();
        int id = service.getUniqueItemId(SsiItem.TYPE_BUDDY, parentid);
        addBuddies(Arrays.asList(new BuddyItem(screenname,
                parentid, id).toSsiItem()));
    }

    public void copyBuddies(Collection<? extends Buddy> buddies) {
        SsiServiceImpl service = getSsiService();
        int parentid = getItem().getId();
        List<SsiItem> items = new ArrayList<SsiItem>();
        List<Integer> ids = new ArrayList<Integer>();
        for (Buddy buddy : buddies) {
            int id;
            do {
                id = service.getUniqueItemId(SsiItem.TYPE_BUDDY, parentid);
            } while (ids.contains(id));
            ids.add(id);

            BuddyItem item;
            if (buddy instanceof SimpleBuddy) {
                SimpleBuddy simpleBuddy = (SimpleBuddy) buddy;
                item = new BuddyItem(simpleBuddy.getItem());
                item.setGroupid(parentid);
                item.setId(id);
            } else {
                item = new BuddyItem(buddy.getScreenname().getFormatted(),
                        parentid, id, buddy.getAlias(), buddy.getBuddyComment(),
                        buddy.getAlertEventMask(), buddy.getAlertActionMask(),
                        buddy.getAlertSound());
            }
            items.add(item.toSsiItem());
        }
        addBuddies(items);
    }

    private void addBuddies(List<SsiItem> items) {
        SsiServiceImpl service = getSsiService();
        final List<Integer> ids = SsiTools.getIdsForItems(items);
        LOGGER.fine("Adding buddies " + items + " - ID's: " + ids);
        CreateItemsCmd cmd = new CreateItemsCmd(items);
        service.sendSsiModification(cmd, new SnacRequestAdapter() {
            public void handleResponse(SnacResponseEvent e) {
                if (e.getSnacCommand() instanceof SsiDataModResponse) {
                    SsiDataModResponse dataModResponse
                            = (SsiDataModResponse) e.getSnacCommand();
                    int[] results = dataModResponse.getResults();
                    if (results.length != 1) {
                        LOGGER.warning("Got multiple results for addBuddies: "
                                + results);
                    }
                    int result = results[0];
                    LOGGER.fine("Got SSI response: " + result);
                    if (result == SsiDataModResponse.RESULT_SUCCESS) {
                        addBuddiesToGroup(ids);
                    }
                }
            }

            private boolean addBuddiesToGroup(List<Integer> toAdd) {
                GroupItem item = getItem();
                LOGGER.fine("Adding buddies to group " + item + ": " + toAdd);
                List<Integer> result = new ArrayList<Integer>();
                int[] oldIds = item.getBuddies();
                if (oldIds != null) {
                    for (int id : oldIds) result.add(id);
                }
                assert oldIds == null || result.size() == oldIds.length;
                LOGGER.finer("Old ID list: " + oldIds);

                // add all toAdd ID's which are not already in result
                List<Integer> ids = new ArrayList<Integer>(toAdd);
                ids.removeAll(result);
                LOGGER.finer("Actually adding " + ids);
                result.addAll(ids);

                int[] newIds = new int[result.size()];
                int i = 0;
                for (int id : result) {
                    newIds[i] = id;
                    i++;
                }
                assert i == newIds.length;

                GroupItem newGroupItem = new GroupItem(item);
                newGroupItem.setBuddies(newIds);
                LOGGER.fine("New group item: " + newGroupItem);
                ModifyItemsCmd cmd = new ModifyItemsCmd(newGroupItem.toSsiItem());
                getSsiService().sendSsiModification(cmd);
                return true;
            }
        });
    }

    private SsiServiceImpl getSsiService() {
        return getBuddyList().getSsiService();
    }

    public void deleteBuddy(Buddy buddy) {
        deleteBuddies(Arrays.asList(buddy));
    }

    public void deleteBuddies(List<Buddy> ingroup) {
        List<SsiItem> items = SsiTools.getBuddiesToDelete(ingroup);
        final List<Integer> ids = SsiTools.getIdsForItems(items);
        assert SsiTools.isOnlyBuddies(items);

        DeleteItemsCmd deleteCmd = new DeleteItemsCmd(items);
        SsiServiceImpl service = getSsiService();
        service.sendSsiModification(deleteCmd, new SnacRequestAdapter() {
            public void handleResponse(SnacResponseEvent e) {
                if (e.getSnacCommand() instanceof SsiDataModResponse) {
                    SsiDataModResponse dataModResponse = (SsiDataModResponse) e.getSnacCommand();
                    int result = dataModResponse.getResults()[0];
                    if (result == SsiDataModResponse.RESULT_SUCCESS) {
                        removeItemsFromGroup(ids);
                    }
                }
            }

            private boolean removeItemsFromGroup(List<Integer> removeIds) {
                // figure out how long to make the new array (there might be
                // duplicate copies of groupid in the list)
                GroupItem groupItem = getItem();
                int[] oldIds = groupItem.getBuddies();
                List<Integer> result = new ArrayList<Integer>();
                if (oldIds == null) return false;

                for (int id : oldIds) result.add(id);
                result.removeAll(removeIds);

                int[] newIds = new int[result.size()];
                int i = 0;
                for (int val : result) {
                    newIds[i] = val;
                    i++;
                }
                assert i == newIds.length;

                // modify the root item
                GroupItem newGroupItem = new GroupItem(groupItem);
                newGroupItem.setBuddies(newIds);
                SsiServiceImpl service = getSsiService();
                service.sendSsiModification(new ModifyItemsCmd(newGroupItem.toSsiItem()));
                return true;
            }
        });
    }

}