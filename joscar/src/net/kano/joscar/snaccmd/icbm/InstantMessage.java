/*
 *  Copyright (c) 2003, The Joust Project
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
 *  File created by keith @ Aug 25, 2003
 *
 */

package net.kano.joscar.snaccmd.icbm;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;

public class InstantMessage {
    public static final int ENCRYPTIONCODE_DEFAULT = 0x0001;

    private final boolean encrypted;
    private final String message;
    private final int encryptionCode;
    private final ByteBlock encryptedData;

    public InstantMessage(String message) {
        DefensiveTools.checkNull(message, "message");

        encrypted = false;

        this.message = message;
        this.encryptionCode = -1;
        this.encryptedData = null;
    }

    public InstantMessage(ByteBlock encryptedData) {
        this(ENCRYPTIONCODE_DEFAULT, encryptedData);
    }

    public InstantMessage(int encryptionCode, ByteBlock encryptedData) {
        DefensiveTools.checkRange(encryptionCode, "encryptionCode", 0);
        DefensiveTools.checkNull(encryptedData, "encryptedData");

        encrypted = true;

        this.message = null;
        this.encryptionCode = encryptionCode;
        this.encryptedData = encryptedData;
    }

    public final boolean isEncrypted() { return encrypted; }

    public final String getMessage() { return message; }

    public final int getEncryptionCode() { return encryptionCode; }

    public final ByteBlock getEncryptedData() { return encryptedData; }

    public String toString() {
        return "IM: " + (message != null ? message
                : "<encrypted, code=" + encryptionCode + ">");
    }
}
