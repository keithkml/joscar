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
 *  File created by keith @ Feb 6, 2004
 *
 */

package net.kano.joustsim.oscar.oscar.service.ssi;

import net.kano.joscar.MiscTools;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snac.SnacRequestListener;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snac.SnacResponseListener;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joscar.snaccmd.ssi.ActivateSsiCmd;
import net.kano.joscar.snaccmd.ssi.CreateItemsCmd;
import net.kano.joscar.snaccmd.ssi.DeleteItemsCmd;
import net.kano.joscar.snaccmd.ssi.ItemsCmd;
import net.kano.joscar.snaccmd.ssi.ModifyItemsCmd;
import net.kano.joscar.snaccmd.ssi.SsiCommand;
import net.kano.joscar.snaccmd.ssi.SsiDataCmd;
import net.kano.joscar.snaccmd.ssi.SsiDataModResponse;
import net.kano.joscar.snaccmd.ssi.SsiDataRequest;
import net.kano.joscar.snaccmd.ssi.SsiItem;
import net.kano.joscar.snaccmd.ssi.SsiRightsRequest;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SsiService extends Service {
    private BuddyList buddyList = new SsiBuddyList(this);

    public SsiService(AimConnection aimConnection,
            OscarConnection oscarConnection) {
        super(aimConnection, oscarConnection, SsiCommand.FAMILY_SSI);

        oscarConnection.getSnacProcessor().addGlobalResponseListener(new SnacResponseListener() {
            public void handleResponse(SnacResponseEvent e) {
                if (e.getSnacCommand() instanceof SsiDataModResponse) {
                    SsiDataModResponse dataModResponse = (SsiDataModResponse) e.getSnacCommand();
                    SnacCommand origCmd = e.getRequest().getCommand();
                    boolean create = origCmd instanceof CreateItemsCmd;
                    boolean modify = origCmd instanceof ModifyItemsCmd;
                    boolean delete = origCmd instanceof DeleteItemsCmd;
                    if (!(create || modify || delete)) {
                        return;
                    }
                    ItemsCmd itemsCmd = (ItemsCmd) origCmd;
                    List<SsiItem> items = itemsCmd.getItems();

                    int[] results = dataModResponse.getResults();
                    for (int i = 0; i < results.length; i++) {
                        int result = results[i];

                        if (result == SsiDataModResponse.RESULT_SUCCESS) {
                            SsiItem item = items.get(i);
                            if (create) itemCreated(item);
                            else if (modify) itemModified(item);
                            else if (delete) itemDeleted(item);
                        }
                    }
                }
            }
        });
    }

    public SnacFamilyInfo getSnacFamilyInfo() {
        return SsiCommand.FAMILY_INFO;
    }

    public void connected() {
        sendSnac(new SsiRightsRequest());
        sendSnac(new SsiDataRequest());
    }

    public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
        SnacCommand snac = snacPacketEvent.getSnacCommand();

        System.out.println("got ssi snac in " + MiscTools.getClassName(this)
                + ": " + snac);
        if (snac == null) {
            System.out.println("- packet: " + snacPacketEvent.getSnacPacket());
        }
        if (snac instanceof SsiDataCmd) {
            SsiDataCmd ssiDataCmd = (SsiDataCmd) snac;
            for (SsiItem item : ssiDataCmd.getItems()) itemCreated(item);

            if ((snac.getFlag2() & SnacCommand.SNACFLAG2_MORECOMING) == 0) {
                sendSnac(new ActivateSsiCmd());
                setReady();
            }
        } else if (snac instanceof CreateItemsCmd) {
            CreateItemsCmd createItemsCmd = (CreateItemsCmd) snac;
            for (SsiItem ssiItem : createItemsCmd.getItems()) {
                itemCreated(ssiItem);
            }
        } else if (snac instanceof ModifyItemsCmd) {
            ModifyItemsCmd modifyItemsCmd = (ModifyItemsCmd) snac;
            for (SsiItem ssiItem : modifyItemsCmd.getItems()) {
                itemModified(ssiItem);
            }
        } else if (snac instanceof DeleteItemsCmd) {
            DeleteItemsCmd deleteItemsCmd = (DeleteItemsCmd) snac;
            for (SsiItem ssiItem : deleteItemsCmd.getItems()) {
                itemDeleted(ssiItem);
            }
        }
    }

    public BuddyList getBuddyList() {
        return buddyList;
    }

    private Map<ItemId, SsiItem> items = new HashMap<ItemId, SsiItem>();

    public void itemCreated(SsiItem item) {
        ItemId id = new ItemId(item);
        SsiItem old = items.get(id);
        if (old != null) {
            throw new IllegalArgumentException("item " + id + " already exists "
                    + "as " + old + ", tried to add as " + item);
        }
        items.put(id, item);
        buddyList.handleItemCreated(item);
    }

    public void itemModified(SsiItem item) {
        ItemId id = new ItemId(item);
        if (items.get(id) == null) {
            throw new IllegalArgumentException("item does not exist: " + id
                    + " - " + item);
        }
        items.put(id, item);
        buddyList.handleItemModified(item);
    }

    public void itemDeleted(SsiItem item) {
        SsiItem removed = items.remove(new ItemId(item));
        if (removed == null) {
            throw new IllegalArgumentException("no such item " + item);
        }
        buddyList.handleItemDeleted(item);
    }

    private Random random = new Random();

    public int getUniqueItemId(int type, int parent) {
        //TODO: include ids in group child lists
        int nextid = random.nextInt();
        while (items.containsKey(new ItemId(type, parent, nextid))) {
            nextid = random.nextInt(0xffff+1);
        }
        return nextid;
    }

    public void sendSsiModification(ItemsCmd cmd,
            SnacRequestListener listener) {
        sendSnacRequest(cmd, listener);
    }

    public void sendSsiModification(ItemsCmd cmd) {
        sendSnac(cmd);
    }

    private static class ItemId {
        private final int type;
        private final int parent;
        private final int id;

        public ItemId(int type, int parent, int id) {
            this.type = type;
            this.parent = parent;
            this.id = id;
        }

        public ItemId(SsiItem item) {
            this(item.getItemType(), item.getParentId(), item.getId());
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ItemId itemId = (ItemId) o;

            if (id != itemId.id) return false;
            if (parent != itemId.parent) return false;
            if (type != itemId.type) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = type;
            result = 29 * result + parent;
            result = 29 * result + id;
            return result;
        }

        public String toString() {
            return "ItemId{" +
                    "type=" + MiscTools.findIntField(SsiItem.class, type, "TYPE_.*") +
                    ", parent=0x" + Integer.toHexString(parent) +
                    ", id=0x" + Integer.toHexString(id) +
                    "}";
        }
    }
}
