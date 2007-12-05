package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.LEBinaryTools;
import net.kano.joscar.flapcmd.SnacPacket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

/**
 * @author jkohen
 * @author yole
 */
public class MetaBasicInfoCmd extends AbstractInfoCmd {
    private static final int NDX_NICKNAME = 0;
    private static final int NDX_FNAME = 1;
    private static final int NDX_LNAME = 2;
    private static final int NDX_EMAIL = 3;
    private static final int NDX_HOME_CITY = 4;
    private static final int NDX_HOME_STATE = 5;
    private static final int NDX_HOME_PHONE = 6;
    private static final int NDX_HOME_FAX = 7;
    private static final int NDX_HOME_ADDRESS = 8;
    private static final int NDX_CELL_PHONE = 9;
    private static final int NDX_HOME_ZIP = 10;

    private String[] s;
    private int countryCode;
    private int gmtOffset;
    private boolean authorization;
    private boolean webAware;
    private boolean publishPrimaryEmail;

    public MetaBasicInfoCmd(SnacPacket packet) {
        super(packet);
    }

    protected void readInfo(InputStream is) throws IOException {
        s = new String[11];
        for (int i = 0; i < s.length; i++) {
            s [i] = LEBinaryTools.readUShortLengthString(is);
            if (s [i] == null) break;
        }
        countryCode = LEBinaryTools.readUShort(is);
        gmtOffset = is.read();
        authorization = (is.read() > 0);
        webAware = (is.read() > 0);
        publishPrimaryEmail = (is.read() > 0);
    }

    protected void writeInfo(OutputStream out) throws IOException {
        for (String value : s) {
            LEBinaryTools.writeUShortLengthString(out, value);
        }
        LEBinaryTools.writeUShort(out, countryCode);
        LEBinaryTools.writeUByte(out, gmtOffset);
        LEBinaryTools.writeUByte(out, authorization ? 1 : 0);
        LEBinaryTools.writeUByte(out, webAware ? 1 : 0);
        LEBinaryTools.writeUByte(out, publishPrimaryEmail ? 1 : 0);
    }

    // TODO[yole]: rework API for creating info packets
    // constructors with 15 parameters suck...
    public MetaBasicInfoCmd(int uin, int id, String nickname, String fname,
            String lname, String email, String homeCity, String homeState,
            String homePhone, String homeFax, String homeAddress,
            String cellPhone, String homeZip) {
        super(uin, AbstractIcqCmd.CMD_META_BASIC_INFO_CMD, id);

        s = new String[11];
        s[NDX_NICKNAME] = nickname;
        s[NDX_FNAME] = fname;
        s[NDX_LNAME] = lname;
        s[NDX_EMAIL] = email;
        s[NDX_HOME_CITY] = homeCity;
        s[NDX_HOME_STATE] = homeState;
        s[NDX_HOME_PHONE] = homePhone;
        s[NDX_HOME_FAX] = homeFax;
        s[NDX_HOME_ADDRESS] = homeAddress;
        s[NDX_CELL_PHONE] = cellPhone;
        s[NDX_HOME_ZIP] = homeZip;
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

    public String getHomeCity() {
        return s[NDX_HOME_CITY];
    }

    public String getHomeState() {
        return s[NDX_HOME_STATE];
    }

    public String getHomePhone() {
        return s[NDX_HOME_PHONE];
    }

    public String getHomeFax() {
        return s[NDX_HOME_FAX];
    }

    public String getHomeAddress() {
        return s[NDX_HOME_ADDRESS];
    }

    public String getCellPhone() {
        return s[NDX_CELL_PHONE];
    }

    public String getHomeZip() {
        return s[NDX_HOME_ZIP];
    }


    public void setCountryCode(int countryCode) {
        this.countryCode = countryCode;
    }

    public void setGmtOffset(int gmtOffset) {
        this.gmtOffset = gmtOffset;
    }

    public void setAuthorization(boolean authorization) {
        this.authorization = authorization;
    }

    public void setWebAware(boolean webAware) {
        this.webAware = webAware;
    }

    public void setPublishPrimaryEmail(boolean publishPrimaryEmail) {
        this.publishPrimaryEmail = publishPrimaryEmail;
    }

    public int getCountryCode() {
        return countryCode;
    }

    public int getGmtOffset() {
        return gmtOffset;
    }

    public boolean isAuthorization() {
        return authorization;
    }

    public boolean isWebAware() {
        return webAware;
    }

    public boolean isPublishPrimaryEmail() {
        return publishPrimaryEmail;
    }

    public String toString() {
        return "MetaBasicInfoCmd: nick=" + getNickname() +
                " firstname=" + getFirstName() +
                " lastname=" + getLastName() +
                " email=" + getEmail() +
                " homeCity=" + getHomeCity() +
                " homeState=" + getHomeState() +
                " homePhone=" + getHomePhone() +
                " homeFax=" + getHomeFax() +
                " homeAddress=" + getHomeAddress() +
                " cellPhone=" + getCellPhone() +
                " homeZip=" + getHomeZip() +
                " countryCode=" + getCountryCode() +
                " gmtOffset=" + getGmtOffset() +
                " authorization=" + isAuthorization() +
                " webAware=" + isWebAware() +
                " publishPrimaryEmail=" + isPublishPrimaryEmail() +
                " in: " + super.toString();
    }
}
