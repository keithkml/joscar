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
 *  File created by keith @ Apr 24, 2003
 *
 */

package net.kano.joscar.rvcmd;

import net.kano.joscar.rv.RvCommandFactory;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.snaccmd.icbm.AbstractRvIcbm;
import net.kano.joscar.rvcmd.sendfile.SendFileRvCmd;
import net.kano.joscar.rvcmd.sendfile.RejectFileSendRvCmd;
import net.kano.joscar.rvcmd.sendfile.AcceptFileSendRvCmd;
import net.kano.joscartests.GenericRvCommand;

public class DefaultRvCommandFactory implements RvCommandFactory {
    private static final CapabilityBlock[] SUPPORTED_CAPS
            = new CapabilityBlock[] {
                CapabilityBlock.BLOCK_FILE_SEND,
            };

    public CapabilityBlock[] getSupportedCapabilities() {
        return (CapabilityBlock[]) SUPPORTED_CAPS.clone();
    }

    public RvCommand genRvCommand(RecvRvIcbm rvIcbm) {
        CapabilityBlock block = rvIcbm.getCapability();
        int status = rvIcbm.getRvStatus();

        if (block.equals(CapabilityBlock.BLOCK_FILE_SEND)) {
            if (status == AbstractRvIcbm.STATUS_REQUEST) {
                return new SendFileRvCmd(rvIcbm);
            } else if (status == AbstractRvIcbm.STATUS_DENY) {
                return new RejectFileSendRvCmd(rvIcbm);
            } else if (status == AbstractRvIcbm.STATUS_ACCEPT) {
                return new AcceptFileSendRvCmd(rvIcbm);
            }
        }

        return new GenericRvCommand(rvIcbm);
    }
}
