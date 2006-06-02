package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.LEBinaryTools;

import java.io.OutputStream;
import java.io.IOException;

/**
 * @author jkohen
 * @author yole
 */
public class MetaFullInfoRequest extends ToIcqCmd {
    private long requestUIN;

    public MetaFullInfoRequest(long ownerUIN, int id, long requestUIN) {
        super(ownerUIN, AbstractIcqCmd.CMD_META_FULL_INFO_REQ, id);
        this.requestUIN = requestUIN;
    }

    protected MetaFullInfoRequest(SnacPacket packet) {
        super(packet);
        ByteBlock block = getIcqData();

        requestUIN = LEBinaryTools.getUInt(block, 0);
    }

    public void writeIcqData(OutputStream out) throws IOException {
       LEBinaryTools.writeUInt(out, requestUIN);
    }

    public long getRequestUIN() {
        return requestUIN;
    }
}
