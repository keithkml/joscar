/*
 *  Copyright (c) 2002, The Joust Project
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

import net.kano.joscar.snaccmd.ssi.SsiItem;

public class DefaultSsiItemObjFactory implements SsiItemObjectFactory {
    public AbstractItem getItemObj(SsiItem item) {
        int type = item.getItemType();
        if (type == SsiItem.TYPE_BUDDY) {
            return BuddyItem.readBuddyItem(item);
        } else if (type == SsiItem.TYPE_GROUP) {
            if (item.getParentId() == 0) return RootItem.readRootItem(item);
            else return GroupItem.readGroupItem(item);
        } else if (type == SsiItem.TYPE_PRIVACY) {
            return PrivacyItem.readPrivacyItem(item);
        } else if (type == SsiItem.TYPE_ICON_INFO) {
            return IconItem.readIconItem(item);
        } else if (type == SsiItem.TYPE_PERMIT) {
            return PermitItem.readPermitItem(item);
        } else if (type == SsiItem.TYPE_DENY) {
            return DenyItem.readDenyItem(item);
        } else if (type == SsiItem.TYPE_VISIBILITY) {
            return VisibilityItem.readVisiblityItem(item);
        } else {
            return null;
        }
    }
}
