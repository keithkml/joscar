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

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joscar.snaccmd.ssi.CreateItemsCmd;
import net.kano.joscar.snaccmd.ssi.ItemsCmd;
import net.kano.joscar.snaccmd.ssi.ModifyItemsCmd;
import net.kano.joscar.snaccmd.ssi.SsiItem;
import net.kano.joscar.ssiitem.DefaultSsiItemObjFactory;
import net.kano.joscar.ssiitem.IconItem;
import net.kano.joscar.ssiitem.SsiItemObj;
import net.kano.joscar.ssiitem.SsiItemObjectFactory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class MyBuddyIconItemManager {
  private static final Logger LOGGER = Logger
      .getLogger(MyBuddyIconItemManager.class.getName());

  private SsiService service;
  private List<IconItem> items = new ArrayList<IconItem>();
  private SsiItemObjectFactory factory = new DefaultSsiItemObjFactory();
  private CopyOnWriteArrayList<MyBuddyIconItemListener> listeners
      = new CopyOnWriteArrayList<MyBuddyIconItemListener>();

  public MyBuddyIconItemManager(SsiService service) {
    this.service = service;
    service.addItemChangeListener(new SsiItemChangeListener() {
      public void handleItemCreated(SsiItem item) {
        if (isIconItem(item)) {
          SsiItemObj itemObj = factory.getItemObj(item);
          if (itemObj instanceof IconItem) {
            final IconItem iconItem = (IconItem) itemObj;
            runAndCheckModifications(new Runnable() {
              public void run() {
                items.add(iconItem);
              }
            });
          }
        }
      }

      public void handleItemModified(SsiItem item) {
        if (isIconItem(item)) {
          SsiItemObj itemObj = factory.getItemObj(item);
          if (itemObj instanceof IconItem) {
            final IconItem iconItem = (IconItem) itemObj;
            runAndCheckModifications(new Runnable() {
              public void run() {
                int id = iconItem.getId();
                boolean removed = removeItemFromList(id);
                if (!removed) {
                  LOGGER.warning("MyBuddyIconManager got "
                      + "modification of item " + iconItem
                      + " which wasn't there (icons : "
                      + items + ")");
                }
                items.add(iconItem);
              }
            });
          }
        }
      }

      public void handleItemDeleted(SsiItem item) {
        if (isIconItem(item)) {
          if (!removeItemFromList(item.getId())) {
            LOGGER.warning("MyBuddyIconManager got "
                + "removal of item " + item
                + " which wasn't there (icons : "
                + items + ")");
          }
        }
      }
    });
  }

  private void runAndCheckModifications(Runnable runnable) {
    IconItem oldItem;
    IconItem newItem;
    synchronized (this) {
      oldItem = getCurrentIconItem();
      runnable.run();
      newItem = getCurrentIconItem();
    }
    if (oldItem != newItem) {
      ExtraInfoData oldInfo = oldItem == null ? null
          : oldItem.getIconInfo();
      ExtraInfoData newInfo = newItem == null ? null
          : newItem.getIconInfo();
      for (MyBuddyIconItemListener listener : listeners) {
        listener.handleMyIconItemChanged(this, oldInfo, newInfo);
      }
    }
  }

  private synchronized @Nullable IconItem getCurrentIconItem() {
    for (IconItem iconItem : items) {
      if (iconItem.getName().equals("1")) return iconItem;
    }
    return null;
  }

  private synchronized boolean removeItemFromList(int id) {
    boolean good = false;
    for (Iterator<IconItem> it = items.iterator();
        it.hasNext();) {
      IconItem otherItem = it.next();
      if (otherItem.getId() == id) {
        it.remove();
        good = true;
        break;
      }
    }
    return good;
  }

  private static boolean isIconItem(SsiItem item) {
    return item.getItemType() == SsiItem.TYPE_ICON_INFO;
  }

  public void setIcon(@Nullable ExtraInfoData iconInfo) {
    IconItem currentItem = getCurrentIconItem();
    ItemsCmd cmd;
    if (currentItem == null) {
      int nextId = service.getUniqueItemId(SsiItem.TYPE_ICON_INFO,
          SsiItem.GROUP_ROOT);
      cmd = new CreateItemsCmd(
          new IconItem("1", nextId, iconInfo).toSsiItem());
    } else {
      IconItem newItem = new IconItem(currentItem);
      newItem.setIconInfo(iconInfo);
      cmd = new ModifyItemsCmd(newItem.toSsiItem());
    }
    service.sendSsiModification(cmd);
  }

//    public void addMyBuddyIconListener(MyBuddyIconListener listener) {
//        listeners.addIfAbsent(listener);
//    }
//
//    public void removeMyBuddyIconListener(MyBuddyIconListener listener) {
//        listeners.remove(listener);
//    }

  public CopyOnWriteArrayList<MyBuddyIconItemListener> getListeners() {
    return new CopyOnWriteArrayList<MyBuddyIconItemListener>(listeners);
  }
}
