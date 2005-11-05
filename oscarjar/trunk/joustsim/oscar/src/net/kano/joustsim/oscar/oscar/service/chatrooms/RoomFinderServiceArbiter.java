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

package net.kano.joustsim.oscar.oscar.service.chatrooms;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.snaccmd.FullRoomInfo;
import net.kano.joscar.snaccmd.MiniRoomInfo;
import net.kano.joscar.snaccmd.rooms.RoomCommand;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.AbstractServiceArbiter;
import net.kano.joustsim.oscar.oscar.service.ServiceArbiterRequest;
import net.kano.joustsim.oscar.oscar.service.ServiceArbitrationManager;
import net.kano.joustsim.JavaTools;

public class RoomFinderServiceArbiter extends AbstractServiceArbiter<RoomManagerService> {
    private CopyOnWriteArrayList<RoomManagerServiceListener> listeners
            = new CopyOnWriteArrayList<RoomManagerServiceListener>();
    private RoomManagerServiceListener delegatingListener
            = JavaTools.getDelegatingProxy(listeners, RoomManagerServiceListener.class);

    public RoomFinderServiceArbiter(ServiceArbitrationManager manager) {
        super(manager);
    }

    public int getSnacFamily() {
        return RoomCommand.FAMILY_ROOM;
    }

    protected void handleRequestsDequeuedEvent(RoomManagerService service) {
    }

    protected void processRequest(RoomManagerService service,
            ServiceArbiterRequest request) {
        if (request instanceof JoinRoomRequest) {
            JoinRoomRequest req = (JoinRoomRequest) request;
            service.joinChatRoom(req.getExchange(), req.getRoomName());

        } else if (request instanceof AcceptInvitationRequest) {
            AcceptInvitationRequest invitationRequest
                    = (AcceptInvitationRequest) request;
            service.joinChatRoom(invitationRequest.getRoomInfo());
        }
    }

    protected RoomManagerService createServiceInstance(
            AimConnection aimConnection, OscarConnection conn) {
        RoomManagerService service = new RoomManagerService(aimConnection, conn);
        service.addRoomManagerServiceListener(delegatingListener);
        return service;
    }

    public void joinChatRoom(int exchange, String name) {
        addRequest(new JoinRoomRequest(exchange, name));
    }

    public void joinChatRoom(MiniRoomInfo roomInfo) {
        addRequest(new AcceptInvitationRequest(roomInfo));
    }

    public void addRoomManagerServiceListener(
            RoomManagerServiceListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeRoomManagerServiceListener(
            RoomManagerServiceListener l) {
        listeners.remove(l);
    }

    private static class JoinRoomRequest implements ServiceArbiterRequest {
        private final int exchange;
        private final String roomName;

        public JoinRoomRequest(int exchange, String name) {
            this.exchange = exchange;
            this.roomName = name;
        }

        public int getExchange() { return exchange; }

        public String getRoomName() { return roomName; }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final JoinRoomRequest that = (JoinRoomRequest) o;

            if (exchange != that.exchange) return false;
            if (!roomName.equals(that.roomName)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = exchange;
            result = 29 * result + roomName.hashCode();
            return result;
        }
    }

    private static class AcceptInvitationRequest implements ServiceArbiterRequest {
        private final MiniRoomInfo roomInfo;

        public AcceptInvitationRequest(MiniRoomInfo roomInfo) {
            this.roomInfo = roomInfo;
        }

        public MiniRoomInfo getRoomInfo() { return roomInfo; }
    }
}
