package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.LEBinaryTools;
import net.kano.joscar.flapcmd.SnacPacket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

/**
 * @author yole
 */
public class MetaNotesInfoCmd extends AbstractInfoCmd {
    private String notes;

    protected MetaNotesInfoCmd(SnacPacket packet) {
        super(packet);
    }

    protected MetaNotesInfoCmd(long uin, int id, String notes) {
        super(uin, AbstractIcqCmd.CMD_META_NOTES_INFO_CMD, id);
        this.notes = notes;
    }

    protected void readInfo(InputStream is) throws IOException {
        notes = LEBinaryTools.readUShortLengthString(is, "US-ASCII");
    }

    protected void writeInfo(OutputStream out) throws IOException {
        LEBinaryTools.writeUShortLengthString(out, notes);
    }

    public String getNotes() {
        return notes;
    }

    public String toString() {
        return "MetaNotesInfoCmd: notes=" + getNotes() +
                " in: " + super.toString();
    }
}
