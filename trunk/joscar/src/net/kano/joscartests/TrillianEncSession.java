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
import net.kano.joscar.rv.*;
import net.kano.joscar.rvcmd.trillcrypt.*;
import net.kano.joscar.snaccmd.icbm.RvCommand;

import javax.crypto.*;
import javax.crypto.spec.DHParameterSpec;
import java.security.*;

public class TrillianEncSession implements RvSessionListener {
    private Cipher encoder;
    private Cipher decoder;

    private final RvSession rvSession;

    public TrillianEncSession(RvSession session) {
        this.rvSession = session;
    }

    public void handleRv(RecvRvEvent event) {
        RvCommand rvc = event.getRvCommand();

        if (rvc instanceof TrillianCryptReqRvCmd) {
            System.out.println("got request for secureim from "
                    + rvSession.getScreenname());
            rvSession.sendRv(new TrillianCryptAcceptRvCmd(0));
            rvSession.addListener(this);

            TrillianCryptReqRvCmd cmd = (TrillianCryptReqRvCmd) rvc;

            // cmd.getP(), cmd.getG()

            try {
                DHParameterSpec spec
                        = new DHParameterSpec(cmd.getP(), cmd.getG(), 1024);

                KeyPairGenerator keyPairGen
                        = KeyPairGenerator.getInstance("DiffieHellman");
                keyPairGen.initialize(spec);
                KeyPair pair = keyPairGen.generateKeyPair();

                KeyAgreement keyAg = KeyAgreement.getInstance("DiffieHellman");
                keyAg.init(pair.getPrivate());
                SecretKey key = keyAg.generateSecret("Blowfish");

                AlgorithmParameterGenerator algParamGen
                        = AlgorithmParameterGenerator.getInstance("Blowfish");
                algParamGen.init(128);

                AlgorithmParameters algParam = algParamGen.generateParameters();

                encoder = Cipher.getInstance("Blowfish");
                encoder.init(Cipher.ENCRYPT_MODE, key, algParam);

                decoder = Cipher.getInstance("Blowfish");
                decoder.init(Cipher.DECRYPT_MODE, key, algParam);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
        } else if (rvc instanceof TrillianCryptBeginRvCmd) {
            System.out.println("encrypted session with "
                    + rvSession.getScreenname() + " begun!");
        } else if (rvc instanceof TrillianCryptMsgRvCmd) {
            TrillianCryptMsgRvCmd cmd = (TrillianCryptMsgRvCmd) rvc;

            try {
                byte[] encrypted = cmd.getEncryptedMsg().toByteArray();
                byte[] decoded = decoder.doFinal(encrypted);

                System.out.println("got encrypted message from "
                        + rvSession.getScreenname() + ": "
                        + BinaryTools.getAsciiString(ByteBlock.wrap(decoded)));
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            }
        } else if (rvc instanceof TrillianCryptCloseRvCmd) {
            System.out.println("encryption session with "
                    + rvSession.getScreenname() + " closed!");
        }
    }

    public void handleSnacResponse(RvSnacResponseEvent event) {
        System.out.println("got response: " + event.getSnacCommand());
    }
}
