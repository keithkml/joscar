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

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.CapabilityManager;
import net.kano.joustsim.oscar.DefaultEnabledCapabilityHandler;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmService;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousCapabilityHandler;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousSessionHandler;

import java.util.logging.Logger;

public class RvConnectionManager {
  private static final Logger LOGGER = Logger
      .getLogger(RvConnectionManager.class.getName());

  private final IcbmService service;
  private CopyOnWriteArrayList<RvConnectionManagerListener> listeners
      = new CopyOnWriteArrayList<RvConnectionManagerListener>();

  public RvConnectionManager(IcbmService service) {
    assert service != null;
    this.service = service;
    CapabilityManager capMgr = service.getAimConnection()
        .getCapabilityManager();
    capMgr.setCapabilityHandler(CapabilityBlock.BLOCK_FILE_SEND,
        new FileTransferCapabilityHandler());
    capMgr.setCapabilityHandler(CapabilityBlock.BLOCK_DIRECTIM,
        new DirectImCapabilityHandler());
  }

  public IcbmService getIcbmService() { return service; }

  public OutgoingFileTransfer createOutgoingFileTransfer(Screenname sn) {
    RvSession session = service.getRvProcessor()
        .createRvSession(sn.getFormatted());
    OutgoingFileTransferImpl outgoingFileTransfer
        = new OutgoingFileTransferImpl(this, session);
    session.addListener(outgoingFileTransfer.getRvSessionHandler());
    return outgoingFileTransfer;
  }

  public void addConnectionManagerListener(RvConnectionManagerListener listener) {
    DefensiveTools.checkNull(listener, "listener");

    listeners.addIfAbsent(listener);
  }

  public void removeConnectionManagerListener(RvConnectionManagerListener listener) {
    DefensiveTools.checkNull(listener, "listener");

    listeners.remove(listener);
  }

  void fireNewIncomingConnection(IncomingRvConnection transfer) {
    assert !Thread.holdsLock(this);

    for (RvConnectionManagerListener listener : listeners) {
      listener.handleNewIncomingConnection(RvConnectionManager.this, transfer);
    }
  }

  private class FileTransferCapabilityHandler
      extends DefaultEnabledCapabilityHandler
      implements RendezvousCapabilityHandler {
    public RendezvousSessionHandler handleSession(IcbmService service,
        RvSession session) {
      IncomingFileTransferImpl transfer = new IncomingFileTransferImpl(
          RvConnectionManager.this, session);
      return transfer.getRvSessionHandler();
    }
  }

  private class DirectImCapabilityHandler
      extends DefaultEnabledCapabilityHandler
      implements RendezvousCapabilityHandler {
    public RendezvousSessionHandler handleSession(IcbmService service,
        RvSession session) {
      IncomingRvConnectionImpl transfer = new IncomingDirectimConnectionImpl(
          RvConnectionManager.this, session);
      return transfer.getRvSessionHandler();
    }

  }

}
