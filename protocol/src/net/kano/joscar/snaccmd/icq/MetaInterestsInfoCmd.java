package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.LEBinaryTools;
import net.kano.joscar.DefensiveTools;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author yole
 */
public class MetaInterestsInfoCmd extends AbstractInfoCmd {
    int[] categoryCodes;
    String[] interests;

    protected MetaInterestsInfoCmd(SnacPacket packet) {
        super(packet);
    }

    protected MetaInterestsInfoCmd(long uin, int id, int[] categoryCodes, String[] interests) {
        super(uin, AbstractIcqCmd.CMD_META_INTERESTS_INFO_CMD, id);
        DefensiveTools.checkNull(categoryCodes, "categoryCodes");
        DefensiveTools.checkNull(interests, "interests");
        if (categoryCodes.length != interests.length) {
            throw new IllegalArgumentException("Count of category codes must equal count of interests");
        }
    }

    protected void readInfo(InputStream is) throws IOException {
        int count = is.read();
        interests = new String[count];
        categoryCodes = new int[count];
        for(int i=0; i<count; i++) {
            categoryCodes [i] = LEBinaryTools.readUShort(is);
            interests [i] = LEBinaryTools.readUShortLengthString(is);
        }
    }

    protected void writeInfo(OutputStream out) throws IOException {
        out.write(interests.length);
        for(int i=0; i<interests.length; i++) {
            LEBinaryTools.writeUShort(out, categoryCodes [i]);
            LEBinaryTools.writeUShortLengthString(out, interests [i]);
        }
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("MetaInterestInfoCmd: interests={");
        for(int i=0; i<interests.length; i++) {
            if (i>0) result.append(",");
            result.append(categoryCodes [i]).append(" :");
            result.append(interests [i]);
        }
        result.append(" } in ").append(super.toString());
        return result.toString();
    }

    public String[] getInterests(){
        return interests;
    }

    public int[] getCategories(){
        return categoryCodes;
    }

}
