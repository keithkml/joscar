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
 *  File created by keith @ Feb 25, 2004
 *
 */

package net.kano.aimcrypto.connection.oscar.service.icbm;

import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.config.BuddyCertificateInfo;
import net.kano.aimcrypto.config.PrivateKeysInfo;
import net.kano.joscar.snaccmd.icbm.InstantMessage;
import net.kano.joscar.DefensiveTools;

import java.util.Date;

public class OutgoingSecureAimMessageInfo extends MessageInfo {
    public static OutgoingSecureAimMessageInfo getInstance(Screenname from,
            Screenname to, Message message, Date date, PrivateKeysInfo keys,
            BuddyCertificateInfo buddyCerts, InstantMessage msg) {
        return new OutgoingSecureAimMessageInfo(from, to, message, date, keys,
                buddyCerts, msg);
    }

    private final PrivateKeysInfo localKeys;
    private final BuddyCertificateInfo buddyCerts;
    private final InstantMessage encodedMsg;

    private OutgoingSecureAimMessageInfo(Screenname from, Screenname to,
            Message message, Date date, PrivateKeysInfo keys,
            BuddyCertificateInfo buddyCerts, InstantMessage encodedMsg) {
        super(from, to, message, date);

        DefensiveTools.checkNull(keys, "keys");
        DefensiveTools.checkNull(buddyCerts, "buddyCerts");
        DefensiveTools.checkNull(encodedMsg, "encodedMsg");

        this.localKeys = keys;
        this.buddyCerts = buddyCerts;
        this.encodedMsg = encodedMsg;
    }

    public final PrivateKeysInfo getLocalKeys() { return localKeys; }

    public final BuddyCertificateInfo getBuddyCerts() { return buddyCerts; }

    public final InstantMessage getEncodedMessage() { return encodedMsg; }
}
