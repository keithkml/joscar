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
 *  File created by keith @ Jan 13, 2004
 *
 */

package net.kano.joscar.snaccmd;

import net.kano.joscar.DefensiveTools;

public final class WarningLevel extends Number {
    public static WarningLevel getInstanceFromX10(int x10) {
        if (x10 < 0) return null;

        if (x10 == 0) return LEVEL_ZERO;
        if (x10 == 999) return LEVEL_999;

        return new WarningLevel(x10);
    }

    public static WarningLevel getInstanceFromPercent(int pc) {
        return getInstanceFromX10(pc*10);
    }

    private static final WarningLevel LEVEL_ZERO = new WarningLevel(0);
    private static final WarningLevel LEVEL_999 = new WarningLevel(999);


    private final float fv;
    private final int rv;
    private final int orig;

    private WarningLevel(int x10) {
        orig = x10;
        fv = x10/10.0f;
        rv = Math.round(fv);
    }

    public double doubleValue() { return fv; }

    public float floatValue() { return fv; }

    public int intValue() { return rv; }

    public long longValue() { return rv; }

    public int getX10Value() { return orig; }
}
