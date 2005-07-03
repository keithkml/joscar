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
import net.kano.joustsim.oscar.CapabilityManager;
import net.kano.joustsim.oscar.DefaultEnabledCapabilityHandler;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmService;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousCapabilityHandler;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousSessionHandler;
import net.kano.joustsim.Screenname;

public class FileTransferManager {
    private final IcbmService service;
    private CopyOnWriteArrayList<FileTransferManagerListener> listeners
            = new CopyOnWriteArrayList<FileTransferManagerListener>();

    public FileTransferManager(IcbmService service) {
        this.service = service;
        CapabilityManager capMgr = service.getAimConnection()
                .getCapabilityManager();
        capMgr.setCapabilityHandler(CapabilityBlock.BLOCK_FILE_SEND,
                new FileTransferCapabilityHandler());
    }

    public IcbmService getIcbmService() {
        return service;
    }

    public OutgoingFileTransfer createOutgoingFileTransfer(Screenname sn) {
        RvSession session = service.getRvProcessor()
                .createRvSession(sn.getFormatted());
        OutgoingFileTransferImpl outgoingFileTransfer = new OutgoingFileTransferImpl(
                this, session);
        session.addListener(outgoingFileTransfer.getRvSessionHandler());
        return outgoingFileTransfer;
    }

    public void addFileTransferListener(FileTransferManagerListener listener) {
        DefensiveTools.checkNull(listener, "listener");

        listeners.addIfAbsent(listener);
    }

    public void removeFileTransferListener(FileTransferManagerListener listener) {
        DefensiveTools.checkNull(listener, "listener");

        listeners.remove(listener);
    }

    void fireNewIncomingTransfer(IncomingFileTransfer transfer) {
        assert !Thread.holdsLock(this);

        for (FileTransferManagerListener listener : listeners) {
            listener.handleNewIncomingFileTransfer(FileTransferManager.this, transfer);
        }
    }

    private class FileTransferCapabilityHandler
            extends DefaultEnabledCapabilityHandler implements
            RendezvousCapabilityHandler {
        public RendezvousSessionHandler handleSession(IcbmService service,
                RvSession session) {
            IncomingFileTransferImpl transfer = new IncomingFileTransferImpl(
                    FileTransferManager.this, session);
            return transfer.getRvSessionHandler();
        }
    }
}
