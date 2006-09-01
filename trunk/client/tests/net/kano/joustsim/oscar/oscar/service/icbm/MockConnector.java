package net.kano.joustsim.oscar.oscar.service.icbm;

import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.Connector;

public interface MockConnector extends Connector {
  void waitForConnectionAttempt();

  boolean hasAttemptedConnection();
}
