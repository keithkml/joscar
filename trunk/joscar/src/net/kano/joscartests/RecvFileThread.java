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
 *  File created by keith @ Apr 25, 2003
 *
 */

package net.kano.joscartests;

import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.sendfile.FileSendAcceptRvCmd;
import net.kano.joscar.rvproto.ft.FileSendHeader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class RecvFileThread extends Thread {
    private InetAddress address;
    private int port;
    private RvSession session;
    private long cookie;
    private ServerSocketChannel serverSocket;

    public RecvFileThread(RvSession session, ServerSocketChannel socket) {
        this.session = session;
        this.serverSocket = socket;
    }

    public RecvFileThread(InetAddress address, int port, RvSession session,
            long cookie) {
        this.address = address;
        this.port = port;
        this.session = session;
        this.cookie = cookie;
    }

    private SocketChannel getSocket() throws IOException {
        if (serverSocket != null) {
            return serverSocket.accept();
        } else {
            InetSocketAddress socketAddr = new InetSocketAddress(address, port);
            SocketChannel socketChannel = SocketChannel.open(socketAddr);
            session.sendRv(new FileSendAcceptRvCmd());
            System.out.println("socket opened..");
            return socketChannel;
        }
    }

    public void run() {
        try {
            SocketChannel socketChannel = getSocket();
            System.out.println("opening socket to " + address + " on " + port);


            InputStream in = Channels.newInputStream(socketChannel);

//            for (;;) {
                FileSendHeader header = FileSendHeader.readFileSendHeader(in);

                System.out.println("header: " + header);

                String[] parts = header.getFilename().getSegments();
                String filename = "dl-" + parts[parts.length-1];
                System.out.println("writing to file " + filename);

                FileChannel fileChannel
                        = new FileOutputStream(filename).getChannel();

                FileSendHeader outHeader = new FileSendHeader(header);
                outHeader.setHeaderType(FileSendHeader.HEADERTYPE_ACK);
                outHeader.setIcbmMessageId(cookie);
                OutputStream socketOut
                        = Channels.newOutputStream(socketChannel);
//                outHeader.write(socketOut);
                System.out.println("sending header: " + outHeader);

                for (int i = 0; i < header.getFileSize();) {
                    long transferred = fileChannel.transferFrom(socketChannel,
                            0, header.getFileSize() - i);

                    System.out.println("transferred " + transferred);

                    if (transferred == -1) return;

                    i += transferred;
                }
                System.out.println("finished transfer!");
                fileChannel.close();

                FileSendHeader doneHeader = new FileSendHeader(header);
                doneHeader.setHeaderType(FileSendHeader.HEADERTYPE_RECEIVED);
                doneHeader.setFlags(doneHeader.getFlags()
                        | FileSendHeader.FLAG_DONE);
                doneHeader.setBytesReceived(doneHeader.getBytesReceived() + 1);
                doneHeader.setIcbmMessageId(cookie);
                doneHeader.write(socketOut);
//            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}
