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
 *  File created by keith @ Mar 6, 2003
 *
 */

package net.kano.joscar.snaccmd.icbm;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.ImEncodedString;
import net.kano.joscar.ImEncodingParams;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.snaccmd.AbstractIcbm;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.tlv.MutableTlvChain;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;
import net.kano.joscar.tlv.TlvTools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;

/**
 * A base class for the two IM-based ICBM commands in this family. These
 * commands are {@link SendImIcbm} and {@link RecvImIcbm}.
 */
public abstract class AbstractImIcbm extends AbstractIcbm {
    /** A block of 4 null bytes for use in an encryption block. */
    private static final ByteBlock ENCBLOCK_NULLS
            = ByteBlock.wrap(new byte[] { 0, 0, 0, 0 });

    /** A TLV type containing the text of the Instant Message. */
    private static final int TYPE_MESSAGE = 0x0002;
    /** A TLV type containing buddy icon information. */
    private static final int TYPE_ICONINFO = 0x0008;
    /** A TLV type containing whether or not this is an autoresponse. */
    private static final int TYPE_AUTORESPONSE = 0x0004;
    /** A TLV type containing whether the user is requesting our icon. */
    private static final int TYPE_ICON_REQ = 0x0009;
    /** A TLV type containing information about the sender's AIM Expression. */
    private static final int TYPE_EXPRESSION_INFO = 0x000d;
    
    /** A TLV type containing a set of "features." */
    private static final int TYPE_FEATURES = 0x0501;
    /** A TLV type containing the message. */
    private static final int TYPE_MESSAGE_PARTS = 0x0101;
    /** A TLV type containing an encryption code. */
    private static final int TYPE_ENCRYPTION_CODE = 0x0d01;

    /**
     * A "features block" used by some versions of AIM that seems to be most
     * compatible with all AIM clients (including SMS cell phones).
     */
    public static final ByteBlock FEATURES_DEFAULT = ByteBlock.wrap(
            new byte[] { 0x01, 0x01, 0x01, 0x02 /* FAILS , 0x01, 0x01 */}
    );

    /** The Instant Message. */
    private InstantMessage message;
    /** Whether this message is an auto-response. */
    private boolean autoResponse;
    /** Whether the user wants our buddy icon. */
    private boolean wantsIcon;
    /** A set of icon data provided by the user who sent this IM. */
    private OldIconHashInfo iconInfo;
    /** A list of AIM Expressions information blocks. */
    private List<ExtraInfoBlock> expressionInfoBlocks;
    /** This ICBM's IM "features" block. */
    private ByteBlock featuresBlock;

    /**
     * Generates an IM ICBM from the given incoming SNAC packet and with the
     * given SNAC command subtype.
     *
     * @param command the SNAC command subtype of this command
     * @param packet an incoming IM ICBM
     */
    protected AbstractImIcbm(int command, SnacPacket packet) {
        super(IcbmCommand.FAMILY_ICBM, command, packet);
    }

    /**
     * Extracts fields such as the message body and icon information from the
     * given TLV chain.
     *
     * @param chain the TLV chain from which to read
     */
    protected final void processImTlvs(TlvChain chain) {
        DefensiveTools.checkNull(chain, "chain");
        
        // get some TLV's
        Tlv messageTlv = chain.getLastTlv(TYPE_MESSAGE);
        Tlv iconTlv = chain.getLastTlv(TYPE_ICONINFO);
        Tlv expInfoTlv = chain.getLastTlv(TYPE_EXPRESSION_INFO);

        // these we just know based on whether the TLV is there
        autoResponse = chain.hasTlv(TYPE_AUTORESPONSE);
        wantsIcon = chain.hasTlv(TYPE_ICON_REQ);

        // and go through the TLV's we actually need to parse
        if (messageTlv != null) {
            // this is a normal IM. there are TLV's in here. two or more of
            // them.
            TlvChain msgTLVs = TlvTools.readChain(messageTlv.getData());

            Tlv featuresTlv = msgTLVs.getLastTlv(TYPE_FEATURES);
            if (featuresTlv != null) {
                featuresBlock = featuresTlv.getData();
            } else {
                featuresBlock = null;
            }

            if (msgTLVs.hasTlv(TYPE_ENCRYPTION_CODE)) {
                Tlv msgDataTlv = msgTLVs.getFirstTlv(TYPE_MESSAGE_PARTS);
                ByteBlock block = msgDataTlv.getData().subBlock(4);
                int encCode = msgTLVs.getUShort(TYPE_ENCRYPTION_CODE);
                message = new InstantMessage(encCode, block);

            } else {
                // read each part of the multipart data
                List<Tlv> parts = msgTLVs.getTlvs(TYPE_MESSAGE_PARTS);
                String msgString;
                if (parts.size() == 1) {
                    msgString = extractMsgFromPart(parts.get(0).getData());
                } else {
                    StringBuffer messageBuffer = new StringBuffer();

                    for (Tlv part : parts) {
                        ByteBlock partBlock = part.getData();

                        String message = extractMsgFromPart(partBlock);

                        messageBuffer.append(message);
                    }

                    // and set the message to the sum of all the parts
                    msgString = messageBuffer.toString();
                }
                message = new InstantMessage(msgString);
            }
        } else {
            featuresBlock = null;
            message = null;
        }

        if (iconTlv != null) {
            ByteBlock iconData = iconTlv.getData();
            iconInfo = OldIconHashInfo.readIconHashFromImTlvData(iconData);
        } else {
            iconInfo = null;
        }

        if (expInfoTlv != null) {
            ByteBlock data = expInfoTlv.getData();
            expressionInfoBlocks = ExtraInfoBlock.readExtraInfoBlocks(data);
        }
    }

    private static String extractMsgFromPart(ByteBlock partBlock) {
        int charsetCode = BinaryTools.getUShort(partBlock, 0);
        int charsetSubcode = BinaryTools.getUShort(partBlock, 2);
        ByteBlock messageBlock = partBlock.subBlock(4);

        ImEncodingParams encoding
                = new ImEncodingParams(charsetCode, charsetSubcode);
        return ImEncodedString.readImEncodedString(
                encoding, messageBlock);
    }

    /**
     * Creates a new outgoing IM ICBM with the given properties.
     *
     * @param command the SNAC command subtype of this command
     * @param messageId the eight-byte ICBM message ID to attach to this command
     * @param message the instant message
     * @param autoResponse whether this is an auto-response or not
     * @param wantsIcon whether to request the receiving user's buddy icon
     * @param iconInfo a set of our own buddy icon information
     * @param expInfoBlocks a list of AIM Expression information blocks
     * @param featuresBlock an AIM "features" block, like {@link #FEATURES_DEFAULT}
     */
    protected AbstractImIcbm(int command, long messageId,
            InstantMessage message, boolean autoResponse, boolean wantsIcon,
            OldIconHashInfo iconInfo, Collection<ExtraInfoBlock> expInfoBlocks,
            ByteBlock featuresBlock) {
        super(IcbmCommand.FAMILY_ICBM, command, messageId, CHANNEL_IM);

        List<ExtraInfoBlock> safeExpInfoBlocks
                = DefensiveTools.getSafeListCopy(
                expInfoBlocks, "expInfoBlocks");

        this.message = message;
        this.autoResponse = autoResponse;
        this.wantsIcon = wantsIcon;
        this.iconInfo = iconInfo;
        this.expressionInfoBlocks = safeExpInfoBlocks;
        this.featuresBlock = featuresBlock;
    }

    /**
     * Returns the instant message sent with this command.
     *
     * @return the instant message sent
     */
    public final InstantMessage getMessage() { return message; }

    /**
     * Returns whether this message was an "auto-response."
     *
     * @return whether this message was an auto-response
     */
    public final boolean isAutoResponse() { return autoResponse; }

    /**
     * Returns whether the sender is requesting a buddy icon.
     *
     * @return whether the sender wants our buddy icon (or whether we want the
     *         receiver's, if this is an outgoing IM)
     */
    public final boolean senderWantsIcon() { return wantsIcon; }

    /**
     * Returns a set of icon data provided by the sender, or <code>null</code>
     * if none was sent.
     *
     * @return the sender's buddy icon information
     */
    public final OldIconHashInfo getIconInfo() { return iconInfo; }

    /**
     * Returns the list of AIM Expression information blocks sent in this
     * command.
     *
     * @return the list of AIM Expression information blocks sent in this
     *         command, or <code>null</code> if none were sent
     */
    public final List<ExtraInfoBlock> getAimExpressionInfo() {
        return expressionInfoBlocks;
    }

    /**
     * Returns this ICBM's "features" block, or <code>null</code> if none was
     * sent.
     *
     * @return this ICBM's "features" block
     */
    public final ByteBlock getFeaturesBlock() { return featuresBlock; }

    /**
     * Writes the IM fields of this ICBM, such as message and icon data, to the
     * given stream, as a set of TLV's.
     *
     * @param out the stream to which to rwite
     * @throws IOException if an I/O error occurs
     */
    final void writeImTlvs(OutputStream out) throws IOException {
      if (autoResponse) new Tlv(TYPE_AUTORESPONSE).write(out);

        InstantMessage message = this.message;
        if (message != null) {
            MutableTlvChain chain = TlvTools.createMutableChain();
            ByteBlock messageData;

            if (message.isEncrypted()) {
                Tlv encCodeTlv = Tlv.getUShortInstance(TYPE_ENCRYPTION_CODE,
                                    message.getEncryptionCode());
                ByteBlock encBlock = ENCBLOCK_NULLS;
                ByteBlock encryptData = message.getEncryptedData();
                messageData = ByteBlock.createByteBlock(
                        Arrays.asList(encBlock, encryptData));

                chain.addTlv(encCodeTlv);

            } else {
                ByteArrayOutputStream msgout = new ByteArrayOutputStream();

                ImEncodedString encInfo
                        = ImEncodedString.encodeString(message.getMessage());

                ImEncodingParams encoding = encInfo.getEncoding();

                BinaryTools.writeUShort(msgout, encoding.getCharsetCode());
                BinaryTools.writeUShort(msgout, encoding.getCharsetSubcode());
                msgout.write(encInfo.getBytes());

                messageData = ByteBlock.wrap(msgout.toByteArray());
            }

            if (featuresBlock != null) {
                chain.addTlv(new Tlv(TYPE_FEATURES, featuresBlock));
            }
            chain.addTlv(new Tlv(TYPE_MESSAGE_PARTS, messageData));
            new Tlv(TYPE_MESSAGE, ByteBlock.createByteBlock(chain)).write(out);
        }

        if (iconInfo != null) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream(20);
            iconInfo.writeToImTlv(bout);

            ByteBlock iconInfoBlock = ByteBlock.wrap(bout.toByteArray());
            new Tlv(TYPE_ICONINFO, iconInfoBlock).write(out);
        }
        if (wantsIcon) new Tlv(TYPE_ICON_REQ).write(out);

        if (expressionInfoBlocks != null) {
            ByteBlock blocks = ByteBlock.createByteBlock(expressionInfoBlocks);
            new Tlv(TYPE_EXPRESSION_INFO, blocks).write(out);
        }
    }
}
