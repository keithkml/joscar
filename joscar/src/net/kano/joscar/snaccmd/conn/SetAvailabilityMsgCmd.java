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
 *  File created by keith @ Aug 1, 2003
 *
 */

package net.kano.joscar.snaccmd.conn;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.BinaryTools;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joscar.tlv.ImmutableTlvChain;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayOutputStream;

/**
 * A SNAC command used to set an "iChat availability message." An availability
 * message for a buddy is shown under his or her screenname in Apple's iChat
 * AIM client. After setting an availability message, it appears as an
 * <code>ExtraInfoBlock</code> in one's {@link
 * net.kano.joscar.snaccmd.FullUserInfo}.
 *
 * @snac.src client
 * @snac.cmd 0x01 0x1e
 */
public class SetAvailabilityMsgCmd extends ConnCommand {
    /** A TLV type for the availability message extra info block. */
    private static final int TYPE_DATA = 0x001d;

    /** The extra info block stored in this set-availability-message command. */
    private final ExtraInfoBlock infoBlock;
    /** The availability message stored in this command. */
    private final String message;

    /**
     * Generates a new set-availability-message command from the given incoming
     * SNAC packet.
     *
     * @param packet a set-availability-message SNAC command
     */
    protected SetAvailabilityMsgCmd(SnacPacket packet) {
        super(CMD_SETAVAILABILITY);

        ByteBlock data = packet.getData();

        TlvChain chain = ImmutableTlvChain.readChain(data);

        Tlv dataTlv = chain.getLastTlv(TYPE_DATA);

        if (dataTlv != null) {
            ByteBlock availData = dataTlv.getData();

            infoBlock = ExtraInfoBlock.readExtraInfoBlock(availData);
            ByteBlock msgData = infoBlock.getExtraData().getData();
            String msg = null;
            try {
                msg = ByteBlock.createString(msgData, "UTF-8");
            } catch (UnsupportedEncodingException impossible) { }
            message = msg;
        } else {
            message = null;
            infoBlock = null;
        }
    }

    /**
     * Creates a new set-availability-message command containing the given
     * message. Note that to unset an availability message, one should send a
     * set-availability-message command with an empty (zero-length) message.
     * <br>
     * <br>
     * (This constructor does the work of creating an
     * <code>ExtraInfoBlock</code> containing the message for you. For
     * information on how to create your own block for use in the {@link
     * #SetAvailabilityMsgCmd(ExtraInfoBlock)} constructor, see the {@link
     * ExtraInfoData} documentation.)
     *
     * @param message the availability message to send
     */
    public SetAvailabilityMsgCmd(String message) {
        super(CMD_SETAVAILABILITY);

        DefensiveTools.checkNull(message, "message");

        this.infoBlock = null;
        this.message = message;
    }

    /**
     * Creates a new set-availability-message command containing the given extra
     * information block.
     *
     * @param infoBlock an extra information block presumably containing an
     *        availability message
     */
    public SetAvailabilityMsgCmd(ExtraInfoBlock infoBlock) {
        super(CMD_SETAVAILABILITY);

        DefensiveTools.checkNull(infoBlock, "infoBlock");

        this.infoBlock = infoBlock;
        this.message = null;
    }

    /**
     * Returns the extra info block stored in this command. Note that the
     * returned value will be <code>null</code> if this object was created using
     * the {@link #SetAvailabilityMsgCmd(String)} constructor. The type code of
     * the returned extra info block will normally be {@link
     * ExtraInfoBlock#TYPE_AVAILMSG} and the flags in the nested
     * <code>ExtraInfoData</code> will normally be {@link
     * ExtraInfoData#FLAG_AVAILMSG_PRESENT}. For details on extracting the
     * availability message from this block, see the {@link ExtraInfoData}
     * documentation.
     *
     * @return the extra info block stored in this command
     */
    public final ExtraInfoBlock getInfoBlock() { return infoBlock; }

    /**
     * Returns the message stored in this command. Note that the returned value
     * will be <code>null</code> if this object was created using the {@link
     * #SetAvailabilityMsgCmd(ExtraInfoBlock)} constructor.
     *
     * @return the availability message sent in this command
     */
    public final String getMessage() { return message; }

    public void writeData(OutputStream out) throws IOException {
        ExtraInfoBlock infoBlock = null;;
        if (this.infoBlock != null) {
            infoBlock = this.infoBlock;
        } else if (message != null) {
            byte[] messageBytes = message.getBytes("UTF-8");

            ByteArrayOutputStream bout
                    = new ByteArrayOutputStream(4 + messageBytes.length);
            BinaryTools.writeUShort(bout, messageBytes.length);
            bout.write(messageBytes);
            BinaryTools.writeUShort(bout, 0);

            ByteBlock block = ByteBlock.wrap(bout.toByteArray());

            ExtraInfoData data = new ExtraInfoData(
                    ExtraInfoData.FLAG_AVAILMSG_PRESENT, block);
            infoBlock = new ExtraInfoBlock(
                    ExtraInfoBlock.TYPE_AVAILMSG, data);
        }

        if (infoBlock != null) {
            new Tlv(TYPE_DATA, ByteBlock.createByteBlock(infoBlock)).write(out);
        }
    }

    public String toString() {
        return "SetAvailabilityMsgCmd: message=" + message + ", block="
                + infoBlock;
    }
}
