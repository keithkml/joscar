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
 *  File created by keith @ Feb 25, 2004
 *
 */

package net.kano.joscar.snaccmd.ssi;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.OscarTools;
import net.kano.joscar.StringBlock;
import net.kano.joscar.flapcmd.SnacPacket;

import java.io.OutputStream;
import java.io.IOException;

/**
 * A SNAC command sent to request that the client's screenname be removed from
 * a given user's buddy list. This command is not currently used by any AIM
 * client; it was first used in a test client released by AOL called TestBuddy
 * 3.0. No warning is given to the user whose buddy list is being modified by
 * this command; it is completely transparent to the remote user.
 *
 * @snac.src client
 * @snac.cmd 0x13 0x16
 */
public class RemoveMeCmd extends SsiCommand {
    /**
     * The screenname of the user from whose buddy list the client's screenname
     * should be removed.
     */
    private final String screenname;

    /**
     * Creates a new remove-me SNAC command from the given incoming SNAC packet.
     *
     * @param packet an incoming remove-me SNAC packet
     */
    protected RemoveMeCmd(SnacPacket packet) {
        super(CMD_REMOVE_ME);

        DefensiveTools.checkNull(packet, "packet");

        ByteBlock data = packet.getData();
        StringBlock sb = OscarTools.readScreenname(data);
        if (sb != null) screenname = sb.getString();
        else screenname = null;
    }

    /**
     * Creates a new remove-me command that will remove your screenname from the
     * given user's buddy list.
     *
     * @param screenname the screenname of the user from whose buddy list this
     *        client's screenname should be removed
     */
    public RemoveMeCmd(String screenname) {
        super(CMD_REMOVE_ME);

        this.screenname = screenname;
    }

    /**
     * Returns the screenname of the user whose buddy list this client's
     * screenname should be removed.
     *
     * @return the screenname sent in this command, or <code>null</code> if none
     *         was sent
     */
    public final String getScreenname() { return screenname; }
    
    public void writeData(OutputStream out) throws IOException {
        if (screenname != null) {
            OscarTools.writeScreenname(out, screenname);
        }
    }

    public String toString() {
        return "SsiRemoveMeCmd: screenname=" + screenname;
    }
}
