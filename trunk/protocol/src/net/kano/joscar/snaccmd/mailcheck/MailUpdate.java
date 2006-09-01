package net.kano.joscar.snaccmd.mailcheck;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.tlv.MutableTlvChain;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;
import net.kano.joscar.tlv.TlvTools;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

public class MailUpdate extends MailCheckCmd {
    private static final int TYPE_UNREAD_MESSAGES = 0x0080;
    private static final int TYPE_DOMAIN = 0x0082;
    private static final int TYPE_URL = 0x0007;
    private static final int TYPE_ALERT_TITLE = 0x0005;
    private static final int TYPE_ALERT_URL = 0x000d;
    private static final int TYPE_CHECK_TIME = 0x001d;

    private ByteBlock block1;
    private ByteBlock block2;
    private int unreadCount;
    private String url;
    private String domain;
    private String alertTitle;
    private String alertUrl;
    private Date checkedAt;

    protected MailUpdate(SnacPacket packet) {
        super(CMD_UPDATE);

        ByteBlock data = packet.getData();
        block1 = data.subBlock(0, 8);
        block2 = data.subBlock(8, 16);
        int numtlvs = BinaryTools.getUShort(data, 8 + 16);
        TlvChain chain = TlvTools.readChain(data.subBlock(8 + 16 + 2), numtlvs);
        unreadCount = chain.getUShort(TYPE_UNREAD_MESSAGES);
        url = chain.getString(TYPE_URL);
        domain = chain.getString(TYPE_DOMAIN);
        alertTitle = chain.getString(TYPE_ALERT_TITLE);
        alertUrl = chain.getString(TYPE_ALERT_URL);
        checkedAt = new Date(chain.getUInt(TYPE_CHECK_TIME));
    }


    public MailUpdate(ByteBlock block1, ByteBlock block2,
            int unreadCount, String url, String domain, String alertTitle,
            String alertUrl, Date checkedAt) {
        super(CMD_UPDATE);
        this.block1 = block1;
        this.block2 = block2;
        this.unreadCount = unreadCount;
        this.url = url;
        this.domain = domain;
        this.alertTitle = alertTitle;
        this.alertUrl = alertUrl;
        this.checkedAt = checkedAt;
    }

    public ByteBlock getBlock1() {
        return block1;
    }

    public ByteBlock getBlock2() {
        return block2;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public String getUrl() {
        return url;
    }

    public String getDomain() {
        return domain;
    }

    public String getAlertTitle() {
        return alertTitle;
    }

    public String getAlertUrl() {
        return alertUrl;
    }

    public Date getCheckedAt() {
        return checkedAt;
    }

    public void writeData(OutputStream out) throws IOException {
        block1.write(out);
        block2.write(out);
        MutableTlvChain chain = TlvTools.createMutableChain();
        if (unreadCount != -1) {
            chain.addTlv(Tlv.getUShortInstance(TYPE_UNREAD_MESSAGES,
                    unreadCount));
        }
        if (url != null) {
            chain.addTlv(Tlv.getStringInstance(TYPE_URL, url));
        }
        if (domain != null) {
            chain.addTlv(Tlv.getStringInstance(TYPE_DOMAIN, domain));
        }
        if (alertTitle != null) {
            chain.addTlv(Tlv.getStringInstance(TYPE_ALERT_TITLE, alertTitle));
        }
        if (alertUrl != null) {
            chain.addTlv(Tlv.getStringInstance(TYPE_ALERT_URL, alertUrl));
        }
        if (checkedAt != null) {
            chain.addTlv(
                    Tlv.getUIntInstance(TYPE_CHECK_TIME, checkedAt.getTime()));
        }
        BinaryTools.writeUShort(out, chain.getTlvCount());
        chain.write(out);
    }
}
