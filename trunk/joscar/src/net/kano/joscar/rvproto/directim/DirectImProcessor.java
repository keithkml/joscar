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
 *  File created by Keith @ 7:54:38 PM
 *
 */

package net.kano.joscar.rvproto.directim;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.net.ConnProcessor;
import net.kano.joscar.net.ConnProcessorExceptionEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DirectImProcessor extends ConnProcessor {
    private final Object readLock = new Object();
    private final Object writeLock = new Object();

    private List listeners = new ArrayList();

    private DirectImStreamHandler streamHandler;

    public DirectImProcessor(DirectImStreamHandler streamHandler) {
        setDirectImStreamHandler(streamHandler);
    }

    public synchronized final void addDirectImListener(DirectImListener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public synchronized final void removeDirectImListener(DirectImListener l) {
        listeners.remove(l);
    }

    public synchronized final DirectImStreamHandler getDirectImStreamHandler() {
        return streamHandler;
    }

    public synchronized final void setDirectImStreamHandler(
            DirectImStreamHandler streamHandler) {
        DefensiveTools.checkNull(streamHandler, "streamHandler");

        this.streamHandler = streamHandler;
    }

    public final void runReadLoop() {
        while (readDirectImHeader());
    }

    public final boolean readDirectImHeader() {
        InputStream stream = getInputStream();
        if (stream == null) return false;

        DirectImHeader header;
        try {
            synchronized(readLock) {
                header = DirectImHeader.readDirectIMHeader(stream);

                return processPacket(header);
            }
        } catch (IOException e) {
            handleException(ConnProcessorExceptionEvent.ERRTYPE_CONNECTION_ERROR,
                    e, null);
            return false;
        }
    }

    private boolean processPacket(DirectImHeader header) {
        DirectImHeaderEvent event = new DirectImHeaderEvent(this, header);

        InputStream in;
        DirectImStreamHandler streamHandler;
        synchronized(this) {
            for (Iterator it = listeners.iterator(); it.hasNext();) {
                DirectImListener handler = (DirectImListener) it.next();

                try {
                    handler.handleHeader(event);

                } catch (Throwable t2) {
                    handleException(ConnProcessorExceptionEvent
                            .ERRTYPE_PACKET_LISTENER, t2, handler);
                }
            }

            // let's save some references for later while we're in this
            // synchronized block
            in = getInputStream();
            streamHandler = this.streamHandler;
        }

        if (in != null) {
            DirectImDataRecvEvent devent
                    = new DirectImDataRecvEvent(this, header, in);

            try {
                return streamHandler.handleIncomingData(devent);

            } catch (IOException e) {
                handleException(ConnProcessorExceptionEvent
                        .ERRTYPE_CONNECTION_ERROR, e, null);

                return false;

            } catch (Throwable t) {
                handleException(ConnProcessorExceptionEvent
                        .ERRTYPE_PACKET_LISTENER, t, streamHandler);

                return false;
            }
        }

        return true;
    }

    public synchronized final void handleException(Object type, Throwable t,
            Object reason) {
        DirectImExceptionEvent event = new DirectImExceptionEvent(type, this, t,
                reason);

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            DirectImListener handler = (DirectImListener) it.next();

            try {
                handler.handleException(event);
            } catch (Throwable t2) {
                // :/
                t2.printStackTrace();
            }
        }
    }

    public final void sendPacket(DirectImHeader header,
            DirectImDataWriter dataWriter) {
        DefensiveTools.checkNull(header, "header");

        if (header.getDataLength() > 0 && dataWriter == null) {
            throw new IllegalArgumentException("data length ("
                    + header.getDataLength() + ") > 0 but dataWriter is null");
        }

        OutputStream out;
        synchronized(this) {
            out = getOutputStream();

            if (out == null) return;
        }

        DirectImDataSendEvent event = new DirectImDataSendEvent(this,
                header, out);

        synchronized(writeLock) {
            try {
                header.write(out);

                if (header.getDataLength() > 0) dataWriter.writeData(event);

            } catch (IOException e) {
                handleException(ConnProcessorExceptionEvent
                        .ERRTYPE_CONNECTION_ERROR, e, null);
                return;
            } catch (Throwable t) {
                handleException(ConnProcessorExceptionEvent.ERRTYPE_CMD_WRITE,
                        t, dataWriter);
                return;
            }
        }
    }
}