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
 *  File created by keith @ Apr 28, 2003
 *
 */

package net.kano.joscar.rvcmd;

import net.kano.joscar.LiveWritable;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.EncodedStringInfo;
import net.kano.joscar.snaccmd.MinimalEncoder;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;

import java.util.Locale;
import java.io.OutputStream;
import java.io.IOException;

public class InvitationMessage implements LiveWritable {
    public static InvitationMessage readInvitationMessage(TlvChain chain) {
        DefensiveTools.checkNull(chain, "chain");

        String charset = chain.getString(TYPE_CHARSET);

        String message = (charset == null
                ? chain.getString(TYPE_MESSAGE)
                : chain.getString(TYPE_MESSAGE, charset));

        String languageStr = chain.getString(TYPE_LANGUAGE);
        Locale language = languageStr == null ? null : new Locale(languageStr);

        return new InvitationMessage(message, language);
    }

    private static final int TYPE_LANGUAGE = 0x000e;
    private static final int TYPE_CHARSET = 0x000d;
    private static final int TYPE_MESSAGE = 0x000c;

    private final Locale language;
    private final String message;

    public InvitationMessage(String message) {
        this(message, Locale.getDefault());
    }

    public InvitationMessage(String message, Locale language) {
        this.language = language;
        this.message = message;
    }

    public final Locale getLanguage() { return language; }

    public final String getMessage() { return message; }

    public void write(OutputStream out) throws IOException {
        if (language != null) {
            String lang = language.getLanguage();
            Tlv.getStringInstance(TYPE_LANGUAGE, lang).write(out);
        }

        if (message != null) {
            EncodedStringInfo encInfo = MinimalEncoder.encodeMinimally(message);
            String charset = encInfo.getCharset();

            Tlv.getStringInstance(TYPE_CHARSET, charset).write(out);
            new Tlv(TYPE_MESSAGE, ByteBlock.wrap(encInfo.getData())).write(out);
        }
    }

    public String toString() {
        return "InvitationMessage: \"" + message + "\" ("
                + (language == null ? "language=null"
                : "in " + language.getLanguage()) + ")";
    }
}
