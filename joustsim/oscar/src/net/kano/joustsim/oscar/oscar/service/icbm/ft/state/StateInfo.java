package net.kano.joustsim.oscar.oscar.service.icbm.ft.state;

import net.kano.joscar.MiscTools;

public abstract class StateInfo {
  StateInfo() { }

  public String toString() {
    return MiscTools.getClassName(this);
  }
}
