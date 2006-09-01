package net.kano.joustsim.oscar.oscar.service.icbm.ft;

public interface ControllerRestartConsultant {
  void handleRestart();

  boolean shouldRestart();
}
