package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.flapcmd.SnacPacket;

/**
 * @author jkohen
 * @author yole
 */
public class OfflineMsgIcqAckCmd extends ToIcqCmd {
    public OfflineMsgIcqAckCmd(long uin, int id) {
        super(uin, AbstractIcqCmd.CMD_OFFLINE_MSG_ACK, id);
    }

    protected OfflineMsgIcqAckCmd(SnacPacket packet) {
        super(packet);
    }
}
