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

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.LiveWritable;
import net.kano.joscar.tlv.ImmutableTlvChain;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;
import net.kano.joscar.tlv.TlvTools;

import java.io.IOException;
import java.io.OutputStream;
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

        TlvChain chain = TlvTools.readChain(block);

        int code = -1;
        Tlv codeTlv = chain.getLastTlv(TYPE_NUMCERTS);
        if (codeTlv != null) {
            code = codeTlv.getDataAsUShort();
        }

        ByteBlock commonCertData = null;
        ByteBlock encCertData = null;
        ByteBlock signCertData = null;
        if (code == 1) {
            Tlv commonCertTlv = chain.getLastTlv(TYPE_COMMONCERTDATA);
            if (commonCertTlv != null) {
                commonCertData = commonCertTlv.getData();
            }
        } else if (code == 2) {
            Tlv encCertTlv = chain.getLastTlv(TYPE_ENCCERTDATA);
            if (encCertTlv != null) {
                encCertData = encCertTlv.getData();
            }

            Tlv signCertTlv = chain.getLastTlv(TYPE_SIGNCERTDATA);
            if (signCertTlv != null) {
                signCertData = signCertTlv.getData();
            }
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

        return new CertificateInfo(commonCertData, encCertData, signCertData,
                hashA, hashB);
    }

    private static final int TYPE_NUMCERTS = 0x0004;
    private static final int TYPE_COMMONCERTDATA = 0x0001;
    private static final int TYPE_ENCCERTDATA = 0x0001;
    private static final int TYPE_SIGNCERTDATA = 0x0002;
    private static final int TYPE_HASH_A = 0x0005;
    private static final int TYPE_HASH_B = 0x0006;

    private final ByteBlock commonCertData;
    private final ByteBlock encCertData;
    private final ByteBlock signCertData;
    private final ByteBlock hashA;
    private final ByteBlock hashB;

    public CertificateInfo(ByteBlock commonCertData) {
        this(commonCertData, null, null, HASHA_DEFAULT, HASHB_DEFAULT);
    }

    public CertificateInfo(ByteBlock encCertData, ByteBlock signCertData,
            ByteBlock hashA, ByteBlock hashB) {
        this(null, encCertData, signCertData, hashA, hashB);
    }

    private CertificateInfo(ByteBlock commonCertData, ByteBlock encCertData,
            ByteBlock signCertData, ByteBlock hashA, ByteBlock hashB) {
        if (commonCertData == null
                && (encCertData == null || signCertData == null)) {
            throw new IllegalArgumentException("commonCertData is null but "
                    + (encCertData == null ? "encCertData is null" : "")
                    + (signCertData == null ? (encCertData == null ? ", " : "")
                    + "signCertData is null" : ""));
        }
        if (commonCertData != null
                && (encCertData != null || signCertData != null)) {
            throw new IllegalArgumentException("commonCertData is not null but "
                    + (encCertData != null ? "encCertData is not null" : "")
                    + (signCertData != null ? (encCertData != null ? ", " : "")
                    + "signCertData is not null" : ""));
        }

        this.commonCertData = commonCertData;
        this.encCertData = encCertData;
        this.signCertData = signCertData;
        this.hashA = hashA;
        this.hashB = hashB;
    }

    public final ByteBlock getCommonCertData() { return commonCertData; }

    public final ByteBlock getEncCertData() { return encCertData; }

    public final ByteBlock getSignCertData() { return signCertData; }

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
        int numCerts;
        if (commonCertData != null) numCerts = 1;
        else numCerts = 2;
        Tlv.getUShortInstance(TYPE_NUMCERTS, numCerts).write(out);

        if (numCerts == 1) {
            if (commonCertData != null) {
                new Tlv(TYPE_COMMONCERTDATA, commonCertData).write(out);
            }
        } else {
            new Tlv(TYPE_ENCCERTDATA, encCertData).write(out);
            new Tlv(TYPE_SIGNCERTDATA, signCertData).write(out);
        }
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
        return "CertificateInfo: certdata=["
                + BinaryTools.describeData(commonCertData) + "]";
    }
}
