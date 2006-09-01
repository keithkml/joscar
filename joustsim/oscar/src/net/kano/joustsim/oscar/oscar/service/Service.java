package net.kano.joustsim.oscar.oscar.service;

import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;

public interface Service {
  AimConnection getAimConnection();

  OscarConnection getOscarConnection();

  int getFamily();

  SnacFamilyInfo getSnacFamilyInfo();

  void addServiceListener(ServiceListener l);

  void removeServiceListener(ServiceListener l);

  boolean isReady();

  boolean isFinished();
}
