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
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.ssiitem.SsiItemObjectFactory;
import net.kano.joscar.ssiitem.DefaultSsiItemObjFactory;
import net.kano.joscar.ssiitem.SsiItemObj;
import net.kano.joscar.ssiitem.GroupItem;
import net.kano.joscar.ssiitem.RootItem;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snac.SnacRequestListener;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snac.SnacResponseListener;
import net.kano.joscar.snac.ClientSnacProcessor;
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
import net.kano.joustsim.oscar.oscar.service.ServiceEvent;
import net.kano.joustsim.oscar.oscar.service.bos.ServerReadyEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;

public class SsiService extends Service {
    private static final Logger LOGGER = Logger
            .getLogger(SsiService.class.getName());

    private boolean requestedList = false;
    private static final int NUM_IDS = 0x7fff + 1;

    private Map<Integer, Collection<Integer>> prospectiveIds
            = new HashMap<Integer, Collection<Integer>>();
    private Collection<Integer> prospectiveGroupIds = new ArrayList<Integer>();
    private CopyOnWriteArrayList<SsiItemChangeListener> listeners
            = new CopyOnWriteArrayList<SsiItemChangeListener>();

    private SsiBuddyList buddyList = new SsiBuddyList(this);
    private SsiPermissionList permissionList = new SsiPermissionList(this);
    private SsiServerStoredSettings settings = new SsiServerStoredSettings(this);
    private MyBuddyIconItemManager buddyIconItemManager = new MyBuddyIconItemManager(this);

    public SsiService(AimConnection aimConnection,
            OscarConnection oscarConnection) {
        super(aimConnection, oscarConnection, SsiCommand.FAMILY_SSI);
    }

    public SnacFamilyInfo getSnacFamilyInfo() {
        return SsiCommand.FAMILY_INFO;
    }

    public void connected() {
        OscarConnection oscarConnection = getOscarConnection();
        ClientSnacProcessor snacProcessor = oscarConnection.getSnacProcessor();
        snacProcessor.addGlobalResponseListener(new ItemsChangeListener());
        boolean serverReady = !oscarConnection
                .getServiceEvents(ServerReadyEvent.class)
                .isEmpty();
        if (serverReady) requestList();
    }

    public void handleEvent(ServiceEvent event) {
        super.handleEvent(event);
        if (event instanceof ServerReadyEvent) requestList();
    }

    private void requestList() {
        synchronized (this) {
            if (requestedList) return;
            requestedList = true;
        }
        sendSnac(new SsiRightsRequest());
        sendSnac(new SsiDataRequest());
    }

    public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
        SnacCommand snac = snacPacketEvent.getSnacCommand();

        LOGGER.fine("Got SSI packet: " + snac);
        if (snac == null) {
            LOGGER.fine("Packet: " + snacPacketEvent.getSnacPacket());
        }
        List<Exception> exceptions = new ArrayList<Exception>();
        if (snac instanceof SsiDataCmd) {
            SsiDataCmd ssiDataCmd = (SsiDataCmd) snac;
            boolean done = (snac.getFlag2() & SnacCommand.SNACFLAG2_MORECOMING)
                    == 0;
            if (LOGGER.isLoggable(Level.FINE)) {
                synchronized(this) {
                    if (done) {
                        LOGGER.fine("Got final buddy list packet: "
                                + items.size() + " items");
                    } else {
                        LOGGER.fine("Got buddy list part: " + items.size()
                                + " items");
                    }
                }
            }
            List<SsiItem> items = ssiDataCmd.getItems();
            for (SsiItem item : items) {
                LOGGER.finer("Buddy list item: " + item);
                try {
                    itemCreated(item);
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }

            if (done) {
                sendSnac(new ActivateSsiCmd());
                setReady();
            }

        } else if (snac instanceof CreateItemsCmd) {
            CreateItemsCmd createItemsCmd = (CreateItemsCmd) snac;
            for (SsiItem ssiItem : createItemsCmd.getItems()) {
                LOGGER.fine("Item created by other client: " + ssiItem);
                try {
                    itemCreated(ssiItem);
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }

        } else if (snac instanceof ModifyItemsCmd) {
            ModifyItemsCmd modifyItemsCmd = (ModifyItemsCmd) snac;
            for (SsiItem ssiItem : modifyItemsCmd.getItems()) {
                LOGGER.fine("Item modified by other client: " + ssiItem);
                try {
                    itemModified(ssiItem);
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }

        } else if (snac instanceof DeleteItemsCmd) {
            DeleteItemsCmd deleteItemsCmd = (DeleteItemsCmd) snac;
            for (SsiItem ssiItem : deleteItemsCmd.getItems()) {
                LOGGER.fine("Item deleted by other client: " + ssiItem);
                try {
                    itemDeleted(ssiItem);
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
        }
        if (exceptions.size() == 1) {
            Exception exception = exceptions.get(0);
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new IllegalStateException(exception);
            }

        } else if (!exceptions.isEmpty()) {
            for (Exception exception : exceptions) {
                LOGGER.log(Level.WARNING,
                        "Exception while processing SSI packets", exception);
            }
            throw new MultipleExceptionsException(exceptions);
        }
    }

    public MutableBuddyList getBuddyList() {
        return buddyList;
    }

    private Map<ItemId, SsiItem> items = new HashMap<ItemId, SsiItem>();

    private void itemCreated(SsiItem item) {
        LOGGER.fine("Item created: " + item);

        ItemId id = new ItemId(item);
        synchronized (this) {
            SsiItem old = items.get(id);
            if (old != null) {
                throw new IllegalArgumentException(
                        "item " + id + " already exists "
                                + "as " + old + ", tried to add as " + item);
            }
            items.put(id, item);
        }
        for (SsiItemChangeListener listener : listeners) {
            try {
                listener.handleItemCreated(item);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Exception thrown by SSI listener "
                        + listener, e);
            }
        }
    }

    private void itemModified(SsiItem item) {
        LOGGER.fine("Item modified: " + item);

        ItemId id = new ItemId(item);
        synchronized (this) {
            SsiItem oldItem = items.get(id);
            if (oldItem == null) {
                throw new IllegalArgumentException("item does not exist: " + id
                        + " - " + item);
            }
            LOGGER.fine("(Old item: " + oldItem + ")");
            items.put(id, item);
        }
        for (SsiItemChangeListener listener : listeners) {
            try {
                listener.handleItemModified(item);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Exception thrown by SSI listener "
                        + listener, e);
            }
        }
    }

    private void itemDeleted(SsiItem item) {
        LOGGER.fine("Item deleted: " + item);

        SsiItem removed;
        synchronized (this) {
            removed = items.remove(new ItemId(item));
        }
        if (removed == null) {
            throw new IllegalArgumentException("no such item " + item);
        }
        LOGGER.fine("(Actual deleted: " + removed + ")");

        for (SsiItemChangeListener listener : listeners) {
            try {
                listener.handleItemDeleted(item);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Exception thrown by SSI listener "
                        + listener, e);
            }
        }
    }

    /** That's so random */
    private Random random = new Random();

    synchronized int getUniqueItemId(int type, int parent) {
        if (type == SsiItem.TYPE_GROUP) {
            throw new IllegalArgumentException("groups all have id 0");
        }
        Set<Integer> idsForType = getIdsForType(type);
        if (type == SsiItem.TYPE_BUDDY) {
            addUsedBuddyIdsInGroup(idsForType, parent);
        }
        int nextid;
        do {
            nextid = random.nextInt(NUM_IDS);
        } while (idsForType.contains(nextid));

        // we don't want to return the same unique ID twice, even if it's never
        // used
        Collection<Integer> ids = prospectiveIds.get(type);
        if (ids == null) {
            ids = new ArrayList<Integer>(10);
            prospectiveIds.put(type, ids);
        }
        ids.add(nextid);
        return nextid;
    }

    private synchronized void addUsedBuddyIdsInGroup(Set<Integer> idsForType,
            int parent) {
        for (Map.Entry<ItemId, SsiItem> id : items.entrySet()) {
            ItemId key = id.getKey();
            if (key.getType() == SsiItem.TYPE_GROUP
                    && key.getParent() == parent) {
                SsiItemObjectFactory objFactory = new DefaultSsiItemObjFactory();
                SsiItemObj itemObj = objFactory.getItemObj(id.getValue());
                if (itemObj instanceof GroupItem) {
                    GroupItem groupItem = (GroupItem) itemObj;
                    int[] buddies = groupItem.getBuddies();
                    if (buddies != null) {
                        for (int bid : buddies) {
                            idsForType.add(bid);
                        }
                    }
                }
            }
        }
    }

    private synchronized void addUsedGroupIdsInRoot(Set<Integer> idsForType) {
        for (Map.Entry<ItemId, SsiItem> id : items.entrySet()) {
            ItemId key = id.getKey();
            if (key.getType() == SsiItem.TYPE_GROUP
                    && key.getParent() == 0) {
                SsiItemObjectFactory objFactory = new DefaultSsiItemObjFactory();
                SsiItemObj itemObj = objFactory.getItemObj(id.getValue());
                if (itemObj instanceof RootItem) {
                    RootItem groupItem = (RootItem) itemObj;
                    int[] groupids = groupItem.getGroupids();
                    if (groupids != null) {
                        for (int bid : groupids) {
                            idsForType.add(bid);
                        }
                    }
                }
            }
        }
    }

    private synchronized Set<Integer> getIdsForType(int type) {
        Set<Integer> idsForType = new HashSet<Integer>(items.size());
        for (ItemId id : items.keySet()) {
            if (id.getType() == type) idsForType.add(id.getId());
        }
        Collection<Integer> prospective = prospectiveIds.get(type);
        if (prospective != null) idsForType.addAll(prospective);
        return idsForType;
    }

    private synchronized Set<Integer> getPossiblyUsedGroupIds() {
        Set<Integer> idsForType = new HashSet<Integer>(items.size());
        for (ItemId id : items.keySet()) {
            if (id.getType() == SsiItem.TYPE_GROUP) {
                idsForType.add(id.getParent());
            }
        }
        idsForType.addAll(prospectiveGroupIds);
        return idsForType;
    }

    //TODO: test new unique group id and buddy id method
    synchronized int getUniqueGroupId() {
        Set<Integer> groupIds = getPossiblyUsedGroupIds();
        addUsedGroupIdsInRoot(groupIds);
        int nextid;
        do {
            nextid = random.nextInt(NUM_IDS);
        } while (groupIds.contains(nextid));

        // we don't want to return the same group ID twice, even if it's not
        // used yet
        prospectiveGroupIds.add(nextid);

        return nextid;
    }

    public PermissionList getPermissionList() {
        return permissionList;
    }

    public ServerStoredSettings getServerStoredSettings() {
        return settings;
    }

    public MyBuddyIconItemManager getBuddyIconItemManager() {
        return buddyIconItemManager;
    }

    public void sendSsiModification(ItemsCmd cmd,
            SnacRequestListener listener) {
        sendSnacRequest(cmd, listener);
    }

    public void sendSsiModification(ItemsCmd cmd) {
        sendSnac(cmd);
    }

    public void addItemChangeListener(SsiItemChangeListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeListener(SsiItemChangeListener listener) {
        listeners.remove(listener);
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

        public int getType() {
            return type;
        }

        public int getParent() {
            return parent;
        }

        public int getId() {
            return id;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ItemId itemId = (ItemId) o;

            if (id != itemId.id) return false;
            if (parent != itemId.parent) return false;
            return type == itemId.type;

        }

        public int hashCode() {
            int result = type;
            result = 29 * result + parent;
            result = 29 * result + id;
            return result;
        }

        public String toString() {
            return "ItemId{" +
                    "type="
                    + MiscTools.findIntField(SsiItem.class, type, "TYPE_.*") +
                    ", parent=0x" + Integer.toHexString(parent) +
                    ", id=0x" + Integer.toHexString(id) +
                    "}";
        }
    }

    private class ItemsChangeListener implements SnacResponseListener {
        public void handleResponse(SnacResponseEvent e) {
            if (e.getSnacCommand() instanceof SsiDataModResponse) {
                SsiDataModResponse dataModResponse = (SsiDataModResponse) e
                        .getSnacCommand();
                SnacCommand origCmd = e.getRequest().getCommand();
                boolean create = origCmd instanceof CreateItemsCmd;
                boolean modify = origCmd instanceof ModifyItemsCmd;
                boolean delete = origCmd instanceof DeleteItemsCmd;
                if (!(create || modify || delete)) {
                    return;
                }
                ItemsCmd itemsCmd = (ItemsCmd) origCmd;
                String className = MiscTools.getClassName(itemsCmd);
                List<SsiItem> items = itemsCmd.getItems();

                int[] results = dataModResponse.getResults();
                for (int i = 0; i < results.length; i++) {
                    int result = results[i];

                    SsiItem item = items.get(i);
                    if (result == SsiDataModResponse.RESULT_SUCCESS) {
                        if (create) {
                            itemCreated(item);
                        } else if (modify) {
                            itemModified(item);
                        } else {
                            assert delete;
                            itemDeleted(item);
                        }

                    } else if (result == SsiDataModResponse.RESULT_ID_TAKEN) {
                        int id = item.getId();
                        Set<Integer> possible = new TreeSet<Integer>(
                                getIdsForType(item.getItemType()));
                        LOGGER.warning("ID taken for " + className + " of "
                                + item);
                        LOGGER.warning("ID: " + id + " of possible ID's: "
                                + possible);

                    } else {
                        LOGGER.warning(
                                "SSI error 0x" + Integer.toHexString(result)
                                        + " for " + className
                                        + " of " + item);
                    }
                }
            }
        }
    }
}
