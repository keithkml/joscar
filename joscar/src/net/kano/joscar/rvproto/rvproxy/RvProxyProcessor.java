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
 *  File created by Keith @ 4:45:16 PM
 *
 */

package net.kano.joscar.rvproto.rvproxy;

import net.kano.joscar.net.ConnProcessor;
import net.kano.joscar.net.ConnProcessorExceptionEvent;
import net.kano.joscar.rvproto.rvproxy.cmd.DefaultRvProxyCmdFactory;
import net.kano.joscar.rvproto.rvproxy.cmd.RvProxyCmdFactory;
import net.kano.joscar.rvproto.rvproxy.cmd.RvProxyHeader;
import net.kano.joscar.rvproto.rvproxy.cmd.RvProxyCmd;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class RvProxyProcessor extends ConnProcessor {
    private final Object readLock = new Object();

    private List cmdListeners = new ArrayList();
    private List exceptionHandlers = new ArrayList();

    private RvProxyCmdFactory factory = new DefaultRvProxyCmdFactory();


    public RvProxyProcessor() { }

    public RvProxyProcessor(Socket socket) throws IOException {
        attachToSocket(socket);
    }

    public synchronized final void addCommandListener(RvProxyCmdListener l) {
        if (!cmdListeners.contains(l)) cmdListeners.add(l);
    }

    public synchronized final void removeCommandListener(
            RvProxyCmdListener l) {
        cmdListeners.remove(l);
    }

    public synchronized final void addExceptionHandler(
            RvProxyExceptionHandler h) {
        if (!exceptionHandlers.contains(h)) exceptionHandlers.add(h);
    }

    public synchronized final void removeExceptionHandler(
            RvProxyExceptionHandler h) {
        exceptionHandlers.remove(h);
    }

    public synchronized final RvProxyCmdFactory getRvProxyCmdFactory() {
        return factory;
    }

    public synchronized final void setRvProxyCmdFactory(
            RvProxyCmdFactory factory) {
        this.factory = factory;
    }

    public final void runReadLoop() {
        while (readRvProxyCmd());
    }

    public final boolean readRvProxyCmd() {
        InputStream stream = getInputStream();
        if (stream == null) return false;

        RvProxyHeader header;
        try {
            synchronized(readLock) {
                header = RvProxyHeader.readRvProxyHeader(stream);
            }

        } catch (IOException e) {
            handleException(ConnProcessorExceptionEvent
                    .ERRTYPE_CONNECTION_ERROR, e, null);

            return false;
        }

        if (header == null) return false;

        processHeader(header);

        return true;
    }

    private synchronized void processHeader(RvProxyHeader header) {
        RvProxyCmd cmd = null;
        if (factory != null) {
            try {
                cmd = factory.getRvProxyCmd(header);
            } catch (Throwable t) {
                handleException(ConnProcessorExceptionEvent.ERRTYPE_CMD_GEN, t,
                        header);
            }
        }

        RvProxyCmdEvent event = new RvProxyCmdEvent(this, header, cmd);

        for (Iterator it = cmdListeners.iterator(); it.hasNext();) {
            RvProxyCmdListener l = (RvProxyCmdListener) it.next();

            try {
                l.handleRvProxyCmd(event);
            } catch (Throwable t) {
                handleException(ConnProcessorExceptionEvent
                        .ERRTYPE_PACKET_LISTENER, t, l);
            }
        }
    }

    public synchronized final void handleException(Object type, Throwable t,
            Object reason) {
        RvProxyExceptionEvent event = new RvProxyExceptionEvent(type, this, t,
                reason);

        for (Iterator it = exceptionHandlers.iterator(); it.hasNext();) {
            RvProxyExceptionHandler handler =
                    (RvProxyExceptionHandler) it.next();

            try {
                handler.handleException(event);
            } catch (Throwable t2) {
                // whatever.
                t2.printStackTrace();
            }
        }
    }

    public synchronized final void sendRvProxyCmd(RvProxyCmd cmd) {
        DefensiveTools.checkNull(cmd, "cmd");

        System.out.println("sending..");
        if (getOutputStream() == null) return;

        RvProxyHeader header = new RvProxyHeader(cmd);

        // maybe there should be a better way of calculating this. maybe not.
        ByteArrayOutputStream out = new ByteArrayOutputStream(100);

        System.out.println("copying..");
        try {
            header.write(out);
        } catch (Throwable t) {
            handleException(ConnProcessorExceptionEvent.ERRTYPE_CMD_WRITE, t,
                    header);
            return;
        }

        System.out.println("writing " + cmd + " to stream...");
        System.out.println(BinaryTools.describeData(ByteBlock.wrap(out.toByteArray())));
        try {
            out.writeTo(getOutputStream());
        } catch (IOException e) {
            handleException(ConnProcessorExceptionEvent
                    .ERRTYPE_CONNECTION_ERROR, e, null);
        }
    }
}