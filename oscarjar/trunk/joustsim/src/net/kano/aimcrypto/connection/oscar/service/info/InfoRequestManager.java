/*
 *  Copyright (c) 2004, The Joust Project
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
 *  File created by keith @ Feb 7, 2004
 *
 */

package net.kano.aimcrypto.connection.oscar.service.info;

import net.kano.aimcrypto.Screenname;
import net.kano.joscar.DefensiveTools;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class InfoRequestManager {
    private final InfoService service;
    private final Map listenerMap = new HashMap();

    protected InfoRequestManager(InfoService service) {
        this.service = service;
    }

    public void request(Screenname sn) {
        DefensiveTools.checkNull(sn, "sn");

        request(sn, null);
    }

    public void request(Screenname sn, InfoResponseListener listener) {
        DefensiveTools.checkNull(sn, "sn");

        boolean shouldRequest;
        synchronized(this) {
            shouldRequest = storeListener(sn, listener);
        }
        if (shouldRequest) sendRequest(sn);
    }

    protected abstract void sendRequest(Screenname sn);

    private synchronized boolean storeListener(Screenname sn,
            InfoResponseListener listener) {
        DefensiveTools.checkNull(sn, "sn");

        boolean shouldRequest = !listenerMap.containsKey(sn);
        Set listeners = getListeners(sn);
        if (listener != null) listeners.add(listener);
        return shouldRequest;
    }

    protected synchronized final Set getListeners(Screenname sn) {
        DefensiveTools.checkNull(sn, "sn");

        Set set = (Set) listenerMap.get(sn);
        if (set == null) {
            set = new LinkedHashSet();
            set.add(service.getInfoRequestListener());
            listenerMap.put(sn, set);
        }
        return set;
    }

    protected synchronized final Set clearListeners(Screenname sn) {
        DefensiveTools.checkNull(sn, "sn");

        Set set = (Set) listenerMap.remove(sn);
        if (set == null) return Collections.EMPTY_SET;
        else return set;
    }

    public InfoService getService() {
        return service;
    }
}
