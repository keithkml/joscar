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

import net.kano.joscar.rvcmd.ConnectionRequestRvCmd;
import net.kano.joscar.rvcmd.RvConnectionInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;

class GenericRequest implements ConnectionRequestRvCmd {
  public static final RvConnectionInfo CONNINFO_REGULAR;
  static {
    try {
      CONNINFO_REGULAR = new RvConnectionInfo(
          InetAddress.getByName("1.1.1.1"), InetAddress.getByName("2.2.2.2"),
          null, 1, false, false);
    } catch (UnknownHostException e) {
      throw new IllegalStateException(e);
    }
  }
  public static final RvConnectionInfo CONNINFO_PROXY;
  static {
    try {
      CONNINFO_PROXY = RvConnectionInfo.createForOutgoingProxiedRequest(
          InetAddress.getByName("10.10.10.10"), 1);
    } catch (UnknownHostException e) {
      throw new IllegalStateException(e);
    }
  }

  private final int reqindex;
  private final RvConnectionInfo conninfo;

  public GenericRequest(int reqindex) {
    this(reqindex, CONNINFO_REGULAR);
  }

  public GenericRequest(int reqindex, RvConnectionInfo conninfo) {
    this.reqindex = reqindex;
    this.conninfo = conninfo;
  }

  public GenericRequest() {
    this(REQINDEX_FIRST);
  }

  public GenericRequest(RvConnectionInfo conninfo) {
    this(REQINDEX_FIRST, conninfo);
  }

  public RvConnectionInfo getConnInfo() {
    return conninfo;
  }

  public boolean isFirstRequest() {
    return getRequestIndex() == reqindex;
  }

  public int getRequestIndex() {
    return reqindex;
  }
}
