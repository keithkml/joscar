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

package net.kano.joscar.flap;

import junit.framework.TestCase;
import net.kano.joscar.ByteBlock;

public class SelfTest extends TestCase {
    public void testFlapHeader() {
        try {
            new FlapHeader(ByteBlock.wrap(new byte[0]));
            fail("Should not create flap header with empty data");
        } catch (IllegalArgumentException e) { }

        try {
            new FlapHeader(ByteBlock.wrap(
                    new byte[] { 1, 2, 3, 4, 5, 6 }));
            fail("Should not create flap header without 0x2a header");
        } catch (IllegalArgumentException e) { }

        FlapHeader header = new FlapHeader(
                ByteBlock.wrap(new byte[] { 0x2a, 9, 0, 120, 1, 2 }));

        assertEquals(9, header.getChannel());
        assertEquals(120, header.getSeqnum());
        assertEquals(258, header.getDataLength());
    }

    public void testFlapPacket() {
        FlapHeader header = new FlapHeader(
                ByteBlock.wrap(new byte[] { 0x2a, 9, 0, 120, 0, 3 }));
        FlapPacket packet = new FlapPacket(header,
                ByteBlock.wrap(new byte[] { 1, 2, 3 }));

        assertTrue(ByteBlock.wrap(new byte[] { 1, 2, 3})
                .equals(packet.getData()));

        try {
            new FlapPacket(header, ByteBlock.wrap(
                    new byte[] { 1, 2, 3, 4, 5, 6 }));
            fail("Should not accept flap packet of greater length than header");
        } catch (IllegalArgumentException e) { }

        try {
            new FlapPacket(header, ByteBlock.wrap(new byte[] { 1, 2 }));
            fail("Should not accept flap packet of smaller length than header");
        } catch (IllegalArgumentException e) { }

        try {
            new FlapPacket(null, ByteBlock.wrap(new byte[] { 1, 2 }));
            fail("Should not accept null header");
        } catch (NullPointerException e) { }

        try {
            new FlapPacket(header, null);
            fail("Should not accept null packet data");
        } catch (NullPointerException e) { }
    }
}
