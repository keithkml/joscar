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
 *  File created by keith @ Feb 22, 2004
 *
 */

package net.kano.joustsim.text.convbox;

import net.kano.joustsim.Screenname;
import net.kano.joustsim.text.AolRtfString;
import net.kano.joscar.DefensiveTools;

import java.util.Date;

public class ConversationLine {
    private final Screenname sender;
    private final AolRtfString message;
    private final Date timestamp;
    private final IconID[] iconIDs;

    public ConversationLine(Screenname sender, AolRtfString message,
            Date timestamp, IconID iconID) {
        this(sender, message, timestamp, new IconID[] { iconID });
    }

    public ConversationLine(Screenname sender, AolRtfString message,
            Date timestamp, IconID[] iconIDs) {
        DefensiveTools.checkNull(sender, "sender");
        DefensiveTools.checkNull(message, "message");

        this.iconIDs = (IconID[]) (iconIDs == null ? null : iconIDs.clone());
        this.message = message;
        this.sender = sender;
        this.timestamp = timestamp;
    }

    public AolRtfString getMessage() { return message; }

    public Screenname getSender() { return sender; }

    public Date getTimestamp() { return timestamp; }

    public IconID[] getIconIDs() {
        return (IconID[]) (iconIDs == null ? null : iconIDs.clone());
    }
}
