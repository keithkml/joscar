/*
 *  Copyright (c) 2002, The Joust Project
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
 *  File created by keith @ Apr 27, 2003
 *
 */

package net.kano.joscartests;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rv.RvSessionListener;
import net.kano.joscar.rv.RvSnacResponseEvent;
import net.kano.joscar.rvcmd.trillcrypt.*;
import net.kano.joscar.snaccmd.icbm.RvCommand;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class TrillianEncSession implements RvSessionListener {
    private static final BigInteger FIVE = new BigInteger("5");

    private Cipher encoder;
    private Cipher decoder;

    private BigInteger modulus;
    private BigInteger myPrivate;
    private BigInteger myPublic;
    private BigInteger otherPublic;
    private BigInteger sessionKey;

    private final RvSession rvSession;

    public TrillianEncSession(RvSession session) {
        this.rvSession = session;
    }

    public void init() {
        Random random = new Random();
        modulus = new BigInteger(128, random);
        myPrivate = new BigInteger(128, random).mod(modulus);
        myPublic = FIVE.modPow(myPrivate, modulus);
        rvSession.sendRv(new TrillianCryptReqRvCmd(modulus, myPublic));
    }

    public RvSession getRvSession() { return rvSession; }

    public BigInteger getModulus() { return modulus; }

    public BigInteger getMyPrivate() { return myPrivate; }

    public BigInteger getMyPublic() { return myPublic; }

    public BigInteger getOtherPublic() { return otherPublic; }

    public BigInteger getSessionKey() { return sessionKey; }

    public void handleRv(RecvRvEvent event) {
        RvCommand rvc = event.getRvCommand();

        System.out.println("encsession handling event!");

        if (rvc instanceof TrillianCryptReqRvCmd) {
            System.out.println("got request for secureim from "
                    + rvSession.getScreenname());

            rvSession.addListener(this);

            TrillianCryptReqRvCmd cmd = (TrillianCryptReqRvCmd) rvc;

            try {
                modulus = cmd.getP();
                otherPublic = cmd.getY();

                myPrivate = new BigInteger(128, new Random()).mod(modulus);
                myPublic = FIVE.modPow(myPrivate, modulus);
                initCiphers();

                rvSession.sendRv(new TrillianCryptAcceptRvCmd(myPublic));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }

        } else if (rvc instanceof TrillianCryptAcceptRvCmd) {
            otherPublic = ((TrillianCryptAcceptRvCmd) rvc).getY();
            try {
                initCiphers();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
            rvSession.sendRv(new TrillianCryptBeginRvCmd());

        } else if (rvc instanceof TrillianCryptBeginRvCmd) {
            System.out.println("encrypted session with "
                    + rvSession.getScreenname() + " begun!");


        } else if (rvc instanceof TrillianCryptMsgRvCmd) {
            TrillianCryptMsgRvCmd cmd = (TrillianCryptMsgRvCmd) rvc;

            byte[] encrypted = cmd.getEncryptedMsg().toByteArray();
            System.out.println("len=" + encrypted.length);

            byte[] decoded = decoder.update(encrypted);
            System.out.println("decoded len=" + decoded.length);
            System.out.println("message: " + BinaryTools.getAsciiString(
                    ByteBlock.wrap(decoded)));

        } else if (rvc instanceof TrillianCryptCloseRvCmd) {
            System.out.println("encryption session with "
                    + rvSession.getScreenname() + " closed!");
        }
    }

    private void initCiphers() throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException {
        sessionKey = otherPublic.modPow(myPrivate, modulus);

        byte[] fbytes = sessionKey.toByteArray();
        if (fbytes.length == 17) {
            System.out.println("trimming off byte: " + fbytes[0]);
            byte[] old = fbytes;
            fbytes = new byte[fbytes.length - 1];
            System.arraycopy(old, 1, fbytes, 0, fbytes.length);
        }
        System.out.println("bit length: " + (fbytes.length * 8));
        System.out.println("first byte: "
                + Integer.toString(fbytes[0], 2));
        SecretKeySpec spec = new SecretKeySpec(fbytes, "Blowfish");

        ///ECB/PKCS5Padding
        encoder = Cipher.getInstance("Blowfish");
        encoder.init(Cipher.ENCRYPT_MODE, spec);

        decoder = Cipher.getInstance("Blowfish");
        decoder.init(Cipher.DECRYPT_MODE, spec);
    }

    public void handleSnacResponse(RvSnacResponseEvent event) {
        System.out.println("got response: " + event.getSnacCommand());
    }

    public void sendMsg(String msg) {
        getRvSession().sendRv(new TrillianCryptMsgRvCmd(ByteBlock.wrap(
                encoder.update(BinaryTools.getAsciiBytes(msg)))));
    }
}
