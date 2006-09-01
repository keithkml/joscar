package net.kano.joustsim.oscar;

import net.kano.joscar.CopyOnWriteArrayList;

public abstract class AbstractAimSession implements AimSession {
  private CopyOnWriteArrayList<AimSessionListener> listeners
      = new CopyOnWriteArrayList<AimSessionListener>();

  protected void fireOpenedConnection(AimConnection conn) {
    for (AimSessionListener l : listeners) {
      l.handleOpenedConnection(this, conn);
    }
  }

  public void addSessionListener(AimSessionListener listener) {
    listeners.addIfAbsent(listener);
  }

  public void removeSessionListener(AimSessionListener listener) {
    listeners.remove(listener);
  }
}
