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
 *  File created by keith @ Jan 22, 2004
 *
 */

package net.kano.aimcrypto.conv;

import net.kano.joscar.DefensiveTools;

import javax.swing.text.DefaultStyledDocument.ElementSpec;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Arrays;

public class AolRtfText {
    private static final char[] SPACE = new char[] { ' ' };

    public static AolRtfText readLine(String text) {
        return readLine(new StyleSheet(), text);
    }

    public static AolRtfText readLine(StyleSheet context, String text) {
        DefensiveTools.checkNull(context, "context");
        DefensiveTools.checkNull(text, "text");
        
        LineReader reader = new LineReader(context);
        ParserDelegator parser = new ParserDelegator();
        try {
            parser.parse(new StringReader(text), reader, false);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return new AolRtfText(text, reader.getElements());
    }

    private final String originalText;
    private final LineElement[] elements;

    public AolRtfText(String originalText, LineElement[] elements) {
        DefensiveTools.checkNull(originalText, "originalText");
        DefensiveTools.checkNull(elements, "elements");

        this.originalText = originalText;
        this.elements = elements;
    }

    public String getOriginalText() {
        return originalText;
    }

    public LineElement[] getElements() {
        return (LineElement[]) elements.clone();
    }

    public ElementSpec[] generateDocumentElements() {
        ElementSpec[] specs = new ElementSpec[elements.length];
        for (int i = 0; i < elements.length; i++) {
            specs[i] = getSpec(elements[i]);
        }
        return specs;
    }

    private static ElementSpec getSpec(LineElement element) {
        if (element instanceof TextElement) {
            TextElement te = (TextElement) element;
            char[] chars = te.getString().toCharArray();
            MutableAttributeSet attrs = new SimpleAttributeSet(te.getAttrs());
            if (!attrs.isDefined(StyleConstants.NameAttribute)) {
                attrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
            }
            return new ElementSpec(attrs, ElementSpec.ContentType,
                    chars, 0, chars.length);

        } else if (element instanceof BreakElement) {
            return getTagSpec(Tag.BR);

        } else if (element instanceof RuleElement) {
            return getTagSpec(Tag.HR);

        } else {
            return null;
        }
    }

    private static ElementSpec getTagSpec(Tag tag) {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        if (!attr.isDefined(StyleConstants.NameAttribute)) {
            attr.addAttribute(StyleConstants.NameAttribute, tag);
        }
        return new ElementSpec(attr, ElementSpec.ContentType, SPACE, 0, 1);
    }

    public static void main(String[] args) {
//        String html = "<html><body bgcolor=\"yo\"><font "
//                + "snarg=\"snarg\"><b>a<i><hr>b</hr></b>c</i> x < y? I thinkso<img "
//                + "snarg=\"garble\"></body></html>";
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StyleSheet ss = new StyleSheet();
        try {
            for (;;) {
                String line = br.readLine();
                if (line == null) break;
                AolRtfText convline = readLine(ss, line);
                System.out.println(Arrays.asList(convline.elements));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printChildren(Element el, int level) {
        for (int i = 0; i < level; i++) System.out.print(' ');

        System.out.println(el.getClass().getName() + ": " + el.toString().trim()
                + ": " + el.getAttributes().copyAttributes());
        for (int i = 0; i < el.getElementCount(); i++) {
            printChildren(el.getElement(i), level+1);
        }
    }
}