/*
 *  Copyright (c) 2002-2003, The Joust Project
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
 *  File created by keith @ Feb 27, 2003
 *
 */

package net.kano.joscar.snaccmd.chat;

import net.kano.joscar.*;
import net.kano.joscar.tlv.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Represents a single message sent or received in a chat room.
 */
public class ChatMsg implements LiveWritable {
    /**
     * Reads a chat message from the given data block.
     *
     * @param msgBlock the data block containing chat message TLV's
     * @return a chat message object read from the given data block
     */
    public static ChatMsg readChatMsg(ByteBlock msgBlock) {
        DefensiveTools.checkNull(msgBlock, "msgBlock");

        TlvChain msgChain = ImmutableTlvChain.readChain(msgBlock);

        String charset = msgChain.getString(TYPE_CHARSET);
        String message = msgChain.getString(TYPE_BODY, charset);
        String language = msgChain.getString(TYPE_LANG);

        return new ChatMsg(message, new Locale(language));
    }

    /** A TLV type containing the charset in which this message is encoded. */
    private static final int TYPE_CHARSET = 0x0002;
    /** A TLV type containing the text of the message. */
    private static final int TYPE_BODY = 0x0001;
    /**
     * A TLV type containing the language in which this message was supposedly
     * written, as a two-letter code.
     */
    private static final int TYPE_LANG = 0x0003;

    /** The chat message. */
    private final String message;
    /** The locale (language code only) under which this message was written. */
    private final Locale language;


    /**
     * Creates a new chat message in the JVM's current language. Calling this
     * method is equivalent to calling {@link #ChatMsg(String, Locale) new
     * ChatMessage(message, Locale.getDefault())}.
     *
     * @param message the text of this chat message
     */
    public ChatMsg(String message) {
        this(message, Locale.getDefault());
    }

    /**
     * Creates a chat message with the given locale's language code.
     *
     * @param message the text of this chat message
     * @param locale the locale in which this message was supposedly written
     *        (only the language field of <code>locale</code> will be used)
     */
    public ChatMsg(String message, Locale locale) {
        this.message = message;
        this.language = locale;
    }

    /**
     * Returns the text of this chat message.
     *
     * @return this chat message's message text
     */
    public final String getMessage() {
        return message;
    }

    /**
     * Returns a <code>Locale</code> containing the language in which this
     * message was allegedly written.
     *
     * @return a <code>Locale</code> containing the language field of this chat
     *         message block
     */
    public final Locale getLanguage() {
        return language;
    }

    public void write(OutputStream out) throws IOException {
        MutableTlvChain msgChain = new DefaultMutableTlvChain();

        if (message != null) {
            EncodedStringInfo encInfo = MinimalEncoder.encodeMinimally(message);

            Object charset = encInfo.getCharset();
            if (charset != null) {
                msgChain.addTlv(Tlv.getStringInstance(TYPE_CHARSET,
                        charset.toString()));
            }

            ByteBlock msgData = ByteBlock.wrap(encInfo.getData());
            msgChain.addTlv(new Tlv(TYPE_BODY, msgData));
        }

        if (language != null) {
            msgChain.addTlv(Tlv.getStringInstance(TYPE_LANG,
                    language.getLanguage()));
        }

        msgChain.write(out);
    }

    public String toString() {
        return "ChatMsg in lang="
                + (language == null ? null : language.getDisplayLanguage())
                + ": " + message;
    }
}
