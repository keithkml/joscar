package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.RvConnectionInfo;

public interface RvSessionBasedConnection extends RvConnection {
  RvSession getRvSession();
  void setConnectionInfo(RvConnectionInfo connInfo);
  RvConnectionInfo getConnectionInfo();
  RvRequestMaker getRvRequestMaker();
}
