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
 * File created by klea
 */

package net.kano.joustsim.oscar.oscar;

import net.kano.joscar.flapcmd.KeepaliveFlapCmd;
import net.kano.joscar.net.ClientConn;
import net.kano.joscar.net.ClientConnEvent;
import net.kano.joscar.net.ClientConnListener;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class KeepaliveSender {
  private static final Logger LOGGER
      = Logger.getLogger(KeepaliveSender.class.getName());
  private static final int KEEPALIVE_INTERVAL = 3*60*1000;

  public static KeepaliveSender start(OscarConnection conn) {
    return new KeepaliveSender(conn);
  }

  private final OscarConnection connection;
  private Timer timer = null;

  private KeepaliveSender(OscarConnection conn) {
    this.connection = conn;

    conn.getClientFlapConn().addConnListener(new ClientConnListener() {
      public void stateChanged(ClientConnEvent e) {
        handleConnectionState(e.getNewState());
      }
    });
    handleConnectionState(conn.getConnectionState());
  }

  private synchronized void handleConnectionState(ClientConn.State newState) {
    if (timer == null && newState == ClientConn.STATE_CONNECTED) {
      LOGGER.fine("Starting KeepaliveSender for " + connection);
      timer = new Timer(true);
      timer.scheduleAtFixedRate(new TimerTask() {
        public void run() {
          if (!connection.isDisconnected()) {
            LOGGER.finer("Sending FLAP keepalive on " + connection);
            connection.sendFlap(new KeepaliveFlapCmd());
          }
        }
      }, 0, KEEPALIVE_INTERVAL);
      
    } else if (timer != null && newState == ClientConn.STATE_NOT_CONNECTED) {
      LOGGER.fine("Stopping KeepaliveSender for " + connection);
      timer.cancel();
      timer = null;
    }
  }
}
