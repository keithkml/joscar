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
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;
import net.kano.joscar.tlv.TlvTools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * A SNAC command used to set the user's online status (ICQ-only) and the list
 * of {@linkplain ExtraInfoBlock "extra info blocks."}
 *
 * @snac.src client
 * @snac.cmd 0x01 0x1e
 */
public class SetExtraInfoCmd extends ConnCommand {
    public static final int ICQSTATUS_NONE = -1;

    /** A TLV type for the ICQ online status. */
    private static final int TYPE_ICQSTATUS = 0x0006;
    /** A TLV type for the extra info blocks. */
    private static final int TYPE_DATA = 0x001d;

    /** The user's ICQ online status, if any. */
    private final long icqstatus;
    /** The extra info blocks stored in this command. */
    private final List<ExtraInfoBlock> blocks;

    /**
     * Generates a new set-extra-info-blocks command from the given incoming
     * SNAC packet.
     *
     * @param packet a set-extra-info-blocks SNAC command
     */
    protected SetExtraInfoCmd(SnacPacket packet) {
        super(CMD_SETEXTRAINFO);

        ByteBlock data = packet.getData();

        TlvChain chain = TlvTools.readChain(data);

        Tlv statusTlv = chain.getLastTlv(TYPE_ICQSTATUS);
        if (statusTlv != null) {
            icqstatus = statusTlv.getDataAsUInt();
        } else {
            icqstatus = -1;
        }

        Tlv dataTlv = chain.getLastTlv(TYPE_DATA);

        if (dataTlv != null) {
            ByteBlock blockData = dataTlv.getData();

            blocks = ExtraInfoBlock.readExtraInfoBlocks(blockData);
        } else {
            blocks = null;
        }
    }

    public SetExtraInfoCmd(long icqstatus) {
        this(icqstatus, null);
    }

    public SetExtraInfoCmd(List<ExtraInfoBlock> blocks) {
        this(ICQSTATUS_NONE, blocks);
    }

    /**
     * Creates a new set-extra-info-blocks command containing the given extra
     * information blocks and with the given ICQ status code. The given status
     * code should be one of the {@code ICQSTATUS_*} constants in
     * {@link FullUserInfo}.
     *
     * @param icqstatus an ICQ availability status code
     * @param blocks a list of extra information blocks
     */
    public SetExtraInfoCmd(long icqstatus, List<ExtraInfoBlock> blocks) {
        super(CMD_SETEXTRAINFO);

        DefensiveTools.checkRange(icqstatus, "icqstatus", -1);

        this.icqstatus = icqstatus;
        this.blocks = DefensiveTools.getSafeListCopy(blocks, "blocks");
    }

    /**
     * Returns the ICQ online status sent in this command, or <code>-1</code> if
     * this field was not set.
     *
     * @return this command's ICQ online status, or <code>-1</code> if this
     *         field was not set
     */
    public final long getIcqStatus() { return icqstatus; }

    /**
     * Returns a list of the extra info blocks sent in this command.
     *
     * @return a list of the extra info blocks stored in this command
     */
    public final List<ExtraInfoBlock> getInfoBlocks() {
        return blocks;
    }

    public void writeData(OutputStream out) throws IOException {
        if (icqstatus != -1) {
            Tlv.getUIntInstance(TYPE_ICQSTATUS, icqstatus).write(out);
        }
        if (blocks != null) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            for (ExtraInfoBlock block : blocks) block.write(bout);

            new Tlv(TYPE_DATA, ByteBlock.wrap(bout.toByteArray())).write(out);
        }
    }

    public String toString() {
        return "SetExtraInfoCmd: blocks="
                + (blocks == null ? null : blocks)
                + (icqstatus != -1 ? ", icqstatus=" + icqstatus : "");
    }
}
