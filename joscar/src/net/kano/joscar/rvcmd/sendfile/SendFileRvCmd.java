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
 *  File created by keith @ Apr 24, 2003
 *
 */

package net.kano.joscar.rvcmd.sendfile;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.EncodedStringInfo;
import net.kano.joscar.snaccmd.MinimalEncoder;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.tlv.ImmutableTlvChain;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Locale;

public class SendFileRvCmd extends AbstractFileSendRvCmd {
    public static final int REQTYPE_INITIAL_REQUEST = 0x0001;
    public static final int REQTYPE_REDIRECT = 0x0002;

    private static final int TYPE_REQTYPE = 0x0a;
    private static final int TYPE_LANGUAGE = 0x0e;
    private static final int TYPE_CHARSET = 0x0d;
    private static final int TYPE_MESSAGE = 0x0c;
    private static final int TYPE_INTERNALIP = 0x03;
    private static final int TYPE_EXTERNALIP = 0x04;
    private static final int TYPE_PORT = 0x05;
    private static final int TYPE_SEND_BLOCK = 0x2711;

    private final int requestType;
    private final Locale language;
    private final String message;
    private final InetAddress internalIP;
    private final InetAddress externalIP;
    private final int port;

    private final FileSendBlock fileSendBlock;

    public SendFileRvCmd(RecvRvIcbm icbm) {
        super(icbm);

        TlvChain chain = ImmutableTlvChain.readChain(icbm.getRvData());

        requestType = chain.getUShort(TYPE_REQTYPE);

        String languageStr = chain.getString(TYPE_LANGUAGE);
        language = languageStr == null ? null : new Locale(languageStr);

        String charset = chain.getString(TYPE_CHARSET);

        message = (charset == null
                ? chain.getString(TYPE_MESSAGE)
                : chain.getString(TYPE_MESSAGE, charset));

        Tlv internalIpTlv = chain.getLastTlv(TYPE_INTERNALIP);
        internalIP = (internalIpTlv == null
                ? null
                : BinaryTools.getIPFromBytes(internalIpTlv.getData(), 0));

        Tlv externalIpTlv = chain.getLastTlv(TYPE_EXTERNALIP);
        externalIP = (externalIpTlv == null
                ? null
                : BinaryTools.getIPFromBytes(externalIpTlv.getData(), 0));

        port = chain.getUShort(TYPE_PORT);


        Tlv sendDataTlv = chain.getLastTlv(TYPE_SEND_BLOCK);
        fileSendBlock = (sendDataTlv == null
                ? null
                : FileSendBlock.readFileSendBlock(sendDataTlv.getData()));
    }

    public SendFileRvCmd(String message, InetAddress localAddress,
            int localPort, FileSendBlock file) {
        this(REQTYPE_INITIAL_REQUEST, Locale.getDefault(), message,
                localAddress, null, localPort, file);
    }

    public SendFileRvCmd(InetAddress localAddress, int localPort) {
        this(REQTYPE_REDIRECT, null, null, localAddress, null, localPort, null);
    }

    public SendFileRvCmd(int requestType, Locale language, String message,
            InetAddress internalIP, InetAddress externalIP, int port,
            FileSendBlock fileSendBlock) {
        super(STATUS_REQUEST);

        DefensiveTools.checkRange(requestType, "requestType", -1);
        DefensiveTools.checkRange(port, "port", -1);

        this.requestType = requestType;
        this.language = language;
        this.message = message;
        this.internalIP = internalIP;
        this.externalIP = externalIP;
        this.port = port;
        this.fileSendBlock = fileSendBlock;
    }

    public final Locale getLanguage() { return language; }

    public final String getMessage() { return message; }

    public final InetAddress getInternalIP() { return internalIP; }

    public final InetAddress getExternalIP() { return externalIP; }

    public final int getPort() { return port; }

    public final FileSendBlock getFileSendBlock() { return fileSendBlock; }

    public void writeRvData(OutputStream out) throws IOException {
        if (requestType != -1) {
            Tlv.getUShortInstance(TYPE_REQTYPE, requestType);
        }
        new Tlv(0x000f).write(out);

        if (language != null && language.getLanguage().length() > 0) {
            String lang = language.getLanguage();
            Tlv.getStringInstance(TYPE_LANGUAGE, lang).write(out);
        }

        if (message != null) {
            EncodedStringInfo encInfo = MinimalEncoder.encodeMinimally(message);
            String charset = encInfo.getCharset();

            Tlv.getStringInstance(TYPE_CHARSET, charset).write(out);
            new Tlv(TYPE_MESSAGE, ByteBlock.wrap(encInfo.getData())).write(out);
        }

        if (internalIP != null) {
            ByteBlock ipBytes = ByteBlock.wrap(internalIP.getAddress());
            new Tlv(TYPE_INTERNALIP, ipBytes).write(out);
        }

        if (externalIP != null) {
            ByteBlock ipBytes = ByteBlock.wrap(externalIP.getAddress());
            new Tlv(TYPE_EXTERNALIP, ipBytes).write(out);
        }

        if (port != -1) {
            Tlv.getUShortInstance(TYPE_PORT, port).write(out);
        }

        if (fileSendBlock != null) {
            ByteBlock fileBlockData = ByteBlock.createByteBlock(fileSendBlock);
            new Tlv(TYPE_SEND_BLOCK, fileBlockData).write(out);
        }
    }

    public String toString() {
        return "SendFileRvCmd: " +
                "language=" + language +
                ", message='" + message + "'" +
                ", internalIP=" + internalIP +
                ", externalIP=" + externalIP +
                ", port=" + port +
                ", fileSendBlock=" + fileSendBlock;
    }
}
