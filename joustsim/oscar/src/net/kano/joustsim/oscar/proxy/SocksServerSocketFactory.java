/*
 * Copyright (c) 2005, The Joust Project
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

package net.kano.joustsim.oscar.proxy;

import socks.SocksServerSocket;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;

public class SocksServerSocketFactory extends ServerSocketFactory {
  private final AimProxyInfo proxy;

  public SocksServerSocketFactory(AimProxyInfo proxy) {
    this.proxy = proxy;
  }

  public ServerSocket createServerSocket() throws IOException {
    return new SocksServerSocket(proxy.createSocksProxy(),
        new Random().nextInt(65535-1025) + 1025);
  }

  public ServerSocket createServerSocket(int port) throws IOException {
    return new SocksServerSocket(proxy.createSocksProxy(), port);
  }

  public ServerSocket createServerSocket(int port, int backlog) throws IOException {
    return new SocksServerSocket(proxy.createSocksProxy(), port);
  }

  public ServerSocket createServerSocket(int port, int backlog, InetAddress localhost)
      throws IOException {
    return new SocksServerSocket(proxy.createSocksProxy(), port);
  }
}
