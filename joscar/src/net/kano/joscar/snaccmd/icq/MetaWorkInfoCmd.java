package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.LEBinaryTools;
import net.kano.joscar.flapcmd.SnacPacket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author yole
 */
public class MetaWorkInfoCmd extends AbstractInfoCmd {
    private String workCity;
    private String workState;
    private String workPhone;
    private String workFax;
    private String workAddress;
    private String workZipCode;
    private int workCountryCode;
    private String workCompany;
    private String workDepartment;
    private String workPosition;
    private int workOccupationCode;
    private String workWebPage;

    protected MetaWorkInfoCmd(SnacPacket packet) {
        super(packet);
    }

    public MetaWorkInfoCmd(long uin, int id) {
        super(uin, AbstractIcqCmd.CMD_META_WORK_INFO_CMD, id);
    }

    protected void readInfo(InputStream is) throws IOException {
        String charset = "US-ASCII";
        workCity = LEBinaryTools.readUShortLengthString(is, charset);
        workState = LEBinaryTools.readUShortLengthString(is, charset);
        workPhone = LEBinaryTools.readUShortLengthString(is, charset);
        workFax = LEBinaryTools.readUShortLengthString(is, charset);
        workAddress = LEBinaryTools.readUShortLengthString(is, charset);
        workZipCode = LEBinaryTools.readUShortLengthString(is, charset);
        workCountryCode = LEBinaryTools.readUShort(is);
        workCompany = LEBinaryTools.readUShortLengthString(is, charset);
        workDepartment = LEBinaryTools.readUShortLengthString(is, charset);
        workPosition = LEBinaryTools.readUShortLengthString(is, charset);
        workOccupationCode = LEBinaryTools.readUShort(is);
        workWebPage = LEBinaryTools.readUShortLengthString(is, charset);
    }

    protected void writeInfo(OutputStream out) throws IOException {
        LEBinaryTools.writeUShortLengthString(out, workCity);
        LEBinaryTools.writeUShortLengthString(out, workState);
        LEBinaryTools.writeUShortLengthString(out, workPhone);
        LEBinaryTools.writeUShortLengthString(out, workFax);
        LEBinaryTools.writeUShortLengthString(out, workAddress);
        LEBinaryTools.writeUShortLengthString(out, workZipCode);
        LEBinaryTools.writeUShort(out, workCountryCode);
        LEBinaryTools.writeUShortLengthString(out, workCompany);
        LEBinaryTools.writeUShortLengthString(out, workDepartment);
        LEBinaryTools.writeUShortLengthString(out, workPosition);
        LEBinaryTools.writeUShort(out, workOccupationCode);
        LEBinaryTools.writeUShortLengthString(out, workWebPage);
    }

    public String getWorkCity() {
        return workCity;
    }

    public void setWorkCity(String workCity) {
        this.workCity = workCity;
    }

    public String getWorkState() {
        return workState;
    }

    public void setWorkState(String workState) {
        this.workState = workState;
    }

    public String getWorkPhone() {
        return workPhone;
    }

    public void setWorkPhone(String workPhone) {
        this.workPhone = workPhone;
    }

    public String getWorkFax() {
        return workFax;
    }

    public void setWorkFax(String workFax) {
        this.workFax = workFax;
    }

    public String getWorkAddress() {
        return workAddress;
    }

    public void setWorkAddress(String workAddress) {
        this.workAddress = workAddress;
    }

    public String getWorkZipCode() {
        return workZipCode;
    }

    public void setWorkZipCode(String workZipCode) {
        this.workZipCode = workZipCode;
    }

    public int getWorkCountryCode() {
        return workCountryCode;
    }

    public void setWorkCountryCode(int workCountryCode) {
        this.workCountryCode = workCountryCode;
    }

    public String getWorkCompany() {
        return workCompany;
    }

    public void setWorkCompany(String workCompany) {
        this.workCompany = workCompany;
    }

    public String getWorkDepartment() {
        return workDepartment;
    }

    public void setWorkDepartment(String workDepartment) {
        this.workDepartment = workDepartment;
    }

    public String getWorkPosition() {
        return workPosition;
    }

    public void setWorkPosition(String workPosition) {
        this.workPosition = workPosition;
    }

    public int getWorkOccupationCode() {
        return workOccupationCode;
    }

    public void setWorkOccupationCode(int workOccupationCode) {
        this.workOccupationCode = workOccupationCode;
    }

    public String getWorkWebPage() {
        return workWebPage;
    }

    public void setWorkWebPage(String workWebPage) {
        this.workWebPage = workWebPage;
    }

    public String toString() {
        return "MetaWorkInfoCmd: city=" + getWorkCity() +
                " state=" + getWorkState() +
                " phone=" + getWorkPhone() +
                " fax=" + getWorkFax() +
                " address=" + getWorkAddress() +
                " zipCode=" + getWorkZipCode() +
                " countryCode=" + getWorkCountryCode() +
                " company=" + getWorkCompany() +
                " department=" + getWorkDepartment() +
                " position=" + getWorkPosition() +
                " occupationCode=" + getWorkOccupationCode() +
                " webpage=" + getWorkWebPage() +
                " in: " + super.toString();
    }
}
