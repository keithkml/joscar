package net.kano.joustsim.oscar.oscar.service.icbm;

import net.kano.joustsim.Screenname;

import java.util.Date;

public class SendFailedEvent extends ConversationEventInfo {
  private final Message message;

  public SendFailedEvent(Screenname myScreenname, Screenname buddy,
      Message failedMessage) {
    super(myScreenname, buddy, new Date());
    this.message = failedMessage;
  }

  public Message getMessage() { return message; }
}
