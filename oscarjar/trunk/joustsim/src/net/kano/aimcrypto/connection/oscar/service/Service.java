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
 *  File created by keith @ Jan 15, 2004
 *
 */

package net.kano.aimcrypto.connection.oscar.service;

import net.kano.aimcrypto.connection.AimConnection;
import net.kano.aimcrypto.connection.oscar.OscarConnection;
import net.kano.aimcrypto.Screenname;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.MiscTools;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joscar.flap.FlapCommand;
import net.kano.joscar.flap.FlapPacketEvent;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snac.SnacRequestListener;

import java.util.Iterator;
import java.util.logging.Logger;

public abstract class Service {
    private static final Logger logger = Logger.getLogger(Service.class.getName());

    private AimConnection aimConnection;
    private final OscarConnection oscarConnection;
    private final int family;
    private CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();
    private boolean ready = false;
    private boolean finished = false;

    protected Service(AimConnection aimConnection,
            OscarConnection oscarConnection, int family) {
        logger.fine("Created new " + getClass().getName());

        this.aimConnection = aimConnection;
        this.oscarConnection = oscarConnection;
        this.family = family;
    }

    public final AimConnection getAimConnection() {
        return aimConnection;
    }

    public final OscarConnection getOscarConnection() {
        return oscarConnection;
    }

    public Screenname getScreenname() {
        return aimConnection.getScreenname();
    }

    public final int getFamily() { return family; }

    public abstract SnacFamilyInfo getSnacFamilyInfo();

    public final int getFamilyVersion() {
        return getSnacFamilyInfo().getVersion();
    }
    public final int getToolId() {
        return getSnacFamilyInfo().getToolID();
    }
    public final int getToolVersion() {
        return getSnacFamilyInfo().getToolVersion();
    }

    public void addServiceListener(ServiceListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeServiceListener(ServiceListener l) {
        listeners.remove(l);
    }

    public synchronized boolean isReady() { return ready; }

    public synchronized boolean isFinished() { return finished; }

    protected void sendFlap(FlapCommand flap) {
        oscarConnection.sendFlap(flap);
    }

    protected void sendDirectedSnac(SnacCommand snac) {
        aimConnection.sendSnac(snac);
    }

    public void sendSnac(SnacCommand snac) {
        DefensiveTools.checkNull(snac, "snac");

        oscarConnection.sendSnac(snac);
    }

    public void sendSnacRequest(SnacRequest request) {
        DefensiveTools.checkNull(request, "request");

        oscarConnection.sendSnacRequest(request);
    }

    public void sendSnacRequest(SnacCommand cmd, SnacRequestListener listener) {
        DefensiveTools.checkNull(cmd, "cmd");
        DefensiveTools.checkNull(listener, "listener");

        oscarConnection.sendSnacRequest(cmd, listener);
    }

    protected void setReady() {
        //TODO: optimize logging statements
        logger.finer(MiscTools.getClassName(this) + " is ready");

        synchronized(this) {
            if (ready) return;
            ready = true;
        }
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            ServiceListener l = (ServiceListener) it.next();
            l.ready(this);
        }
    }

    protected void setFinished() {
        logger.finer(MiscTools.getClassName(this) + " is finished");

        synchronized(this) {
            if (finished) return;
            finished = true;
        }
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            ServiceListener l = (ServiceListener) it.next();
            l.finished(this);
        }
    }

    public void connected() { }

    public void disconnected() { }

    public void handleFlapPacket(FlapPacketEvent flapPacketEvent) {
    }

    public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
    }

    public void handleSnacResponse(SnacResponseEvent snacResponseEvent) {
        handleSnacPacket(snacResponseEvent);
    }
}
