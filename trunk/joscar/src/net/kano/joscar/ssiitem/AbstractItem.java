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
 *  File created by keith @ Mar 3, 2003
 *
 */

package net.kano.joscar.ssiitem;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.snaccmd.ssi.SsiItem;
import net.kano.joscar.tlv.AbstractTlvChain;
import net.kano.joscar.tlv.ImmutableTlvChain;
import net.kano.joscar.tlv.MutableTlvChain;

/**
 * A base class for each of the item classes in this package.
 */
public abstract class AbstractItem {
    /** The "extra TLV's" in this item. */
    private final AbstractTlvChain extraTlvs;

    /**
     * Creates a new item object with the given set of unprocessed or otherwise
     * unrecognized TLV's in this item's type-specific TLV list.
     *
     * @param extraTlvs the extra TLV's in this item
     */
    protected AbstractItem(AbstractTlvChain extraTlvs) {
        this.extraTlvs = extraTlvs == null ? new MutableTlvChain() : extraTlvs;
    }

    /**
     * Returns a copy of this item's extra TLV's, or <code>null</code> if this
     * item's extra TLV list is <code>null</code>.
     *
     * @return a copy of this item's extra TLV's
     */
    protected final AbstractTlvChain copyExtraTlvs() {
        return extraTlvs == null ? null : new ImmutableTlvChain(extraTlvs);
    }

    /**
     * Returns the "extra TLV list" for this item. This list contains the TLV's
     * present in this item's type-specific data block that were not processed
     * into fields; in practice, this likely means fields inserted by another
     * client like WinAIM that joscar does not yet recognize.
     *
     * @return this item's "extra TLV's"
     */
    public final AbstractTlvChain getExtraTlvs() {
        return extraTlvs;
    }

    /**
     * Generates a new <code>SsiItem</code> from this item object with the given
     * properties.
     *
     * @param name the name of the item
     * @param parentid the "parent ID" of this item
     * @param subid the "sub ID" of this item in its parent
     * @param type the type of item, like {@link SsiItem#TYPE_PRIVACY}
     * @param customTlvs a list of TLV's to insert into the type-specific data
     *        block of the returned item
     * @return a new SSI item with the given properties
     */
    protected final SsiItem generateItem(String name, int parentid, int subid,
            int type, AbstractTlvChain customTlvs) {
        MutableTlvChain chain = new MutableTlvChain(extraTlvs);
        if (customTlvs != null) chain.replaceAll(customTlvs);

        return new SsiItem(name, parentid, subid, type,
                ByteBlock.createByteBlock(chain));
    }

    /**
     * Returns an <code>SsiItem</code> that represents this item object.
     *
     * @return an <code>SsiItem</code> that represents this item object
     */
    public abstract SsiItem getSsiItem();
}
