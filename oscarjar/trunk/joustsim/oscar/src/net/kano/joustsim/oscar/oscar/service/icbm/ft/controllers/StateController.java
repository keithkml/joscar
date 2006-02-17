package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;

public interface StateController {
  void start(RvConnection transfer, StateController last);

  void addControllerListener(ControllerListener listener);

  void removeControllerListener(ControllerListener listener);

  StateInfo getEndStateInfo();

  void stop();
}
