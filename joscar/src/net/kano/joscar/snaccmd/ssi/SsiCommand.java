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
 *  File created by keith @ Mar 3, 2003
 *
 */

package net.kano.joscar.snaccmd.ssi;

import net.kano.joscar.snac.SnacCommand;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;

public abstract class SsiCommand extends SnacCommand {
    public static final int FAMILY_SSI = 0x0013;

    public static final SnacFamilyInfo FAMILY_INFO
            = new SnacFamilyInfo(FAMILY_SSI, 0x0003, 0x0110, 0x0629);

    public static final int CMD_RIGHTS_REQ = 0x0002;
    public static final int CMD_RIGHTS = 0x0003;
    public static final int CMD_DATA_REQ = 0x0004;
    public static final int CMD_DATA_CHECK = 0x0005;
    public static final int CMD_SSI_DATA = 0x0006;
    public static final int CMD_ACTIVATE = 0x0007;
    public static final int CMD_UNCHANGED = 0x000f;
    public static final int CMD_CREATE_ITEMS = 0x0008;
    public static final int CMD_MODIFY_ITEMS = 0x0009;
    public static final int CMD_DELETE_ITEMS = 0x000a;
    public static final int CMD_MOD_ACK = 0x000e;
    public static final int CMD_PRE_MOD = 0x0011;
    public static final int CMD_POST_MOD = 0x0012;

    protected SsiCommand(int command) {
        super(FAMILY_SSI, command);
    }
}
