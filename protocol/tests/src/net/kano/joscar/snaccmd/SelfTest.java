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
 *  File created by keith @ Mar 1, 2003
 *
 */

package net.kano.joscar.snaccmd;

import junit.framework.TestCase;
import net.kano.joscar.MinimalEncoder;
import net.kano.joscar.OscarTools;

import java.util.Arrays;
import java.util.Map;

public class SelfTest extends TestCase {
    public void testMinimalEncoder() {
        MinimalEncoder me = new MinimalEncoder();

        assertSame(MinimalEncoder.ENCODING_ASCII, me.getCharset());

        me.update("hi");
        assertSame(MinimalEncoder.ENCODING_ASCII, me.getCharset());

        me.update(String.valueOf((char) 162));
        assertSame(MinimalEncoder.ENCODING_ISO, me.getCharset());

        me.update(String.valueOf((char) 0xfff));
        assertSame(MinimalEncoder.ENCODING_UTF16, me.getCharset());

        me.update("hello");
        assertSame(MinimalEncoder.ENCODING_UTF16, me.getCharset());


        me = new MinimalEncoder();

        me.updateAll(Arrays.asList("hi", "hello", "ok"));
        assertSame(MinimalEncoder.ENCODING_ASCII, me.getCharset());

        me.updateAll(Arrays.asList("ascii", "\u0fbfhi"));
        assertSame(MinimalEncoder.ENCODING_UTF16, me.getCharset());


        me = new MinimalEncoder();

        assertSame(MinimalEncoder.ENCODING_ASCII, me.getCharset());

        assertSame(MinimalEncoder.ENCODING_UTF16,
                me.encode("\u0fafhello").getCharset());

        assertSame(MinimalEncoder.ENCODING_UTF16, me.getCharset());
    }

    public void testCharsetParser() {
        Map<String,String> map = OscarTools.parseContentTypeString(
                "text/aolrtf; charset=\"us-ascii\"");

        assertEquals(2, map.size());
        assertTrue(map.containsKey("text/aolrtf"));
        assertSame(null, map.get("text/aolrtf"));
        assertEquals("us-ascii", map.get("charset"));

        map = OscarTools.parseContentTypeString("a  =b; c=;;;  ;=;;= d;");

        assertEquals(3, map.size());
        assertEquals("b", map.get("a"));
        assertEquals("", map.get("c"));
        assertTrue(map.containsKey("d"));
        assertSame(null, map.get("d"));

        map = OscarTools.parseContentTypeString("a=b=c=d");

        assertEquals(1, map.size());
        assertEquals("b=c=d", map.get("a"));

        map = OscarTools.parseContentTypeString("   a   =  \"  b \"");

        assertEquals(1, map.size());
        assertEquals("  b ", map.get("a"));
    }
}
