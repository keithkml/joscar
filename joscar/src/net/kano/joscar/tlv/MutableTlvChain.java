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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Provides a means of modifying the contents of a TLV chain.
 */
public class MutableTlvChain extends TlvChain {
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
        List tlvs = (List) tlvMap.get(type);

        int insertAt = -1;
        if (tlvs == null) {
            tlvs = new LinkedList();
            tlvMap.put(type, tlvs);
        } else if (!tlvs.isEmpty()) {
            // find the first instance of a tlv of this type
            int i = 0;
            for (Iterator it = tlvList.iterator(); it.hasNext(); i++) {
                Tlv next = (Tlv) it.next();
                if (next.getType() == typeCode) {
                    // we found one!
                    if (insertAt == -1) insertAt = i;
                    it.remove();
                }
            }

            tlvs.clear();
        }
        if (insertAt == -1) insertAt = tlvList.size();

        tlvs.add(tlv);
        tlvList.add(insertAt, tlv);
    }

    /**
     * Removes the given TLV from the chain, if it is present.
     *
     * @param tlv the TLV to remove
     */
    public synchronized final void removeTlv(Tlv tlv) {
        int typeCode = tlv.getType();
        Integer type = new Integer(typeCode);
        List tlvs = (List) tlvMap.get(type);

        if (tlvs != null) while (tlvs.remove(tlv));
        while (tlvList.remove(tlv));
    }

    /**
     * Removes all TLV's in this chain of the given TLV type.
     *
     * @param type the type of TLV of which to remove all instances
     */
    public synchronized final void removeTlvs(int type) {
        Integer typeKey = new Integer(type);
        List tlvs = (List) tlvMap.remove(typeKey);

        if (tlvs != null) tlvList.removeAll(tlvs);
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
    public synchronized final void addAll(TlvChain other) {
        for (Iterator it = other.tlvList.iterator(); it.hasNext();) {
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
    public synchronized final void replaceAll(TlvChain other) {
        for (Iterator it = other.tlvList.iterator(); it.hasNext();) {
            Tlv tlv = (Tlv) it.next();

            replaceTlv(tlv);
        }
    }

    // all this for thread safety..

    protected synchronized void addTlvImpl(Tlv tlv) { super.addTlvImpl(tlv); }

    public synchronized boolean hasTlv(int type) { return super.hasTlv(type); }

    public synchronized Tlv[] getTlvs() { return super.getTlvs(); }

    public synchronized int getTlvCount() { return super.getTlvCount(); }

    public synchronized Tlv getFirstTlv(int type) {
        return super.getFirstTlv(type);
    }

    public synchronized Tlv getLastTlv(int type) {
        return super.getLastTlv(type);
    }

    public synchronized Tlv[] getTlvs(int type) { return super.getTlvs(type); }

    public synchronized long getWritableLength() {
        return super.getWritableLength();
    }

    public synchronized void write(OutputStream out) throws IOException {
        super.write(out);
    }

    public synchronized String getString(int type) {
        return super.getString(type);
    }

    public synchronized String getString(int type, String charset) {
        return super.getString(type, charset);
    }

    public synchronized int getUShort(int type) {
        return super.getUShort(type);
    }

    public synchronized int getTotalSize() {
        return super.getTotalSize();
    }
}
