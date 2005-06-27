/*
 *  Copyright (c) 2004, The Joust Project
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
 *  File created by keith @ Jan 24, 2004
 *
 */

package net.kano.joustsim.text;

import junit.framework.TestCase;

import javax.swing.text.html.CSS;
import javax.swing.text.html.StyleSheet;

public class SelfTest extends TestCase {
    private LineElement[] getElements(String str) {
        StyleSheet ss = new StyleSheet();
        AolRtfString cl = AolRtfString.readLine(ss, str);
        LineElement[] elements = cl.getElements();
        return elements;
    }

    public void testSimpleLine() {
        LineElement[] elements = getElements("<b>test</b>");
        assertEquals(1, elements.length);
        TextElement textElement = (TextElement) elements[0];
        assertEquals("test", textElement.getString());
        assertCssEquals(textElement, CSS.Attribute.FONT_WEIGHT, "bold");
    }

    public void testRule() {
        LineElement[] elements = getElements("z<s>a<hr>bb<em>c");
        assertEquals(5, elements.length);
        TextElement z = (TextElement) elements[0];
        assertEquals("z", z.getString());
        TextElement a = (TextElement) elements[1];
        assertEquals("a", a.getString());
        assertCssEquals(a, CSS.Attribute.TEXT_DECORATION, "line-through");
        RuleElement hr = (RuleElement) elements[2];
        TextElement b = (TextElement) elements[3];
        assertEquals("b b", b.getString());
        TextElement c = (TextElement) elements[4];
        assertEquals("c", c.getString());
        assertCssEquals(c, CSS.Attribute.FONT_STYLE, "italic");
    }

    public void testSpacing() {
        LineElement[] elements = getElements(
                "  plain    <b>   bold &nbsp;</b>  p    l   a i n");
        assertEquals(3, elements.length);
        TextElement plain1 = (TextElement) elements[0];
        assertEquals(" plain ", plain1.getString());
        TextElement bold = (TextElement) elements[1];
        assertEquals(" bold  ", bold.getString());
        TextElement plain2 = (TextElement) elements[2];
        assertEquals(" p l a i n", plain2.getString());
    }

    private void assertCssEquals(TextElement a, CSS.Attribute key,
            String value) {
        assertEquals(value, a.getAttrs().getAttribute(key).toString());
    }
}
