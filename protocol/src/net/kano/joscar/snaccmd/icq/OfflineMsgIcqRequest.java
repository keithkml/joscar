package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.flapcmd.SnacPacket;

/**
 * @author jkohen
 * @author yole
 */
public class OfflineMsgIcqRequest extends ToIcqCmd {
    public OfflineMsgIcqRequest(long uin, int id) {
        super(uin, AbstractIcqCmd.CMD_OFFLINE_MSG_REQ, id);
    }

    protected OfflineMsgIcqRequest(SnacPacket packet) {
        super(packet);
    }
}
