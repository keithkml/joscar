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
 *  File created by keith @ Mar 1, 2003
 *
 */

package net.kano.joscar.snaccmd.conn;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.snaccmd.ExtraIconInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * A SNAC command sent to tell the client what his or her current buddy icon
 * is.
 *
 * @snac.src server
 * @snac.cmd 0x01 0x21
 */
public class YourIconAck extends ConnCommand {
    /** The set of icon information blocks contained in this command. */
    private final ExtraIconInfo[] iconInfos;

    /**
     * Generates a new icon acknowledgement command from the given incoming
     * SNAC packet.
     *
     * @param packet the incoming icon acknowledgement packet
     */
    protected YourIconAck(SnacPacket packet) {
        super(CMD_YOUR_ICON);

        DefensiveTools.checkNull(packet, "packet");

        ByteBlock snacData = packet.getData();

        iconInfos = ExtraIconInfo.readExtraIconInfos(snacData);
    }

    /**
     * Creates a new outgoing icon acknowledgement command with the given list
     * of icon information blocks.
     *
     * @param iconInfos the icon information blocks to send in this command
     */
    public YourIconAck(ExtraIconInfo[] iconInfos) {
        super(CMD_YOUR_ICON);

        this.iconInfos = (ExtraIconInfo[]) (iconInfos == null
                ? null
                : iconInfos.clone());
    }

    /**
     * Returns the list of icon information blocks sent in this command. See
     * {@link ExtraIconInfo} for details.
     *
     * @return this command's icon information blocks
     */
    public final ExtraIconInfo[] getIconInfos() {
        return (ExtraIconInfo[]) (iconInfos == null ? null : iconInfos.clone());
    }

    public void writeData(OutputStream out) throws IOException {
        if (iconInfos != null) ByteBlock.createByteBlock(iconInfos).write(out);
    }

    public String toString() {
        return "YourIconAck: infos=" +
                (iconInfos == null ? null : Arrays.asList(iconInfos));
    }
}
