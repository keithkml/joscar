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
public class MetaAffiliationsInfoCmd extends AbstractInfoCmd {
    int[] backgroundCategoryCodes;
    String[] backgrounds;
    int[] affiliationCategoryCodes;
    String[] affiliations;

    protected MetaAffiliationsInfoCmd(SnacPacket packet) {
        super(packet);
    }

    protected MetaAffiliationsInfoCmd(long uin, int id,
            int[] backgroundCategoryCodes, String[] backgrounds,
            int[] affiliationCategoryCodes, String[] affiliations) {
        super(uin, AbstractIcqCmd.CMD_META_AFFILIATIONS_INFO_CMD, id);
        DefensiveTools.checkNull(backgroundCategoryCodes, "backgroundCategoryCodes");
        DefensiveTools.checkNull(backgrounds, "backgrounds");
        DefensiveTools.checkNull(affiliationCategoryCodes, "affiliationCategoryCodes");
        DefensiveTools.checkNull(affiliations, "affiliations");
        if (backgroundCategoryCodes.length != backgroundCategoryCodes.length) {
            throw new IllegalArgumentException("Count of background category codes must equal count of backgrounds");
        }
        if (affiliationCategoryCodes.length != affiliationCategoryCodes.length) {
            throw new IllegalArgumentException("Count of affiliation category codes must equal count of affiliations");
        }
        this.backgroundCategoryCodes = backgroundCategoryCodes;
        this.backgrounds = backgrounds;
        this.affiliationCategoryCodes = affiliationCategoryCodes;
        this.affiliations = affiliations;
    }

    protected void readInfo(InputStream is) throws IOException {
        int count = is.read();
        backgrounds = new String[count];
        backgroundCategoryCodes = new int[count];
        for(int i=0; i<count; i++) {
            backgroundCategoryCodes [i] = LEBinaryTools.readUShort(is);
            backgrounds [i] = LEBinaryTools.readUShortLengthString(is, "US-ASCII");
        }
        count = is.read();
        affiliations = new String[count];
        affiliationCategoryCodes = new int[count];
        for(int i=0; i<count; i++) {
            affiliationCategoryCodes [i] = LEBinaryTools.readUShort(is);
            affiliations [i] = LEBinaryTools.readUShortLengthString(is, "US-ASCII");
        }
    }

    protected void writeInfo(OutputStream out) throws IOException {
        out.write(backgrounds.length);
        for(int i=0; i<backgrounds.length; i++) {
            LEBinaryTools.writeUShort(out, backgroundCategoryCodes [i]);
            LEBinaryTools.writeUShortLengthString(out, backgrounds [i]);
        }
        out.write(affiliations.length);
        for(int i=0; i<affiliations.length; i++) {
            LEBinaryTools.writeUShort(out, affiliationCategoryCodes [i]);
            LEBinaryTools.writeUShortLengthString(out, affiliations [i]);
        }
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("MetaAffiliationsInfoCmd: backgrounds={");
        for(int i=0; i<backgrounds.length; i++) {
            if (i>0) result.append(",");
            result.append(backgroundCategoryCodes [i]).append(" :");
            result.append(backgrounds [i]);
        }
        result.append("} affiliations={");
        for(int i=0; i<affiliations.length; i++) {
            if (i>0) result.append(",");
            result.append(affiliationCategoryCodes [i]).append(" :");
            result.append(affiliations [i]);
        }
        result.append(" } in ").append(super.toString());
        return result.toString();
    }
}
