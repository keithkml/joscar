package net.kano.joscar.snaccmd.mailcheck;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.flapcmd.SnacPacket;

import java.io.IOException;
import java.io.OutputStream;

public class ActivateMailCmd extends MailCheckCmd {
    private final ByteBlock data;

    protected ActivateMailCmd(SnacPacket packet) {
        super(CMD_ACTIVATE);
        data = packet.getData();
    }

    public ActivateMailCmd() {
        super(CMD_ACTIVATE);
        data = ByteBlock.createFromUnsigned(
                0x02,
                0x04, 0x00, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00);
    }

    public ByteBlock getData() {
        return data;
    }

    public void writeData(OutputStream out) throws IOException {
        data.write(out);
    }
}
