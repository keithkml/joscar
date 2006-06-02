package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.ByteBlock;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Base class for commands used for sending user info data.
 *
 * @author yole
 */
public abstract class AbstractInfoCmd extends FromIcqCmd {
    protected AbstractInfoCmd(SnacPacket packet) {
        super(packet);
        ByteBlock block = getIcqData();

        ByteArrayInputStream bais = (ByteArrayInputStream) ByteBlock.createInputStream(block);
        if (bais.read() != 0x0A) {
            return;
        }

        try {
            readInfo(bais);
        } catch (IOException e) {
            // ignore
        }
    }

    protected AbstractInfoCmd(long uin, IcqType type, int id) {
        super(uin, type, id);
    }


    public final void writeIcqData(OutputStream out) throws IOException {
        out.write(0x0A);
        writeInfo(out);
    }

    protected abstract void readInfo(InputStream is) throws IOException;
    protected abstract void writeInfo(OutputStream out) throws IOException;
}
