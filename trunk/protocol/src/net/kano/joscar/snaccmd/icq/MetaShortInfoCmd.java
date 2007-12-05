package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.LEBinaryTools;
import net.kano.joscar.flapcmd.SnacPacket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author jkohen
 * @author yole
 */
public class MetaShortInfoCmd extends AbstractInfoCmd {
    private static final int NDX_NICKNAME = 0;
    private static final int NDX_FNAME = 1;
    private static final int NDX_LNAME = 2;
    private static final int NDX_EMAIL = 3;

    private String[] s;   
    private byte authorizationFlag;
    private byte gender;

    public MetaShortInfoCmd(SnacPacket packet) {
        super(packet);
    }

    public MetaShortInfoCmd(int uin, int id, String nickname, String fname,
            String lname, String email, byte authorizationFlag, byte gender) {
        super(uin, AbstractIcqCmd.CMD_META_SHORT_INFO_CMD, id);

        s = new String[4];
        s[NDX_NICKNAME] = nickname;
        s[NDX_FNAME] = fname;
        s[NDX_LNAME] = lname;
        s[NDX_EMAIL] = email;
        this.authorizationFlag = authorizationFlag;
        this.gender = gender;
    }

    protected void readInfo(InputStream is) throws IOException {
        s = new String[4];
        for (int i = 0; i < s.length; i++) {
            s [i] = LEBinaryTools.readUShortLengthString(is, "US-ASCII");
            if (s [i] == null) break;
        }
        authorizationFlag = (byte) is.read();
        is.read();  // unknown
        gender = (byte) is.read();
    }

    protected void writeInfo(OutputStream out) throws IOException {
        for (String value : s) {
            LEBinaryTools.writeUShortLengthString(out, value);
        }
        LEBinaryTools.writeUByte(out, authorizationFlag);
        LEBinaryTools.writeUByte(out, 0);
        LEBinaryTools.writeUByte(out, gender);
    }

    /**
     * Returns the nick name of the user whose information is represented here,
     * or <code>null</code> if none was given.
     *
     * @return the nick name, or <code>null</code>.
     */
    public String getNickname() {
        if (s == null) return null;
        return s[NDX_NICKNAME];
    }

    /**
     * Returns the first name of the user whose information is represented here,
     * or <code>null</code> if none was given.
     *
     * @return the first name, or <code>null</code>.
     */
    public String getFirstName() {
        if (s == null) return null;
        return s[NDX_FNAME];
    }

    /**
     * Returns the last name of the user whose information is represented here,
     * or <code>null</code> if none was given.
     *
     * @return the last name, or <code>null</code>.
     */
    public String getLastName() {
        if (s == null) return null;
        return s[NDX_LNAME];
    }

    /**
     * Returns the email address of the user whose information is represented here,
     * or <code>null</code> if none was given.
     *
     * @return the email address, or <code>null</code>.
     */
    public String getEmail() {
        if (s == null) return null;
        return s[NDX_EMAIL];
    }

    public byte getAuthorizationFlag() {
        return authorizationFlag;
    }

    public byte getGender() {
        return gender;
    }

    public String toString() {
        return "MetaShortInfoCmd: nick=" + getNickname() +
                " firstname=" + getFirstName() +
                " lastname=" + getLastName() +
                " email=" + getEmail() +
                " authorizationFlag=" + getAuthorizationFlag() +
                " gender=" + getGender() +
                " in: " + super.toString();
    }
}
