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
 *  File created by keith @ Feb 22, 2003
 *
 */

package net.kano.joscar.tlv;

import junit.framework.TestCase;
import net.kano.joscar.ByteBlock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class SelfTest extends TestCase {
    public void testTlvs() {
        ByteBlock block = ByteBlock.wrap(new byte[] { 1, 3, 5, 7, 9 });
        Tlv a = new Tlv(9000000, block);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            a.write(out);
        } catch (IOException e) { }

        byte[] correct = new byte[] {
            84, 64, 0, 5, 1, 3, 5, 7, 9
        };

        assertTrue(Arrays.equals(out.toByteArray(), correct));

        assertFalse(Tlv.isValidTLV(ByteBlock.wrap(new byte[0])));
        assertFalse(Tlv.isValidTLV(ByteBlock.wrap(
                new byte[] { 1, 1, 0, 2, 1 })));

        assertTrue(Tlv.isValidTLV(ByteBlock.wrap(
                new byte[] { 0, 0, 0, 0, 0, 0})));
        assertTrue(Tlv.isValidTLV(ByteBlock.wrap(
                new byte[] { 1, 2, 0, 5, 1, 2, 3, 4, 5 })));

        assertEquals(10, Tlv.getUIntInstance(5, 10).getDataAsUInt());
        assertEquals(10, Tlv.getUShortInstance(5, 10).getDataAsUShort());

        Tlv tlv = new Tlv(ByteBlock.wrap(correct));
        assertEquals(21568, tlv.getType());
        assertEquals(5, tlv.getData().getLength());
        assertEquals(9, tlv.getTotalSize());
        assertEquals(16975111, tlv.getDataAsUInt());
        assertEquals(259, tlv.getDataAsUShort());

        new Tlv(ByteBlock.wrap(new byte[] { 0, 0, 0, 0 }));

        try {
            new Tlv(ByteBlock.wrap(new byte[] { 1, 2, 3 }));
            fail("Should not allow TLV less than 4 bytes");
        } catch (IllegalArgumentException e) { }

        try {
            new Tlv(ByteBlock.wrap(new byte[] { 1, 2, 0, 5, 0, 0, 0, 0 }));
            fail("Should not allow TLV with length < header");
        } catch (IllegalArgumentException e) { }
    }

    public void testTlvChainRead() {
        ByteBlock chainBlock = ByteBlock.wrap(new byte[] {
            0, 2, 0, 4, 1, 2, 3, 4,
            0, 6, 0, 2, 60, 62,
            0, 7, 0, 5, 5, 6, 7, 8,
        });
        TlvChain big = TlvChain.readChain(chainBlock);

        assertEquals(14, big.getTotalSize());
        assertEquals(2, big.getTlvCount());

        assertTrue(ByteBlock.wrap(new byte[] { 1, 2, 3, 4 })
                .equals(big.getFirstTlv(2).getData()));
        assertTrue(ByteBlock.wrap(new byte[] { 60, 62 })
                .equals(big.getLastTlv(6).getData()));

        assertEquals(258, big.getUShort(2));
        assertTrue("<>".equals(big.getString(6)));

        assertNull(big.getFirstTlv(100));
        assertNull(big.getLastTlv(100));
        assertNull(big.getString(-1));
        assertEquals(-1, big.getUShort(-1));

        Tlv[] tlvs = big.getTlvs();
        assertEquals(2, tlvs.length);
        assertEquals(big.getTlvCount(), tlvs.length);
        assertEquals(2, tlvs[0].getType());
        assertEquals(6, tlvs[1].getType());

        Tlv[] type2 = big.getTlvs(2);
        assertEquals(1, type2.length);
        assertEquals(2, type2[0].getType());


        TlvChain firstOfBig = TlvChain.readChain(chainBlock, 1);

        assertEquals(1, firstOfBig.getTlvCount());
        assertEquals(2, firstOfBig.getTlvs()[0].getType());
        assertTrue(ByteBlock.wrap(new byte[] { 1, 2, 3, 4 })
                .equals(firstOfBig.getFirstTlv(2).getData()));

        TlvChain emptyBlockChain
                = TlvChain.readChain(ByteBlock.wrap(new byte[0]), 0);

        assertEquals(0, emptyBlockChain.getTlvCount());

        TlvChain duplicates = TlvChain.readChain(ByteBlock.wrap(new byte[] {
            0, 1, 0, 2, 0, 100,
            0, 1, 0, 0,
            0, 1, 0, 5, 1, 2, 3, 4, 5,
        }));

        assertEquals(3, duplicates.getTlvCount());

        assertEquals(100, duplicates.getFirstTlv(1).getDataAsUShort());
        assertTrue(ByteBlock.wrap(new byte[] { 1, 2, 3, 4, 5})
                .equals(duplicates.getLastTlv(1).getData()));

        Tlv[] matches = duplicates.getTlvs(1);
        assertEquals(3, matches.length);
        assertEquals(100, matches[0].getDataAsUShort());
        assertEquals(0, matches[1].getData().getLength());
        assertEquals(5, matches[2].getData().getLength());

        TlvChain tooMany = TlvChain.readChain(ByteBlock.wrap(new byte[0]), 100);

        assertEquals(0, tooMany.getTlvCount());
        assertEquals(0, tooMany.getTlvs(100).length);

        TlvChain tooShort
                = TlvChain.readChain(ByteBlock.wrap(new byte[] { 1, 2 }));

        assertEquals(0, tooShort.getTlvCount());
    }

    public void testSimpleTlvChainCreate() {
        MutableTlvChain chain = new MutableTlvChain();

        chain.addTlv(new Tlv(2, ByteBlock.wrap(new byte[] { 10, 11 })));
        chain.addTlv(new Tlv(2, ByteBlock.wrap(new byte[0])));
        chain.addTlv(new Tlv(9, ByteBlock.wrap(new byte[] { -1 })));

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            chain.write(out);
        } catch (IOException e) { }

        byte[] array = out.toByteArray();

        byte[] correct = new byte[] {
            0, 2, 0, 2, 10, 11,
            0, 2, 0, 0,
            0, 9, 0, 1, -1,
        };

        assertTrue(Arrays.equals(correct, array));

        assertEquals(3, chain.getTlvCount());
        assertEquals(-1, chain.getUShort(2));
        assertEquals(2, chain.getFirstTlv(2).getType());

        assertNotSame(chain.getFirstTlv(2), chain.getLastTlv(2));
        assertEquals(0, chain.getLastTlv(2).getData().getLength());
        assertEquals(2, chain.getFirstTlv(2).getData().getLength());
    }

    public void testTlvMutation() {
        Tlv tlv1a = Tlv.getStringInstance(1, "1a");
        Tlv tlv1b = Tlv.getStringInstance(1, "1b");
        Tlv tlv1c = Tlv.getStringInstance(1, "1c");
        Tlv tlv2a = Tlv.getStringInstance(2, "2a");
        Tlv tlv3a = Tlv.getStringInstance(3, "3a");

        MutableTlvChain chain = new MutableTlvChain();

        chain.addTlv(tlv1a);
        chain.addTlv(tlv2a);

        assertTrue(Arrays.equals(new Tlv[] { tlv1a, tlv2a }, chain.getTlvs()));
        assertTrue(Arrays.equals(new Tlv[] { tlv1a }, chain.getTlvs(1)));

        chain.removeTlv(tlv1a);

        assertEquals(1, chain.getTlvCount());
        assertTrue(Arrays.equals(new Tlv[] { tlv2a }, chain.getTlvs()));
        assertEquals(0, chain.getTlvs(1).length);

        chain.addTlv(tlv2a);

        assertTrue(Arrays.equals(new Tlv[] { tlv2a, tlv2a }, chain.getTlvs()));
        assertTrue(Arrays.equals(new Tlv[] { tlv2a, tlv2a }, chain.getTlvs(2)));

        chain.removeTlvs(2);

        assertEquals(0, chain.getTlvs().length);
        assertEquals(0, chain.getTlvs(2).length);

        chain.addTlv(tlv1a);
        chain.addTlv(tlv3a);
        chain.addTlv(tlv1b);

        assertTrue(Arrays.equals(new Tlv[] { tlv1a, tlv3a, tlv1b },
                chain.getTlvs()));
        assertTrue(Arrays.equals(new Tlv[] { tlv1a, tlv1b }, chain.getTlvs(1)));

        chain.replaceTlv(tlv1c);

        assertTrue(Arrays.equals(new Tlv[] { tlv1c, tlv3a }, chain.getTlvs()));
        assertTrue(Arrays.equals(new Tlv[] { tlv1c }, chain.getTlvs(1)));

        chain.addTlv(tlv1a);

        assertTrue(Arrays.equals(new Tlv[] { tlv1c, tlv3a, tlv1a },
                chain.getTlvs()));
        assertTrue(Arrays.equals(new Tlv[] { tlv1c, tlv1a }, chain.getTlvs(1)));

        chain.removeTlv(tlv1c);

        assertTrue(Arrays.equals(new Tlv[] { tlv3a, tlv1a }, chain.getTlvs()));
        assertTrue(Arrays.equals(new Tlv[] { tlv1a }, chain.getTlvs(1)));
    }
}
