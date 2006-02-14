package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import net.kano.joscar.rvproto.rvproxy.RvProxyCmd;

import java.io.IOException;

public interface ProxyConnection {
  RvProxyCmd readPacket() throws IOException;

  void sendProxyPacket(RvProxyCmd initCmd) throws IOException;
}
