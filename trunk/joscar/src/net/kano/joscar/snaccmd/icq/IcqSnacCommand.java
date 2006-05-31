package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;

public abstract class IcqSnacCommand extends SnacCommand {
    /** The SNAC family code of this family. */
    public static final int FAMILY_ICQ = 0x0015;
    /** A SNAC family info block for this family. */
    public static final SnacFamilyInfo FAMILY_INFO
        = new SnacFamilyInfo(FAMILY_ICQ, 0x0001, 0x0110, 0x08e4);
    /** Send a command to the old ICQ server. */
    public static final int CMD_TO_ICQ = 0x0002;
    /** A message from the old ICQ server. */
    public static final int CMD_FROM_ICQ = 0x0003;

    public IcqSnacCommand(int family, int command) {
        super(family, command);
    }
}
