package net.kano.joustsim.oscar.oscar.service.mailcheck;

import net.kano.joscar.snaccmd.mailcheck.MailCheckCmd;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.AbstractServiceArbiter;
import net.kano.joustsim.oscar.oscar.service.ServiceArbiterRequest;
import net.kano.joustsim.oscar.oscar.service.ServiceArbitrationManager;

public class MailCheckServiceArbiter
    extends AbstractServiceArbiter<MailCheckService> {
  public MailCheckServiceArbiter(ServiceArbitrationManager manager) {
    super(manager);
  }

  public int getSnacFamily() {
    return MailCheckCmd.FAMILY_MAILCHECK;
  }

  public boolean shouldKeepAlive() {
    // this service should always be alive
    return true;
  }

  protected void handleRequestsDequeuedEvent(MailCheckService service) {
  }

  protected void processRequest(MailCheckService service,
      ServiceArbiterRequest request) {
  }

  protected MailCheckService createServiceInstance(AimConnection aimConnection,
      OscarConnection conn) {
    return new MailCheckService(aimConnection, conn);
  }
}
