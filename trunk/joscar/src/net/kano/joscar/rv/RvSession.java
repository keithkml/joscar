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

package net.kano.joscar.rv;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.icbm.RvCommand;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class RvSession {
    private final RvProcessor rvProcessor;
    private final long rvCookie;
    private final String sn;

    private List listeners = new ArrayList();

    RvSession(RvProcessor rvProcessor, long rvCookie, String sn) {
        this.rvProcessor = rvProcessor;
        this.rvCookie = rvCookie;
        this.sn = sn;
    }

    public synchronized final void addListener(RvSessionListener l) {
        DefensiveTools.checkNull(l, "l");

        if (!listeners.contains(l)) listeners.add(l);
    }

    public synchronized final void removeListener(RvSessionListener l) {
        DefensiveTools.checkNull(l, "l");

        listeners.remove(l);
    }

    public final RvProcessor getRvProcessor() {
        return rvProcessor;
    }

    public final long getSessionId() {
        return rvCookie;
    }

    public final String getScreenname() {
        return sn;
    }

    synchronized final void processRv(RecvRvEvent event) {
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            RvSessionListener listener = (RvSessionListener) it.next();

            listener.handleRv(event);
        }
    }

    synchronized final void processSnacResponse(RvSnacResponseEvent event) {
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            RvSessionListener listener = (RvSessionListener) it.next();

            listener.handleSnacResponse(event);
        }
    }

    public final void sendRv(RvCommand command) {
        DefensiveTools.checkNull(command, "command");

        rvProcessor.sendRv(this, command);
    }

    public String toString() {
        return "RvSession with " + getScreenname() + " (sessionid=0x"
                + Long.toHexString(rvCookie) + ")";
    }
}
