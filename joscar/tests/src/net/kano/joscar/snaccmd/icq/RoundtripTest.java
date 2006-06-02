package net.kano.joscar.snaccmd.icq;

import junit.framework.TestCase;
import net.kano.joscar.flapcmd.SnacFlapCmd;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.snaccmd.DefaultClientFactoryList;
import net.kano.joscar.snac.CmdFactoryMgr;
import net.kano.joscar.snac.DefaultSnacCmdFactoryList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author yole
 */
public class RoundtripTest extends TestCase {
    private SnacCommand roundtrip(SnacCommand cmd,
            DefaultSnacCmdFactoryList factoryList) throws IOException {
        SnacFlapCmd flapCmd = new SnacFlapCmd(0, cmd);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        flapCmd.writeData(baos);
        ByteBlock flapData = ByteBlock.wrap(baos.toByteArray());

        SnacPacket packet = SnacPacket.readSnacPacket(flapData);
        CmdFactoryMgr factories = new CmdFactoryMgr();
        factories.setDefaultFactoryList(factoryList);
        return factories.generateSnacCommand(packet);
    }

    public void testMetaShortInfo() throws Exception {
        String nickname = "yole@work";
        String firstName = "Dmitry";
        String lastName = "Jemerov";
        String email = "yole@yole.ru";
        MetaShortInfoCmd cmd = new MetaShortInfoCmd(123, 1,
                nickname, firstName, lastName, email, (byte) 0, (byte) 0);
        SnacCommand receivedCmd = roundtrip(cmd, new DefaultClientFactoryList());
        assertTrue(receivedCmd instanceof MetaShortInfoCmd);
        MetaShortInfoCmd newCmd = (MetaShortInfoCmd) receivedCmd;
        assertEquals(nickname, newCmd.getNickname());
        assertEquals(firstName, newCmd.getFirstName());
        assertEquals(lastName, newCmd.getLastName());
        assertEquals(email, newCmd.getEmail());
    }

    public void testMetaBasicInfo() throws Exception {
        String nickname = "yole@work";
        String firstName = "Dmitry";
        String lastName = "Jemerov";
        String email = "yole@yole.ru";
        String homeZip = "197341";
        MetaBasicInfoCmd cmd = new MetaBasicInfoCmd(123, 1,
                nickname, firstName, lastName, email, "", "", "", "", "", "",
                homeZip);
        SnacCommand receivedCmd = roundtrip(cmd, new DefaultClientFactoryList());
        MetaBasicInfoCmd newCmd = (MetaBasicInfoCmd) receivedCmd;
        assertEquals(nickname, newCmd.getNickname());
        assertEquals(firstName, newCmd.getFirstName());
        assertEquals(lastName, newCmd.getLastName());
        assertEquals(email, newCmd.getEmail());
        assertEquals(homeZip, newCmd.getHomeZip());
    }

    public void testOfflineMsg() throws Exception {
        Date date = new GregorianCalendar(2006, 6, 1, 11, 34).getTime();
        int uin = 654;
        int msgType = 5;
        String contents = "Test contents";
        OfflineMsgIcqCmd cmd = new OfflineMsgIcqCmd(123, 1, uin, date, msgType,
                contents);
        OfflineMsgIcqCmd newCmd = (OfflineMsgIcqCmd) roundtrip(cmd,
                new DefaultClientFactoryList());
        assertEquals(uin, newCmd.getFromUIN());
        assertEquals(date, newCmd.getDate());
        assertEquals(msgType, newCmd.getMsgType());
        assertEquals(contents, newCmd.getContents());
    }
}
