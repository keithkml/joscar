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
 *  File created by keith @ Feb 22, 2003
 *
 */

package net.kano.joscar;

import junit.framework.TestCase;

import java.io.*;
import java.net.Inet4Address;
import java.util.*;

public class SelfTest extends TestCase {
    public void testByteBlock() {
        byte[] array = new byte[] {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18
        };
        ByteBlock big = ByteBlock.wrap(array);

        assertEquals(0, big.get(0));
        assertEquals(18, big.get(18));
        assertEquals(19, big.getLength());
        assertEquals(19, big.getWritableLength());
        assertEquals(0, big.getOffset());

        try {
            big.get(19);
            fail("Should not allow indexing past end");
        } catch (IndexOutOfBoundsException e) { }

        try {
            big.get(-1);
            fail("Should not allow indexing before start");
        } catch (IndexOutOfBoundsException e) { }

        ByteBlock clone = ByteBlock.createByteBlock(big);
        ByteBlock copy = ByteBlock.wrap(big.toByteArray());

        assertTrue(big.equals(copy));
        assertTrue(big.equals(clone));

        assertTrue(Arrays.equals(array, big.toByteArray()));

        // this is legal!
        big.subBlock(19, 0);

        try {
            big.subBlock(19, 1);
            fail("Should not allow subblocking past end");
        } catch (IndexOutOfBoundsException e) { }

        try {
            big.subBlock(20, 0);
            fail("Should not allow subblocking past end");
        } catch (IndexOutOfBoundsException e) { }

        try {
            big.subBlock(-1, 0);
            fail("Should not allow subblocking before start");
        } catch (IndexOutOfBoundsException e) { }

        ByteBlock sub = big.subBlock(4, 5);

        assertEquals(5, sub.getLength());
        assertEquals(4, sub.getOffset());
        assertEquals(5, sub.get(1));

        try {
            sub.subBlock(6, 0);
            fail("Should not allow subblocking past end");
        } catch (IndexOutOfBoundsException e) { }

        try {
            sub.subBlock(-1, 0);
            fail("Should not allow subblocking before start");
        } catch (IndexOutOfBoundsException e) { }

        try {
            sub.subBlock(2, -1);
            fail("Should not allow negative subblock length");
        } catch (IndexOutOfBoundsException e) { }

        sub.subBlock(0, 0);

        ByteBlock sub2 = big.subBlock(2, 10);
        assertTrue(sub2.equals(ByteBlock.wrap(sub2.toByteArray())));

        ByteBlock.wrap(new byte[0]).subBlock(0).subBlock(0, 0);
    }

    public void testByteBlockSerialization() throws IOException, ClassNotFoundException {
        ByteBlock block = ByteBlock.wrap(new byte[] {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
        });
        ByteBlock subblock = block.subBlock(5, 5);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);

        oout.writeObject(block);
        oout.writeObject(subblock);

        ByteArrayInputStream bin = new ByteArrayInputStream(
                bout.toByteArray());

        ObjectInputStream oin = new ObjectInputStream(bin);

        ByteBlock big = (ByteBlock) oin.readObject();
        ByteBlock small = (ByteBlock) oin.readObject();

        assertEquals(big.get(0), 1);
        assertEquals(big.get(15), 16);
        assertEquals(big.getLength(), 16);
        assertEquals(big, block);

        assertEquals(small.get(0), 6);
        assertEquals(small.get(4), 10);
        assertEquals(small.getLength(), 5);
        assertEquals(small, subblock);

    }

    public void testBinaryTools() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            BinaryTools.writeLong(out,   90000);
            BinaryTools.writeUInt(out,   90000);
            BinaryTools.writeUShort(out, 90000);
            BinaryTools.writeUByte(out,  90000);
        } catch (IOException e) { }

        byte[] bytes = out.toByteArray();
        byte[] correct = new byte[] {
            0, 0, 0, 0, 0, 1, 95, -112, // long
            0, 1, 95, -112,             // int
            95, -112,                   // short
            -112                        // byte
        };

        assertTrue(Arrays.equals(bytes, correct));

        ByteBlock block = ByteBlock.wrap(bytes);

        assertEquals(90000, BinaryTools.getLong(block, 0));
        assertEquals(90000, BinaryTools.getUInt(block, 8));
        assertEquals(24464, BinaryTools.getUShort(block, 12));
        assertEquals(144, BinaryTools.getUByte(block, 14));

        assertTrue(Arrays.equals(BinaryTools.getUInt(4294967296L),
                new byte[] { 0, 0, 0, 0 }));

        assertEquals(Long.MAX_VALUE, BinaryTools.getLong(
                ByteBlock.wrap(BinaryTools.getLong(Long.MAX_VALUE)), 0));
        assertEquals(Long.MIN_VALUE, BinaryTools.getLong(
                ByteBlock.wrap(BinaryTools.getLong(Long.MIN_VALUE)), 0));

        assertEquals(4294967295L, BinaryTools.getUInt(
                ByteBlock.wrap(BinaryTools.getUInt(4294967295L)), 0));

        assertEquals(65535, BinaryTools.getUShort(
                ByteBlock.wrap(BinaryTools.getUShort(65535)), 0));

        String str = "Hello.";
        String result = BinaryTools.getAsciiString(ByteBlock.wrap(
                BinaryTools.getAsciiBytes(str)));

        assertEquals(str, result);

        byte[] ipBytes = new byte[] { 119, 40, 0, 2 };
        Inet4Address ip = BinaryTools.getIPFromBytes(ByteBlock.wrap(ipBytes),
                0);

        assertTrue(Arrays.equals(ipBytes, ip.getAddress()));
    }

//    public void testMultiCollection() {
//        List a = Arrays.asList(new String[] { "1", "2", "3", "4", "5" });
//        Set b = new LinkedHashSet(Arrays.asList(new String[] { "6", "7", "8" }));
//        List c = new LinkedList();
//        Stack d = new Stack();
//        d.push("9");
//        d.push("10");
//
//        Collection mc = new MultiCollection(new Collection[] { a, b, c, d });
//
//        assertEquals(10, mc.size());
//
//        List result = new ArrayList(mc);
//        assertEquals(Arrays.asList(new String[] {
//            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" }), result);
//    }
}
