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
import net.kano.joscar.tlv.MutableTlvChain;
import net.kano.joscar.tlv.TlvChain;
import net.kano.joscar.MiscTools;

abstract class SimpleNamedItem extends AbstractItem {
    protected static final int GROUPID_DEFAULT = 0;

    private final String sn;
    private final int id;

    protected SimpleNamedItem(SimpleNamedItem other) {
        this(other.sn, other.id, other.copyExtraTlvs());
    }

    protected SimpleNamedItem(String sn, int id) {
        this(sn, id, null);
    }

    protected SimpleNamedItem(String sn, int id, TlvChain extraTlvs) {
        super(extraTlvs);

        this.sn = sn;
        this.id = id;
    }

    public final String getScreenname() { return sn; }

    public final int getId() { return id; }

    public SsiItem getSsiItem() {
        return generateItem(sn, GROUPID_DEFAULT, id, getItemType(), null);
    }

    public String toString() {
        return MiscTools.getClassName(this) + " for " + sn + " (id=0x"
                + Integer.toHexString(id) + ")";
    }

    protected abstract int getItemType();
}
