package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.flapcmd.*;
import net.kano.joscar.*;

/**
 * Result for client change full-info tlv-based request.
 * If success byte equal 0x0A - operation was finished succesfully,
 * if not - database error.
 * Request was sent by SNAC(15,02)/07D0/0C3A.
 *
 * @author Damian Minkov
 */
public class MetaFullInfoAckCmd
    extends FromIcqCmd
{
    private static final int SUCCESS_BYTE = 0x0A;

    private boolean isSuccess = false;

    /**
     * Constructs incoming Command
     *
     * @param packet FromIcqCmd
     */
    public MetaFullInfoAckCmd(SnacPacket packet)
    {
        super(packet);

        byte[] result = getIcqData().toByteArray();

        if(result.length == 1 &&
           result[0] == SUCCESS_BYTE)
        {
            this.isSuccess = true;
        }
    }

    /**
     * Return the data from this command
     * whether the command which this packet is reply to
     * is succesful or not
     *
     * @return boolean
     */
    public boolean isCommandSuccesful()
    {
        return isSuccess;
    }
}
