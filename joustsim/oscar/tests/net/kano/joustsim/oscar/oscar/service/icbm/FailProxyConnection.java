package net.kano.joustsim.oscar.oscar.service.icbm;

import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ProxyConnection;
import net.kano.joscar.rvproto.rvproxy.RvProxyCmd;

import java.io.IOException;

class FailProxyConnection implements ProxyConnection {
  public RvProxyCmd readPacket() throws IOException {
    throw new IOException("read fail");
  }

  public void sendProxyPacket(RvProxyCmd initCmd) throws IOException {
    throw new IOException("write fail");
  }
}
