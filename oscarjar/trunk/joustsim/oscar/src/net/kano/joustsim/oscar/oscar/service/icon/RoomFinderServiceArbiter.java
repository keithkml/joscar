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

package net.kano.joustsim.oscar.oscar.service.icon;

import net.kano.joscar.snaccmd.icon.IconCommand;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.ServiceArbiter;
import net.kano.joustsim.oscar.oscar.service.ServiceArbitrationManager;
import net.kano.joustsim.oscar.oscar.service.ServiceListener;
import net.kano.joustsim.oscar.oscar.service.chatrooms.RoomManagerService;

import java.util.logging.Logger;

public class RoomFinderServiceArbiter
        implements ServiceArbiter<RoomManagerService> {
    private static final Logger LOGGER = Logger
            .getLogger(RoomFinderServiceArbiter.class.getName());

    private RoomManagerService currentService = null;
    private ServiceArbitrationManager manager;

    public RoomFinderServiceArbiter(ServiceArbitrationManager manager) {
        this.manager = manager;
    }

    public int getSnacFamily() {
        return IconCommand.FAMILY_ICON;
    }

    public synchronized boolean shouldKeepAlive() {
        return false;
    }

    public RoomManagerService createService(AimConnection aimConnection,
            OscarConnection conn) {
        final RoomManagerService service = new RoomManagerService(aimConnection, conn);
        service.addServiceListener(new ServiceListener() {
            public void handleServiceReady(Service s) {
                synchronized(RoomFinderServiceArbiter.this) {
                    currentService = service;
                }
//                dequeueRequests(service);
            }

            public void handleServiceFinished(Service s) {
                synchronized(RoomFinderServiceArbiter.this) {
                    if (currentService == service) {
                        currentService = null;
                    }
                }
            }
        });
        return service;
    }
}
