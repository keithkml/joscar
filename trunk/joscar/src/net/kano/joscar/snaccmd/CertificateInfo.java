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
 *  File created by keith @ Aug 25, 2003
 *
 */

package net.kano.joscar.snaccmd;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.LiveWritable;
import net.kano.joscar.BinaryTools;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;
import net.kano.joscar.tlv.ImmutableTlvChain;

import java.io.OutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CertificateInfo implements LiveWritable {
    public static final int CODE_DEFAULT = 1;

    private static final ByteBlock HASH_DEFAULT = ByteBlock.wrap(new byte[] {
        (byte) 0xd4, 0x1d, (byte) 0x8c, (byte) 0xd9, (byte) 0x8f, 0x00,
        (byte) 0xb2, 0x04, (byte) 0xe9, (byte) 0x80, 0x09, (byte) 0x98,
        (byte) 0xec, (byte) 0xf8, 0x42, 0x7e });

    public static final ByteBlock HASHA_DEFAULT = HASH_DEFAULT;
    public static final ByteBlock HASHB_DEFAULT = HASHA_DEFAULT;

    public static byte[] getCertInfoHash(CertificateInfo certInfo) {
        DefensiveTools.checkNull(certInfo, "certInfo");

        ByteBlock data = ByteBlock.createByteBlock(certInfo);

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException impossible) { return null; }

        byte[] hash = digest.digest(data.toByteArray());

        return hash;
    }

    public static CertificateInfo readCertInfoBlock(ByteBlock block) {
        DefensiveTools.checkNull(block, "block");

        TlvChain chain = ImmutableTlvChain.readChain(block);

        int code = -1;
        Tlv codeTlv = chain.getLastTlv(TYPE_CODE);
        if (codeTlv != null) {
            code = codeTlv.getDataAsUShort();
        }

        ByteBlock certData = null;
        Tlv certTlv = chain.getLastTlv(TYPE_CERTDATA);
        if (certTlv != null) {
            certData = certTlv.getData();
        }

        ByteBlock hashA = null;
        Tlv hashATlv = chain.getLastTlv(TYPE_HASH_A);
        if (hashATlv != null) {
            ByteBlock data = hashATlv.getData();
            ExtraInfoBlock infoBlock = ExtraInfoBlock.readExtraInfoBlock(data);
            if (infoBlock != null) hashA = infoBlock.getExtraData().getData();
        }

        ByteBlock hashB = null;

        Tlv hashBTlv = chain.getLastTlv(TYPE_HASH_B);
        if (hashBTlv != null) {
            ByteBlock data = hashBTlv.getData();
            ExtraInfoBlock infoBlock = ExtraInfoBlock.readExtraInfoBlock(data);
            if (infoBlock != null) hashB = infoBlock.getExtraData().getData();
        }

        return new CertificateInfo(code, certData, hashA, hashB);
    }

    private static final int TYPE_CODE = 0x0004;
    private static final int TYPE_CERTDATA = 0x0001;
    private static final int TYPE_HASH_A = 0x0005;
    private static final int TYPE_HASH_B = 0x0006;


    private final int code;
    private final ByteBlock certData;
    private final ByteBlock hashA;
    private final ByteBlock hashB;

    public CertificateInfo(ByteBlock certData) {
        this(CODE_DEFAULT, certData, HASHA_DEFAULT, HASHB_DEFAULT);
    }

    public CertificateInfo(int code, ByteBlock certData, ByteBlock hashA,
            ByteBlock hashB) {
        DefensiveTools.checkRange(code, "code", -1);

        this.code = code;
        this.certData = certData;
        this.hashA = hashA;
        this.hashB = hashB;
    }

    public final int getCode() { return code; }

    public final ByteBlock getCertData() { return certData; }

    public final ByteBlock getHashA() { return hashA; }

    public final ByteBlock getHashB() { return hashB; }

    private static void writeHash(OutputStream out, int tlvType,
            int extraInfoType, ByteBlock hash) throws IOException {
        ExtraInfoData data = new ExtraInfoData(
                ExtraInfoData.FLAG_HASH_PRESENT, hash);
        ExtraInfoBlock block = new ExtraInfoBlock(extraInfoType, data);
        new Tlv(tlvType, ByteBlock.createByteBlock(block)).write(out);
    }

    public void write(OutputStream out) throws IOException {
        if (code != -1) Tlv.getUShortInstance(TYPE_CODE, code).write(out);
        if (certData != null) new Tlv(TYPE_CERTDATA, certData).write(out);
        if (hashA != null) {
            writeHash(out, TYPE_HASH_A, ExtraInfoBlock.TYPE_CERTINFO_HASHA,
                    hashA);
        }
        if (hashB != null) {
            writeHash(out, TYPE_HASH_B, ExtraInfoBlock.TYPE_CERTINFO_HASHB,
                    hashB);
        }
    }

    public String toString() {
        return "CertificateInfo: code=" + code + ", certdata=["
                + BinaryTools.describeData(certData) + "]";
    }
}
