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
 *  File created by keith @ Apr 25, 2003
 *
 */

package net.kano.joscar.rvproto.ft;

import java.util.zip.Checksum;

public final class FileSendChecksum implements Checksum {
    private static final long CHECKSUM_INIT = 0xffff0000;

    private long checksum;

    { // init
        reset();
    }

    public void update(int value) {
        update(new byte[] { (byte) value }, 0, 1);
    }

    public void update(final byte[] input, final int offset, final int len) {
        long check = (checksum >> 16) & 0xffffL;

        for (int i = 0; i < len; i++) {
            final long oldcheck = check;

            final int byteVal = input[offset + i] & 0xff;

            final int val;
            if ((i & 1) != 0) val = byteVal;
            else val = byteVal << 8;

            check -= val;

            if (check > oldcheck) check--;
        }

        check = ((check & 0x0000ffff) + (check >> 16));
        check = ((check & 0x0000ffff) + (check >> 16));

        checksum = check << 16 & 0xffffffff;
    }

    public long getValue() {
        return checksum;
    }

    public void reset() {
        checksum = CHECKSUM_INIT;
    }
}
