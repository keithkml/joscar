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
 *  File created by keith @ Feb 22, 2004
 *
 */

package net.kano.aimcrypto.temps;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.ParagraphView;
import javax.swing.text.html.HTML;
import javax.swing.text.Document;
import javax.swing.text.ViewFactory;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.IconView;
import javax.swing.text.JTextComponent;
import javax.swing.text.Highlighter;
import javax.swing.SizeRequirements;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Component;

public class ConversationEditorKit extends HTMLEditorKit {
    public Document createDefaultDocument() {
        StyleSheet styles = getStyleSheet();
        StyleSheet ss = new StyleSheet();

        ss.addStyleSheet(styles);

        ConversationDocument doc = new ConversationDocument(ss);
        doc.setParser(getParser());
        doc.setAsynchronousLoadPriority(4);
        doc.setTokenThreshold(100);
        return doc;
    }

    public ViewFactory getViewFactory() {
        return new MyViewFactory(super.getViewFactory());
    }

    private static class MyViewFactory implements ViewFactory {
        private final ViewFactory parent;

        public MyViewFactory(ViewFactory parent) {
            this.parent = parent;
        }

        public View create(Element elem) {
            AttributeSet attr = elem.getAttributes();
            Object name = attr.getAttribute(StyleConstants.NameAttribute);
            if (name == HTML.Tag.P || name == HTML.Tag.IMPLIED) {
                Boolean val = (Boolean) attr.getAttribute(
                        ConversationDocument.ATTR_SPLIT_WORDS);
                if (val != null && val.booleanValue() == true) {
                    return new WordSplittingParagraphView(elem);
                }
            }

            return parent.create(elem);
        }
    }

    private static class WordSplittingParagraphView extends ParagraphView {
        public WordSplittingParagraphView(Element elem) {
            super(elem);
        }

        protected SizeRequirements calculateMinorAxisRequirements(int axis,
                SizeRequirements r) {
            SizeRequirements sup = super.calculateMinorAxisRequirements(axis, r);
            sup.minimum = 1;
            return sup;
        }
    }
}
