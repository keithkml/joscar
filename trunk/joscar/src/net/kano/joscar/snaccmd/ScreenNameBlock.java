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
 *  File created by keith @ Feb 21, 2003
 *
 */

package net.kano.joscar.snaccmd;

import net.kano.joscar.DefensiveTools;

/**
 * A very simple but very common data structure containing a screenname and the
 * byte length of that screenname.
 */
public final class ScreenNameBlock {
    /** The screen name. */
    private final String sn;
    /** The length of this object. */
    private final int dataSize;

    /**
     * Creates a new <code>ScreenNameInfo</code> object with the given
     * properties.
     *
     * @param sn the user's screenname
     * @param dataSize the size of this structure, as read from a block of
     *        binary data
     */
    protected ScreenNameBlock(String sn, int dataSize) {
        DefensiveTools.checkNull(sn, "sn");
        DefensiveTools.checkRange(dataSize, "dataSize", 0);

        this.sn = sn;
        this.dataSize = dataSize;
    }

    /**
     * Returns the screenname read.
     *
     * @return this object's screenname
     */
    public final String getScreenname() {
        return sn;
    }

    /**
     * Returns the total size of this object. This is generally equivalent to
     * <code>getScreenname().length() + 1</code>, but unicode is funny
     * sometimes.
     *
     * @return the total size of this structure: how many bytes were read to
     *         read this screenname
     */
    public final int getTotalSize() {
        return dataSize;
    }
}
