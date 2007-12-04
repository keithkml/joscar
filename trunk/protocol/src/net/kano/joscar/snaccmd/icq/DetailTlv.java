package net.kano.joscar.snaccmd.icq;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;

import net.kano.joscar.*;

/**
 * Tlv set in command for changing user account info stored on server
 * The methods of this class work with
 * little-endian data, which is mainly required by the old-ICQ commands in family 21
 * of SNAC packets.
 *
 * @author Damian Minkov
 */
public class DetailTlv
    implements Writable
{
    private byte[] data = new byte[0];
    private int type;

    public DetailTlv(int type)
    {
        this.type = type;
    }

    public void write(OutputStream out)
        throws IOException
    {
        LEBinaryTools.writeUShort(out, type);
        LEBinaryTools.writeUShort(out, data.length);
        out.write(data);
    }

    public long getWritableLength()
    {
        return 4 + data.length;
    }

    public void writeUInt(long number)
    {
        byte[] tmp = LEBinaryTools.getUInt(number);
        byte[] newData = new byte[data.length + tmp.length];

        System.arraycopy(data, 0, newData, 0, data.length);
        System.arraycopy(tmp, 0, newData, data.length, tmp.length);

        data = newData;
    }

    public void writeUShort(int number)
    {
        byte[] tmp = LEBinaryTools.getUShort(number);
        byte[] newData = new byte[data.length + tmp.length];

        System.arraycopy(data, 0, newData, 0, data.length);
        System.arraycopy(tmp, 0, newData, data.length, tmp.length);

        data = newData;
    }

    public void writeUByte(int number)
    {
        byte[] tmp = LEBinaryTools.getUByte(number);
        byte[] newData = new byte[data.length + tmp.length];

        System.arraycopy(data, 0, newData, 0, data.length);
        System.arraycopy(tmp, 0, newData, data.length, tmp.length);

        data = newData;
    }

    public void writeString(String str)
    {
        if(str == null)
            str = "";// empty string so length will be 0 and nothing to be writen

        byte[] tmp = BinaryTools.getAsciiBytes(str);

        // save the string length before we process the string bytes
        writeUShort(tmp.length);

        byte[] newData = new byte[data.length + tmp.length];

        System.arraycopy(data, 0, newData, 0, data.length);
        System.arraycopy(tmp, 0, newData, data.length, tmp.length);

        data = newData;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("TLV: type=0x");
        buffer.append(Integer.toHexString(type));
        if (data == null) {
            buffer.append(" (no data block)");
        } else {
            int len = data.length;
            buffer.append(", length=" + len);

            if (len > 0) {
                CharsetDecoder ascii = Charset.forName("US-ASCII").newDecoder();

                CharBuffer chars = null;
                try {
                    chars = ascii.decode(ByteBuffer.wrap(data));
                } catch (CharacterCodingException e) { }

                boolean alternatevalue = false;
                if (chars != null) {
                    buffer.append(", ascii value=\"" + chars.toString() + "\"");
                    alternatevalue = true;
                }

                if (!alternatevalue) buffer.append(" - hex: ");
                else buffer.append(": ");
            }
        }

        return buffer.toString();
    }

}
