package net.kano.joscar.snaccmd.icq;

import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.LEBinaryTools;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author yole
 */
public class MetaHomepageCategoryInfoCmd extends AbstractInfoCmd {
    private int categoryCode;
    private String keywords;

    protected MetaHomepageCategoryInfoCmd(SnacPacket packet) {
        super(packet);
    }

    protected MetaHomepageCategoryInfoCmd(long uin, IcqType type, int id) {
        super(uin, AbstractIcqCmd.CMD_META_HOMEPAGE_CATEGORY_INFO_CMD, id);
    }

    protected void readInfo(InputStream is) throws IOException {
        int enabled = is.read();
        if (enabled != 0) {
            categoryCode = LEBinaryTools.readUShort(is);
            keywords = LEBinaryTools.readUShortLengthString(is, "US-ASCII");
        }
    }

    protected void writeInfo(OutputStream out) throws IOException {
        out.write(1);
        LEBinaryTools.writeUShort(out, categoryCode);
        LEBinaryTools.writeUShortLengthString(out, keywords);
        out.write(0);
    }

    public String toString() {
        return "MetaHomepageCategoryInfoCmd: categoryCode=" + categoryCode +
                " keywords=" + keywords +
                " in " + super.toString();
    }

    public int getCategory(){
        return categoryCode;
    }

    public String getKeywords(){
        return keywords;
    }
}
