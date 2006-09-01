package net.kano.joustsim.oscar.oscar.service;

import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snac.SnacRequestListener;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snac.SnacResponseEvent;

public interface MutableService extends Service {
  void sendSnac(SnacCommand snac);

  void sendSnacRequest(SnacRequest request);

  void sendSnacRequest(SnacCommand cmd,
      SnacRequestListener listener);

  void connected();

  void disconnected();

  void handleSnacPacket(SnacPacketEvent snacPacketEvent);

  void handleSnacResponse(SnacResponseEvent snacResponseEvent);

  void handleEvent(ServiceEvent event);
}
