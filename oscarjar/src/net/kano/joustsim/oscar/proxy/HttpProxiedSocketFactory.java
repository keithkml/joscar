package net.kano.joustsim.oscar.proxy;

import net.kano.joscar.DefensiveTools;
import sun.misc.BASE64Encoder;
import sun.net.www.protocol.http.HttpURLConnection;

import javax.net.SocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HttpProxiedSocketFactory extends SocketFactory {
  private static final Logger LOGGER = Logger
      .getLogger(HttpProxiedSocketFactory.class.getName());
  private static final Pattern RESPONSE_PATTERN
      = Pattern.compile("HTTP/\\S+\\s(\\d+)\\s(.*)\\s*");

  private final AimProxyInfo aimProxyInfo;

  public HttpProxiedSocketFactory(AimProxyInfo aimProxyInfo) {
    this.aimProxyInfo = aimProxyInfo;
  }

  public Socket createSocket(String host, int port)
      throws IOException, UnknownHostException {
    Socket socket = new Socket(aimProxyInfo.getHost(), aimProxyInfo.getPort());
    prepareHttpProxySocket(socket, host, port);

    return socket;
  }

  public Socket createSocket(String host, int port, InetAddress localaddr,
      int localPort) throws IOException, UnknownHostException {
    Socket socket = new Socket(aimProxyInfo.getHost(), aimProxyInfo.getPort(),
        localaddr, localPort);
    prepareHttpProxySocket(socket, host, port);
    return socket;
  }

  public Socket createSocket(InetAddress inetAddress, int port)
      throws IOException {
    Socket socket = new Socket(aimProxyInfo.getHost(), aimProxyInfo.getPort());
    prepareHttpProxySocket(socket, inetAddress.getHostAddress(), port);
    return socket;
  }

  public Socket createSocket(InetAddress inetAddress, int port,
      InetAddress localaddr, int localPort) throws IOException {
    Socket socket = new Socket(aimProxyInfo.getHost(), aimProxyInfo.getPort(),
        localaddr, localPort);
    prepareHttpProxySocket(socket, inetAddress.getHostAddress(), port);
    return socket;
  }

  private void prepareHttpProxySocket(Socket socket, String host, int port)
      throws IOException {
    DefensiveTools.checkNull(host, "host");
    DefensiveTools.checkRange(port, "port", 1, 65535);

    String proxyhost = aimProxyInfo.getHost();
    String hostport = "CONNECT " + host + ":" + port;
    String proxyLine;
    String username = aimProxyInfo.getUsername();
    if (username == null) {
      proxyLine = "";
    } else {
      String password = aimProxyInfo.getPassword();
      assert password != null : username;

      proxyLine = "\r\nProxy-Authorization: Basic "
          + new BASE64Encoder().encode((username + ":"
          + password).getBytes("UTF-8"));
    }
    socket.getOutputStream().write((hostport + " HTTP/1.1\r\nHost: "
        + hostport + proxyLine + "\r\n\r\n").getBytes("UTF-8"));
    InputStream in = socket.getInputStream();
    StringBuilder got = new StringBuilder(100);
    int nlchars = 0;
    while (true) {
      char c = (char) in.read();
      got.append(c);
      if (got.length() > 1024) {
        LOGGER.warning("Recieved header of >1024 characters from "
            + proxyhost + ", cancelling connection");
        throw new HttpProxyException();
      }
      if (c == -1) throw new HttpProxyException();
      if ((nlchars == 0 || nlchars == 2) && c == '\r') {
        nlchars++;
      } else if ((nlchars == 1 || nlchars == 3) && c == '\n') {
        nlchars++;
      } else {
        nlchars = 0;
      }
      if (nlchars == 4) {
        break;
      }
    }

    if (nlchars != 4) {
      LOGGER.warning("Never received blank line from " + proxyhost
          + ", cancelling connection");
      throw new HttpProxyException();
    }

    String gotstr = got.toString();
    LOGGER.finer("Got response from HTTP proxy:\n" + gotstr);
    BufferedReader br = new BufferedReader(new StringReader(gotstr));
    String response = br.readLine();
    if (response == null) {
      LOGGER.warning("Empty proxy response from " + proxyhost
          + ", cancelling");
      throw new HttpProxyException();
    }
    Matcher m = RESPONSE_PATTERN.matcher(response);
    if (!m.matches()) {
      LOGGER.warning("Unexpected proxy response from "
          + proxyhost + ": " + response);
      throw new HttpProxyException();
    }
    int code = Integer.parseInt(m.group(1));
    LOGGER.info("Got HTTP " + code + " response from "
        + proxyhost + " (" + m.group(2) + ")");
    if (code != HttpURLConnection.HTTP_OK) throw new HttpProxyException();
  }

  private static class HttpProxyException extends IOException {
    public HttpProxyException(IOException e) {
      initCause(e);
    }

    public HttpProxyException() {
    }
  }

}
