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
 *  File created by keith @ Mar 3, 2003
 *
 */

package net.kano.joscar.tlv;

import java.util.*;

/**
 * Provides a means of modifying the contents of a TLV chain.
 */
public class MutableTlvChain extends AbstractTlvChain {
    /** A list of the TLV's in this chain, in order. */
    private final List tlvList = new LinkedList();
    /**
     * A map from TLV type codes to <code>List</code>s of the TLV's in this
     * chain with that type.
     */
    private final Map tlvMap = new HashMap();

    /**
     * Creates an empty TLV chain.
     */
    public MutableTlvChain() { }

    /**
     * Creates a TLV chain containing the same TLV's as the given chain.
     *
     * @param other a TLV chain to copy
     */
    public MutableTlvChain(TlvChain other) {
        super(other);
    }

    /**
     * Adds the given TLV to this chain.
     *
     * @param tlv the TLV to add
     */
    public synchronized final void addTlv(Tlv tlv) {
        addTlvImpl(tlv);
    }

    /**
     * Removes all TLV's of the given type from the chain, and inserts the given
     * TLV at the index of the first TLV removed, or at the end of the chain if
     * no TLV's of the same type were found.
     *
     * @param tlv the TLV to replace its "siblings" of the same TLV type
     */
    public synchronized final void replaceTlv(Tlv tlv) {
        int typeCode = tlv.getType();
        Integer type = new Integer(typeCode);
        List tlvs = (List) getTlvMap().get(type);

        int insertAt = -1;
        if (tlvs == null) {
            tlvs = new LinkedList();
            getTlvMap().put(type, tlvs);
        } else if (!tlvs.isEmpty()) {
            // find the first instance of a tlv of this type
            int i = 0;
            for (Iterator it = getTlvList().iterator(); it.hasNext(); i++) {
                Tlv next = (Tlv) it.next();
                if (next.getType() == typeCode) {
                    // we found one!
                    if (insertAt == -1) insertAt = i;
                    it.remove();
                }
            }

            tlvs.clear();
        }
        if (insertAt == -1) insertAt = getTlvList().size();

        tlvs.add(tlv);
        getTlvList().add(insertAt, tlv);
    }

    /**
     * Removes the given TLV from the chain, if it is present.
     *
     * @param tlv the TLV to remove
     */
    public synchronized final void removeTlv(Tlv tlv) {
        int typeCode = tlv.getType();
        Integer type = new Integer(typeCode);
        List tlvs = (List) getTlvMap().get(type);

        if (tlvs != null) while (tlvs.remove(tlv));
        while (getTlvList().remove(tlv));
    }

    /**
     * Removes all TLV's in this chain of the given TLV type.
     *
     * @param type the type of TLV of which to remove all instances
     */
    public synchronized final void removeTlvs(int type) {
        Integer typeKey = new Integer(type);
        List tlvs = (List) getTlvMap().remove(typeKey);

        if (tlvs != null) getTlvList().removeAll(tlvs);
    }

    /**
     * Removes all TLV's in this chain having any of the given types.
     *
     * @param types the TLV types of which to remove all instances
     */
    public synchronized final void removeTlvs(int[] types) {
        for (int i = 0; i < types.length; i++) {
            removeTlvs(types[i]);
        }
    }

    /**
     * Adds all TLV's in the given chain to the end of this chain.
     *
     * @param other the chain whose TLV's will be appended to this chain
     */
    public synchronized final void addAll(AbstractTlvChain other) {
        for (Iterator it = other.getTlvList().iterator(); it.hasNext();) {
            Tlv tlv = (Tlv) it.next();

            addTlvImpl(tlv);
        }
    }

    /**
     * Deletes all TLV's currently in this chain having the same type as any of
     * the TLV's in the other chain, and replaces them with their counterparts
     * in the given chain. Behavior is undefined if the given chain contains two
     * or more TLV's of the same type. Any TLV's in the given chain without
     * counterparts in this chain will be appended to the end of this chain.
     *
     * @param other the TLV whose TLV's will replace and/or add to TLV's in this
     *        chain
     */
    public synchronized final void replaceAll(AbstractTlvChain other) {
        for (Iterator it = other.getTlvList().iterator(); it.hasNext();) {
            Tlv tlv = (Tlv) it.next();

            replaceTlv(tlv);
        }
    }

    protected synchronized List getTlvList() { return tlvList; }

    protected synchronized Map getTlvMap() { return tlvMap; }
}