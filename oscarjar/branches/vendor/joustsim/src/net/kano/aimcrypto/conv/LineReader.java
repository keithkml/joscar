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

package net.kano.aimcrypto.conv;

import net.kano.joscar.DefensiveTools;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.CSS;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.util.List;
import java.util.LinkedList;

class LineReader extends HTMLEditorKit.ParserCallback {
    private final StyleSheet styles;

    private List elements = new LinkedList();
    private LinkedList attrs = new LinkedList();

    public LineReader(StyleSheet styles) {
        DefensiveTools.checkNull(styles, "styles");

        this.styles = styles;

        push(styles.getEmptySet());
    }

    public LineElement[] getElements() {
        return (LineElement[])
                elements.toArray(new LineElement[elements.size()]);
    }

    private AttributeSet getCurrentAttr() {
        return (AttributeSet) attrs.getLast();
    }

    private void pop() {
        if (attrs.size() > 1) attrs.removeLast();
    }

    private void push(AttributeSet attr) {
        attrs.addLast(attr);
    }

    public void handleText(char[] data, int pos) {
        //TODO: manually handle spaces
        elements.add(new TextElement(new String(data), getCurrentAttr()));
    }

    public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        boolean endtag = a.isDefined(HTML.Attribute.ENDTAG);
        if (t == HTML.Tag.HR && !endtag) {
            elements.add(new RuleElement());
        } else if (t == HTML.Tag.IMG && !endtag) {
            //TODO: add image element
        } else if (t == HTML.Tag.BR && !endtag) {
            elements.add(new BreakElement());
        }
    }

    public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        if (t == HTML.Tag.B || t == HTML.Tag.STRONG) {
            pushCssAttr(CSS.Attribute.FONT_WEIGHT, "bold");

        } else if (t == HTML.Tag.I || t == HTML.Tag.EM) {
            pushCssAttr(CSS.Attribute.FONT_STYLE, "italic");

        } else if (t == HTML.Tag.U) {
            pushCssAttr(CSS.Attribute.TEXT_DECORATION, "underline");

        } else if (t == HTML.Tag.S || t == HTML.Tag.STRIKE) {
            pushCssAttr(CSS.Attribute.TEXT_DECORATION, "line-through");

        } else if (t == HTML.Tag.A) {
            AttributeSet attrs = styles.addAttribute(getCurrentAttr(),
                    StyleConstants.NameAttribute, t);
            attrs = styles.addAttribute(attrs, t, a.copyAttributes());
            pushAttrs(attrs);

        } else if (t == HTML.Tag.FONT) {
            MutableAttributeSet html = new SimpleAttributeSet(a);
            MutableAttributeSet css = new SimpleAttributeSet();
            styles.removeAttribute(css, HTML.Tag.IMPLIED);
            convertKey(html, HTML.Attribute.SIZE, css, CSS.Attribute.FONT_SIZE);
            convertKey(html, HTML.Attribute.FACE, css, CSS.Attribute.FONT_FAMILY);
            convertKey(html, HTML.Attribute.COLOR, css, CSS.Attribute.COLOR);
            convertKey(html, "back", css, CSS.Attribute.BACKGROUND_COLOR);
            css.addAttribute(t, html);
            pushAttrs(css);

        } else {
            pushNothing();
        }
    }

    private void pushNothing() {
        push(getCurrentAttr());
    }

    private void convertKey(MutableAttributeSet src, Object htmlkey,
            MutableAttributeSet dest, CSS.Attribute csskey) {
        Object val = src.getAttribute(htmlkey);
        if (val != null && val instanceof String) {
            styles.addCSSAttributeFromHTML(dest, csskey, (String) val);
            src.removeAttribute(htmlkey);
        }
    }

    private void pushAttrs(AttributeSet attrs) {
        push(styles.addAttributes(getCurrentAttr(), attrs));
    }

    private void pushCssAttr(CSS.Attribute attr, String val) {
        AttributeSet current = getCurrentAttr();
        Object existingval = current.getAttribute(attr);
        String proval = val;
        if (existingval != null) {
            // this is risky, because the string representation, theoretically,
            // might not completely describe the existing value
            proval = existingval.toString() + ',' + val;
        }
        MutableAttributeSet newset = new SimpleAttributeSet(current);
        styles.addCSSAttribute(newset, attr, proval);
        push(newset);
    }

    public void handleEndTag(HTML.Tag t, int pos) {
        pop();
    }

    public void flush() {
        attrs = null;
    }
}
