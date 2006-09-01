package net.kano.joscar.snaccmd.mailcheck;

import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;

public abstract class MailCheckCmd extends SnacCommand {
    public static final int FAMILY_MAILCHECK = 0x0018;

    public static final int CMD_MAIL_REQUEST = 0x0006;
    public static final int CMD_ACTIVATE = 0x0016;
    public static final int CMD_UPDATE = 0x0007;

    public static final SnacFamilyInfo FAMILY_INFO
            = new SnacFamilyInfo(FAMILY_MAILCHECK, 0x0001, 0x0010, 0x08aa);

    protected MailCheckCmd(int cmd) {
        super(FAMILY_MAILCHECK, cmd);
    }
}
