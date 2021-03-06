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
 *  File created by keith @ Feb 18, 2003
 *
 */

package net.kano.joscar.snaccmd.auth;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.snac.CmdType;
import net.kano.joscar.snac.SnacCmdFactory;

import java.util.List;

/**
 * A SNAC command factory for the server-bound commands provided by this
 * package.
 */
public class ServerAuthCmdFactory implements SnacCmdFactory {
    /** A list of command types supported by this package. */
    protected static final List<CmdType> SUPPORTED_TYPES = DefensiveTools.asUnmodifiableList(
        new CmdType(AuthCommand.FAMILY_AUTH, AuthCommand.CMD_KEY_REQ),
        new CmdType(AuthCommand.FAMILY_AUTH, AuthCommand.CMD_AUTH_REQ),
        new CmdType(AuthCommand.FAMILY_AUTH, AuthCommand.CMD_SECURID_RESPONSE));

    public List<CmdType> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    public SnacCommand genSnacCommand(SnacPacket packet) {
        if (packet.getFamily() != AuthCommand.FAMILY_AUTH) return null;

        int command = packet.getCommand();

        if (command == AuthCommand.CMD_KEY_REQ) {
            return new KeyRequest(packet);
        } else if (command == AuthCommand.CMD_AUTH_REQ) {
            return new AuthRequest(packet);
        } else if (command == AuthCommand.CMD_SECURID_RESPONSE) {
            return new SecuridResponse(packet);
        } else {
            return null;
        }
    }
}
