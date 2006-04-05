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
import net.kano.joscar.snaccmd.ssi.CreateItemsCmd;
import net.kano.joscar.snaccmd.ssi.ModifyItemsCmd;
import net.kano.joscar.snaccmd.ssi.SsiItem;
import net.kano.joscar.ssiitem.DefaultSsiItemObjFactory;
import net.kano.joscar.ssiitem.PrivacyItem;
import static net.kano.joscar.ssiitem.PrivacyItem.VISMASK_HIDE_WIRELESS;
import net.kano.joscar.ssiitem.SsiItemObj;
import net.kano.joscar.ssiitem.SsiItemObjectFactory;
import net.kano.joscar.ssiitem.VisibilityItem;

import java.util.SortedSet;
import java.util.TreeSet;

public class SsiServerStoredSettings
    implements ServerStoredSettings, SsiItemChangeListener {
  private SsiService ssiService;
  private CopyOnWriteArrayList<ServerStoredSettingsListener> listeners
      = new CopyOnWriteArrayList<ServerStoredSettingsListener>();

  private SsiItemObjectFactory factory = new DefaultSsiItemObjFactory();
  private SortedSet<PrivacyItem> privacyItems
      = new TreeSet<PrivacyItem>(new ItemIdComparator());
  private SortedSet<VisibilityItem> visibilityItems
      = new TreeSet<VisibilityItem>(new ItemIdComparator());

  public SsiServerStoredSettings(SsiService ssiService) {
    this.ssiService = ssiService;
    ssiService.addItemChangeListener(this);
  }

  public void handleItemCreated(final SsiItem item) {
    modifyItems(new Runnable() {
      public void run() {
        SsiItemObj itemObj = factory.getItemObj(item);
        handleItemActuallyCreated(itemObj);
      }
    });
  }

  public void handleItemModified(final SsiItem item) {
    modifyItems(new Runnable() {
      public void run() {
        SsiItemObj itemObj = factory.getItemObj(item);
        handleItemActuallyModified(itemObj);
      }
    });
  }

  public void handleItemDeleted(final SsiItem item) {
    modifyItems(new Runnable() {
      public void run() {
        SsiItemObj itemObj = factory.getItemObj(item);
        handleItemActuallyDeleted(itemObj);
      }
    });
  }

  private void modifyItems(Runnable runnable) {
    boolean wasMobileDeviceShown;
    boolean wasIdleTimeShown;
    boolean wasTypingShown;
    boolean wasRecentUsed;
    boolean mobileDeviceShown;
    boolean idleTimeShown;
    boolean typingShown;
    boolean recentUsed;
    synchronized (this) {
      wasMobileDeviceShown = isMobileDeviceShown();
      wasIdleTimeShown = isIdleTimeShown();
      wasTypingShown = isTypingShown();
      wasRecentUsed = isRecentBuddiesUsed();

      runnable.run();

      mobileDeviceShown = isMobileDeviceShown();
      idleTimeShown = isIdleTimeShown();
      typingShown = isTypingShown();
      recentUsed = isRecentBuddiesUsed();
    }
    if (wasMobileDeviceShown != mobileDeviceShown) {
      for (ServerStoredSettingsListener listener : listeners) {
        listener.handleMobileDeviceShownChanged(this,
            mobileDeviceShown);
      }
    }
    if (wasIdleTimeShown != idleTimeShown) {
      for (ServerStoredSettingsListener listener : listeners) {
        listener.handleIdleTimeShownChanged(this, idleTimeShown);
      }
    }
    if (wasTypingShown != typingShown) {
      for (ServerStoredSettingsListener listener : listeners) {
        listener.handleTypingShownChanged(this, typingShown);
      }
    }
    if (wasRecentUsed != recentUsed) {
      for (ServerStoredSettingsListener listener : listeners) {
        listener.handleRecentBuddiesUsedChanged(this, recentUsed);
      }
    }
  }

  @SuppressWarnings({"SimplifiableConditionalExpression"})
  public boolean isMobileDeviceShown() {
    PrivacyItem privacyItem = getPrivacyItem();
    return privacyItem == null ? true
        : (privacyItem.getVisibleMask() & VISMASK_HIDE_WIRELESS) == 0;
  }

  public boolean isIdleTimeShown() {
    return isVisiblityFlagOn(VisibilityItem.MASK_SHOW_IDLE_TIME);
  }

  @SuppressWarnings({"SimplifiableConditionalExpression"})
  private boolean isVisiblityFlagOn(long mask) {
    VisibilityItem visibilityItem = getVisibilityItem();
    return visibilityItem == null ? true
        : (visibilityItem.getVisFlags() & mask) != 0;
  }

  public boolean isTypingShown() {
    return isVisiblityFlagOn(VisibilityItem.MASK_SHOW_TYPING);
  }

  public boolean isRecentBuddiesUsed() {
    return !isVisiblityFlagOn(VisibilityItem.MASK_DISABLE_RECENT_BUDDIES);
  }

  public void changeMobileDeviceShown(boolean shown) {
    PrivacyItem oldItem = getPrivacyItem();
    if (oldItem == null) {
      int id = ssiService
          .getUniqueItemId(SsiItem.TYPE_PRIVACY, SsiItem.GROUP_ROOT);
      long newMask = shown ? 0 : VISMASK_HIDE_WIRELESS;
      PrivacyItem item = new PrivacyItem(id, PrivacyItem.MODE_ALLOW_ALL,
          newMask);
      ssiService.sendSsiModification(new CreateItemsCmd(item.toSsiItem()));
    } else {
      PrivacyItem item = new PrivacyItem(oldItem);
      long oldMask = item.getVisibleMask();
      long newMask = shown ? oldMask & ~VISMASK_HIDE_WIRELESS
          : oldMask | VISMASK_HIDE_WIRELESS;
      item.setVisibleMask(newMask);
      ssiService.sendSsiModification(new ModifyItemsCmd(item.toSsiItem()));
    }
  }

  public void changeIdleTimeShown(boolean shown) {
    changeVisibilityMaskBit(VisibilityItem.MASK_SHOW_IDLE_TIME, shown);
  }

  public void changeTypingShown(boolean shown) {
    changeVisibilityMaskBit(VisibilityItem.MASK_SHOW_TYPING, shown);
  }

  public void changeRecentBuddiesUsed(boolean used) {
    changeVisibilityMaskBit(VisibilityItem.MASK_DISABLE_RECENT_BUDDIES, !used);
  }

  private void changeVisibilityMaskBit(long mask, boolean shown) {
    VisibilityItem oldItem = getVisibilityItem();
    if (oldItem == null) {
      int id = ssiService
          .getUniqueItemId(SsiItem.TYPE_VISIBILITY, SsiItem.GROUP_ROOT);
      long newMask = shown ? mask : 0;
      VisibilityItem item = new VisibilityItem(id, newMask);
      ssiService.sendSsiModification(new CreateItemsCmd(item.toSsiItem()));

    } else {
      VisibilityItem item = new VisibilityItem(oldItem);
      long oldMask = item.getVisFlags();
      long newMask = shown ? oldMask | mask
          : oldMask & ~mask;
      item.setVisFlags(newMask);
      ssiService.sendSsiModification(new ModifyItemsCmd(item.toSsiItem()));
    }
  }

  private synchronized void handleItemActuallyCreated(SsiItemObj itemObj) {
    if (itemObj instanceof PrivacyItem) {
      PrivacyItem privacyItem = (PrivacyItem) itemObj;
      privacyItems.add(privacyItem);

    } else if (itemObj instanceof VisibilityItem) {
      VisibilityItem visibilityItem = (VisibilityItem) itemObj;
      visibilityItems.add(visibilityItem);

    }
  }

  private synchronized void handleItemActuallyModified(SsiItemObj itemObj) {
    if (itemObj instanceof PrivacyItem) {
      PrivacyItem privacyItem = (PrivacyItem) itemObj;
      SsiTools.removeItemsWithId(privacyItems, privacyItem.getId());
      privacyItems.add(privacyItem);

    } else if (itemObj instanceof VisibilityItem) {
      VisibilityItem visibilityItem = (VisibilityItem) itemObj;
      SsiTools.removeItemsWithId(visibilityItems, visibilityItem.getId());
      visibilityItems.add(visibilityItem);
    }
  }

  private synchronized void handleItemActuallyDeleted(SsiItemObj itemObj) {
    if (itemObj instanceof PrivacyItem) {
      PrivacyItem privacyItem = (PrivacyItem) itemObj;
      SsiTools.removeItemsWithId(privacyItems, privacyItem.getId());

    } else if (itemObj instanceof VisibilityItem) {
      VisibilityItem visibilityItem = (VisibilityItem) itemObj;
      SsiTools.removeItemsWithId(visibilityItems, visibilityItem.getId());
    }
  }

  private synchronized PrivacyItem getPrivacyItem() {
    return privacyItems.isEmpty() ? null : privacyItems.last();
  }

  private synchronized VisibilityItem getVisibilityItem() {
    return visibilityItems.isEmpty() ? null : visibilityItems.last();
  }

  public void addListener(ServerStoredSettingsListener listener) {
    listeners.addIfAbsent(listener);
  }

  public void removeListener(ServerStoredSettingsListener listener) {
    listeners.remove(listener);
  }
}
