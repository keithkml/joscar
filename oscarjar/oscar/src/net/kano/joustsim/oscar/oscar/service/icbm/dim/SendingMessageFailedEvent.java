package net.kano.joustsim.oscar.oscar.service.icbm.dim;

import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.Message;

public class SendingMessageFailedEvent extends RvConnectionEvent {
  private final Message message;

  public SendingMessageFailedEvent(Message message) {
    this.message = message;
  }

  public Message getMessage() { return message; }
}
