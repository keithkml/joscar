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

import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rvcmd.ConnectionRequestRvCmd;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectToProxyForIncomingController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.OutgoingConnectionController;

import java.util.logging.Logger;

public abstract class AbstractIncomingRvSessionHandler extends AbstractRvSessionHandler {
  private static final Logger LOGGER = Logger
      .getLogger(AbstractIncomingRvSessionHandler.class.getName());

  private final IncomingRvConnectionImpl incomingRvConnection;

  public AbstractIncomingRvSessionHandler(
      IncomingRvConnectionImpl incomingRvConnection) {
    super(incomingRvConnection);
    this.incomingRvConnection = incomingRvConnection;
  }

  protected void handleIncomingRequest(RecvRvEvent event,
      ConnectionRequestRvCmd reqCmd) {
    int index = reqCmd.getRequestIndex();
    if (index == FileSendReqRvCmd.REQINDEX_FIRST) {
      handleFirstRequest(reqCmd);

      RvConnectionInfo connInfo = reqCmd.getConnInfo();
      incomingRvConnection.getRvSessionInfo().setConnectionInfo(connInfo);
      incomingRvConnection.getRvSessionInfo().setInitiator(Initiator.BUDDY);

      incomingRvConnection.getEventPost().fireEvent(new NewIncomingConnectionEvent(incomingRvConnection));

    } else if (index > FileSendReqRvCmd.REQINDEX_FIRST) {
      HowToConnect how = processRedirect(reqCmd);
      if (how == HowToConnect.PROXY) {
        incomingRvConnection.changeStateController(new ConnectToProxyForIncomingController());
      } else if (how == HowToConnect.NORMAL) {
        incomingRvConnection.changeStateController(
            new OutgoingConnectionController(ConnectionType.LAN));
      } else {
        throw new IllegalStateException("How to connect was " + how);
      }
    } else {
      LOGGER.warning("Got unknown request index " + index + " for "
          + reqCmd);
    }
  }

  protected abstract void handleFirstRequest(ConnectionRequestRvCmd reqCmd);
}
