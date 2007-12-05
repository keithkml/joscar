package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.LEBinaryTools;

import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author yole
 */
public class MetaEmailInfoCmd extends AbstractInfoCmd {
    private String[] emails;
    private boolean[] emailsPublished;

    protected MetaEmailInfoCmd(SnacPacket packet) {
        super(packet);
    }

    protected MetaEmailInfoCmd(long uin, int id, String[] emails, boolean[] emailsPublished) {
        super(uin, AbstractIcqCmd.CMD_META_EMAIL_INFO_CMD, id);
        this.emails = emails;
        this.emailsPublished = emailsPublished;
    }

    protected void readInfo(InputStream is) throws IOException {
        int emailCount = is.read();
        if (emailCount > 0) {
            emails = new String[emailCount];
            emailsPublished = new boolean[emailCount];
            for(int i=0; i<emailCount; i++) {
                emailsPublished [i] = (is.read() > 0);
                emails [i] = LEBinaryTools.readUShortLengthString(is);
            }
        }
    }

    protected void writeInfo(OutputStream out) throws IOException {
        int emailCount = emails == null ? 0 : emails.length;
        LEBinaryTools.writeUByte(out, emailCount);
        for(int i=0; i<emailCount; i++) {
            LEBinaryTools.writeUByte(out, emailsPublished [i] ? 1 : 0);
            LEBinaryTools.writeUShortLengthString(out, emails [i]);
        }
    }


    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("MetaEmailInfoCmd: emails={");
        if (emails != null) {
            for(int i=0; i<emails.length; i++) {
                if (i>0) result.append(",");
                result.append(emails [i]);
                result.append(emailsPublished [i] ? " (+)" : " (-)");
            }
        }
        result.append(" } in ").append(super.toString());
        return result.toString();
    }

    public String[] getEmails(){
        return emails;
    }

    public boolean[] getEmailsPublished(){
        return emailsPublished;
    }
}
