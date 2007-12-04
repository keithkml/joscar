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
 *  File created by jkohen @ Oct 13, 2003
 *
 */

package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.LEBinaryTools;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;
import net.kano.joscar.tlv.TlvTools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A base class for the two ICQ commands provided in this
 * package. The two commands are {@link FromIcqCmd} and {@link ToIcqCmd}.
 */
public abstract class AbstractIcqCmd extends IcqSnacCommand {
	/** A command subtype for requesting offline messages. */
	public static final IcqType CMD_OFFLINE_MSG_REQ = new IcqType(60);
	/** A command subtype for acknoweldging the reception of offline messages. */
	public static final IcqType CMD_OFFLINE_MSG_ACK = new IcqType(62);
	/** A command subtype for sending an offline message. */
	public static final IcqType CMD_OFFLINE_MSG = new IcqType(65);
	/** A command subtype for marking the end of the offline messages. */
	public static final IcqType CMD_OFFLINE_MSG_DONE = new IcqType(66);

	/** A command subtype for requesting full information about another user. */
    public static final IcqType CMD_META_FULL_INFO_REQ = new IcqType(2000, 1202);
    /** A command subtype for requesting short information about another user. */
	public static final IcqType CMD_META_SHORT_INFO_REQ = new IcqType(2000, 1210);
    /** A command subtype for sending basic information about a user. */
    public static final IcqType CMD_META_BASIC_INFO_CMD = new IcqType(2010, 0xC8);
    /** A command subtype for sending information about a user's job. */
    public static final IcqType CMD_META_WORK_INFO_CMD = new IcqType(2010, 0xD2);
    /** A command subtype for sending additional information about a user. */
    public static final IcqType CMD_META_MORE_INFO_CMD = new IcqType(2010, 0xDC);
    /** A command subtype for sending user's notes (about) information. */
    public static final IcqType CMD_META_NOTES_INFO_CMD = new IcqType(2010, 0xE6);
    /** A command subtype for sending user's additional e-mail addresses. */
    public static final IcqType CMD_META_EMAIL_INFO_CMD = new IcqType(2010, 0xEB);
    /** A command subtype for sending user's interests. */
    public static final IcqType CMD_META_INTERESTS_INFO_CMD = new IcqType(2010, 0xF0);
    /** A command subtype for sending user's background and affiliations. */
    public static final IcqType CMD_META_AFFILIATIONS_INFO_CMD = new IcqType(2010, 0xFA);
    /** A command subtype for sending short information about a user. */
	public static final IcqType CMD_META_SHORT_INFO_CMD = new IcqType(2010, 0x104);
    /** A command subtype for sending user's homepage category. */
    public static final IcqType CMD_META_HOMEPAGE_CATEGORY_INFO_CMD = new IcqType(2010, 0x10E);

    /** A command subtype for marking setting full info information. */
    public static final IcqType CMD_META_SET_ACK = new IcqType(0x07DA, 0x0C3F);
    /** A command subtype for changing user's homepage category. */
    public static final IcqType CMD_META_SET_CMD = new IcqType(0x07D0, 0x0c3a);

	/** A command subtype for setting additional security information about the user. */
	public static final IcqType CMD_META_SECURITY_CMD = new IcqType(2000, 1060);
	/** A command subtype for replying to {@link #CMD_META_SECURITY_CMD}. */
	public static final IcqType CMD_META_SECURITY_ACK = new IcqType(2010, 160);

	/** A TLV type containing the ICQ-specific data. */
	private static final int TYPE_ICQ_DATA = 0x0001;

	/**
	 * For ToIcqCmd packets this is the destination UIN;
	 * for FromIcqCmd, this is the sender's UIN.
	 */
	private long icqUIN;
    /** The command subtype. */
    private IcqType icqType;
    /** The sequence id for this command. */
    private int icqID;
    /** Command-specific data. */
    private ByteBlock icqData;

    /**
     * Generates an ICQ command from the given incoming SNAC packet.
     *
     * @param command the SNAC command subtype of this command
     * @param packet an incoming ICQ packet
     */
    protected AbstractIcqCmd(int command, SnacPacket packet) {
        super(FAMILY_ICQ, command);

        DefensiveTools.checkNull(packet, "packet");

        ByteBlock tlvBlock = packet.getData();
        TlvChain chain = TlvTools.readChain(tlvBlock);

        processIcqTlvs(chain);
    }

    /**
     * Creates a new outgoing ICQ command with the given properties.
     *
     * @param command the SNAC command subtype of this command
     * @param uin an ICQ UIN as an integer value
     * @param type the ICQ subtype for the command
	 * @param id the sequence ID for this command
     */
    protected AbstractIcqCmd(int command, long uin, IcqType type, int id) {
        super(FAMILY_ICQ, command);

        DefensiveTools.checkNull(type, "type");

		icqUIN = uin;
		icqType = type;
		icqID = id;
		icqData = null;
    }

    /**
     * Returns the UIN of the user associated with this command.
     *
     * @return an ICQ UIN as an integer value
     */
    public final long getUIN() {
	    return icqUIN;
    }

    /**
     * Returns this ICQ-specific command's subtype.
     *
     * @return the old ICQ command subtype.
     */
    public final IcqType getType() {
	    return icqType;
    }

    /**
     * Returns the sequence ID for this command.
     *
     * @return the sequence ID.
     */
    public final int getId() {
	    return icqID;
    }

    /**
     * Extracts ICQ-specific fields from the given TLV chain.
     *
     * @param chain the chain from which to read.
     */
    private void processIcqTlvs(TlvChain chain) {
		DefensiveTools.checkNull(chain, "chain");

		Tlv icqDataTlv = chain.getLastTlv(TYPE_ICQ_DATA);
		if (icqDataTlv == null) {
	    	icqUIN = -1;
		    icqType = null;
		    icqID = -1;

        } else {
	    	ByteBlock icqBlock = icqDataTlv.getData();

		    icqUIN = LEBinaryTools.getUInt(icqBlock, 2);
		    icqID = LEBinaryTools.getUShort(icqBlock, 8);

            int hdrlen = 8; // The expected header length, not counting the length field itself.
		    int primary = LEBinaryTools.getUShort(icqBlock, 6);
		    if (-1 == primary) {
		    	icqType = null;
		    } else if (primary < 1000) { // Is there a secondary command type?
		    	icqType = new IcqType(primary);
		    } else {
				int secondary = LEBinaryTools.getUShort(icqBlock, 10);
				icqType = -1 == secondary ? null : new IcqType(primary, secondary);
				hdrlen = 10;
		    }

	    	if (icqBlock.getLength() >= hdrlen + 2) {
				icqData = icqBlock.subBlock(hdrlen + 2);
	    	} else {
				icqData = null;
		    }
		}
    }

    /**
     * Returns the ICQ-specific data in this ICQ command. The
     * contents of this block vary from subcommand to subcommand.
     *
     * @return the ICQ-specific data
     */
    public final ByteBlock getIcqData() {
		return icqData;
    }

    /**
     * Writes the ICQ-specific fields of this command to the given
     * stream.
     *
     * @param out the stream to write to
     * @throws IOException if an I/O error occurs
     */
    public final void writeData(OutputStream out) throws IOException {
        /* The length of icqData is needed early in the write process.
		 * Write it now to an OutputStream and use that to calculate its size. */
		ByteArrayOutputStream icqDataOut = new ByteArrayOutputStream();
		writeIcqData(icqDataOut);

		int hdrlen = 8; // The expected header length, not counting the length field itself.
		int primary = null != icqType ? icqType.getPrimary() : 0;
		int secondary = null != icqType ? icqType.getSecondary() : 0;
		if (0 != secondary) {
            hdrlen = 10;
        }

        ByteArrayOutputStream icqout = new ByteArrayOutputStream();
        int length = hdrlen + icqDataOut.size();
        LEBinaryTools.writeUShort(icqout, length);
        LEBinaryTools.writeUInt(icqout, icqUIN);
		LEBinaryTools.writeUShort(icqout, primary);
        LEBinaryTools.writeUShort(icqout, icqID);
		if (0 != secondary)
            LEBinaryTools.writeUShort(icqout, secondary);

        icqDataOut.writeTo(icqout);

        new Tlv(TYPE_ICQ_DATA, ByteBlock.wrap(icqout.toByteArray())).write(out);
    }

    public abstract void writeIcqData(OutputStream out) throws IOException;

    public String toString() {
        return "AbstractIcqCmd: type=" + icqType
        	+ ", uin=" + icqUIN
			+ ", id=" + icqID
			+ ", on top of " + super.toString();
    }

    protected static IcqType readIcqType(SnacPacket packet) {
        ByteBlock tlvBlock = packet.getData();
        TlvChain chain = TlvTools.readChain(tlvBlock);
        Tlv icqDataTlv = chain.getLastTlv(TYPE_ICQ_DATA);
        if (icqDataTlv == null) {
            return null;
        }
        ByteBlock icqBlock = icqDataTlv.getData();
        int primary = LEBinaryTools.getUShort(icqBlock, 6);
        if (-1 == primary) {
            return null;
        } else if (primary < 1000) { // Is there a secondary command type?
            return new IcqType(primary);
        } else {
            int secondary = LEBinaryTools.getUShort(icqBlock, 10);
            return -1 == secondary ? null : new IcqType(primary, secondary);
        }
    }
}
