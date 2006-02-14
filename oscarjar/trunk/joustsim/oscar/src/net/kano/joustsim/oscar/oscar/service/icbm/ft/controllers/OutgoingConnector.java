package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import java.net.InetAddress;
import java.io.IOException;

public interface OutgoingConnector extends Connector {
  InetAddress getIpAddress() throws IOException;

  int getConnectionPort();
}
