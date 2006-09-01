package net.kano.joustsim.oscar.oscar.service.icbm;

import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rvcmd.RejectRvCmd;
import net.kano.joscar.rvcmd.AcceptRvCmd;
import net.kano.joscar.rvcmd.ConnectionRequestRvCmd;

public interface MockRvSessionHandler extends RendezvousSessionHandler {
  void handleIncomingReject(RecvRvEvent event,
      RejectRvCmd rejectCmd);

  void handleIncomingAccept(RecvRvEvent event,
      AcceptRvCmd acceptCmd);


  void handleIncomingRequest(RecvRvEvent event,
      ConnectionRequestRvCmd reqCmd);
}
