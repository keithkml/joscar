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

package net.kano.joustsim.oscar.oscar.service.info;

import net.kano.joustsim.Screenname;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;

public class BuddyHashHolder {
    private final Screenname buddy;
    private final ByteBlock hash;

    public BuddyHashHolder(Screenname buddy, ByteBlock hash) {
        DefensiveTools.checkNull(buddy, "buddy");

        this.buddy = buddy;
        this.hash = hash;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o.getClass().equals(BuddyHashHolder.class))) return false;

        final BuddyHashHolder key = (BuddyHashHolder) o;

        if (!buddy.equals(key.buddy)) return false;
        if (hash != null ? !hash.equals(key.hash) : key.hash != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = buddy.hashCode();
        result = 29 * result + (hash != null ? hash.hashCode() : 0);
        return result;
    }
}
