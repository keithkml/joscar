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
 *  File created by keith @ Apr 27, 2003
 *
 */

package net.kano.joscar.rvcmd;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A base class for an RV request command. RV request commands contain a
 * mysterious empty <code>0x000f</code> TLV, a "request index" which starts at
 * {@link #REQINDEX_FIRST}, and, normally, more type-specific TLV's.
 */
public abstract class AbstractRequestRvCmd
        extends AbstractRvCmd implements RequestRvCmd {
    /**
     * The default value of the <code>fPresent</code> field. This value indicates
     * that the mysterious <code>0x000f</code> TLV is present in a command.
     */
    public static final boolean FPRESENT_DEFAULT = true;

    /** A TLV type containing the "request index." */
    private static final int TYPE_REQINDEX = 0x000a;
    /** The TLV type of the "mysterious <code>0x000f</code> TLV." */
    private static final int TYPE_F = 0x000f;

    /** This command's request index code. */
    private final int reqIndex;
    /** Whether this commanc contained the mysterious <code>0x000f</code> TLV. */
    private final boolean fPresent;

    /**
     * Creates a new RV request command from the given incoming RV request ICBM
     * command.
     *
     * @param icbm an incoming RV request ICBM command
     */
    protected AbstractRequestRvCmd(RecvRvIcbm icbm) {
        super(icbm);

        TlvChain chain = getRvTlvs();

        reqIndex = chain.getUShort(TYPE_REQINDEX);
        fPresent = chain.hasTlv(TYPE_F);

        getMutableTlvs().removeTlvs(TYPE_REQINDEX, TYPE_F);
    }

    /**
     * Creates a new outgoing initial RV request command with the given ICBM
     * message ID, associated capability block, a request index of {@link
     * #REQINDEX_FIRST}, and a <code>0x000f</code> TLV present. Using this
     * constructor is equivalent to using {@link #AbstractRequestRvCmd(CapabilityBlock,
     * int) AbstractRequestRvCmd(cap, REQTYPE_INITIALREQUEST)}.
     *
     * @param cap the capability block associated with this RV command
     */
    protected AbstractRequestRvCmd(CapabilityBlock cap) {
        this(cap, REQINDEX_FIRST);
    }

    /**
     * Creates a new outgoing initial RV request command with the given
     * associated capability block, and request index, and a <code>0x000f</code>
     * TLV present. Using this constructor is equivalent to using {@link
     * #AbstractRequestRvCmd(CapabilityBlock, int, boolean)
     * AbstractRequestRvCmd(cap, REQTYPE_INITIALREQUEST, FPRESENT_DEFAULT)}.
     *
     * @param cap          the capability block associated with this RV command
     * @param requestIndex a request index, like {@link #REQINDEX_FIRST}
     */
    protected AbstractRequestRvCmd(CapabilityBlock cap, int requestIndex) {
        this(cap, requestIndex, FPRESENT_DEFAULT);
    }

    /**
     * Creates a new outgoing initial RV request command with the given
     * associated capability block, and request index, and a <code>0x000f</code>
     * TLV present. Using this constructor is equivalent to using {@link
     * #AbstractRequestRvCmd(CapabilityBlock, int, boolean)
     * AbstractRequestRvCmd(cap, REQTYPE_INITIALREQUEST, FPRESENT_DEFAULT)}.
     *
     * @param cap          the capability block associated with this RV command
     * @param requestIndex a request index, like {@link #REQINDEX_FIRST}
     * @param fPresent     whether this command should contain the mysterious
     *                     type <code>0x000f</code> TLV
     */
    protected AbstractRequestRvCmd(CapabilityBlock cap, int requestIndex,
            boolean fPresent) {
        super(RVSTATUS_REQUEST, cap);

        DefensiveTools.checkRange(requestIndex, "requestIndex", -1);

        this.reqIndex = requestIndex;
        this.fPresent = fPresent;
    }

    public boolean isFirstRequest() {
        return getRequestIndex() == REQINDEX_FIRST;
    }

    public final int getRequestIndex() { return reqIndex; }

    /**
     * Returns whether this RV command contains the mysteroius
     * <code>0x000f</code> TLV. The significance of said TLV is unknown as of
     * this writing.
     *
     * @return <code>true</code> if this RV command contains the mysterious
     *         <code>0x000f</code> TLV; <code>false</code> otherwise
     */
    protected final boolean isFPresent() { return fPresent; }

    protected final void writeHeaderRvTlvs(OutputStream out)
            throws IOException {
        if (reqIndex != -1) {
            Tlv.getUShortInstance(TYPE_REQINDEX, reqIndex).write(out);
        }
        if (fPresent) {
            new Tlv(TYPE_F).write(out);
        }
    }
}
