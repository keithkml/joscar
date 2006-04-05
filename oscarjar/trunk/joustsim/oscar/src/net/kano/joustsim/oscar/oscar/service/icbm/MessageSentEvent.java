package net.kano.joustsim.oscar.oscar.service.icbm;

import net.kano.joustsim.Screenname;

import java.util.Date;

public class MessageSentEvent extends ConversationEventInfo {
  private final Message msg;

  public MessageSentEvent(Message msg, Screenname mysn, Screenname buddy) {
    super(mysn, buddy, new Date());
    this.msg = msg;
  }

  public Message getMessage() { return msg; }
}
