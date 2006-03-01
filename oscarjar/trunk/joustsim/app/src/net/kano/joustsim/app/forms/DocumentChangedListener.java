package net.kano.joustsim.app.forms;

import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

public abstract class DocumentChangedListener implements DocumentListener {
  public void changedUpdate(DocumentEvent e) {
    changed();
  }

  public void insertUpdate(DocumentEvent e) {
    changed();
  }

  public void removeUpdate(DocumentEvent e) {
    changed();
  }

  protected abstract void changed();
}
