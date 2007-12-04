package net.kano.joustsim.oscar.oscar.service.icbm;

import net.kano.joustsim.oscar.oscar.service.MutableService;
import net.kano.joustsim.Screenname;
import net.kano.joscar.snac.SnacRequestListener;
import net.kano.joscar.snaccmd.icbm.InstantMessage;

public interface MutableIcbmService extends MutableService, IcbmService {
  void sendIM(Screenname buddy, String body, boolean autoresponse);

  void sendIM(Screenname buddy, String body, boolean autoresponse,
      SnacRequestListener listener);

  void sendIM(Screenname buddy, String body, boolean autoresponse,
      SnacRequestListener listener, boolean isOffline);

  void sendIM(Screenname buddy, InstantMessage im, boolean autoresponse);

  void sendIM(Screenname buddy, InstantMessage im, boolean autoresponse,
      SnacRequestListener listener);

  void sendIM(Screenname buddy, InstantMessage im, boolean autoresponse,
      SnacRequestListener listener, boolean isOffline);

  void sendTypingStatus(Screenname buddy, TypingState typingState);
}
