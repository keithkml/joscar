/*
 *  Copyright (c) 2002-2003, The Joust Project
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions 
 *  are met:
 *
 *  - Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer. 
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution. 
 *  - Neither the name of the Joust Project nor the names of its
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 *  File created by keith @ Mar 25, 2003
 *
 */

package net.kano.joscardemo;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.OscarTools;
import net.kano.joscar.SeqNum;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.net.ConnDescriptor;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snac.SnacRequestListener;
import net.kano.joscar.snaccmd.FullRoomInfo;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.chat.ChatMsg;
import net.kano.joscar.snaccmd.conn.ServiceRequest;
import net.kano.joscar.snaccmd.icbm.OldIconHashInfo;
import net.kano.joscar.snaccmd.icbm.SendImIcbm;
import net.kano.joscar.snaccmd.rooms.JoinRoomCmd;
import net.kano.joscardemo.security.SecureSession;
import net.kano.joscardemo.security.SecureSessionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JoscarTester implements CmdLineListener, ServiceListener {
    public static byte[] hashIcon(String filename) throws IOException {
        FileInputStream in = new FileInputStream(filename);
        try {
            byte[] block = new byte[1024];
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }
            for (;;) {
                int count = in.read(block);
                if (count == -1) break;

                md.update(block, 0, count);
            }
            return md.digest();
        } finally {
            in.close();
        }
    }

    private static final int DEFAULT_SERVICE_PORT = 5190;

    private CLHandler clHandler = new CLHandler(this);

    private String sn = null;
    private String pass = null;
    private int uin = -1;
    private SeqNum icqSeqNum = new SeqNum(0, Integer.MAX_VALUE);

    private LoginConn loginConn = null;
    private BosFlapConn bosConn = null;
    private Set<ServiceConn> services = new HashSet<ServiceConn>();
    private Map<String,ChatConn> chats = new HashMap<String, ChatConn>();

    private NumberFormat formatter = NumberFormat.getNumberInstance();
    { // init
        formatter.setGroupingUsed(true);
        formatter.setMaximumFractionDigits(1);
    }

    private SecureSession secureSession = SecureSession.getInstance();
    private String aimExp = "the60s";

    private String mb(long bytes) {
        float divby;
        String suffix;

        if (bytes > 1000000) {
            divby = 1048576.0f;
            suffix = "MB";

        } else {
            divby = 1024.0f;
            suffix = "KB";
        }

        return formatter.format(bytes / divby) + suffix;
    }


    public JoscarTester(String sn, String pass) {
        new CmdLineReader(this);
        this.sn = sn;
        this.pass = pass;
        try {
            this.uin = Integer.parseInt(this.sn);
        }
        catch(NumberFormatException ex) {
            this.uin = -1;
        }
    }

    public String getScreenname() { return sn; }

    public String getPassword() { return pass; }

    public int getUIN() {
        if (uin == -1) {
            throw new IllegalStateException("Not connected to ICQ");
        }
        return uin;
    }

    public long nextIcqId() { return icqSeqNum.next(); }

    public void connect() {
        ConnDescriptor cd = new ConnDescriptor("login.oscar.aol.com",
                DEFAULT_SERVICE_PORT);
        loginConn = new LoginConn(cd, this);
        loginConn.connect();
    }

    void loginFailed(String reason) {
        System.err.println("login failed: " + reason);
    }

    void setScreennameFormat(String screenname) {
        sn = screenname;
    }

    void startBosConn(String server, int port, ByteBlock cookie) {
        bosConn = new BosFlapConn(new ConnDescriptor(server, port), this, cookie);
        bosConn.connect();
    }

    void registerSnacFamilies(BasicConn conn) {
        snacMgr.register(conn);
    }

    public void connectToService(int snacFamily, String host, ByteBlock cookie) {
        ConnDescriptor cd = new ConnDescriptor(host, DEFAULT_SERVICE_PORT);
        ServiceConn conn = new ServiceConn(cd, this, cookie, snacFamily);

        conn.connect();
    }

    public void serviceFailed(ServiceConn conn) {
    }

    public void serviceConnected(ServiceConn conn) {
        services.add(conn);
    }

    public void serviceReady(ServiceConn conn) {
        snacMgr.dequeueSnacs(conn);
    }

    public void serviceDied(ServiceConn conn) {
        services.remove(conn);
        snacMgr.unregister(conn);
    }

    public void joinChat(int exchange, String roomname) {
        FullRoomInfo roomInfo
                = new FullRoomInfo(exchange, roomname, "us-ascii", "en");
        handleRequest(new SnacRequest(new JoinRoomCmd(roomInfo), null));
    }

    void connectToChat(FullRoomInfo roomInfo, String host,
            ByteBlock cookie) {
        ConnDescriptor cd = new ConnDescriptor(host, DEFAULT_SERVICE_PORT);
        ChatConn conn = new ChatConn(cd, this, cookie, roomInfo);

        conn.addChatListener(new MyChatConnListener());

        conn.connect();
    }

    public ChatConn getChatConn(String name) {
        return chats.get(OscarTools.normalize(name));
    }

    private SnacManager snacMgr = new SnacManager(new PendingSnacListener() {
        public void dequeueSnacs(List<SnacRequest> pending) {
            System.out.println("dequeuing " + pending.size() + " snacs");
            for (SnacRequest request : pending) {
                handleRequest(request);
            }
        }
    });

    synchronized void handleRequest(SnacRequest request) {
        int family = request.getCommand().getFamily();
        if (snacMgr.isPending(family)) {
            snacMgr.addRequest(request);
            return;
        }

        BasicConn conn = snacMgr.getConn(family);

        if (conn != null) {
            conn.sendRequest(request);
        } else {
            // it's time to request a service
            if (!(request.getCommand() instanceof ServiceRequest)) {
                System.out.println("requesting " + Integer.toHexString(family)
                        + " service.");
                snacMgr.setPending(family, true);
                snacMgr.addRequest(request);
                request(new ServiceRequest(family));
            } else {
                System.err.println("eep! can't find a service redirector " +
                        "server.");
            }
        }
    }

    public SnacRequest request(SnacCommand cmd) {
        return request(cmd, null);
    }

    private SnacRequest request(SnacCommand cmd, SnacRequestListener listener) {
        SnacRequest req = new SnacRequest(cmd, listener);
        handleRequest(req);
        return req;
    }

    private OldIconHashInfo oldIconInfo;
    private File iconFile = null;
    {
        if (false) {
            // this is not executed
            try {
                ClassLoader classLoader = getClass().getClassLoader();
                URL iconResource = classLoader.getResource("images/beck.gif");
                String ext = iconResource.toExternalForm();
                System.out.println("ext: " + ext);
                URI uri = new URI(ext);
                iconFile = new File(uri);
                oldIconInfo = new OldIconHashInfo(iconFile);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    public OldIconHashInfo getOldIconInfo() { return oldIconInfo; }

    private static final Pattern cmdRE = Pattern.compile(
            "\\s*(?:\"(.*?[^\\\\]?)\"|(\\S+))(?:\\s|$)");

    public void processCmd(CmdLineReader reader, String line) {
        Matcher m = cmdRE.matcher(line);
        LinkedList<String> arglist = new LinkedList<String>();
        while (m.find()) {
            String arg = m.group(1);
            if (arg == null) arg = m.group(2);
            if (arg == null) {
                System.err.println("Error: line parser failed to read "
                        + "arguments");
                return;
            }
            arglist.add(arg);
        }

        if (arglist.isEmpty()) return;

        String cmd = arglist.removeFirst();

        CLCommand handler = clHandler.getCommand(cmd);

        if (handler == null) {
            System.err.println("!! There is no command called '" + cmd + "'");
            System.err.println("!! Try typing 'help'");
        } else {
            try {
                handler.handle(this, line, cmd, arglist);
            } catch (Throwable t) {
                System.err.println("!! Error executing command '" + cmd + "'!");
                System.err.println("!! Try typing 'help " + cmd + "'");
                System.err.println("!! Stack trace of error:");
                t.printStackTrace();
                System.err.println("!! Try typing 'help " + cmd + "'");
            }
        }
    }

    public void printMemUsage() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        System.out.println("Using " + mb(total-free) + " memory of "
                + mb(total) + " allocated");
    }

    public void sendIM(String nick, String text) {
        request(new SendImIcbm(nick, text));
    }

    public SecureSession getSecureSession() { return secureSession; }

    public void setAimExp(String aimExp) { this.aimExp = aimExp; }

    public String getAimExp() { return aimExp; }

    public BasicConn getBosConn() { return bosConn; }

    private class MyChatConnListener implements ChatConnListener {
        public void connFailed(ChatConn conn, Object reason) { }

        public void connected(ChatConn conn) { }

        public void joined(ChatConn conn, List<FullUserInfo> members) {
            String name = conn.getRoomInfo().getName();
            chats.put(OscarTools.normalize(name), conn);

            System.out.println("*** Joined "
                    + conn.getRoomInfo().getRoomName() + ", members:");
            for (FullUserInfo member : members) {
                System.out.println("  " + member.getScreenname());
            }
        }

        public void left(ChatConn conn, Object reason) {
            String name = conn.getRoomInfo().getName();
            chats.remove(OscarTools.normalize(name));

            System.out.println("*** Left "
                    + conn.getRoomInfo().getRoomName());
        }

        public void usersJoined(ChatConn conn, List<FullUserInfo> members) {
            for (FullUserInfo member : members) {
                System.out.println("*** " + member.getScreenname()
                        + " joined " + conn.getRoomInfo().getRoomName());
            }
        }

        public void usersLeft(ChatConn conn, List<FullUserInfo> members) {
            for (FullUserInfo member : members) {
                System.out.println("*** " + member.getScreenname()
                        + " left " + conn.getRoomInfo().getRoomName());
            }
        }

        public void gotMsg(ChatConn conn, FullUserInfo sender,
                ChatMsg msg) {
            String msgStr = msg.getMessage();
            String ct = msg.getContentType();
            if (msgStr == null && ct.equals(ChatMsg.CONTENTTYPE_SECURE)) {
                ByteBlock msgData = msg.getMessageData();

                try {
                    msgStr = secureSession.parseChatMessage(conn.getRoomName(),
                            sender.getScreenname(), msgData);
                } catch (SecureSessionException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("<" + sender.getScreenname()
                    + ":#" + conn.getRoomInfo().getRoomName() + "> "
                    + OscarTools.stripHtml(msgStr));
        }


    }

    public static void main(String[] args) {
        String levelstr = "fine";
        System.out.println("Connecting to AIM as " + args[0] + "...");

        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() {
            private String lineSeparator = System.getProperty("line.separator");

            public String format(LogRecord record) {
                StringBuffer sb = new StringBuffer();
                sb.append("[");
                sb.append(record.getLevel().getLocalizedName());
                sb.append("] ");
                sb.append(record.getMessage());
                sb.append(lineSeparator);

                if (record.getThrown() != null) {
                    try {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        record.getThrown().printStackTrace(pw);
                        pw.close();
                        sb.append(sw.toString());
                    } catch (Exception ex) {
                        // SimpleFormatter in the JDK does this, so I do too
                    }
                }
                return sb.toString();
            }
        });
        Level level = Level.parse(levelstr.toUpperCase());
        handler.setLevel(level);
        Logger logger = Logger.getLogger("net.kano.joscar");
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);

        final JoscarTester tester = new JoscarTester(args[0], args[1]);
        tester.connect();

        new Timer(true).scheduleAtFixedRate(new TimerTask() {
            public void run() {
                tester.printMemUsage();
            }
        },
                30*1000, 5*60*1000);
    }

//    static void something(int x, ByteBuffer buffer) {}
//    static void something(int x, String buffer) {}
//
//    private static void x() {
//        byte[] x = null;
//        something(5, x)
//
//    }
}
