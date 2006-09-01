/*
 * Copyright (c) 2006, The Joust Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 * - Neither the name of the Joust Project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * File created by keithkml
 */

package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.RvConnectionInfo;

import java.util.logging.Logger;

public abstract class AbstractRvSessionConnectionInfo
    implements RvSessionConnectionInfo {
  private static final Logger LOGGER
      = Logger.getLogger(AbstractRvSessionConnectionInfo.class.getName());

  private final RvSession session;
  private RvConnectionInfo connInfo = null;
  private int requestIndex = 1;
  private Initiator initiator = null;
  private boolean buddyAccepted = false;

  public AbstractRvSessionConnectionInfo(RvSession session) {
    this.session = session;
  }

  public RvSession getRvSession() { return session; }

  public synchronized void setConnectionInfo(RvConnectionInfo connInfo) {
    this.connInfo = connInfo;
  }

  public synchronized RvConnectionInfo getConnectionInfo() { return connInfo; }

  public synchronized int getRequestIndex() { return requestIndex; }

  public synchronized int increaseRequestIndex() {
    return ++requestIndex;
  }

  public synchronized void setRequestIndex(int requestIndex) {
    this.requestIndex = requestIndex;
  }

  public synchronized void setInitiator(Initiator initiator) {
    LOGGER.fine("Setting initiator for " + session + " to " + initiator);
    this.initiator = initiator;
  }

  public synchronized Initiator getInitiator() {
    if (initiator == null) {
      throw new IllegalStateException("No initiator has been set");
    }
    return initiator;
  }

  public synchronized boolean buddyAccepted() {
    return buddyAccepted;
  }

  public synchronized void setBuddyAccepted(boolean accepted) {
    buddyAccepted = accepted;
  }
}
