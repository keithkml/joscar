package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.LEBinaryTools;

import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Calendar;

/**
 * @author yole
 */
public class MetaMoreInfoCmd extends AbstractInfoCmd {
    private int age;
    private int gender;
    private String homepage;
    private Date birthDate;
    private int[] speakingLanguages = new int[3];
    private String originalCity;
    private String originalState;
    private int originalCountryCode;
    private int timeZone;

    protected MetaMoreInfoCmd(SnacPacket packet) {
        super(packet);
    }

    protected MetaMoreInfoCmd(long uin, int id) {
        super(uin, AbstractIcqCmd.CMD_META_MORE_INFO_CMD, id);
    }

    protected void readInfo(InputStream is) throws IOException {
        age = LEBinaryTools.readUShort(is);
        gender = is.read();
        homepage = LEBinaryTools.readUShortLengthString(is);
        int year = LEBinaryTools.readUShort(is);
        int month = is.read();
        int day = is.read();
        birthDate = new GregorianCalendar(year, month, day).getTime();
        speakingLanguages = new int[3];
        for(int i=0; i<3; i++) {
            speakingLanguages [i] = is.read();
        }
        is.skip(2); // unknown
        originalCity = LEBinaryTools.readUShortLengthString(is);
        originalState = LEBinaryTools.readUShortLengthString(is);
        originalCountryCode = LEBinaryTools.readUShort(is);
        timeZone = is.read();
    }

    protected void writeInfo(OutputStream out) throws IOException {
        LEBinaryTools.writeUShort(out, age);
        LEBinaryTools.writeUByte(out, gender);
        LEBinaryTools.writeUShortLengthString(out, homepage);
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(birthDate);
        LEBinaryTools.writeUShort(out, gc.get(Calendar.YEAR));
        LEBinaryTools.writeUByte(out, gc.get(Calendar.MONTH));
        LEBinaryTools.writeUByte(out, gc.get(Calendar.DAY_OF_MONTH));
        for(int i=0; i<3; i++) {
            LEBinaryTools.writeUByte(out, speakingLanguages [i]);
        }
        LEBinaryTools.writeUShort(out, 0);
        LEBinaryTools.writeUShortLengthString(out, originalCity);
        LEBinaryTools.writeUShortLengthString(out, originalState);
        LEBinaryTools.writeUShort(out, originalCountryCode);
        LEBinaryTools.writeUByte(out, timeZone);
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public int[] getSpeakingLanguages() {
        return speakingLanguages;
    }

    public void setSpeakingLanguages(int[] speakingLanguages) {
        this.speakingLanguages = speakingLanguages;
    }

    public String getOriginalCity() {
        return originalCity;
    }

    public void setOriginalCity(String originalCity) {
        this.originalCity = originalCity;
    }

    public String getOriginalState() {
        return originalState;
    }

    public void setOriginalState(String originalState) {
        this.originalState = originalState;
    }

    public int getOriginalCountryCode() {
        return originalCountryCode;
    }

    public void setOriginalCountryCode(int originalCountryCode) {
        this.originalCountryCode = originalCountryCode;
    }

    public int getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(int timeZone) {
        this.timeZone = timeZone;
    }

    public String toString() {
        return "MetaMoreInfoCmd: age=" + getAge() +
                " gender=" + getGender() +
                " homepage=" + getHomepage() +
                " birthDate=" + getBirthDate() +
                " speakingLanguages=[" +
                speakingLanguages [0] + "," + speakingLanguages[1] + "," + speakingLanguages[2] +
                "] originalCity=" + getOriginalCity() +
                " originalState=" + getOriginalState() +
                " originalCountry=" + getOriginalCountryCode() +
                " timeZone=" + getTimeZone() +
                " in: " + super.toString();
    }
}
