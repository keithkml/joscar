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
import net.kano.joscar.FileWritable;
import net.kano.joscar.OscarTools;
import net.kano.joscar.flap.ClientFlapConn;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.InvitationMessage;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.rvcmd.chatinvite.ChatInvitationRvCmd;
import net.kano.joscar.rvcmd.directim.DirectIMReqRvCmd;
import net.kano.joscar.rvcmd.getfile.GetFileReqRvCmd;
import net.kano.joscar.rvcmd.sendbl.SendBuddyListGroup;
import net.kano.joscar.rvcmd.sendbl.SendBuddyListRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendBlock;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joscar.rvcmd.trillcrypt.TrillianCryptReqRvCmd;
import net.kano.joscar.rvcmd.voice.VoiceReqRvCmd;
import net.kano.joscar.snac.ClientSnacProcessor;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snac.SnacRequestListener;
import net.kano.joscar.snaccmd.DefaultClientFactoryList;
import net.kano.joscar.snaccmd.DirInfo;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joscar.snaccmd.FullRoomInfo;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.MiniRoomInfo;
import net.kano.joscar.snaccmd.acct.AcctInfoRequest;
import net.kano.joscar.snaccmd.acct.AcctModCmd;
import net.kano.joscar.snaccmd.acct.ConfirmAcctCmd;
import net.kano.joscar.snaccmd.chat.ChatMsg;
import net.kano.joscar.snaccmd.conn.ServiceRequest;
import net.kano.joscar.snaccmd.conn.SetExtraInfoCmd;
import net.kano.joscar.snaccmd.icbm.InstantMessage;
import net.kano.joscar.snaccmd.icbm.OldIconHashInfo;
import net.kano.joscar.snaccmd.icbm.SendImIcbm;
import net.kano.joscar.snaccmd.icon.UploadIconCmd;
import net.kano.joscar.snaccmd.invite.InviteFriendCmd;
import net.kano.joscar.snaccmd.loc.GetDirInfoCmd;
import net.kano.joscar.snaccmd.loc.GetInfoCmd;
import net.kano.joscar.snaccmd.loc.SetDirInfoCmd;
import net.kano.joscar.snaccmd.loc.SetInterestsCmd;
import net.kano.joscar.snaccmd.rooms.ExchangeInfoReq;
import net.kano.joscar.snaccmd.rooms.JoinRoomCmd;
import net.kano.joscar.snaccmd.rooms.RoomRightsRequest;
import net.kano.joscar.snaccmd.search.InterestListReq;
import net.kano.joscar.snaccmd.search.SearchBuddiesCmd;
import net.kano.joscar.snaccmd.ssi.CreateItemsCmd;
import net.kano.joscar.snaccmd.ssi.DeleteItemsCmd;
import net.kano.joscar.snaccmd.ssi.ModifyItemsCmd;
import net.kano.joscar.snaccmd.ssi.SsiItem;
import net.kano.joscar.ssiitem.BuddyItem;
import net.kano.joscar.ssiitem.DenyItem;
import net.kano.joscar.ssiitem.GroupItem;
import net.kano.joscar.ssiitem.IconItem;
import net.kano.joscar.ssiitem.PermitItem;
import net.kano.joscar.ssiitem.PrivacyItem;
import net.kano.joscar.ssiitem.RootItem;
import net.kano.joscar.ssiitem.VisibilityItem;
import net.kano.joscardemo.security.SecureSession;
import net.kano.joscardemo.security.SecureSessionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JoscarTester implements CmdLineListener {
    protected static final int DEFAULT_SERVICE_PORT = 5190;

    protected DefaultClientFactoryList factoryList
            = new DefaultClientFactoryList();

    protected ClientFlapConn loginFlapConn = null, mainConn = null;
    protected ClientSnacProcessor loginSnacProcessor = null;

    protected List serviceConns = new ArrayList();

    protected String sn = null;
    protected String pass = null;

    protected LoginConn loginConn = null;
    protected BosFlapConn bosConn = null;
    protected Set services = new HashSet();
    protected Map chats = new HashMap();

    private SecureSession secureSession = new SecureSession();

    public JoscarTester(String sn, String pass) {
        new CmdLineReader(this);
        this.sn = sn;
        this.pass = pass;
    }

    public String getScreenname() { return sn; }

    public String getPassword() { return pass; }

    public void connect() {
        loginConn = new LoginConn("login.oscar.aol.com", DEFAULT_SERVICE_PORT,
                this);
        loginConn.connect();
    }

    void loginFailed(String reason) {
        System.out.println("login failed: " + reason);
    }

    void setScreennameFormat(String screenname) {
        sn = screenname;
    }

    void startBosConn(String server, int port, ByteBlock cookie) {
        bosConn = new BosFlapConn(server, port, this, cookie);
        bosConn.connect();
    }

    void registerSnacFamilies(BasicConn conn) {
        snacMgr.register(conn);
    }

    void connectToService(int snacFamily, String host, ByteBlock cookie) {
        ServiceConn conn = new ServiceConn(host, DEFAULT_SERVICE_PORT, this,
                cookie, snacFamily);

        conn.connect();
    }

    void serviceFailed(ServiceConn conn) {
    }

    void serviceConnected(ServiceConn conn) {
        services.add(conn);
    }

    void serviceReady(ServiceConn conn) {
        snacMgr.dequeueSnacs(conn);
    }

    void serviceDied(ServiceConn conn) {
        services.remove(conn);
        snacMgr.unregister(conn);
    }

    void joinChat(int exchange, String roomname) {
        FullRoomInfo roomInfo
                = new FullRoomInfo(exchange, roomname, "us-ascii", "en");
        handleRequest(new SnacRequest(new JoinRoomCmd(roomInfo), null));
    }

    void connectToChat(FullRoomInfo roomInfo, String host,
            ByteBlock cookie) {
        ChatConn conn = new ChatConn(host, DEFAULT_SERVICE_PORT, this, cookie,
                roomInfo);

        conn.addChatListener(new MyChatConnListener());

        conn.connect();
    }

    public ChatConn getChatConn(String name) {
        return (ChatConn) chats.get(OscarTools.normalize(name));
    }

    protected SnacManager snacMgr = new SnacManager(new PendingSnacListener() {
        public void dequeueSnacs(SnacRequest[] pending) {
            System.out.println("dequeuing " + pending.length + " snacs");
            for (int i = 0; i < pending.length; i++) {
                handleRequest(pending[i]);
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
                System.out.println("eep! can't find a service redirector " +
                        "server.");
            }
        }
    }

    SnacRequest request(SnacCommand cmd) {
        return request(cmd, null);
    }

    private SnacRequest request(SnacCommand cmd, SnacRequestListener listener) {
        SnacRequest req = new SnacRequest(cmd, listener);
        handleRequest(req);
        return req;
    }

    protected SortedMap cmdMap = new TreeMap();

    OldIconHashInfo oldIconInfo;
    File iconFile = null;
    { if (false) {
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

    protected static byte[] hashIcon(String filename) throws IOException {
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

    private String aimexp = "the60s";

    {
        cmdMap.put("setexp", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                aimexp = args[0];
            }
        });
        cmdMap.put("im", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                byte[] textBytes;
                try {
                    textBytes = aimexp.getBytes("US-ASCII");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return;
                }
                byte[] data = new byte[textBytes.length + 1];
                data[0] = 0x05;
                System.arraycopy(textBytes, 0, data, 1, textBytes.length);
                ByteBlock block = ByteBlock.wrap(data);

                request(new SendImIcbm(args[0], new InstantMessage(args[1]),
                        false, 0, false, oldIconInfo, new ExtraInfoBlock[] {
                            new ExtraInfoBlock(0x0080, new ExtraInfoData(0, block)),
                            new ExtraInfoBlock(0x0082, new ExtraInfoData(0, block))
                        }, true));
            }
        });
        cmdMap.put("info", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
//                request(new OldGetInfoCmd(OldGetInfoCmd.TYPE_AWAYMSG, args[0]));
//                request(new OldGetInfoCmd(OldGetInfoCmd.TYPE_INFO, args[0]));
                request(new GetInfoCmd(GetInfoCmd.FLAG_AWAYMSG | GetInfoCmd.FLAG_INFO, args[0]));
            }
        });
        cmdMap.put("dirinfo", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new GetDirInfoCmd(args[0]));
            }
        });
        cmdMap.put("reformat", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new AcctModCmd(args[0], null));
            }
        });
        cmdMap.put("setemail", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new AcctModCmd(null, args[0]));
            }
        });
        cmdMap.put("confirm", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new ConfirmAcctCmd());
            }
        });
        cmdMap.put("getformat", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new AcctInfoRequest(AcctInfoRequest.TYPE_SN));
            }
        });
        cmdMap.put("getemail", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new AcctInfoRequest(AcctInfoRequest.TYPE_EMAIL));
            }
        });
        cmdMap.put("chatrights", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new RoomRightsRequest());
            }
        });
        cmdMap.put("joinroom", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                joinChat(Integer.parseInt(args[0]), args[1]);
            }
        });
        cmdMap.put("exinfo", new CLCommand() {
            // request exchange info
            public void handle(String line, String cmd, String[] args) {
                request(new ExchangeInfoReq(Integer.parseInt(args[0])));
            }
        });
        cmdMap.put("chatsay", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                getChatConn(args[0]).sendMsg(args[1]);
            }
        });
        cmdMap.put("inviteafriend", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new InviteFriendCmd(args[0], args[1]));
            }
        });
        cmdMap.put("getinterests", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new InterestListReq());
            }
        });
        cmdMap.put("searchbyname", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(SearchBuddiesCmd.createSearchByDirInfoCmd(new DirInfo(
                        null, null, args[0], null, null, null, null, null, null,
                        null, null, null, null)));
            }
        });
        cmdMap.put("searchbyemail", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(SearchBuddiesCmd.createSearchByEmailCmd(args[0]));
            }
        });
        cmdMap.put("searchbyinterest", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(SearchBuddiesCmd.createSearchByInterestCmd(args[0]));
            }
        });
        cmdMap.put("setdir", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new SetDirInfoCmd(new DirInfo(
                        "email", "first", "middle", "last", "maiden", "nick",
                        "address", "city", "st", "zip", "CA", "en")));
            }
        });
        cmdMap.put("unsetdir", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new SetDirInfoCmd(null));
            }
        });
        cmdMap.put("setinterests", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new SetInterestsCmd(args));
            }
        });
        cmdMap.put("setavail", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
//                request(new SetExtraInfoCmd(
//                        args.length == 0 ? "" : args[0]));
            }
        });
        cmdMap.put("testav", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new SetExtraInfoCmd(new ExtraInfoBlock[] {
                    new ExtraInfoBlock(3, new ExtraInfoData(0,
                            ByteBlock.wrap("hi".getBytes()))),
                    new ExtraInfoBlock(2, new ExtraInfoData(4,
                            ByteBlock.wrap(
                                    new byte[] { 0, 3, 50, 40, 30, 0, 0 })))
                }));
            }
        });
        cmdMap.put("getcertinfo", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new GetInfoCmd(GetInfoCmd.FLAG_CERT, args[0]));
            }
        });
        // WORKS
        cmdMap.put("addbuddy", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new CreateItemsCmd(new SsiItem[] {
                    new BuddyItem(args[0], Integer.parseInt(args[1]),
                            Integer.parseInt(args[2]), "ALIASDUDE",
                            "COMMENTDUDE", BuddyItem.MASK_WHEN_ONLINE,
                            BuddyItem.MASK_ACTION_PLAY_SOUND,
                            "newalert").toSsiItem() }));
            }
        });
        // WORKS
        cmdMap.put("delbuddy", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new DeleteItemsCmd(new SsiItem[] {
                    new BuddyItem(args[0], Integer.parseInt(args[1]),
                            Integer.parseInt(args[2])).toSsiItem() }));
            }
        });
        // WORKS
        cmdMap.put("addgroup", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new CreateItemsCmd(new SsiItem[] {
                    new GroupItem(args[0], Integer.parseInt(args[1]),
                            new int[] { Integer.parseInt(args[2]) })
                        .toSsiItem() }));
            }
        });
        // WORKS
        cmdMap.put("delgroup", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new DeleteItemsCmd(new SsiItem[] {
                    new GroupItem(args[0], Integer.parseInt(args[1]))
                        .toSsiItem() }));
            }
        });
        // WORKS
        cmdMap.put("setprivacyvis", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new ModifyItemsCmd(new SsiItem[] {
                    new PrivacyItem(Integer.parseInt(args[0]),
                            PrivacyItem.MODE_BLOCK_DENIES, 0xffffffffL, 0)
                            .toSsiItem(),
                    new VisibilityItem(Integer.parseInt(args[1]),
                            VisibilityItem.MASK_SHOW_TYPING
                            | VisibilityItem.MASK_SHOW_IDLE_TIME)
                            .toSsiItem()
                }));
            }
        });
        // WORKS
        cmdMap.put("deletevis", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new DeleteItemsCmd(new SsiItem[] {
                    new VisibilityItem(Integer.parseInt(args[0]), 0)
                        .toSsiItem()
                }));
            }
        });
        // WORKS
        cmdMap.put("setprivacy", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new ModifyItemsCmd(new SsiItem[] {
                    new PrivacyItem(Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]), 0xffffffffL, 0)
                        .toSsiItem(),
                }));
            }
        });
        // WORKS
        cmdMap.put("adddeny", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new CreateItemsCmd(new SsiItem[] {
                    new DenyItem(args[0], Integer.parseInt(args[1]))
                        .toSsiItem()
                }));
            }
        });
        // WORKS
        cmdMap.put("deldeny", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new DeleteItemsCmd(new SsiItem[] {
                    new DenyItem(args[0], Integer.parseInt(args[1]))
                        .toSsiItem()
                }));
            }
        });
        // WORKS
        cmdMap.put("addpermit", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new CreateItemsCmd(new SsiItem[] {
                    new PermitItem(args[0], Integer.parseInt(args[1]))
                        .toSsiItem()
                }));
            }
        });
        // WORKS
        cmdMap.put("delpermit", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new DeleteItemsCmd(new SsiItem[] {
                    new PermitItem(args[0], Integer.parseInt(args[1]))
                        .toSsiItem()
                }));
            }
        });
        // WHATEVER
        cmdMap.put("addroot", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new CreateItemsCmd(new SsiItem[] {
                    new RootItem()
                        .toSsiItem()
                }));
            }
        });
        // WORKS
        cmdMap.put("setroot", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new ModifyItemsCmd(new SsiItem[] {
                    new RootItem(new int[] { Integer.parseInt(args[0]),
                    Integer.parseInt(args[1]) })
                        .toSsiItem()
                }));
            }
        });
        cmdMap.put("seticon", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                try {
                    request(new ModifyItemsCmd(new SsiItem[] {
                        new IconItem(args[0], Integer.parseInt(args[1]),
                                new ExtraInfoData(0, ByteBlock.wrap(
                                hashIcon(args[2])
                        )))
                            .toSsiItem()
                    }));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        cmdMap.put("starticon", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new ServiceRequest(0x10));
            }
        });
        cmdMap.put("addicon",  new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                try {
                    request(new CreateItemsCmd(new SsiItem[] {
                        new IconItem(args[0], Integer.parseInt(args[1]),
                                new ExtraInfoData(0, ByteBlock.wrap(
                                        hashIcon(args[2])
                                )))
                            .toSsiItem()
                    }));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        cmdMap.put("delicon", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new DeleteItemsCmd(new SsiItem[] {
                    new IconItem(args[0], Integer.parseInt(args[1]), null)
                        .toSsiItem()
                }));
            }
        });
        cmdMap.put("uploadicon", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new UploadIconCmd(ByteBlock.createByteBlock(
                        new FileWritable(args[0]))));
            }
        });
        cmdMap.put("noicon", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                request(new ModifyItemsCmd(new SsiItem[] {
                    new IconItem(args[0], Integer.parseInt(args[1]),
                            new ExtraInfoData(0, ExtraInfoData.HASH_SPECIAL))
                        .toSsiItem()
                }));
            }
        });
        cmdMap.put("logims", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                bosConn.setLogIms(new ImTestFrame(JoscarTester.this));
            }
        });
        cmdMap.put("sendfile", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                RvSession session = bosConn.rvProcessor.createRvSession(
                        args[0]);

                ServerSocket socket;
                try {
                    socket = new ServerSocket(7050);

                    new SendFileThread(session, socket).start();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                session.addListener(bosConn.rvSessionListener);

                InetAddress localHost = bosConn.getSocket().getLocalAddress();
                int port = socket.getLocalPort();
                session.sendRv(new FileSendReqRvCmd(
                        new InvitationMessage("take this file lol"),
                        RvConnectionInfo.createForOutgoingRequest(localHost,
                                port),
                        new FileSendBlock("wut up.gif", 2000000)));
            }
        });
        cmdMap.put("secureimold", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                RvSession session = bosConn.rvProcessor.createRvSession(
                        args[0]);
                System.out.println("created rv session: "
                        + session.getRvSessionId());

                session.addListener(bosConn.rvSessionListener);

                session.sendRv(new TrillianCryptReqRvCmd(
                        new BigInteger(args[1]),
                        new BigInteger(args[2])));
            }
        });
        cmdMap.put("secureim", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                RvSession session = bosConn.rvProcessor.createRvSession(
                        args[0]);
                System.out.println("created rv session: "
                        + session.getRvSessionId());

                session.addListener(bosConn.rvSessionListener);

                TrillianEncSession encSession = new TrillianEncSession(session);
                encSession.init();
                bosConn.trillianEncSessions.put(OscarTools.normalize(args[0]),
                        encSession);
            }
        });
        cmdMap.put("sim", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                System.out.println("sending secure IM to " + args[0]);
                TrillianEncSession encSession = (TrillianEncSession)
                        bosConn.trillianEncSessions.get(
                                OscarTools.normalize(args[0]));

                encSession.sendMsg(args[1]);

            }
        });
        cmdMap.put("sendbl", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                RvSession session = bosConn.rvProcessor.createRvSession(
                        args[0]);

                session.addListener(bosConn.rvSessionListener);

                session.sendRv(new SendBuddyListRvCmd(new SendBuddyListGroup[] {
                    new SendBuddyListGroup("HEYTOM", new String[] {
                        "HEYTOMBUDDY"
                    }),
                }));
            }
        });
        cmdMap.put("invite", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                RvSession session = bosConn.rvProcessor.createRvSession(
                        args[0]);

                ChatConn conn = (ChatConn) chats.get(args[1]);

                session.addListener(bosConn.rvSessionListener);

                session.sendRv(new ChatInvitationRvCmd(
                        new MiniRoomInfo(conn.getRoomInfo()),
                        new InvitationMessage("wut up d00d")));
            }
        });
        cmdMap.put("directim", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                RvSession session = bosConn.rvProcessor.createRvSession(
                        args[0]);

                session.addListener(bosConn.rvSessionListener);

                try {
                    session.sendRv(new DirectIMReqRvCmd(
                            RvConnectionInfo.createForOutgoingRequest(
                            InetAddress.getLocalHost(),
                            3453)));
                } catch (UnknownHostException e) {

                }
            }
        });
        cmdMap.put("getfile", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                RvSession session = bosConn.rvProcessor.createRvSession(
                        args[0]);

                session.addListener(bosConn.rvSessionListener);

                ServerSocket socket;
                try {
                    socket = new ServerSocket(0);

                    new GetFileThread(session, socket).start();
                } catch (IOException e) {
                    e.printStackTrace();

                    return;
                }

                try {
                    session.sendRv(new GetFileReqRvCmd(
                            RvConnectionInfo.createForOutgoingRequest(
                                    InetAddress.getLocalHost(),
                                    socket.getLocalPort())));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        });
        cmdMap.put("voicechat", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                RvSession session = bosConn.rvProcessor.createRvSession(
                        args[0]);

                session.addListener(bosConn.rvSessionListener);

                try {
                    session.sendRv(new VoiceReqRvCmd(0,
                            RvConnectionInfo.createForOutgoingRequest(
                                    InetAddress.getLocalHost(),
                                    -1)));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        });
        cmdMap.put("encim", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                try {
                    ByteBlock encodedMsg = secureSession.encryptIM(args[0],
                            args[1]);

                    if (encodedMsg == null) {
                        System.out.println("no cert for " + args[0] + "!");
                    }

                    request(new SendImIcbm(args[0], new InstantMessage(
                            encodedMsg), false, 0,
                            false, null, null, true));

                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
        });
        cmdMap.put("encsendfile", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                final String sn = args[0];
                RvSession session = bosConn.rvProcessor.createRvSession(
                        sn);

                ServerSocket socket;
                try {
                    socket = secureSession.createSSLServerSocket(sn);

                    new SendFileThread(session, socket).start();
                } catch (SecureSessionException e) {
                    e.printStackTrace();
                    return;
                }

                session.addListener(bosConn.rvSessionListener);

                InetAddress localHost = bosConn.getSocket().getLocalAddress();
                int port = socket.getLocalPort();
                session.sendRv(new FileSendReqRvCmd(
                        new InvitationMessage("take this file lol"),
                        new RvConnectionInfo(localHost, null, null, port, false, true),
                        new FileSendBlock("wut up.gif", 2000000)));
            }
        });
        cmdMap.put("encjoinroom", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                FullRoomInfo roomInfo = new FullRoomInfo(
                        Integer.parseInt(args[0]), args[1], "us-ascii", "en",
                        "application/pkcs7-mime");
                handleRequest(new SnacRequest(new JoinRoomCmd(roomInfo), null));
            }
        });
        cmdMap.put("encchatsay", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                getChatConn(args[0]).sendEncMsg(args[1]);
            }
        });
        cmdMap.put("fakeinvite", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                RvSession session = bosConn.rvProcessor.createRvSession(
                        args[0]);

                session.addListener(bosConn.rvSessionListener);

                session.sendRv(new ChatInvitationRvCmd(
                        new MiniRoomInfo(4, args[1], 0),
                        new InvitationMessage("wut up d00d")));
            }
        });
        cmdMap.put("encinvite", new CLCommand() {


            public void handle(String line, String cmd, String[] args) {
                RvSession session = bosConn.rvProcessor.createRvSession(
                        args[0]);

                ChatConn conn = (ChatConn) chats.get(args[1]);

                session.addListener(bosConn.rvSessionListener);

                try {
                    ByteBlock secInfo = secureSession.genChatSecurityInfo(
                            conn.getRoomInfo(), args[0]);
                    session.sendRv(new ChatInvitationRvCmd(
                            new MiniRoomInfo(conn.getRoomInfo()),
                            new InvitationMessage("yo"),
                            secInfo));
                } catch (SecureSessionException e) {
                    e.printStackTrace();
                }
            }
        });
        cmdMap.put("enccreateroom", new CLCommand() {
            public void handle(String line, String cmd, String[] args) {
                FullRoomInfo roomInfo = new FullRoomInfo(
                        Integer.parseInt(args[0]), args[1], "us-ascii", "en",
                        "application/pkcs7-mime");
                handleRequest(new SnacRequest(new JoinRoomCmd(roomInfo), null));

            }
        });
    }


    public void processCmd(CmdLineReader reader, String line) {
        StringTokenizer tokenizer = new StringTokenizer(line, " ");
        LinkedList cmds = new LinkedList();
        while (tokenizer.hasMoreTokens()) {
            cmds.add(tokenizer.nextToken());
        }

        if (cmds.isEmpty()) {
            // this is a joke. :(
            System.out.println("*** Empty lines are prohibited.");
            return;
        }

        String cmd = (String) cmds.removeFirst();
        String[] args = (String[]) cmds.toArray(new String[0]);

        CLCommand handler = (CLCommand) cmdMap.get(cmd);

        if (handler == null) {
            System.out.println("!! Unknown command \"" + cmd + "\" !!");
        } else {
            try {
                handler.handle(line, cmd, args);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("connecting to AIM as " + args[0] + "...");

        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.OFF);
        Logger logger = Logger.getLogger("net.kano.joscar");
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);

        JoscarTester tester = new JoscarTester(args[0], args[1]);
        tester.connect();

        new Timer(true).scheduleAtFixedRate(new TimerTask() {
            protected NumberFormat format = NumberFormat.getNumberInstance();

            { // init
                format.setGroupingUsed(true);
                format.setMaximumFractionDigits(1);
            }

            protected String mb(long bytes) {
                if (bytes > 1000000) {
                    return format.format(bytes / 1048576f) + "MB";
                } else {
                    return format.format(bytes / 1024f) + "KB";
                }
            }

            public void run() {
                Runtime runtime = Runtime.getRuntime();
                long total = runtime.totalMemory();
                long free = runtime.freeMemory();
                System.out.println("Using " + mb(total-free) + " memory of "
                        + mb(total) + " allocated");
            }
        },
                30*1000, 5*60*1000);
    }

    public void sendIM(String nick, String text) {
        request(new SendImIcbm(nick, text));
    }

    public SecureSession getSecureSession() { return secureSession; }

    private class MyChatConnListener implements ChatConnListener {
        public void connFailed(ChatConn conn, Object reason) { }

        public void connected(ChatConn conn) { }

        public void joined(ChatConn conn, FullUserInfo[] members) {
            String name = conn.getRoomInfo().getName();
            chats.put(OscarTools.normalize(name), conn);

            System.out.println("*** Joined "
                    + conn.getRoomInfo().getRoomName() + ", members:");
            for (int i = 0; i < members.length; i++) {
                System.out.println("  " + members[i].getScreenname());
            }
        }

        public void left(ChatConn conn, Object reason) {
            String name = conn.getRoomInfo().getName();
            chats.remove(OscarTools.normalize(name));

            System.out.println("*** Left "
                    + conn.getRoomInfo().getRoomName());
        }

        public void usersJoined(ChatConn conn, FullUserInfo[] members) {
            for (int i = 0; i < members.length; i++) {
                System.out.println("*** " + members[i].getScreenname()
                        + " joined " + conn.getRoomInfo().getRoomName());
            }
        }

        public void usersLeft(ChatConn conn, FullUserInfo[] members) {
            for (int i = 0; i < members.length; i++) {
                System.out.println("*** " + members[i].getScreenname()
                        + " left " + conn.getRoomInfo().getRoomName());
            }
        }

        public void gotMsg(ChatConn conn, FullUserInfo sender,
                ChatMsg msg) {
            String msgStr = msg.getMessage();
            String ct = msg.getContentType();
            if (msgStr == null && ct.equals("application/pkcs7-mime")) {
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
}
