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
 *  File created by keith @ Jan 28, 2004
 *
 */

package net.kano.aimcrypto.connection.oscar.service.icbm;

import net.kano.aimcrypto.Screenname;
import net.kano.aimcrypto.config.BuddyCertificateInfo;
import net.kano.joscar.DefensiveTools;

import java.util.Date;

public class UndecryptableAimMessageInfo extends MessageInfo {

    static UndecryptableAimMessageInfo getInstance(
            EncryptedAimMessageInfo msgInfo, BuddyCertificateInfo certInfo,
            Reason reason, Exception ex) {

        return new UndecryptableAimMessageInfo(msgInfo.getFrom(), msgInfo.getTo(),
                (EncryptedAimMessage) msgInfo.getMessage(), certInfo,
                msgInfo.getTimestamp(), reason, ex);
    }

    private final BuddyCertificateInfo certInfo;
    private final Reason reason;
    private final Exception exception;

    private UndecryptableAimMessageInfo(Screenname from, Screenname to,
            EncryptedAimMessage message, BuddyCertificateInfo certInfo,
            Date date, Reason reason, Exception ex) {
        super(from, to, message, date);

        DefensiveTools.checkNull(reason, "reason");

        this.certInfo = certInfo;
        this.reason = reason;
        this.exception = ex;
    }

    public BuddyCertificateInfo getMessageCertificateInfo() { 
        return certInfo;
    }

    public Reason getReason() {
        return reason;
    }

    public Exception getException() {
        return exception;
    }

    public static class Reason {
        public static final Reason UNKNOWN = new Reason("UNKNOWN");
        public static final Reason DECRYPT_ERROR = new Reason("DECRYPT_ERROR");
        public static final Reason BAD_SIGNATURE = new Reason("BAD_SIGNATURE");

        private final String name;

        private Reason(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }
}
