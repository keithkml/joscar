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
 *  File created by keith @ Apr 26, 2003
 *
 */

package net.kano.joscar.rvcmd.trillcrypt;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joscar.tlv.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

public abstract class AbstractTrillianCryptRvCmd extends RvCommand {
    public static final int VERSION_DEFAULT = 0x0001;

    public static final int ENCSTATUS_REQUEST = 0x0000;
    public static final int ENCSTATUS_ACCEPT = 0x0001;
    public static final int ENCSTATUS_BEGIN = 0x0002;
    public static final int ENCSTATUS_MESSAGE = 0x0003;
    public static final int ENCSTATUS_CLOSE = 0x0004;

    private static final int TYPE_VERSION = 0x03e7;
    private static final int TYPE_ENCSTATUS = 0x03e8;

    public static int getStatusCode(RecvRvIcbm icbm) {
        TlvChain chain = ImmutableTlvChain.readChain(icbm.getRvData());

        return chain.getUShort(TYPE_ENCSTATUS);
    }

    protected static BigInteger getBigIntFromHexBlock(ByteBlock block) {
        String str = BinaryTools.getAsciiString(block.subBlock(0, 32));

        return new BigInteger(str, 16);
    }

    protected static byte[] getBigIntHexBlock(BigInteger num)
            throws IOException {
        byte[] data = num.toString(16).getBytes("US-ASCII");
        ByteArrayOutputStream bout = new ByteArrayOutputStream(33);
        for (int i = data.length; i < 32; i++) {
            bout.write('0');
        }
        bout.write(data);
        bout.write(0);

        return bout.toByteArray();
    }

    private final int version;
    private final int encStatus;
    private final TlvChain extraTlvs;

    protected AbstractTrillianCryptRvCmd(RecvRvIcbm icbm) {
        super(icbm);

        TlvChain chain = ImmutableTlvChain.readChain(icbm.getRvData());

        version = chain.getUShort(TYPE_VERSION);
        encStatus = chain.getUShort(TYPE_ENCSTATUS);

        MutableTlvChain extras = new DefaultMutableTlvChain(chain);
        extras.removeTlvs(new int[] { TYPE_VERSION, TYPE_ENCSTATUS });

        extraTlvs = extras;
    }

    protected AbstractTrillianCryptRvCmd(int encStatus) {
        this(VERSION_DEFAULT, encStatus);
    }

    protected AbstractTrillianCryptRvCmd(int encVersion, int encStatus) {
        super(ENCSTATUS_REQUEST,
                CapabilityBlock.BLOCK_TRILLIANCRYPT);

        this.version = encVersion;
        this.encStatus = encStatus;
        this.extraTlvs = null;
    }

    protected final int getVersion() { return version; }

    protected final int getEncStatus() { return encStatus; }

    protected final TlvChain getExtraTlvs() { return extraTlvs; }

    public void writeRvData(OutputStream out) throws IOException {
        Tlv.getUShortInstance(TYPE_VERSION, version).write(out);
        Tlv.getUShortInstance(TYPE_ENCSTATUS, encStatus).write(out);

        writeExtraTlvs(out);
    }

    protected abstract void writeExtraTlvs(OutputStream out) throws IOException;
}
