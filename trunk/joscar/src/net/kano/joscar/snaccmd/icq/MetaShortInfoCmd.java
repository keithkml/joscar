package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.LEBinaryTools;
import net.kano.joscar.OscarTools;
import net.kano.joscar.BinaryTools;

import java.io.OutputStream;
import java.io.IOException;

/**
 * @author jkohen
 * @author yole
 */
public class MetaShortInfoCmd extends FromIcqCmd {
    private static final int NDX_NICKNAME = 0;
    private static final int NDX_FNAME = 1;
    private static final int NDX_LNAME = 2;
    private static final int NDX_EMAIL = 3;

    private String[] s = new String[4];
    public MetaShortInfoCmd(SnacPacket packet) {
        super(packet);
        ByteBlock block = getIcqData();

        if (block.get(0) != 0x0A) {
            return;
        }
        int offset = 1;
        for (int i = 0; i < s.length; i++) {
            final int textlen = LEBinaryTools.getUShort(block, offset) - 1; // Don't include the ending NUL.
            offset += 2;

            if (textlen > 0) {
                ByteBlock field = block.subBlock(offset, textlen);
                s[i] = OscarTools.getString(field, "US-ASCII");
                offset += textlen;
            }
            offset++; // Skip trailing NUL.
        }
    }

    public MetaShortInfoCmd(int uin, int id, String nickname, String fname, String lname, String email) {
        super(uin, AbstractIcqCmd.CMD_META_SHORT_INFO_CMD, id);

        s[NDX_NICKNAME] = nickname;
        s[NDX_FNAME] = fname;
        s[NDX_LNAME] = lname;
        s[NDX_EMAIL] = email;
    }

    public void writeIcqData(OutputStream out) throws IOException {
        LEBinaryTools.writeUByte(out, 0x0a); // Unknown.
        for (String value : s) {
            LEBinaryTools.writeUShort(out,
                    value.length() + 1); // Plus an ending NUL.
            byte[] bytes = BinaryTools.getAsciiBytes(value + '\0');
            out.write(bytes);
        }
    }

    /**
     * Returns the nick name of the user whose information is represented here,
     * or <code>null</code> if none was given.
     *
     * @return the nick name, or <code>null</code>.
     */
    public String getNickname() {
        return s[NDX_NICKNAME];
    }

    /**
     * Returns the first name of the user whose information is represented here,
     * or <code>null</code> if none was given.
     *
     * @return the first name, or <code>null</code>.
     */
    public String getFirstName() {
        return s[NDX_FNAME];
    }

    /**
     * Returns the last name of the user whose information is represented here,
     * or <code>null</code> if none was given.
     *
     * @return the last name, or <code>null</code>.
     */
    public String getLastName() {
        return s[NDX_LNAME];
    }

    /**
     * Returns the email address of the user whose information is represented here,
     * or <code>null</code> if none was given.
     *
     * @return the email address, or <code>null</code>.
     */
    public String getEmail() {
        return s[NDX_EMAIL];
    }


    public String toString() {
        return "MetaShortInfoCmd: nick=" + getNickname() +
                " firstname=" + getFirstName() +
                " lastname=" + getLastName() +
                " email=" + getEmail() + " in: " + super.toString();
    }
}
