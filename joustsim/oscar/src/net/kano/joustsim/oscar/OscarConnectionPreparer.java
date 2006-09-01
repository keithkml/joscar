package net.kano.joustsim.oscar;

import net.kano.joustsim.oscar.oscar.BasicConnection;
import net.kano.joustsim.oscar.oscar.LoginConnection;

public interface OscarConnectionPreparer {
  void prepareMainBosConnection(ConnectionManager mgr, BasicConnection conn);

  void prepareLoginConnection(ConnectionManager mgr, LoginConnection conn);
}
