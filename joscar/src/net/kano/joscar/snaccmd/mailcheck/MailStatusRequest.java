package net.kano.joscar.snaccmd.mailcheck;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.BinaryTools;
import net.kano.joscar.flapcmd.SnacPacket;

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;

public class MailStatusRequest extends MailCheckCmd {
    private final List<ByteBlock> cookies;

    protected MailStatusRequest(SnacPacket packet) {
        super(CMD_MAIL_REQUEST);
        ByteBlock data = packet.getData();
        int count = BinaryTools.getUShort(data, 0);
        List<ByteBlock> mcookies = new ArrayList<ByteBlock>();
        for (int i = 0; i < count; i++) {
            mcookies.add(data.subBlock(2+(i*16)));
        }
        this.cookies = Collections.unmodifiableList(mcookies);
    }

    public MailStatusRequest() {
        super(CMD_MAIL_REQUEST);
        cookies = Collections.unmodifiableList(Arrays.asList(
                ByteBlock.createFromUnsigned(
                0xb3, 0x80, 0x9a, 0xd8, 0x0d, 0xba, 0x11, 0xd5,
                0x9f, 0x8a, 0x00, 0x60, 0xb0, 0xee, 0x06, 0x31),
                ByteBlock.createFromUnsigned(
                0x5d, 0x5e, 0x17, 0x08, 0x55, 0xaa, 0x11, 0xd3,
                0xb1, 0x43, 0x00, 0x60, 0xb0, 0xfb, 0x1e, 0xcb)));
    }


    @SuppressWarnings({"ReturnOfCollectionOrArrayField"})
    public List<ByteBlock> getCookies() {
        return cookies;
    }

    public void writeData(OutputStream out) throws IOException {
        BinaryTools.writeUShort(out, cookies.size());
        for (ByteBlock block : cookies) block.write(out);
    }
}
