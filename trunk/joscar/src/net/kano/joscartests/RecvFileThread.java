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
 *  File created by keith @ Apr 25, 2003
 *
 */

package net.kano.joscartests;

import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.sendfile.FileSendAcceptRvCmd;
import net.kano.joscar.rvproto.ft.FileTransferChecksum;
import net.kano.joscar.rvproto.ft.FileTransferHeader;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class RecvFileThread extends Thread {
    private JoscarTester tester;
    private InetAddress address;
    private int port;
    private RvSession session;
    private long cookie;
    private boolean encrypted;
    private ServerSocket serverSocket;

    public RecvFileThread(RvSession session, ServerSocket socket) {
        this.session = session;
        this.serverSocket = socket;
    }

    public RecvFileThread(JoscarTester tester, InetAddress address, int port,
            RvSession session, long cookie, boolean encrypted) {
        this.tester = tester;
        this.address = address;
        this.port = port;
        this.session = session;
        this.cookie = cookie;
        this.encrypted = encrypted;
    }

    private Socket getSocket() throws IOException {
        if (serverSocket != null) {
            return serverSocket.accept();
        } else {
            session.sendRv(new FileSendAcceptRvCmd(encrypted));
            Socket socket = null;
            if (encrypted) {
                try {
                    System.out.println("creating secure socket");
                    socket = createSecureSocket();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return null;
                } catch (KeyStoreException e) {
                    e.printStackTrace();
                    return null;
                } catch (UnrecoverableKeyException e) {
                    e.printStackTrace();
                    return null;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                } catch (KeyManagementException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                socket = new Socket(address, port);
            }
            System.out.println("socket opened..");
            return socket;
        }
    }

    private Socket createSecureSocket() throws NoSuchAlgorithmException,
            KeyStoreException, UnrecoverableKeyException,
            KeyManagementException, IOException {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(tester.keystore, "pass".toCharArray());
        SSLContext context = SSLContext.getInstance("SSL");
        KeyManager[] kms = kmf.getKeyManagers();
        X509KeyManager xkm = null;
        for (int i = 0; i < kms.length; i++) {
            if (kms[i] instanceof X509KeyManager) {
                System.out.println("found x509keymgr");
                if (xkm == null) xkm = (X509KeyManager) kms[i];
            }
        }
        final X509KeyManager xkm1 = xkm;
        context.init(/*kmf.getKeyManagers()*/
                new KeyManager[] {
                  new X509KeyManager() {
                      public PrivateKey getPrivateKey(String string) {
                          System.out.println("getPrivateKey:" + string);
                          return xkm1.getPrivateKey(string);
                      }

                      public X509Certificate[] getCertificateChain(String string) {
                          System.out.println("getCertificateChain:" + string);
                          return xkm1.getCertificateChain(string);
                      }

                      public String[] getClientAliases(String string, Principal[] principals) {
                          System.out.println("getClientAliases:" + string + ", " + Arrays.asList(principals));
                          return xkm1.getClientAliases(string, principals);
                      }

                      public String[] getServerAliases(String string, Principal[] principals) {
                          System.out.println("getServerAliases:" + string + ", " + Arrays.asList(principals));
                          return xkm1.getServerAliases(string, principals);
                      }

                      public String chooseServerAlias(String string, Principal[] principals,
                              Socket socket) {
                          System.out.println("chooseServerAlias:" + string + ", " + Arrays.asList(principals));
                          return xkm1.chooseServerAlias(string, principals, socket);
                      }

                      public String chooseClientAlias(String[] strings, Principal[] principals,
                              Socket socket) {
                          System.out.println("chooseClientAlias:" + Arrays.asList(strings) + ", " + Arrays.asList(principals));
                          String alias = xkm1.chooseClientAlias(strings, null, socket);
                          System.out.println("chose " + alias);
                          return alias;
                      }
                  }
                },
                new TrustManager[]{new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(X509Certificate[] certs,
                            String string) throws CertificateException {
                        checkTrusted(certs);
                    }

                    public void checkServerTrusted(X509Certificate[] certs,
                            String string) throws CertificateException {
                        checkTrusted(certs);
                    }

                    private void checkTrusted(X509Certificate[] certs)
                            throws CertificateException {
                        System.out.println("accepting all certificates");
                        return;
//                X509Certificate usercert = tester.getCert(session.getScreenname());
//                for (int i = 0; i < certs.length; i++) {
//                    System.out.println("*** checking trust for " + certs[i]
//                            + " ***");
//                    if (certs[i].equals(usercert)) {
//                        System.out.println("trusted!!");
//                        return;
//                    }
//                }
//                System.out.println("couldn't find cert matching " + usercert);
//                throw new CertificateException();
                    }
                }},
                new SecureRandom());

        SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket(address, port);
        socket.startHandshake();
        return socket;
    }

    public void run() {
        try {
            Socket socket = getSocket();
            System.out.println("opening socket to " + address + " on " + port);

            InputStream in = socket.getInputStream();

            for (;;) {
                FileTransferHeader header = FileTransferHeader.readHeader(in);

                if (header == null) break;

                System.out.println("header: " + header);

                String[] parts = header.getFilename().getSegments();
                String filename;
                if (parts.length > 0) filename = "dl-" + parts[parts.length-1];
                else filename = "dl-" + session.getScreenname();
                System.out.println("writing to file " + filename);

                long sum = 0;
                if (new File(filename).exists()) {
                    FileInputStream fis = new FileInputStream(filename);
                    byte[] block = new byte[10];
                    for (int i = 0; i < block.length;) {
                        int count = fis.read(block);

                        if (count == -1) break;

                        i += count;
                    }

                    FileTransferChecksum summer = new FileTransferChecksum();
                    summer.update(block, 0, 10);
                    sum = summer.getValue();
                }

                FileChannel fileChannel
                        = new FileOutputStream(filename).getChannel();

                FileTransferHeader outHeader = new FileTransferHeader(header);
                outHeader.setHeaderType(FileTransferHeader.HEADERTYPE_ACK);
                outHeader.setIcbmMessageId(cookie);
                outHeader.setBytesReceived(0);
                outHeader.setReceivedChecksum(sum);

                OutputStream socketOut = socket.getOutputStream();
                System.out.println("sending header: " + outHeader);
                outHeader.write(socketOut);

//                FileTransferHeader resh = FileTransferHeader.readHeader(in);
//                System.out.println("resume header: " + resh);

                for (int i = 0; i < header.getFileSize();) {
                    long transferred = fileChannel.transferFrom(
                            Channels.newChannel(in),
                            0, header.getFileSize() - i);

                    System.out.println("transferred " + transferred);

                    if (transferred == -1) return;

                    i += transferred;
                }
                System.out.println("finished transfer!");
                fileChannel.close();

                FileTransferHeader doneHeader = new FileTransferHeader(header);
                doneHeader.setHeaderType(FileTransferHeader.HEADERTYPE_RECEIVED);
                doneHeader.setFlags(doneHeader.getFlags()
                        | FileTransferHeader.FLAG_DONE);
                doneHeader.setBytesReceived(doneHeader.getBytesReceived() + 1);
                doneHeader.setIcbmMessageId(cookie);
                doneHeader.setFilesLeft(doneHeader.getFilesLeft() - 1);
                doneHeader.write(socketOut);
                if (doneHeader.getFilesLeft() - 1 <= 0) {
                    socket.close();
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}
