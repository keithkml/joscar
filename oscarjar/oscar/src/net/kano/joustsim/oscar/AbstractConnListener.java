package net.kano.joustsim.oscar;

import net.kano.joustsim.oscar.oscar.OscarConnListener;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.OscarConnStateEvent;

public abstract class AbstractConnListener implements OscarConnListener {
  public void registeredSnacFamilies(OscarConnection conn) {
  }

  public void connStateChanged(OscarConnection conn,
      OscarConnStateEvent event) {
  }

  public void allFamiliesReady(OscarConnection conn) {
  }
}
