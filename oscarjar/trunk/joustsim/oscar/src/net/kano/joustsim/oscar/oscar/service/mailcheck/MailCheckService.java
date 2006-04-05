package net.kano.joustsim.oscar.oscar.service.mailcheck;

import net.kano.joustsim.oscar.oscar.service.AbstractService;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joscar.snaccmd.mailcheck.MailCheckCmd;
import net.kano.joscar.snaccmd.mailcheck.MailStatusRequest;
import net.kano.joscar.snaccmd.mailcheck.ActivateMailCmd;
import net.kano.joscar.snaccmd.mailcheck.MailUpdate;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.flapcmd.SnacCommand;

public class MailCheckService extends AbstractService {
  public SnacFamilyInfo getSnacFamilyInfo() {
    return MailCheckCmd.FAMILY_INFO;
  }

  public MailCheckService(AimConnection connection, OscarConnection oscar) {
    super(connection, oscar, MailCheckCmd.FAMILY_MAILCHECK);
  }

  public void connected() {
    sendSnac(new MailStatusRequest());
    sendSnac(new ActivateMailCmd());
    setReady();
  }


  public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
    SnacCommand cmd = snacPacketEvent.getSnacCommand();
    if (cmd instanceof MailUpdate) {
      MailUpdate update = (MailUpdate) cmd;
      //TODO(klea): store unread message count, fire listeners
      System.out.println("got mail update");
    }
  }
}
