/*
 *  Copyright (c) 2003, The Joust Project
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
 *  File created by keith @ Dec 28, 2003
 *
 */

package net.kano.joscardemo;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.FileWritable;
import net.kano.joscar.OscarTools;
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
import net.kano.joscar.snaccmd.DirInfo;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joscar.snaccmd.FullRoomInfo;
import net.kano.joscar.snaccmd.MiniRoomInfo;
import net.kano.joscar.snaccmd.acct.AcctInfoRequest;
import net.kano.joscar.snaccmd.acct.AcctModCmd;
import net.kano.joscar.snaccmd.acct.ConfirmAcctCmd;
import net.kano.joscar.snaccmd.conn.SetExtraInfoCmd;
import net.kano.joscar.snaccmd.icbm.InstantMessage;
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
import net.kano.joscardemo.gui.ImTestFrame;
import net.kano.joscardemo.rv.GetFileThread;
import net.kano.joscardemo.rv.SendFileThread;
import net.kano.joscardemo.rv.TrillianEncSession;
import net.kano.joscardemo.security.SecureSession;
import net.kano.joscardemo.security.SecureSessionException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CLHandler {

    private SortedMap cmdMap = new TreeMap();
    private JoscarTester tester;

    public CLHandler(JoscarTester tester) {
        this.tester = tester;
    }

    public CLCommand getCommand(String cmd) {
        return (CLCommand) cmdMap.get(cmd);
    }

    private static final Pattern wrapRE = Pattern.compile(".{0,79}(?:\\S(?:-| |$)|$)|.{0,80}");

    private static String[] wordWrap(String str) {
        List list = new LinkedList();
        Matcher m = wrapRE.matcher(str);
        while (m.find()) list.add(m.group(0));
        return (String[]) list.toArray(new String[list.size()]);
    }

    {
        cmdMap.put("setexp", new CLCommand() {
            public String getDescription() {
                return "Sets your AIM Expression to the given expression name.";
            }

            public String getExampleArgs() {
                return "the60s";
            }

            public String getArgumentsUsage() {
                return "<AIM expression name>";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.setAimExp(args[0]);
            }
        });
        cmdMap.put("im", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname> <message>";
            }

            public String getDescription() {
                return "Sends an instant message to the given screenname.";
            }

            public String getExampleArgs() {
                return "joustacular \"hey joustacular\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                byte[] textBytes;
                try {
                    textBytes = tester.getAimExp().getBytes("US-ASCII");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return;
                }
                byte[] data = new byte[textBytes.length + 1];
                data[0] = 0x05;
                System.arraycopy(textBytes, 0, data, 1, textBytes.length);
                ByteBlock block = ByteBlock.wrap(data);

                tester.request(new SendImIcbm(args[0], args[1],
                        false, 0, false, tester.getOldIconInfo(),
                        new ExtraInfoBlock[] {
                            new ExtraInfoBlock(ExtraInfoBlock.TYPE_AIMEXPINFO_A,
                                    new ExtraInfoData(0, block)),
                            new ExtraInfoBlock(ExtraInfoBlock.TYPE_AIMEXPINFO_B,
                                    new ExtraInfoData(0, block))
                        }, true));
            }
        });
        cmdMap.put("info", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname>";
            }

            public String getDescription() {
                return "Gets a user's info and away message.";
            }

            public String getExampleArgs() {
                return "joustacular";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
//                request(new OldGetInfoCmd(OldGetInfoCmd.TYPE_AWAYMSG, args[0]));
//                request(new OldGetInfoCmd(OldGetInfoCmd.TYPE_INFO, args[0]));
                tester.request(new GetInfoCmd(GetInfoCmd.FLAG_AWAYMSG | GetInfoCmd.FLAG_INFO, args[0]));
            }
        });
        cmdMap.put("dirinfo", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname>";
            }

            public String getDescription() {
                return "Gets the directory information for the given user.";
            }

            public String getExampleArgs() {
                return "joustacular";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new GetDirInfoCmd(args[0]));
            }
        });
        cmdMap.put("reformat", new CLCommand() {
            public String getArgumentsUsage() {
                return "<your new screenname format>";
            }

            public String getDescription() {
                return "Reformats your screenname.";
            }

            public String getExampleArgs() {
                return "\"JoUsT aC uLaR\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new AcctModCmd(args[0], null));
            }
        });
        cmdMap.put("setemail", new CLCommand() {
            public String getArgumentsUsage() {
                return "<a new registered email address>";
            }

            public String getDescription() {
                return "Sets your registered email address.";
            }

            public String getExampleArgs() {
                return "user@something.com";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new AcctModCmd(null, args[0]));
            }
        });
        cmdMap.put("confirm", new CLCommand() {
            public String getArgumentsUsage() {
                return null;
            }

            public String getDescription() {
                return "Requests that a confirmation email be sent to your "
                        + "registered email address.";
            }

            public String getExampleArgs() {
                return null;
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new ConfirmAcctCmd());
            }
        });
        cmdMap.put("getformat", new CLCommand() {
            public String getArgumentsUsage() {
                return null;
            }

            public String getDescription() {
                return "Requests your current screenname format from the "
                        + "server.";
            }

            public String getExampleArgs() {
                return null;
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new AcctInfoRequest(AcctInfoRequest.TYPE_SN));
            }
        });
        cmdMap.put("getemail", new CLCommand() {
            public String getArgumentsUsage() {
                return null;
            }

            public String getDescription() {
                return "Requests your current registered email address from "
                        + "the server.";
            }

            public String getExampleArgs() {
                return null;
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new AcctInfoRequest(AcctInfoRequest.TYPE_EMAIL));
            }
        });
        cmdMap.put("chatrights", new CLCommand() {
            public String getArgumentsUsage() {
                return null;
            }

            public String getDescription() {
                return "Requests chat exchange rights from the server.";
            }

            public String getExampleArgs() {
                return null;
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new RoomRightsRequest());
            }
        });
        cmdMap.put("joinroom", new CLCommand() {
            public String getArgumentsUsage() {
                return "<chat exchange number> <chat room name>";
            }

            public String getDescription() {
                return "Joins the given chat room on the given exchange. (You "
                        + "should probably use exchange 4.)";
            }

            public String getExampleArgs() {
                return "4 \"Movies\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.joinChat(Integer.parseInt(args[0]), args[1]);
            }
        });
        cmdMap.put("exinfo", new CLCommand() {
            public String getArgumentsUsage() {
                return "<chat exchange number>";
            }

            public String getDescription() {
                return "Requests information about the given chat exchange.";
            }

            public String getExampleArgs() {
                return "4";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new ExchangeInfoReq(Integer.parseInt(args[0])));
            }
        });
        cmdMap.put("chatsay", new CLCommand() {
            public String getArgumentsUsage() {
                return "<chat room name> <message>";
            }

            public String getDescription() {
                return "Sends the given message to the given chat room. (You "
                        + "must have joined this chat room first. See "
                        + "'joinroom'.)";
            }

            public String getExampleArgs() {
                return "\"My Chat Room\" \"hey guys\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.getChatConn(args[0]).sendMsg(args[1]);
            }
        });
        cmdMap.put("inviteafriend", new CLCommand() {
            public String getArgumentsUsage() {
                return "<email address to invite> <invitation message>";
            }

            public String getDescription() {
                return "Requests that an invitation email be sent by AOL to "
                        + "the given email address to join AIM.";
            }

            public String getExampleArgs() {
                return "user@something.com \"Use AIM\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new InviteFriendCmd(args[0], args[1]));
            }
        });
        cmdMap.put("getinterests", new CLCommand() {
            public String getArgumentsUsage() {
                return null;
            }

            public String getDescription() {
                return "Requests a list of chat interests from the server.";
            }

            public String getExampleArgs() {
                return null;
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new InterestListReq());
            }
        });
        cmdMap.put("searchbyname", new CLCommand() {
            public String getArgumentsUsage() {
                return "<name to search for>";
            }

            public String getDescription() {
                return "Searches the buddy directory for a user with the given "
                        + "name.";
            }

            public String getExampleArgs() {
                return "\"Donnie Darko\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(SearchBuddiesCmd.createSearchByDirInfoCmd(new DirInfo(
                        null, null, args[0], null, null, null, null, null, null,
                        null, null, null, null)));
            }
        });
        cmdMap.put("searchbyemail", new CLCommand() {
            public String getArgumentsUsage() {
                return "<email address to search for>";
            }

            public String getDescription() {
                return "Searches the buddy directory for a user with the given "
                        + "email address.";
            }

            public String getExampleArgs() {
                return "user@something.com";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(SearchBuddiesCmd.createSearchByEmailCmd(args[0]));
            }
        });
        cmdMap.put("searchbyinterest", new CLCommand() {
            public String getArgumentsUsage() {
                return "<interest name to search for>";
            }

            public String getDescription() {
                return "Searches for users who like the given chat interest. "
                        + "(See 'getinterests' for a list of valid interests.)";
            }

            public String getExampleArgs() {
                return "Games";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(SearchBuddiesCmd.createSearchByInterestCmd(args[0]));
            }
        });
        cmdMap.put("setdir", new CLCommand() {
            public String getArgumentsUsage() {
                return null;
            }

            public String getDescription() {
                return "Sets your directory information to a predefined set of "
                        + "information (for testing).";
            }

            public String getExampleArgs() {
                return null;
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new SetDirInfoCmd(new DirInfo(
                        "email", "first", "middle", "last", "maiden", "nick",
                        "address", "city", "st", "zip", "CA", "en")));
            }
        });
        cmdMap.put("unsetdir", new CLCommand() {
            public String getArgumentsUsage() {
                return null;
            }

            public String getDescription() {
                return "Removes your directory information, if previously set "
                        + "with 'setdir'.";
            }

            public String getExampleArgs() {
                return null;
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new SetDirInfoCmd(null));
            }
        });
        cmdMap.put("setinterests", new CLCommand() {
            public String getArgumentsUsage() {
                return "[interest name 1] [interest name 2] "
                        + ".. [interest name n]";
            }

            public String getDescription() {
                return "Sets your public chat interests to the given list of "
                        + "interests. (For a list of valid interests, type "
                        + "'getinterests'.)";
            }

            public String getExampleArgs() {
                return "Games Movies Travel";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new SetInterestsCmd(args));
            }
        });
        cmdMap.put("setavail", new CLCommand() {
            public String getArgumentsUsage() {
                return "[iChat availability message]";
            }

            public String getDescription() {
                return "Sets your iChat availability message. iChat and gaim "
                        + "users will see these as tooltips or under your "
                        + "screenname in the buddy list.";
            }

            public String getExampleArgs() {
                return "\"At work\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                String msg = args[0];
                if (msg == null) msg = "";

                byte[] msgData;
                try {
                    msgData = msg.getBytes("UTF8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return;
                }
                byte[] data = new byte[msgData.length + 4];
                System.arraycopy(msgData, 0, data, 2, msgData.length);
                data[0] = (byte) ((msgData.length >> 2) & 0xff);
                data[1] = (byte) (msgData.length & 0xff);

                // this should work
                tester.request(new SetExtraInfoCmd(new ExtraInfoBlock[] {
                    new ExtraInfoBlock(ExtraInfoBlock.TYPE_AVAILMSG,
                            new ExtraInfoData(
                                    ExtraInfoData.FLAG_AVAILMSG_PRESENT,
                                    ByteBlock.wrap(data)))
                }));
            }
        });
        cmdMap.put("getcertinfo", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname>";
            }

            public String getDescription() {
                return "Requests the given user's security certificate "
                        + "information from the server. This command is "
                        + "required before making any encrypted communications "
                        + "with the given user.";
            }

            public String getExampleArgs() {
                return "joustacular";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new GetInfoCmd(GetInfoCmd.FLAG_CERT, args[0]));
            }
        });
        // WORKS
        cmdMap.put("addbuddy", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname> <buddy group ID> <buddy item ID>";
            }

            public String getDescription() {
                return "Adds the given screenname to the given group and with "
                        + "the given buddy ID. The given buddy ID must not be "
                        + "the ID of a buddy already on your list. The group "
                        + "ID must be the ID of a group already on your list.";
            }

            public String getExampleArgs() {
                return "joustacular 12 9238";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new CreateItemsCmd(new SsiItem[] {
                    new BuddyItem(args[0],
                            Integer.parseInt(args[1]),
                            Integer.parseInt(args[2]),
                            "ALIASDUDE",
                            "COMMENTDUDE",
                            BuddyItem.MASK_WHEN_ONLINE,
                            BuddyItem.MASK_ACTION_PLAY_SOUND,
                            "newalert"
                    ).toSsiItem() }));
            }
        });
        // WORKS
        cmdMap.put("delbuddy", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname> <buddy group ID> <buddy item ID>";
            }

            public String getDescription() {
                return "Removes the given buddy with the given buddy ID in "
                        + "the given group from your buddy list";
            }

            public String getExampleArgs() {
                return "joustacular 12 9238";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new DeleteItemsCmd(new SsiItem[] {
                    new BuddyItem(args[0], Integer.parseInt(args[1]),
                            Integer.parseInt(args[2])).toSsiItem() }));
            }
        });
        // WORKS
        cmdMap.put("addgroup", new CLCommand() {
            public String getArgumentsUsage() {
                return "<new group name> <new group ID>";
            }

            public String getDescription() {
                return "Adds the given group to your buddy list with the given "
                        + "group ID. The given group ID must be different than "
                        + "all other group ID's on your list.";
            }

            public String getExampleArgs() {
                return "Buddies 12";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new CreateItemsCmd(new SsiItem[] {
                    new GroupItem(args[0], Integer.parseInt(args[1]))
                        .toSsiItem() }));
            }
        });
        // WORKS
        cmdMap.put("delgroup", new CLCommand() {
            public String getArgumentsUsage() {
                return "<group name> <group ID>";
            }

            public String getDescription() {
                return "Removes the given group with the given group ID.";
            }

            public String getExampleArgs() {
                return "Buddies 12";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new DeleteItemsCmd(new SsiItem[] {
                    new GroupItem(args[0], Integer.parseInt(args[1]))
                        .toSsiItem() }));
            }
        });
        cmdMap.put("setprivacyvis", new CLCommand() {
            public String getArgumentsUsage() {
                return "<privacy item ID> <visibility item ID>";
            }

            public String getDescription() {
                return "Sets your privacy and visibility SSI items to a "
                        + "predefined set of values (for testing). The given "
                        + "privacy and visiblity ID's must be the ID's of those"
                        + "items on your list.";
            }

            public String getExampleArgs() {
                return "19 13";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new ModifyItemsCmd(new SsiItem[] {
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
        cmdMap.put("deletevis", new CLCommand() {
            public String getArgumentsUsage() {
                return "<visiblity item ID>";
            }

            public String getDescription() {
                return "Deletes your visibility SSI item.";
            }

            public String getExampleArgs() {
                return "13";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new DeleteItemsCmd(new SsiItem[] {
                    new VisibilityItem(Integer.parseInt(args[0]), 0)
                        .toSsiItem()
                }));
            }
        });
        cmdMap.put("setprivacymode", new CLCommand() {
            public String getArgumentsUsage() {
                return "<privacy item ID> <privacy mode number 1-5>";
            }

            public String getDescription() {
                return "Sets your privacy mode: 1=allow all, 2=block all, "
                        + "3=only allow permitted, 4=allow all but blocked, "
                        + "5=allow all but on buddy list";
            }

            public String getExampleArgs() {
                return "19 3";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new ModifyItemsCmd(new SsiItem[] {
                    new PrivacyItem(Integer.parseInt(args[0]),
                            Integer.parseInt(args[1]), 0xffffffffL, 0)
                        .toSsiItem(),
                }));
            }
        });
        cmdMap.put("blockuser", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname to block> <new block item ID>";
            }

            public String getDescription() {
                return "Blocks the given user, adding a block item with the "
                        + "given ID. The given block item ID must not be the "
                        + "ID of any other block item.";
            }

            public String getExampleArgs() {
                return "joustacular 132";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new CreateItemsCmd(new SsiItem[] {
                    new DenyItem(args[0], Integer.parseInt(args[1]))
                        .toSsiItem()
                }));
            }
        });
        cmdMap.put("unblockuser", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname to unblock> <block item ID>";
            }

            public String getDescription() {
                return "Removes the given block item from your SSI, unblocking "
                        + "the user. The given block item ID must be the ID "
                        + "of that user's block item.";
            }

            public String getExampleArgs() {
                return "joustacular 132";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new DeleteItemsCmd(new SsiItem[] {
                    new DenyItem(args[0], Integer.parseInt(args[1]))
                        .toSsiItem()
                }));
            }
        });
        cmdMap.put("addpermit", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname to allow> <new allow item ID>";
            }

            public String getDescription() {
                return "Adds the given user to your permit list.";
            }

            public String getExampleArgs() {
                return "joustacular 204";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new CreateItemsCmd(new SsiItem[] {
                    new PermitItem(args[0], Integer.parseInt(args[1]))
                        .toSsiItem()
                }));
            }
        });
        cmdMap.put("delpermit", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname to remove from allow list> <allow item ID>";
            }

            public String getDescription() {
                return "Removes the given user from your permit list. The "
                        + "given allow item ID must be the ID of that user's "
                        + "allow item.";
            }

            public String getExampleArgs() {
                return "joustacular 204";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new DeleteItemsCmd(new SsiItem[] {
                    new PermitItem(args[0], Integer.parseInt(args[1]))
                        .toSsiItem()
                }));
            }
        });
        cmdMap.put("addrootitem", new CLCommand() {
            public String getArgumentsUsage() {
                return null;
            }

            public String getDescription() {
                return "Adds the root group SSI item, also known as the group "
                        + "group. This group is a group that contains no "
                        + "buddies, only other groups.";
            }

            public String getExampleArgs() {
                return null;
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new CreateItemsCmd(new SsiItem[] {
                    new RootItem().toSsiItem()
                }));
            }
        });
        cmdMap.put("setroot", new CLCommand() {
            public String getArgumentsUsage() {
                return "<group ID 1> <group ID 2>";
            }

            public String getDescription() {
                return "Sets the groups contained in the root group item to "
                        + "the given two groups (for testing).";
            }

            public String getExampleArgs() {
                return "12 9";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new ModifyItemsCmd(new SsiItem[] {
                    new RootItem(new int[] { Integer.parseInt(args[0]),
                                             Integer.parseInt(args[1]) })
                        .toSsiItem()
                }));
            }
        });
        cmdMap.put("seticon", new CLCommand() {
            public String getArgumentsUsage() {
                return "<icon number> <icon item ID> "
                        + "<local icon filename>";
            }

            public String getDescription() {
                return "Sets your buddy icon (via SSI) to the given file. The "
                        + "file must exist and contain no spaces, and should "
                        + "be a GIF image.";
            }

            public String getExampleArgs() {
                return "1 309 \"/home/keith/in/icon.gif\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                try {
                    ExtraInfoData extrainfo = new ExtraInfoData(0,
                            ByteBlock.wrap(JoscarTester.hashIcon(args[2])));
                    IconItem iconItem = new IconItem(args[0],
                            Integer.parseInt(args[1]), extrainfo);
                    tester.request(new ModifyItemsCmd(new SsiItem[] {
                        iconItem.toSsiItem()
                    }));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        cmdMap.put("addicon",  new CLCommand() {
            public String getArgumentsUsage() {
                return "<icon number> <icon item ID> "
                        + "<local icon filename>";
            }

            public String getDescription() {
                return "Adds a new SSI icon item and sets it as your buddy "
                        + "icon. The given filename must exist and must "
                        + "contain no spaces.";
            }

            public String getExampleArgs() {
                return "2 905 \"/home/keith/in/icon.gif\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                try {
                    tester.request(new CreateItemsCmd(new SsiItem[] {
                        new IconItem(args[0], Integer.parseInt(args[1]),
                                new ExtraInfoData(0, ByteBlock.wrap(
                                        JoscarTester.hashIcon(args[2])
                                )))
                            .toSsiItem()
                    }));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        cmdMap.put("delicon", new CLCommand() {
            public String getArgumentsUsage() {
                return "<icon number> <icon item ID>";
            }

            public String getDescription() {
                return "Deletes the given buddy icon SSI item.";
            }

            public String getExampleArgs() {
                return "2 905";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new DeleteItemsCmd(new SsiItem[] {
                    new IconItem(args[0], Integer.parseInt(args[1]), null)
                        .toSsiItem()
                }));
            }
        });
        cmdMap.put("uploadicon", new CLCommand() {
            public String getArgumentsUsage() {
                return "<local icon filename>";
            }

            public String getDescription() {
                return "Uploads the given buddy icon file to the buddy icon "
                        + "server.";
            }

            public String getExampleArgs() {
                return "\"/home/keith/in/icon.gif\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new UploadIconCmd(ByteBlock.createByteBlock(
                        new FileWritable(args[0]))));
            }
        });
        cmdMap.put("noicon", new CLCommand() {
            public String getArgumentsUsage() {
                return "<icon number> <icon item ID>";
            }

            public String getDescription() {
                return "Sets the given buddy icon SSI item to indicate that "
                        + "you have no buddy icon.";
            }

            public String getExampleArgs() {
                return "2 905";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.request(new ModifyItemsCmd(new SsiItem[] {
                    new IconItem(args[0], Integer.parseInt(args[1]),
                            new ExtraInfoData(0, ExtraInfoData.HASH_SPECIAL))
                        .toSsiItem()
                }));
            }
        });
        cmdMap.put("logims", new CLCommand() {
            public String getArgumentsUsage() {
                return null;
            }

            public String getDescription() {
                return "Opens up a terrible GUI window showing incoming IM's, "
                        + "and logs IM's to <user's home>/ims.log";
            }

            public String getExampleArgs() {
                return null;
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.getBosConn().setLogIms(new ImTestFrame(tester));
            }
        });
        cmdMap.put("sendfile", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname>";
            }

            public String getDescription() {
                return "Sends a fake file to the given user. The file is "
                        + "called hey.gif and is 2000000 bytes long. It "
                        + "contains only null bytes.";
            }

            public String getExampleArgs() {
                return "joustacular";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                RvSession session = tester.getBosConn().rvProcessor.createRvSession(
                        args[0]);

                ServerSocket socket;
                try {
                    socket = new ServerSocket(7050);

                    new SendFileThread(session, socket).start();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                session.addListener(tester.getBosConn().rvSessionListener);

                InetAddress localHost = tester.getBosConn().getSocket().getLocalAddress();
                int port = socket.getLocalPort();
                session.sendRv(new FileSendReqRvCmd(
                        new InvitationMessage("take this file"),
                        RvConnectionInfo.createForOutgoingRequest(localHost,
                                port),
                        new FileSendBlock("hey.gif", 2000000)));
            }
        });
        cmdMap.put("secureimold", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname> <modulus> <public value>";
            }

            public String getDescription() {
                return "Sends a Trillian SecureIM invitation to the given "
                        + "screenname with the given Diffie-Hellman modulus "
                        + "and public value. Multiples of 5 for the modulus "
                        + "should crash Trillian users (provided that they "
                        + "have SecureIM enabled). This command does not "
                        + "initiate an actual SecureIM session; for that, see "
                        + "the 'secureim' command.";
            }

            public String getExampleArgs() {
                return "joustacular 25 125";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                RvSession session = tester.getBosConn().rvProcessor.createRvSession(
                        args[0]);
                System.out.println("created rv session: "
                        + session.getRvSessionId());

                session.addListener(tester.getBosConn().rvSessionListener);

                session.sendRv(new TrillianCryptReqRvCmd(
                        new BigInteger(args[1]),
                        new BigInteger(args[2])));
            }
        });
        cmdMap.put("secureim", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname>";
            }

            public String getDescription() {
                return "Initiates a Trillian SecureIM session with the given "
                        + "user, provided he or she is using Trillian and has "
                        + "SecureIM enabled. To send secure IM's through this "
                        + "session, see the 'sendsim' command.";
            }

            public String getExampleArgs() {
                return "joustacular";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                RvSession session = tester.getBosConn().rvProcessor.createRvSession(
                        args[0]);
                System.out.println("created rv session: "
                        + session.getRvSessionId());

                session.addListener(tester.getBosConn().rvSessionListener);

                TrillianEncSession encSession = new TrillianEncSession(session);
                encSession.init();
                tester.getBosConn().trillianEncSessions.put(OscarTools.normalize(args[0]),
                        encSession);
            }
        });
        cmdMap.put("sendsim", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname with secureim session open> <message>";
            }

            public String getDescription() {
                return "Sends an enrypted message over the Trillian SecureIM "
                        + "session open with the given user. A SecureIM "
                        + "session must have been opened with the 'secureim' "
                        + "command before sending messages.";
            }

            public String getExampleArgs() {
                return "joustacular \"Hey what's up\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                System.out.println("sending secure IM to " + args[0]);
                TrillianEncSession encSession = (TrillianEncSession)
                        tester.getBosConn().trillianEncSessions.get(
                                OscarTools.normalize(args[0]));

                encSession.sendMsg(args[1]);

            }
        });
        cmdMap.put("sendbl", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname>";
            }

            public String getDescription() {
                return "Sends a fake buddy list via Send Buddy List to the "
                        + "given user (for testing).";
            }

            public String getExampleArgs() {
                return "joustacular";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                RvSession session = tester.getBosConn().rvProcessor.createRvSession(
                        args[0]);

                session.addListener(tester.getBosConn().rvSessionListener);

                session.sendRv(new SendBuddyListRvCmd(new SendBuddyListGroup[] {
                    new SendBuddyListGroup("HEY", new String[] {
                        "HEYBUDDY", "YOBUDDY",
                    }),
                    new SendBuddyListGroup("YO", new String[] {
                        "HEYBUDDY2", "YOBUDDY2",
                    }),
                }));
            }
        });
        cmdMap.put("invite", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname to invite> <chat room name>";
            }

            public String getDescription() {
                return "Invites the given user to the given chat room. You "
                        + "must have previously joined the given chat room, "
                        + "though you do not need to currently be in it.";
            }

            public String getExampleArgs() {
                return "joustacular \"Movies\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                RvSession session = tester.getBosConn().rvProcessor.createRvSession(
                        args[0]);

                ChatConn conn = tester.getChatConn(args[1]);

                session.addListener(tester.getBosConn().rvSessionListener);

                session.sendRv(new ChatInvitationRvCmd(
                        new MiniRoomInfo(conn.getRoomInfo()),
                        new InvitationMessage("hey")));
            }
        });
        cmdMap.put("directim", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname>";
            }

            public String getDescription() {
                return "Attempts to open a Direct IM session on port 3453 with "
                        + "the given user.";
            }

            public String getExampleArgs() {
                return "joustacular";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                RvSession session = tester.getBosConn().rvProcessor.createRvSession(
                        args[0]);

                session.addListener(tester.getBosConn().rvSessionListener);

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
            public String getArgumentsUsage() {
                return "<screenname>";
            }

            public String getDescription() {
                return "Requests a Get File session with the given user, to "
                        + "browse his or her shared files.";
            }

            public String getExampleArgs() {
                return "joustacular";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                RvSession session = tester.getBosConn().rvProcessor.createRvSession(
                        args[0]);

                session.addListener(tester.getBosConn().rvSessionListener);

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
            public String getArgumentsUsage() {
                return "<screenname>";
            }

            public String getDescription() {
                return "Requests that the given user join a voice chat with "
                        + "you. There is no support for the actual voice chat "
                        + "in joscar, as it is not OSCAR-based.";
            }

            public String getExampleArgs() {
                return "joustacular";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                RvSession session = tester.getBosConn().rvProcessor.createRvSession(
                        args[0]);

                session.addListener(tester.getBosConn().rvSessionListener);

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
            public String getArgumentsUsage() {
                return "<screenname> <message>";
            }

            public String getDescription() {
                return "Sends an encrypted message to the given user. This "
                        + "will only work if you have set a certificate "
                        + "yourself (see the file USING in the joscar "
                        + "release) and if the given user has a certificate "
                        + "set, AND if his or her certificate has been "
                        + "retrieved with the 'getcertinfo' command.";
            }

            public String getExampleArgs() {
                return "joustacular \"hey\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                try {
                    SecureSession ss = tester.getSecureSession();
                    ByteBlock encodedMsg = ss.encryptIM(args[0], args[1]);

                    if (encodedMsg == null) {
                        System.err.println("no cert for " + args[0] + "!");
                        return;
                    }

                    tester.request(new SendImIcbm(args[0], new InstantMessage(
                            encodedMsg), false, 0,
                            false, null, null, true));

                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
        });
        cmdMap.put("encsendfile", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname>";
            }

            public String getDescription() {
                return "Sends a fake file encryptedly (via SSL) to the given "
                        + "user. This will only work if you have set a "
                        + "certificate yourself (see the file USING in the "
                        + "joscar release) and if the given user has a "
                        + "certificate set, AND if his or her certificate has "
                        + "been retrieved with the 'getcertinfo' command.";
            }

            public String getExampleArgs() {
                return "joustacular";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                final String sn = args[0];
                RvSession session = tester.getBosConn().rvProcessor.createRvSession(
                        sn);

                ServerSocket socket;
                try {
                    socket = tester.getSecureSession().createSSLServerSocket(sn);

                    new SendFileThread(session, socket).start();
                } catch (SecureSessionException e) {
                    e.printStackTrace();
                    return;
                }

                session.addListener(tester.getBosConn().rvSessionListener);

                InetAddress localHost = tester.getBosConn().getSocket().getLocalAddress();
                int port = socket.getLocalPort();
                session.sendRv(new FileSendReqRvCmd(
                        new InvitationMessage("take this file"),
                        new RvConnectionInfo(localHost, null, null, port, false, true),
                        new FileSendBlock("yo.gif", 2000000)));
            }
        });
        cmdMap.put("encjoinroom", new CLCommand() {
            public String getArgumentsUsage() {
                return "<exchange number> <room name>";
            }

            public String getDescription() {
                return "Joins the given encrypted chat room on the given"
                        + "exchange. To decode anyone anybody is saying in the "
                        + "room, you must obtain the room key by being "
                        + "invited to the room or by creating it with the "
                        + "'enccreateroom' command.";
            }

            public String getExampleArgs() {
                return "4 \"Encryption Party\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                FullRoomInfo roomInfo = new FullRoomInfo(
                        Integer.parseInt(args[0]), args[1], "us-ascii", "en",
                        "application/pkcs7-mime");
                tester.request(new JoinRoomCmd(roomInfo));
            }
        });
        cmdMap.put("encchatsay", new CLCommand() {
            public String getArgumentsUsage() {
                return "<chat room name> <message>";
            }

            public String getDescription() {
                return "Sends an encrypted message to the given chat room. "
                        + "You must have obtained the chat room key by being "
                        + "invited to the room or by creating it with the "
                        + "'enccreateroom' command.";
            }

            public String getExampleArgs() {
                return "\"Encryption Party\" \"hey guys\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                tester.getChatConn(args[0]).sendEncMsg(args[1]);
            }
        });
        cmdMap.put("fakeinvite", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname> <room name>";
            }

            public String getDescription() {
                return "Invites the given user to the given chat room, even if "
                        + "you aren't in that room.";
            }

            public String getExampleArgs() {
                return "joustacular SomeChatRoom";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                RvSession session = tester.getBosConn().rvProcessor.createRvSession(
                        args[0]);

                session.addListener(tester.getBosConn().rvSessionListener);

                session.sendRv(new ChatInvitationRvCmd(
                        new MiniRoomInfo(4, args[1], 0),
                        new InvitationMessage("yo")));
            }
        });
        cmdMap.put("encinvite", new CLCommand() {
            public String getArgumentsUsage() {
                return "<screenname> <chat room name>";
            }

            public String getDescription() {
                return "Invites the given user to the given chat room. You "
                        + "must have obtained the chat key for the given room "
                        + "by being invited to the room or creating it with "
                        + "the 'enccreateroom' command.";
            }

            public String getExampleArgs() {
                return "joustacular EncryptionParty";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                RvSession session = tester.getBosConn().rvProcessor.createRvSession(
                        args[0]);

                ChatConn conn = tester.getChatConn(args[1]);

                session.addListener(tester.getBosConn().rvSessionListener);

                try {
                    ByteBlock secInfo = tester.getSecureSession().genChatSecurityInfo(
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
            public String getArgumentsUsage() {
                return "<exchange number> <chat room name>";
            }

            public String getDescription() {
                return "Creates a Secure Chat Room and generates its chat key.";
            }

            public String getExampleArgs() {
                return "4 \"Encryption Party\"";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                FullRoomInfo roomInfo = new FullRoomInfo(
                        Integer.parseInt(args[0]), args[1], "us-ascii", "en",
                        "application/pkcs7-mime");
                tester.request(new JoinRoomCmd(roomInfo));
            }
        });
        cmdMap.put("help", new CLCommand() {
            public String getArgumentsUsage() {
                return "[command name]";
            }

            public String getDescription() {
                return "Displays command-line help.";
            }

            public String getExampleArgs() {
                return "im";
            }

            public void handle(JoscarTester tester, String line, String cmd, String[] args) {
                if (args.length == 0) {
                    printMainHelp();
                } else if (args.length == 1) {
                    printUsage(args[0]);
                } else {
                    printUsage("help");
                }
            }

            private void printMainHelp() {
                ArrayList cmdNames = new ArrayList(cmdMap.keySet());
                Collections.sort(cmdNames);
                System.out.println("Commands (" + cmdNames.size() + "): ");
                int longest = 0;
                for (Iterator it = cmdNames.iterator(); it.hasNext();) {
                    String name = (String) it.next();
                    longest = Math.max(longest, name.length());
                }
                int perline = 80/(longest+1);
                int i = 0;
                for (Iterator it = cmdNames.iterator(); it.hasNext();) {
                    String name = (String) it.next();
                    boolean eol = (i % perline) == (perline-1) || !it.hasNext();

                    System.out.print(name);
                    if (!eol) {
                        int spaces = (longest - name.length()) + 1;
                        for (int j = 0; j < spaces; j++) {
                            System.out.print(' ');
                        }
                        i++;
                    } else {
                        System.out.println();
                        i = 0;
                    }
                }
                System.out.println("   * For help on a specific command, type 'help xxx',");
                System.out.println("     where xxx is the command's name");
            }

            private void printUsage(String command) {
                CLCommand clc = (CLCommand) cmdMap.get(command);
                if (clc == null) {
                    System.err.println("There is no command called '"
                            + command + "' - try typing 'help'");
                    return;
                }

                String args = clc.getArgumentsUsage();
                System.out.println("Usage: " + command
                        + (args == null ? "" : " " + args));

                if (args == null) {
                    System.out.println("   * (This command takes no arguments)");

                } else {
                    if (args.indexOf("<") != -1) {
                        System.out.println("   * Arguments in <angle brackets> are required");
                    }
                    if (args.indexOf("[") != -1) {
                        System.out.println("   * Arguments in [square brackets] are optional");
                    }
//                    System.out.println("   * All arguments must be single words with no spaces");
                }
                String desc = clc.getDescription();
                if (desc != null) {
                    String[] wrapped = wordWrap(desc);
                    for (int i = 0; i < wrapped.length; i++) {
                        String line = wrapped[i];
                        System.out.print("  ");
                        System.out.println(line);
                    }
                }
                String exargs = clc.getExampleArgs();
                if (exargs != null) {
                    System.out.println("Example: " + command + " " + exargs);
                }
            }
        });
    }

}
