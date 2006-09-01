/*
 *  Copyright (c) 2005, The Joust Project
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
 */

package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joscar.MiscTools;
import net.kano.joscar.rvcmd.InvitationMessage;
import net.kano.joscar.rvcmd.sendfile.FileSendBlock;

public class FileTransferHelper {
  private final FileTransfer transfer;
  private final FileTransferRequestMaker requestMaker;
  private FileSendBlock fileInfo;
  private InvitationMessage message;

  protected FileTransferHelper(FileTransfer transfer) {
    this.transfer = transfer;
    requestMaker = new FileTransferRequestMaker(transfer);
  }

  protected synchronized void setInvitationMessage(InvitationMessage message) {
    this.message = message;
  }

  protected synchronized void setFileInfo(FileSendBlock fileInfo) {
    this.fileInfo = fileInfo;
  }

  public synchronized FileSendBlock getFileInfo() { return fileInfo; }

  public synchronized InvitationMessage getInvitationMessage() {
    return message;
  }

  public RvRequestMaker getRvRequestMaker() {
    return requestMaker;
  }

  public String toString() {
    return MiscTools.getClassName(this) + " with "
        + transfer.getBuddyScreenname() + " of " + getFileInfo().getFilename();
  }
}
