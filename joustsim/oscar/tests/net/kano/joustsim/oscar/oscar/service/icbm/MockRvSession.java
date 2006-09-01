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

package net.kano.joustsim.oscar.oscar.service.icbm;

import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rv.RvSessionListener;
import net.kano.joscar.rv.RvProcessor;
import net.kano.joscar.snaccmd.icbm.RvCommand;

class MockRvSession implements RvSession {
  public static final int MOCK_SESSION_ID = 500;
  public static final String MOCK_BUDDY_SCREENNAME = "Buddy";

  private long sessionId = MOCK_SESSION_ID;
  private String screenname = MOCK_BUDDY_SCREENNAME;

  public void setSessionId(long sessionId) {
    this.sessionId = sessionId;
  }

  public void setScreenname(String screenname) {
    this.screenname = screenname;
  }

  public void addListener(RvSessionListener l) {
    throw new UnsupportedOperationException();
  }

  public void removeListener(RvSessionListener l) {
    throw new UnsupportedOperationException();
  }

  public RvProcessor getRvProcessor() {
    throw new UnsupportedOperationException();
  }

  public long getRvSessionId() {
    return sessionId;
  }

  public String getScreenname() {
    return screenname;
  }

  public void sendRv(RvCommand command) {
    throw new UnsupportedOperationException();
  }

  public void sendRv(RvCommand command, long icbmMessageId) {
    throw new UnsupportedOperationException();
  }

  public void sendResponse(int code) {
    throw new UnsupportedOperationException();
  }
}
