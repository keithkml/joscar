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
 *  File created by Keith @ 4:16:31 AM
 *
 */

package net.kano.joscar.rvcmd;

import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.BinaryTools;
import net.kano.joscar.tlv.*;

import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

public abstract class AbstractRvCmd extends RvCommand {
    public static final long ICBMMSGID_DEFAULT = 0;

    private static final int TYPE_SERVICE_DATA = 0x2711;

    private final ByteBlock serviceData;
    private final MutableTlvChain rvTlvs;

    public AbstractRvCmd(RecvRvIcbm icbm) {
        super(icbm);

        ByteBlock block = icbm.getRvData();

        TlvChain chain = ImmutableTlvChain.readChain(block);

        System.out.println("RV status=" + icbm.getRvStatus());
        System.out.println("TLVs (" + chain.getTlvCount() + "):");
        for (Iterator it = chain.iterator(); it.hasNext();) {
            System.out.println("- " + it.next());
        }

        Tlv serviceDataTlv = chain.getLastTlv(TYPE_SERVICE_DATA);
        if (serviceDataTlv == null) serviceData = null;
        else serviceData = serviceDataTlv.getData();

        MutableTlvChain extras = new DefaultMutableTlvChain(chain);
        extras.removeTlvs(new int[] {
            TYPE_SERVICE_DATA
        });

        rvTlvs = extras;
    }

    public AbstractRvCmd(long icbmMessageId, int rvStatus, CapabilityBlock cap) {
        super(icbmMessageId, rvStatus, cap);

        serviceData = null;
        rvTlvs = null;
    }

    protected final ByteBlock getServiceData() { return serviceData; }

    protected final TlvChain getRvTlvs() { return rvTlvs; }

    final MutableTlvChain getMutableTlvs() { return rvTlvs; }

    public final void writeRvData(OutputStream out) throws IOException {
        writeHeaderRvTlvs(out);
        writeRvTlvs(out);

        if (hasServiceData()) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            writeServiceData(bout);

            ByteBlock serviceBlock = ByteBlock.wrap(bout.toByteArray());

            System.out.println("service data: "
                    + BinaryTools.describeData(serviceBlock));

            new Tlv(TYPE_SERVICE_DATA, serviceBlock).write(out);
        }
    }

    protected abstract void writeHeaderRvTlvs(OutputStream out)
            throws IOException;
    protected abstract void writeRvTlvs(OutputStream out) throws IOException;

    protected boolean hasServiceData() { return true; }

    protected void writeServiceData(OutputStream out) throws IOException { }
}