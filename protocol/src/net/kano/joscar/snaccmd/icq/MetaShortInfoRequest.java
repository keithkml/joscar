package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.LEBinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.flapcmd.SnacPacket;

import java.io.OutputStream;
import java.io.IOException;

/**
 * @author jkohen
 * @author yole
 */
public class MetaShortInfoRequest extends ToIcqCmd {
    private long requestUIN;

    public MetaShortInfoRequest(long ownerUIN, int id, long requestUIN) {
        super(ownerUIN, AbstractIcqCmd.CMD_META_SHORT_INFO_REQ, id);
        this.requestUIN = requestUIN;
    }

    protected MetaShortInfoRequest(SnacPacket packet) {
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
