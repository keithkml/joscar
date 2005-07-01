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

import java.io.IOException;

public abstract class TransferController extends StateController {
    private boolean stop = false;

    public void start(final FileTransfer transfer,
            StateController last) {
        StateInfo endState = last.getEndState();
        if (endState instanceof Stream) {
            final Stream stream = (Stream) endState;
            Thread receiveThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        transferInThread(stream, (FileTransferImpl) transfer);

                    } catch (Exception e) {
                        e.printStackTrace();
                        fireFailed(e);
                        return;
                    }
                }
            });
            receiveThread.start();
        } else {
            throw new IllegalArgumentException("I don't know how to deal with "
                    + "previous end state " + endState);
        }
    }

    public synchronized void stop() {
        stop = true;
    }

    protected synchronized boolean shouldStop() {
        return stop;
    }

    protected abstract void transferInThread(Stream stream,
            FileTransferImpl transfer)
            throws IOException;
}
