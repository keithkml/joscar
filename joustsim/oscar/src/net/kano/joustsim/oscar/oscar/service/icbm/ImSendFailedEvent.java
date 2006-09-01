package net.kano.joustsim.oscar.oscar.service.icbm;

import net.kano.joscar.snaccmd.error.SnacError;
import net.kano.joustsim.Screenname;

public class ImSendFailedEvent extends SendFailedEvent {
  protected final SnacError error;

  public ImSendFailedEvent(Screenname from, Screenname to,
      SnacError error, Message msg) {
    super(from, to, msg);
    this.error = error;
  }

  public int getErrorCode() { return error == null ? -1 : error.getErrorCode(); }
}
