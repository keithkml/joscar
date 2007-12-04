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
 *  File created by keith @ Mar 3, 2003
 *
 */

package net.kano.joscar.snaccmd.ssi;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.snac.CmdType;
import net.kano.joscar.snac.SnacCmdFactory;

import java.util.List;

/**
 * A SNAC command factory for the client-bound commands provided in this
 * package, appropriate for use by an AIM client.
 */
public class ClientSsiCmdFactory implements SnacCmdFactory {
    /** The SNAC command types supported by this factory. */
    protected static final List<CmdType> SUPPORTED_TYPES = DefensiveTools.asUnmodifiableList(
        new CmdType(SsiCommand.FAMILY_SSI, SsiCommand.CMD_RIGHTS),
        new CmdType(SsiCommand.FAMILY_SSI, SsiCommand.CMD_SSI_DATA),
        new CmdType(SsiCommand.FAMILY_SSI, SsiCommand.CMD_UNCHANGED),
        new CmdType(SsiCommand.FAMILY_SSI, SsiCommand.CMD_MOD_ACK),
        new CmdType(SsiCommand.FAMILY_SSI, SsiCommand.CMD_PRE_MOD),
        new CmdType(SsiCommand.FAMILY_SSI, SsiCommand.CMD_POST_MOD),
        new CmdType(SsiCommand.FAMILY_SSI, SsiCommand.CMD_CREATE_ITEMS),
        new CmdType(SsiCommand.FAMILY_SSI, SsiCommand.CMD_MODIFY_ITEMS),
        new CmdType(SsiCommand.FAMILY_SSI, SsiCommand.CMD_DELETE_ITEMS),

        new CmdType(SsiCommand.FAMILY_SSI, SsiCommand.CMD_AUTH_REPLY_RECV),
        new CmdType(SsiCommand.FAMILY_SSI, SsiCommand.CMD_AUTH_REQUEST_RECV),
        new CmdType(SsiCommand.FAMILY_SSI, SsiCommand.CMD_YOU_WERE_ADDED_RECV));

    public List<CmdType> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    public SnacCommand genSnacCommand(SnacPacket packet) {
        if (packet.getFamily() != SsiCommand.FAMILY_SSI) return null;

        int command = packet.getCommand();

        if (command == SsiCommand.CMD_RIGHTS) {
            return new SsiRightsCmd(packet);
        } else if (command == SsiCommand.CMD_SSI_DATA) {
            return new SsiDataCmd(packet);
        } else if (command == SsiCommand.CMD_UNCHANGED) {
            return new SsiUnchangedCmd(packet);
        } else if (command == SsiCommand.CMD_MOD_ACK) {
            return new SsiDataModResponse(packet);
        } else if (command == SsiCommand.CMD_PRE_MOD) {
            return new PreModCmd(packet);
        } else if (command == SsiCommand.CMD_POST_MOD) {
            return new PostModCmd(packet);
        } else if (command == SsiCommand.CMD_CREATE_ITEMS) {
            return new CreateItemsCmd(packet);
        } else if (command == SsiCommand.CMD_MODIFY_ITEMS) {
            return new ModifyItemsCmd(packet);
        } else if (command == SsiCommand.CMD_DELETE_ITEMS) {
            return new DeleteItemsCmd(packet);

        } else if (command == SsiCommand.CMD_AUTH_REPLY_RECV) {
            return new AuthReplyCmd(packet);
        } else if (command == SsiCommand.CMD_AUTH_REQUEST_RECV) {
            return new BuddyAuthRequest(packet);
        } else if (command == SsiCommand.CMD_YOU_WERE_ADDED_RECV) {
            return new BuddyAddedYouCmd(packet);

        } else {
            return null;
        }
    }
}
