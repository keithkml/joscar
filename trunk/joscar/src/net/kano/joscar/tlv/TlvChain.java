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
 *  File created by keith @ Feb 18, 2003
 *
 */

package net.kano.joscar.tlv;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.Writable;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Represents a "chain," or block, or sequence, of <code>Tlv</code>s. This class
 * is immutable, but subclasses may not be, so to ensure immutability of a
 * <code>TlvChain</code> that you are passed, you may want to make an immutable
 * copy using <code>new TlvChain(possiblyMutableChain)</code>.
 */
public class TlvChain implements Writable {
    /** A list of the TLV's in this chain, in order. */
    protected final List tlvList;
    /**
     * A map from TLV type codes to <code>List</code>s of the TLV's in this
     * chain with that type.
     */
    protected final Map tlvMap;
    /** The total size of this chain, as read from an incoming stream. */
    protected final int totalSize;

    /**
     * Reads a TLV chain from the given block of TLV's. Calling this method is
     * equivalent to calling {@link #readChain(ByteBlock, int) readChain(block,
     * -1)}. The total number of bytes read can be read by calling the
     * <code>getTotalSize</code> method of the returned <code>TlvChain</code>.
     *
     * @param block the data block containing zero or more TLV's
     * @return a TLV chain object containing TLV's read from the given block of
     *         data
     */
    public static TlvChain readChain(ByteBlock block) {
        return readChain(block, -1);
    }

    /**
     * Reads a TLV chain from the given block of TLV's, stopping after reading
     * the number of TLV's specified by <code>maxTlvs</code>. If
     * <code>maxTlvs</code> is <code>-1</code>, all possible TLV's are read. The
     * total number of bytes read can be read by calling the
     * <code>getTotalSize</code> method of the returned <code>TlvChain</code>.
     *
     * @param block block the data block containing zero or more TLV's
     * @param maxTlvs the maximum number of TLV's to read, or <code>-1</code> to
     *        read all possible TLV's in the given block
     * @return a TLV chain object containing TLV's read from the given block of
     *         data
     */
    public static TlvChain readChain(ByteBlock block, int maxTlvs) {
        return new TlvChain(block, maxTlvs);
    }

    /**
     * Creates a TLV chain object from the given block, only reading the given
     * number of TLV's (specified by <code>maxTlvs</code>).
     *
     * @param block a data block containing zero or more TLV's
     * @param maxTlvs the maximum number of TLV's to read, or <code>-1</code> to
     *        read all possible TLV's in the given block
     */
    private TlvChain(ByteBlock block, int maxTlvs) {
        tlvList = new LinkedList();
        tlvMap = new HashMap();

        int start = block.getOffset();
        for (int i = 0; Tlv.isValidTLV(block)
                && (maxTlvs == -1 || i < maxTlvs); i++) {
            Tlv tlv = new Tlv(block);

            addTlvImpl(tlv);

            block = block.subBlock(tlv.getTotalSize());
        }

        totalSize = block.getOffset() - start;
    }

    /**
     * Creates a new, empty TLV chain.
     */
    protected TlvChain() {
        tlvList = new LinkedList();
        tlvMap = new HashMap();
        totalSize = -1;
    }

    /**
     * Creates a TLV chain containing the same TLV's as the given chain, in the
     * same order.
     *
     * @param other a TLV chain to copy
     */
    protected TlvChain(TlvChain other) {
        tlvList = new LinkedList(other.tlvList);
        tlvMap = new HashMap(other.tlvMap);
        totalSize = other.totalSize;
    }

    /**
     * Adds a TLV to this chain. Presumably for use by mutable subclasses.
     *
     * @param tlv a TLV to add to this chain
     */
    protected void addTlvImpl(Tlv tlv) {
        tlvList.add(tlv);

        Integer type = new Integer(tlv.getType());
        List siblings = (List) tlvMap.get(type);

        if (siblings == null) {
            siblings = new LinkedList();
            tlvMap.put(type, siblings);
        }

        siblings.add(tlv);
    }

    /**
     * Returns <code>true</code> if this TLV chain contains any TLV's of the
     * given TLV type.
     *
     * @param type a TLV type
     * @return whether or not this chain contains one or more TLV's of the given
     *         type
     */
    public boolean hasTlv(int type) {
        return tlvMap.containsKey(new Integer(type));
    }

    /**
     * Returns an array of all TLV's in this chain, in order.
     *
     * @return all of this chain's TLV's
     */
    public Tlv[] getTlvs() {
        return (Tlv[]) tlvList.toArray(new Tlv[0]);
    }

    /**
     * Returns the number of TLV's in this chain.
     *
     * @return the number of TLV's in this chain
     */
    public int getTlvCount() {
        return tlvList.size();
    }

    /**
     * Returns the first TLV in this chain with the given type, or
     * <code>null</code> of TLV of the given type is present.
     *
     * @param type the type of TLV whose first match will be returned
     * @return the first TLV in this chain with the given type, or
     *         <code>null</code> if none was found
     */
    public Tlv getFirstTlv(int type) {
        Integer typeNum = new Integer(type);

        List list = (List) tlvMap.get(typeNum);
        return list == null ? null : (Tlv) list.get(0);
    }

    /**
     * Returns the last TLV in this chain with the given type, or
     * <code>null</code> of TLV of the given type is present.
     *
     * @param type the type of TLV whose last match will be returned
     * @return the last TLV in this chain with the given type, or
     *         <code>null</code> if none was found
     */
    public Tlv getLastTlv(int type) {
        Integer typeNum = new Integer(type);

        List list = (List) tlvMap.get(typeNum);
        return list == null ? null : (Tlv) list.get(list.size() - 1);
    }

    /**
     * Returns an array containing all TLV's in this chain with the given TLV
     * type, with original order preserved. Note that if there are no matches
     * this will return a zero-length array and not <code>null</code>.
     *
     * @param type the type of TLV whose matching TLV's will be returned
     * @return a list of the TLV's in this chain with the given type
     */
    public Tlv[] getTlvs(int type) {
        Integer typeNum = new Integer(type);

        List list = (List) tlvMap.get(typeNum);
        if (list == null) {
            return new Tlv[0];
        }
        else {
            return (Tlv[]) list.toArray(new Tlv[0]);
        }
    }

    /**
     * Returns the ASCII string contained in the <i>last</i> TLV in this chain
     * with the given type, or <code>null</code> if no TLV with the given type
     * is present in this chain. Equivalent to <code>chain.hasTlv(type) ?
     * chain.getLastTlv(type).getDataAsString() : null</code>.
     *
     * @param type the type of TLV whose ASCII string value will be returned
     * @return the ASCII string stored in the value of the last TLV in this
     *         chain that has the given TLV type
     * @see #getLastTlv
     * @see Tlv#getDataAsString
     */
    public String getString(int type) {
        return hasTlv(type) ? getLastTlv(type).getDataAsString() : null;
    }

    /**
     * Returns the string contained in the <i>last</i> TLV in this chain
     * with the given type, decoded with the given charset, or <code>null</code>
     * if no TLV with the given type is present in this chain or if the given
     * charset is not supported in this JVM.
     *
     * @param type the type of TLV whose string value will be returned
     * @return the ASCII string stored in the value of the last TLV in this
     *         chain that has the given TLV type
     * @see #getLastTlv
     */
    public String getString(int type, String charset) {
        if (!hasTlv(type)) return null;

        final byte[] stringArray = getLastTlv(type).getData().toByteArray();
        String value = null;
        try {
            value = new String(stringArray, charset);
        } catch (UnsupportedEncodingException ignored) { }

        return value;
    }

    /**
     * Returns an unsigned two-byte integer read from the value of the
     * <i>last</i> TLV of the given type in this chain, or <code>-1</code> if
     * either no TLV of the given type is present in this chain or if the data
     * block for the TLV contains fewer than two bytes.
     *
     * @param type the type of the TLV whose value will be returned
     * @return the two-byte integer value stored in the last TLV of the given
     *         type, or <code>-1</code> if none is present
     * @see #getLastTlv
     * @see Tlv#getDataAsUShort
     */
    public int getUShort(int type) {
        return hasTlv(type) ? getLastTlv(type).getDataAsUShort() : -1;
    }

    /**
     * Returns the total size, in bytes, of this chain, as read from an incoming
     * stream. Will be <code>-1</code> if this chain was not read from a stream.
     *
     * @return the total size, in bytes, of this chain
     */
    public int getTotalSize() {
        return totalSize;
    }

    public long getWritableLength() {
        int sum = 0;
        for (Iterator it = tlvList.iterator(); it.hasNext();) {
            sum += ((Tlv) it.next()).getWritableLength();
        }
        return sum;
    }

    public void write(OutputStream out) throws IOException {
        for (Iterator it = tlvList.iterator(); it.hasNext();) {
            Tlv tlv = (Tlv) it.next();
            tlv.write(out);
        }
    }

}
