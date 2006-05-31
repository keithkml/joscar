package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.flapcmd.SnacPacket;

/**
 * @author jkohen
 * @author yole
 */
public class OfflineMsgDoneCmd extends FromIcqCmd {
    protected OfflineMsgDoneCmd(SnacPacket packet) {
        super(packet);
    }

    public OfflineMsgDoneCmd(long uin, int id) {
        super(uin, AbstractIcqCmd.CMD_OFFLINE_MSG_DONE, id);
    }

    public String toString() {
        return "OfflineMsgDoneCmd in: " + super.toString();
    }
}
