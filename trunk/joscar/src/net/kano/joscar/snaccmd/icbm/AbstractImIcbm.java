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
 *  File created by keith @ Mar 6, 2003
 *
 */

package net.kano.joscar.snaccmd.icbm;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.snaccmd.AbstractIcbm;
import net.kano.joscar.snaccmd.EncodedStringInfo;
import net.kano.joscar.snaccmd.MinimalEncoder;
import net.kano.joscar.tlv.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * A base class for the two IM-based ICBM commands in this family. These
 * commands are {@link SendImIcbm} and {@link RecvImIcbm}.
 */
public abstract class AbstractImIcbm extends AbstractIcbm {
    /** A TLV type containing the text of the Instant Message. */
    private static final int TYPE_MESSAGE = 0x0002;
    /** A TLV type containing buddy icon information. */
    private static final int TYPE_ICONINFO = 0x0008;
    /** A TLV type containing whether or not this is an autoresponse. */
    private static final int TYPE_AUTO = 0x0004;
    /** A TLV type containing whether the user is requesting our icon. */
    private static final int TYPE_ICON_REQ = 0x0009;

    /** A TLV type containing a set of "features." */
    private static final int TYPE_FEATURES = 0x0501;
    /** A TLV type containing the message. */
    private static final int TYPE_MESSAGE_PARTS = 0x0101;

    /** A charset code indicating US-ASCII encoding. */
    private static final int CHARSET_ASCII = 0x0000;
    /** A charset code indicating ISO-8859-1 encoding. */
    private static final int CHARSET_ISO = 0x0003;
    /** A charset code indicating UCS-2BE, or UTF-16BE. */
    private static final int CHARSET_UTF16 = 0x0002;

    /** A charset "subcode" that is sent by default. */
    private static final int CHARSETSUBCODE_DEFAULT = 0x0000;

    /** The Instant Message. */
    private String message;
    /** Whether this message is an auto-response. */
    private boolean autoResponse;
    /** Whether the user wants our buddy icon. */
    private boolean wantsIcon;
    /** A set of icon data provided by the user who sent this IM. */
    private OldIconHashData iconInfo;

    /**
     * Generates an IM ICBM from the given incoming SNAC packet and with the
     * given SNAC command subtype.
     *
     * @param command the SNAC command subtype of this command
     * @param packet an incoming IM ICBM
     */
    AbstractImIcbm(int command, SnacPacket packet) {
        super(IcbmCommand.FAMILY_ICBM, command, packet);
    }

    /**
     * Extracts fields such as the message body and icon information from the
     * given TLV chain.
     *
     * @param chain the TLV chain from which to read
     */
    final void processImTlvs(TlvChain chain) {
        DefensiveTools.checkNull(chain, "chain");
        
        // get some TLV's
        Tlv messageTlv = chain.getLastTlv(TYPE_MESSAGE);
        Tlv iconTlv = chain.getLastTlv(TYPE_ICONINFO);

        // these we just know based on whether the TLV is there
        autoResponse = chain.hasTlv(TYPE_AUTO);
        wantsIcon = chain.hasTlv(TYPE_ICON_REQ);

        // and go through the TLV's we actually need to parse
        if (messageTlv != null) {
            // this is a normal IM. there are TLV's in here. two or more of
            // them.
            TlvChain msgTLVs = ImmutableTlvChain.readChain(messageTlv.getData());

            // read each part of the multipart data
            StringBuffer messageBuffer = new StringBuffer();
            Tlv[] parts = msgTLVs.getTlvs(TYPE_MESSAGE_PARTS);
            for (int i = 0; i < parts.length; i++) {
                ByteBlock partBlock = parts[i].getData();

                int charsetCode = BinaryTools.getUShort(partBlock, 0);
                // skip the two-byte character subset. I guess it doesn't
                // matter.
                ByteBlock messageBlock = partBlock.subBlock(4);

                String charset;
                if (charsetCode == CHARSET_ASCII) charset = "US-ASCII";
                else if (charsetCode == CHARSET_ISO) charset = "ISO-8859-1";
                else if (charsetCode == CHARSET_UTF16) charset = "UTF-16BE";
                else continue;

                String string;
                try {
                    string = ByteBlock.createString(messageBlock, charset);
                } catch (UnsupportedEncodingException impossible) { continue; }

                messageBuffer.append(string);
            }

            // and set the message to the sum of all the parts
            message = messageBuffer.toString();
        } else {
            message = null;
        }
        if (iconTlv != null) {
            ByteBlock iconData = iconTlv.getData();
            iconInfo = OldIconHashData.readOldIconHashData(iconData);
        } else {
            iconInfo = null;
        }
    }

    /**
     * Creates a new outgoing IM ICBM with the given properties.
     *
     * @param command the SNAC command subtype of this command
     * @param icbmCookie the eight-byte ICBM cookie to attach to this command
     * @param message the instant message
     * @param autoResponse whether this is an auto-response or not
     * @param wantsIcon whether to request the receiving user's buddy icon
     * @param iconInfo a set of our own buddy icon information
     */
    AbstractImIcbm(int command, long icbmCookie, String message,
            boolean autoResponse, boolean wantsIcon, OldIconHashData iconInfo) {
        super(IcbmCommand.FAMILY_ICBM, command, icbmCookie, CHANNEL_IM);

        this.message = message;
        this.autoResponse = autoResponse;
        this.wantsIcon = wantsIcon;
        this.iconInfo = iconInfo;
    }

    /**
     * Returns the instant message sent with this command.
     *
     * @return the text of the message
     */
    public final String getMessage() {
        return message;
    }

    /**
     * Returns whether this message was an "auto-response."
     *
     * @return whether this message was an auto-response
     */
    public final boolean isAutoResponse() {
        return autoResponse;
    }

    /**
     * Returns whether the sender is requesting a buddy icon.
     *
     * @return whether the sender wants our buddy icon (or whether we want the
     *         receiver's, if this is an outgoing IM)
     */
    public final boolean senderWantsIcon() {
        return wantsIcon;
    }

    /**
     * Returns a set of icon data provided by the sender, or <code>null</code>
     * if none was sent.
     *
     * @return the sender's buddy icon information
     */
    public final OldIconHashData getIconInfo() {
        return iconInfo;
    }

    /**
     * Writes the IM fields of this ICBM, such as message and icon data, to the
     * given stream, as a set of TLV's.
     *
     * @param out the stream to which to rwite
     * @throws IOException if an I/O error occurs
     */
    final void writeImTlvs(OutputStream out) throws IOException {
        if (message != null) {
            ByteArrayOutputStream msgout = new ByteArrayOutputStream();

            EncodedStringInfo encInfo = MinimalEncoder.encodeMinimally(message);

            String charset = encInfo.getCharset();
            int charsetCode;
            if (charset == MinimalEncoder.ENCODING_ASCII) {
                charsetCode = CHARSET_ASCII;
            } else if (charset == MinimalEncoder.ENCODING_ISO) {
                charsetCode = CHARSET_ISO;
            } else if (charset == MinimalEncoder.ENCODING_UTF16) {
                charsetCode = CHARSET_UTF16;
            } else {
                // this shouldn't ever really happen, but it's nice to have
                // something in case it does.
                charsetCode = CHARSET_ASCII;
            }

            BinaryTools.writeUShort(msgout, charsetCode);
            BinaryTools.writeUShort(msgout, CHARSETSUBCODE_DEFAULT);
            msgout.write(encInfo.getData());

            // create the two TLV's inside a TLV chain, and write it out.
            MutableTlvChain chain = new DefaultMutableTlvChain();
            Tlv featuresTlv = new Tlv(TYPE_FEATURES);
            chain.addTlv(featuresTlv);

            ByteBlock msgPartBlock = ByteBlock.wrap(msgout.toByteArray());
            Tlv msgPartTlv = new Tlv(TYPE_MESSAGE_PARTS, msgPartBlock);
            chain.addTlv(msgPartTlv);

            new Tlv(TYPE_MESSAGE, ByteBlock.createByteBlock(chain)).write(out);
        }

        if (autoResponse) new Tlv(TYPE_AUTO).write(out);
        if (wantsIcon) new Tlv(TYPE_ICON_REQ).write(out);

        if (iconInfo != null) {
            ByteBlock iconInfoBlock = ByteBlock.createByteBlock(iconInfo);
            new Tlv(TYPE_ICONINFO, iconInfoBlock).write(out);
        }
    }
}
