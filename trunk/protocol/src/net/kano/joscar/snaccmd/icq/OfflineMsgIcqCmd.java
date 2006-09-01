package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.LEBinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.OscarTools;
import net.kano.joscar.BinaryTools;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.io.OutputStream;
import java.io.IOException;

/**
 * @author jkohen
 * @author yole
 */
public class OfflineMsgIcqCmd extends FromIcqCmd {
    private long fromUIN;
    private Calendar cal;
    private int msgType;
    private String contents;

    protected OfflineMsgIcqCmd(SnacPacket packet) {
        super(packet);

        ByteBlock block = getIcqData();

        fromUIN = LEBinaryTools.getUInt(block, 0);
        cal = new GregorianCalendar(
            LEBinaryTools.getUShort(block, 4),
            LEBinaryTools.getUByte(block, 6),
            LEBinaryTools.getUByte(block, 7),
            LEBinaryTools.getUByte(block, 8),
            LEBinaryTools.getUByte(block, 9));
        msgType = LEBinaryTools.getUShort(block, 10);
        final int textlen = LEBinaryTools.getUShort(block, 12) - 1; // Don't include the ending NUL.
        block = block.subBlock(14, textlen);
        contents = OscarTools.getString(block, "US-ASCII");
    }

    /**
     * Creates a new instance of this command given the specified values.
     *
     * @param uin the UIN of the sender.
     * @param date the date this message was sent.
     * @param msgType the type of the message.
     * @param contents the content of the message.
     */
    public OfflineMsgIcqCmd(long ownerUIN, int id, long uin, Date date, int msgType, String contents) {
        super(ownerUIN, AbstractIcqCmd.CMD_OFFLINE_MSG, id);

        this.fromUIN = uin;
        this.cal = new GregorianCalendar();
        cal.setTime(date);
        this.msgType = msgType;
        this.contents = contents;
    }


    public void writeIcqData(OutputStream out) throws IOException {
        LEBinaryTools.writeUInt(out, fromUIN);
        LEBinaryTools.writeUShort(out, cal.get(Calendar.YEAR));
        LEBinaryTools.writeUByte(out, cal.get(Calendar.MONTH));
        LEBinaryTools.writeUByte(out, cal.get(Calendar.DAY_OF_MONTH));
        LEBinaryTools.writeUByte(out, cal.get(Calendar.HOUR_OF_DAY));
        LEBinaryTools.writeUByte(out, cal.get(Calendar.MINUTE));
        LEBinaryTools.writeUShort(out, msgType);
        LEBinaryTools.writeUShort(out, contents.length() + 1); // Plus an ending NUL.

        byte[] bytes = BinaryTools.getAsciiBytes(contents + '\0');
        out.write(bytes);
    }

    /**
     * Returns the time the message was sent.
     *
     * @return the time this message was sent.
     */
    public Date getDate() {
        return cal.getTime();
    }

    /**
     * Returns the content of this message. The meaning varies accordingly
     * with the {@link #getMsgType() "message type"}.
     *
     * @return the content of this message.
     */
    public String getContents() {
        return contents;
    }

    /**
     * Returns the UIN of the sender of this message.
     *
     * @return the UIN of the sender.
     */
    public long getFromUIN() {
        return fromUIN;
    }

    /**
     * Returns the type of this message.
     *
     * @return the type of this message.
     */
    public int getMsgType() {
        return msgType;
    }

    public String toString() {
        return "OfflineMsgIcqCmd: <" + getContents() + "> in: "
                + super.toString();
    }
}
