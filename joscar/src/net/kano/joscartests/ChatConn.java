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
 *  File created by keith @ Mar 26, 2003
 *
 */

package net.kano.joscartests;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.flap.ClientFlapConn;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.net.ClientConnEvent;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snaccmd.FullRoomInfo;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.chat.ChatMsg;
import net.kano.joscar.snaccmd.chat.RecvChatMsgIcbm;
import net.kano.joscar.snaccmd.chat.SendChatMsgIcbm;
import net.kano.joscar.snaccmd.chat.UsersJoinedCmd;
import net.kano.joscar.snaccmd.chat.UsersLeftCmd;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.BERConstructedOctetString;
import org.bouncycastle.asn1.BERSequence;
import org.bouncycastle.asn1.BERTaggedObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.cms.EncryptedContentInfo;
import org.bouncycastle.asn1.cms.EncryptedData;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.CMSException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class ChatConn extends ServiceConn {
    protected FullRoomInfo roomInfo;

    protected List listeners = new ArrayList();

    protected boolean joined = false;

    protected Set members = new HashSet();
    private SecretKey secretKey;
    private Random random = new Random();

    public ChatConn(JoscarTester tester, ByteBlock cookie,
            FullRoomInfo roomInfo) {
        super(tester, cookie, 0x000e);
        this.roomInfo = roomInfo;
    }

    public ChatConn(String host, int port, JoscarTester tester,
            ByteBlock cookie, FullRoomInfo roomInfo) {
        super(host, port, tester, cookie, 0x000e);
        this.roomInfo = roomInfo;
    }

    public ChatConn(InetAddress ip, int port, JoscarTester tester,
            ByteBlock cookie, FullRoomInfo roomInfo) {
        super(ip, port, tester, cookie, 0x000e);
        this.roomInfo = roomInfo;
    }

    public void sendMsg(String msg) {
        request(new SendChatMsgIcbm(new ChatMsg(msg)));
    }

    public FullRoomInfo getRoomInfo() { return roomInfo; }

    public String getRoomName() {
        return roomInfo.getRoomName();
    }

    public FullUserInfo[] getMembers() {
        return (FullUserInfo[]) members.toArray(new FullUserInfo[0]);
    }

    protected void handleStateChange(ClientConnEvent e) {
        super.handleStateChange(e);

        Object state = e.getNewState();

        if (state == ClientFlapConn.STATE_CONNECTED) {
            fireConnectedEvent();

        } else if (state == ClientFlapConn.STATE_FAILED) {
            fireConnFailedEvent(e.getReason());

        } else if (state == ClientFlapConn.STATE_NOT_CONNECTED) {
            if (joined) fireLeftEvent(e.getReason());
            else fireConnFailedEvent(e.getReason());
        }
    }

    protected void handleSnacPacket(SnacPacketEvent e) {
        super.handleSnacPacket(e);
        
        SnacCommand cmd = e.getSnacCommand();

        if (cmd instanceof UsersJoinedCmd) {
            UsersJoinedCmd ujc = (UsersJoinedCmd) cmd;

            members.addAll(Arrays.asList(ujc.getUsers()));

            if (!joined) {
                fireJoinedEvent(ujc.getUsers());
                joined = true;
            } else {
                fireUsersJoinedEvent(ujc.getUsers());
            }
        } else if (cmd instanceof UsersLeftCmd) {
            UsersLeftCmd ulc = (UsersLeftCmd) cmd;

            members.removeAll(Arrays.asList(ulc.getUsers()));

            fireUsersLeftEvent(ulc.getUsers());
        } else if (cmd instanceof RecvChatMsgIcbm) {
            RecvChatMsgIcbm icbm = (RecvChatMsgIcbm) cmd;

            fireMsgEvent(icbm.getSenderInfo(), icbm.getMessage());
        }
    }

    public void addChatListener(ChatConnListener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeChatListener(ChatConnListener l) {
        listeners.remove(l);
    }

    protected void fireConnectedEvent() {
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            ChatConnListener l = (ChatConnListener) it.next();

            l.connected(this);
        }
    }

    protected void fireConnFailedEvent(Object reason) {
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            ChatConnListener l = (ChatConnListener) it.next();

            l.connFailed(this, reason);
        }
    }
    protected void fireJoinedEvent(FullUserInfo[] members) {
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            ChatConnListener l = (ChatConnListener) it.next();

            l.joined(this, members);
        }
    }
    protected void fireLeftEvent(Object reason) {
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            ChatConnListener l = (ChatConnListener) it.next();

            l.left(this, reason);
        }
    }
    protected void fireUsersJoinedEvent(FullUserInfo[] members) {
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            ChatConnListener l = (ChatConnListener) it.next();

            l.usersJoined(this, members);
        }
    }
    protected void fireUsersLeftEvent(FullUserInfo[] members) {
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            ChatConnListener l = (ChatConnListener) it.next();

            l.usersLeft(this, members);
        }
    }
    protected void fireMsgEvent(FullUserInfo sender, ChatMsg msg) {
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            ChatConnListener l = (ChatConnListener) it.next();

            l.gotMsg(this, sender, msg);
        }
    }

    public String toString() {
        return "ChatConn: " + roomInfo.getRoomName();
    }

    SecretKey getKey() {
        return secretKey;
    }

    void setKey(SecretKey key) {
        this.secretKey = key;
    }


    /*
        ASN1Sequence seq = (ASN1Sequence) ain.readObject();
        BERTaggedObject bert = (BERTaggedObject) seq.getObjectAt(1);
        ASN1Sequence seq2 = (ASN1Sequence) bert.getObject();
        EncryptedData encd = new EncryptedData(seq2);
        EncryptedContentInfo enci = encd.getEncryptedContentInfo();
        byte[] encryptedData = enci.getEncryptedContent().getOctets();

        AlgorithmIdentifier alg = enci.getContentEncryptionAlgorithm();

        byte[] iv = ((ASN1OctetString) alg.getParameters()).getOctets();

        Cipher c = Cipher.getInstance(alg.getObjectId().getId(), "BC");
        c.init(Cipher.DECRYPT_MODE, conn.getKey(), new IvParameterSpec(iv));

        ByteBlock result = ByteBlock.wrap(c.doFinal(encryptedData));

        OscarTools.HttpHeaderInfo hinfo = OscarTools.parseHttpHeader(result);
        InputStream csdin = ByteBlock.createInputStream(hinfo.getData());
        CMSSignedData csd = new CMSSignedData(csdin);
        byte[] scBytes = (byte[]) csd.getSignedContent().getContent();
        ByteBlock signedContent = ByteBlock.wrap(scBytes);
        OscarTools.HttpHeaderInfo hinfo2
                = OscarTools.parseHttpHeader(signedContent);
        return OscarTools.getInfoString(hinfo2.getData(),
        (String) hinfo2.getHeaders().get("content-type"));
    */

    void sendEncMsg(String msg) {
        byte[] encrypted;
        try {
            encrypted = encryptMsg(msg);

            FileOutputStream fout = new FileOutputStream("tmpmyencryptedmsg");
            fout.write(encrypted);
            fout.close();

            request(new SendChatMsgIcbm(
                    new ChatMsg("application/pkcs7-mime", "binary", "us-ascii",
                            ByteBlock.wrap(encrypted), Locale.getDefault())));
            System.out.println("sent encrypted msg..");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (CMSException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
    }

    private byte[] encryptMsg(String msg) throws IOException,
            NoSuchAlgorithmException, NoSuchProviderException, CMSException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(bout);
        osw.write("Content-Transfer-Encoding: binary\r\n"
                + "Content-Type: text/x-aolrtf; charset=us-ascii\r\n"
                + "Content-Language: en\r\n"
                + "\r\n");
        osw.flush();
        bout.write(msg.getBytes());

        byte[] dataToSign = bout.toByteArray();
        byte[] signedData = tester.signData(dataToSign);

        bout = new ByteArrayOutputStream();
        osw = new OutputStreamWriter(bout);
        osw.write("Content-Transfer-Encoding: binary\r\n"
                + "Content-Type: application/pkcs7-mime; charset=us-ascii\r\n"
                + "Content-Language: en\r\n"
                + "\r\n");
        osw.flush();
        bout.write(signedData);

        byte[] iv = new byte[16];
        random.nextBytes(iv);

        Cipher c = Cipher.getInstance("2.16.840.1.101.3.4.1.42", "BC");
        c.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

        byte[] encrypted = c.doFinal(bout.toByteArray());
        EncryptedContentInfo eci = new EncryptedContentInfo(
                new DERObjectIdentifier("1.2.840.113549.1.7.1"),
                new AlgorithmIdentifier(
                        new DERObjectIdentifier("2.16.840.1.101.3.4.1.42"),
                        new DEROctetString(iv)),
                        new BERConstructedOctetString(encrypted));
        EncryptedData ed = new EncryptedData(eci, null);
        BERTaggedObject bert = new BERTaggedObject(0, ed.getDERObject());
        DERObjectIdentifier rootid = new DERObjectIdentifier("1.2.840.113549.1.7.6");
        ASN1EncodableVector vec = new ASN1EncodableVector();
        vec.add(rootid);
        vec.add(bert);
        ByteArrayOutputStream fout = new ByteArrayOutputStream();
        ASN1OutputStream out = new ASN1OutputStream(fout);
        out.writeObject(new BERSequence(vec));
        out.close();
        return fout.toByteArray();
    }
}
