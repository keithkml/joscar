/*
 * Copyright (c) 2006, The Joust Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 * - Neither the name of the Joust Project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * File created by keithkml
 */

package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousSessionHandler;
import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvSnacResponseEvent;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joscar.rvcmd.ConnectionRequestRvCmd;
import net.kano.joscar.rvcmd.AcceptRvCmd;
import net.kano.joscar.rvcmd.RejectRvCmd;

abstract class AbstractRvSessionHandler implements RendezvousSessionHandler {
  private RvConnection transfer;

  public AbstractRvSessionHandler(RvConnection transfer) {
    this.transfer = transfer;
  }

  public final void handleRv(RecvRvEvent event) {
    RvCommand cmd = event.getRvCommand();
    if (cmd instanceof ConnectionRequestRvCmd) {
      handleIncomingRequest(event, (ConnectionRequestRvCmd) cmd);

    } else if (cmd instanceof AcceptRvCmd) {
      handleIncomingAccept(event, (AcceptRvCmd) cmd);

    } else if (cmd instanceof RejectRvCmd) {
      handleIncomingReject(event, (RejectRvCmd) cmd);
    }
  }

  protected abstract void handleIncomingReject(RecvRvEvent event,
                                               RejectRvCmd rejectCmd);

  protected abstract void handleIncomingAccept(RecvRvEvent event,
                                               AcceptRvCmd acceptCmd);

  protected abstract void handleIncomingRequest(RecvRvEvent event,
                                                ConnectionRequestRvCmd reqCmd);

  public void handleSnacResponse(RvSnacResponseEvent event) {
  }

  protected RvConnection getFileTransfer() { return transfer; }
}
