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
 *  File created by keith @ Mar 6, 2003
 *
 */

package net.kano.joscar.rvproto.rvproxy;

import net.kano.joscar.net.ClientConn;
import net.kano.joscar.net.ClientConnEvent;
import net.kano.joscar.net.ClientConnListener;
import net.kano.joscar.net.ClientConnStreamHandler;
import net.kano.joscar.rvproto.rvproxy.RvProxyProcessor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/*
normally connects to ars.oscar.aol.com
*/
public class ClientRvProxyConn extends ClientConn {
    private final RvProxyProcessor rvProxyProcessor = new RvProxyProcessor();

    public ClientRvProxyConn(String proxyHost, int proxyPort) {
        super(proxyHost, proxyPort);

        init();
    }

    public ClientRvProxyConn(InetAddress proxyAddress, int proxyPort) {
        super(proxyAddress, proxyPort);

        init();
    }

    private void init() {
        setStreamHandler(new ClientConnStreamHandler() {
            public void handleStream(ClientConn conn, Socket socket)
                    throws IOException {
                rvProxyProcessor.runReadLoop();
            }
        });
        addConnListener(new ClientConnListener() {
            public void stateChanged(ClientConnEvent e) {
                Object state = e.getNewState();
                if (state == ClientConn.STATE_CONNECTED) {
                    try {
                        rvProxyProcessor.attachToSocket(getSocket());
                    } catch (IOException e1) {
                        processError(e1);
                        return;
                    }
                } else if (state == ClientConn.STATE_NOT_CONNECTED
                        || state == ClientConn.STATE_FAILED) {
                    rvProxyProcessor.detach();
                }
            }
        });
    }

    public final RvProxyProcessor getRvProxyProcessor() {
        return rvProxyProcessor;
    }
}