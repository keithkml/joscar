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
 *  File created by keith @ Jan 17, 2004
 *
 */

package net.kano.aimcrypto.connection.oscar.service;

import net.kano.joscar.snaccmd.icbm.MissedMsgInfo;
import net.kano.aimcrypto.Screenname;

public class MissedImInfo {
    public static final Reason REASON_TOO_FAST
            = new Reason("TOO_FAST", MissedMsgInfo.REASON_TOO_FAST);
    public static final Reason REASON_TOO_LARGE
            = new Reason("TOO_LARGE", MissedMsgInfo.REASON_TOO_LARGE);
    public static final Reason REASON_SENDER_WARNING_LEVEL
            = new Reason("SENDER_WARNING_LEVEL",
                    MissedMsgInfo.REASON_SENDER_WARNING_LEVEL);
    public static final Reason REASON_YOUR_WARNING_LEVEL
            = new Reason("YOUR_WARNING_LEVEL",
                    MissedMsgInfo.REASON_YOUR_WARNING_LEVEL);

    static MissedImInfo getInstance(Screenname to, MissedMsgInfo msg) {
        int reason = msg.getReasonCode();
        Reason robj;
        if (reason == REASON_TOO_FAST.getCode()) {
            robj = REASON_TOO_FAST;
        } else if (reason == REASON_TOO_LARGE.getCode()) {
            robj = REASON_TOO_LARGE;
        } else if (reason == REASON_SENDER_WARNING_LEVEL.getCode()) {
            robj = REASON_SENDER_WARNING_LEVEL;
        } else if (reason == REASON_YOUR_WARNING_LEVEL.getCode()) {
            robj = REASON_YOUR_WARNING_LEVEL;
        } else {
            robj = new Reason(reason);
        }

        Screenname from = new Screenname(msg.getUserInfo().getScreenname());

        return new MissedImInfo(from, to, msg.getNumberMissed(), robj);
    }

    private final Screenname from;
    private final Screenname to;
    private final int count;
    private final Reason reason;

    private MissedImInfo(Screenname from, Screenname to, int count, Reason reason) {
        this.from = from;
        this.to = to;
        this.count = count;
        this.reason = reason;
    }

    public Screenname getFrom() { return from; }

    public Screenname getTo() { return to; }

    public int getCount() { return count; }

    public Reason getReason() { return reason; }

    public static final class Reason {
        private final String name;
        private final int code;

        private Reason(int code) {
            this("UNKNOWN", code);
        }

        private Reason(String name, int code) {
            this.name = name;
            this.code = code;
        }

        public String getName() { return name; }

        public int getCode() { return code; }

        public String toString() {
            return getName() + "(" + getCode() + ")";
        }
    }
}
