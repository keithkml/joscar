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

package net.kano.joustsim.oscar.oscar.service;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.MiscTools;
import net.kano.joscar.flap.FlapCommand;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snac.SnacRequestListener;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;

import java.util.logging.Logger;

public abstract class AbstractService implements MutableService {
  private static final Logger logger
      = Logger.getLogger(AbstractService.class.getName());

  private AimConnection aimConnection;
  private final OscarConnection oscarConnection;
  private final int family;
  private CopyOnWriteArrayList<ServiceListener> listeners
      = new CopyOnWriteArrayList<ServiceListener>();
  private boolean ready = false;
  private boolean finished = false;

  protected AbstractService(AimConnection aimConnection,
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

  public final int getFamily() { return family; }

  public void addServiceListener(ServiceListener l) {
    listeners.addIfAbsent(l);
  }

  public void removeServiceListener(ServiceListener l) {
    listeners.remove(l);
  }

  public synchronized final boolean isReady() { return ready; }

  public synchronized final boolean isFinished() { return finished; }

  protected final void sendFlap(FlapCommand flap) {
    oscarConnection.sendFlap(flap);
  }

  /** Automatically sends snac through the correct service */
  protected final void sendDirectedSnac(SnacCommand snac) {
    aimConnection.sendSnac(snac);
  }

  public final void sendSnac(SnacCommand snac) {
    DefensiveTools.checkNull(snac, "snac");

    oscarConnection.sendSnac(snac);
  }

  public final void sendSnacRequest(SnacRequest request) {
    DefensiveTools.checkNull(request, "request");

    oscarConnection.sendSnacRequest(request);
  }

  public final void sendSnacRequest(SnacCommand cmd,
      SnacRequestListener listener) {
    DefensiveTools.checkNull(cmd, "cmd");
    DefensiveTools.checkNull(listener, "listener");

    oscarConnection.sendSnacRequest(cmd, listener);
  }

  protected final void setReady() {

    synchronized (this) {
      if (ready) return;
      ready = true;
    }
    logger.finer(MiscTools.getClassName(this) + " is ready");
    
    for (ServiceListener l : listeners) l.handleServiceReady(this);
  }

  protected final void setFinished() {
    logger.finer(MiscTools.getClassName(this) + " is finished");

    synchronized (this) {
      if (finished) return;
      finished = true;
    }
    for (ServiceListener l : listeners) {
      l.handleServiceFinished(this);
    }
  }

  public void connected() { }

  public final void disconnected() {
    finishUp();
    setFinished();
  }

  /**
   * Called when the service is disconnected, before any
   * {@linkplain ServiceListener#handleServiceFinished finished events} are
   * fired.
   */
  protected void finishUp() {

  }

  //TODO(klea): extract this into a new interface or something
  public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
  }

  public void handleSnacResponse(SnacResponseEvent snacResponseEvent) {
    handleSnacPacket(snacResponseEvent);
  }

  public void handleEvent(ServiceEvent event) {

  }
}
