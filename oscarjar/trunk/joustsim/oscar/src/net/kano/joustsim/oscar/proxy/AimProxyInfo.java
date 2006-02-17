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

package net.kano.joustsim.oscar.proxy;

import net.kano.joscar.DefensiveTools;
import org.jetbrains.annotations.Nullable;
import socks.Proxy;
import socks.Socks4Proxy;
import socks.Socks5Proxy;
import socks.UserPasswordAuthentication;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.net.UnknownHostException;

public class AimProxyInfo {
  private SocketFactory socketFactory;
  private ServerSocketFactory serverSocketFactory;

  private String host;
  private int port;
  private String username;
  private String password;
  private Type type;

  public static AimProxyInfo forSocks4(String host, int port,
      @Nullable String username) {
    DefensiveTools.checkNull(host, "host");
    DefensiveTools.checkRange(port, "port", 1, 65535);

    return new AimProxyInfo(Type.SOCKS4, host, port, username, null);
  }

  public static AimProxyInfo forSocks5(String host, int port,
      @Nullable String username, @Nullable String password) {
    DefensiveTools.checkNull(host, "host");
    DefensiveTools.checkRange(port, "port", 1, 65535);
    if(!((username == null) == (password == null))) {
      throw new IllegalArgumentException("Username and password must either "
          + "be both null or neither null; username was " + username
          + ", password was " + password);
    }

    return new AimProxyInfo(Type.SOCKS5, host, port, username, password);
  }

  public static AimProxyInfo forNoProxy() {
    return new AimProxyInfo(Type.NONE, null, -1, null, null);
  }

  /**
   * There are cooler factory methods.
   */
  public AimProxyInfo(Type type, String host, int port, String username,
      String password) {
    this.type = type;
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;

    if (type == Type.SOCKS4 || type == Type.SOCKS5) {
      socketFactory = new SocksSocketFactory(this);
      serverSocketFactory = new SocksServerSocketFactory(this);
    }
  }

  public Type getType() { return type; }

  public String getHost() { return host; }

  public int getPort() { return port; }

  public String getUsername() { return username; }

  public String getPassword() { return password; }

  public @Nullable SocketFactory getSocketFactory() {
    return socketFactory;
  }

  public @Nullable ServerSocketFactory getServerSocketFactory() {
    return serverSocketFactory;
  }

  public Proxy createSocksProxy() throws UnknownHostException {
    if (type == Type.SOCKS4) {
      return new Socks4Proxy(getHost(), getPort(), getUsername());

    } else if (type == Type.SOCKS5) {
      Socks5Proxy proxy = new Socks5Proxy(getHost(), getPort());
      String username = getUsername();
      String password = getPassword();
      if (username != null) {
        assert password != null;
        proxy.setAuthenticationMethod(UserPasswordAuthentication.METHOD_ID,
            new UserPasswordAuthentication(username, password));
      }
      return proxy;
    } else {
      throw new IllegalStateException(type.name());
    }
  }

  public static enum Type { NONE, SOCKS4, SOCKS5, HTTP }
}
