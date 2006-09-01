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
 *  File created by jkohen @ Oct 12, 2003
 *
 */

package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.snac.CmdType;
import net.kano.joscar.snac.SnacCmdFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;

public class ClientIcqCmdFactory implements SnacCmdFactory {
    private static final List<CmdType> SUPPORTED_TYPES
            = Collections.unmodifiableList(Arrays.asList(
        new CmdType(IcqSnacCommand.FAMILY_ICQ, IcqSnacCommand.CMD_FROM_ICQ)));

    public List<CmdType> getSupportedTypes() { return SUPPORTED_TYPES; }

    public SnacCommand genSnacCommand(SnacPacket packet) {
        if (packet.getFamily() != IcqSnacCommand.FAMILY_ICQ)
            return null;

        int command = packet.getCommand();
        if (command == IcqSnacCommand.CMD_FROM_ICQ) {
            IcqType type = AbstractIcqCmd.readIcqType(packet);
            if (type.equals(AbstractIcqCmd.CMD_META_SHORT_INFO_CMD)) {
                return new MetaShortInfoCmd(packet);
            }
            if (type.equals(AbstractIcqCmd.CMD_META_BASIC_INFO_CMD)) {
                return new MetaBasicInfoCmd(packet);
            }
            if (type.equals(AbstractIcqCmd.CMD_META_WORK_INFO_CMD)) {
                return new MetaWorkInfoCmd(packet);
            }
            if (type.equals(AbstractIcqCmd.CMD_META_MORE_INFO_CMD)) {
                return new MetaMoreInfoCmd(packet);
            }
            if (type.equals(AbstractIcqCmd.CMD_META_NOTES_INFO_CMD)) {
                return new MetaNotesInfoCmd(packet);
            }
            if (type.equals(AbstractIcqCmd.CMD_META_EMAIL_INFO_CMD)) {
                return new MetaEmailInfoCmd(packet);
            }
            if (type.equals(AbstractIcqCmd.CMD_META_INTERESTS_INFO_CMD)) {
                return new MetaInterestsInfoCmd(packet);
            }
            if (type.equals(AbstractIcqCmd.CMD_META_AFFILIATIONS_INFO_CMD)) {
                return new MetaAffiliationsInfoCmd(packet);
            }
            if (type.equals(AbstractIcqCmd.CMD_META_HOMEPAGE_CATEGORY_INFO_CMD)) {
                return new MetaHomepageCategoryInfoCmd(packet);
            }
            if (type.equals(AbstractIcqCmd.CMD_OFFLINE_MSG)) {
                return new OfflineMsgIcqCmd(packet);
            }
            if (type.equals(AbstractIcqCmd.CMD_OFFLINE_MSG_DONE)) {
                return new OfflineMsgDoneCmd(packet);
            }
        }
        return null;
    }
}
