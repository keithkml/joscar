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

import net.kano.joscar.DefensiveTools;
import net.kano.joustsim.oscar.proxy.AimProxyInfo;

import java.util.HashMap;
import java.util.Map;

public class RvConnectionSettings {
  private static final long DEFAULT_RV_CONNECTION_TIMEOUT = 10 * 1000;
  public static final long DEFAULT_LAN_TIMEOUT = 2000L;
  public static final long DEFAULT_INTERNET_TIMEOUT = 3000L;
  public static final long DEFAULT_INCOMING_MODIFICATION = 2000L;

  private boolean onlyUsingProxy = false;
  private boolean proxyRequestTrusted = true;
  private long perConnectionTimeout = DEFAULT_RV_CONNECTION_TIMEOUT;
  private Map<ConnectionType, Long> timeouts
      = new HashMap<ConnectionType, Long>();
  private Map<Initiator,Long> timeoutModifications
      = new HashMap<Initiator, Long>();
  private AimProxyInfo proxyInfo = AimProxyInfo.forNoProxy();

  {
    timeouts.put(ConnectionType.LAN, DEFAULT_LAN_TIMEOUT);
    timeouts.put(ConnectionType.INTERNET, DEFAULT_INTERNET_TIMEOUT);
    timeoutModifications.put(Initiator.BUDDY, DEFAULT_INCOMING_MODIFICATION);
  }

  public synchronized AimProxyInfo getProxyInfo() {
    return proxyInfo;
  }

  public synchronized void setProxyInfo(AimProxyInfo proxyInfo) {
    this.proxyInfo = proxyInfo;
  }

  public synchronized boolean isProxyRequestTrusted() {
    return proxyRequestTrusted;
  }

  public synchronized void setProxyRequestTrusted(boolean trusted) {
    this.proxyRequestTrusted = trusted;
  }

  public synchronized boolean isOnlyUsingProxy() {
    return onlyUsingProxy;
  }

  public synchronized void setOnlyUsingProxy(boolean onlyUsingProxy) {
    this.onlyUsingProxy = onlyUsingProxy;
  }

  public synchronized void setDefaultPerConnectionTimeout(long millis) {
    perConnectionTimeout = millis;
  }

  public synchronized long getDefaultPerConnectionTimeout(Initiator initiator) {
    return perConnectionTimeout + getTimeoutModification(initiator);
  }

  public synchronized void setTimeoutModification(Initiator initiator, Long mod) {
    timeoutModifications.put(initiator, mod);
  }

  public synchronized long getTimeoutModification(Initiator initiator) {
    Long mod = timeoutModifications.get(initiator);
    if (mod == null) return 0;
    return mod;
  }

  public synchronized void setPerConnectionTimeout(ConnectionType type,
      long millis) {
    timeouts.put(type, millis);
  }

  public synchronized long getPerConnectionTimeout(Initiator initiator,
      ConnectionType type) {
    DefensiveTools.checkNull(type, "type");

    Long timeout = timeouts.get(type);
    if (timeout == null) {
      return getDefaultPerConnectionTimeout(initiator);

    } else {
      return timeout + getTimeoutModification(initiator);
    }
  }
}
