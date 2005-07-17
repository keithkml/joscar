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
 *  File created by keith @ Mar 27, 2003
 *
 */

package net.kano.joscar.ssiitem;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.ssi.SsiItem;

/**
 * Provides a default implementation of an SSI item object factory, converting
 * SSI items to their respective classes provided in this package.
 */
public class DefaultSsiItemObjFactory implements SsiItemObjectFactory {
    /**
     * Returns whether the given SSI item is the "root group" item, which is the
     * parent of every buddy group and most other types of items.
     * <br /><br />
     * The root group item is characterized by being of type
     * {@link SsiItem#TYPE_GROUP} and having a parent ID of 0.
     *
     * @param item an SSI item
     * @return whether {@code item} is the "root group" item
     */
    public static boolean isRootItem(SsiItem item) {
        return item.getItemType() == SsiItem.TYPE_GROUP && item.getParentId() == 0;
    }

    public SsiItemObj getItemObj(SsiItem item) {
        DefensiveTools.checkNull(item, "item");

        int type = item.getItemType();
        if (type == SsiItem.TYPE_BUDDY) {
            return new BuddyItem(item);

        } else if (type == SsiItem.TYPE_GROUP) {
            if (isRootItem(item)) return new RootItem(item);
            else return new GroupItem(item);

        } else if (type == SsiItem.TYPE_PRIVACY) {
            return new PrivacyItem(item);

        } else if (type == SsiItem.TYPE_ICON_INFO) {
            return new IconItem(item);

        } else if (type == SsiItem.TYPE_PERMIT) {
            return new PermitItem(item);

        } else if (type == SsiItem.TYPE_DENY) {
            return new DenyItem(item);

        } else if (type == SsiItem.TYPE_VISIBILITY) {
            return new VisibilityItem(item);

        } else {
            return null;
        }
    }
}
