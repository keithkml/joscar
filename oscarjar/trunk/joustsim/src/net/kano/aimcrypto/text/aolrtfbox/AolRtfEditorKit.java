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
 *  File created by keith @ Feb 27, 2004
 *
 */

package net.kano.aimcrypto.text.aolrtfbox;

import net.kano.aimcrypto.text.FontSizeTranslator;
import net.kano.aimcrypto.text.WinAimFontSizeTranslator;
import net.kano.joscar.DefensiveTools;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLWriter;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.parser.ParserDelegator;
import java.awt.Color;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AolRtfEditorKit extends HTMLEditorKit {
    private FontSizeTranslator fontSizeTranslator = new WinAimFontSizeTranslator();

    public synchronized FontSizeTranslator getFontSizeTranslator() {
        return fontSizeTranslator;
    }

    public synchronized void setFontSizeTranslator(FontSizeTranslator fontSizeTranslator) {
        this.fontSizeTranslator = fontSizeTranslator;
    }

    public Document createDefaultDocument() {
        StyleSheet styles = getStyleSheet();
        StyleSheet ss = new StyleSheet();

        ss.addStyleSheet(styles);

        AolRtfDocument doc = new AolRtfDocument(ss);
        doc.setParser(getParser());
        doc.setAsynchronousLoadPriority(4);
        doc.setTokenThreshold(100);
        return doc;
    }

    protected HTMLEditorKit.Parser getParser() {
        return new AolRtfTranslatingParser();
    }

//    public void read(Reader in, Document doc, int pos) throws IOException,
//            BadLocationException {
//        AolRtfDocument aolrtfdoc = (AolRtfDocument) doc;
//        Parser p = new AolRtfTranslatingParser(aolrtfdoc);
//        if (pos > aolrtfdoc.getLength()) {
//            throw new BadLocationException("Invalid location", pos);
//        }
//
//        ParserCallback receiver = aolrtfdoc.getReader(pos);
//        Boolean ignoreCharset = (Boolean) aolrtfdoc.getProperty(
//                "IgnoreCharsetDirective");
//
//        boolean ignoreCharsetBool;
//        if (ignoreCharset == null) ignoreCharsetBool = false;
//        else ignoreCharsetBool = ignoreCharset.booleanValue();
//
//        p.parse(in, receiver, ignoreCharsetBool);
//        receiver.flush();
//    }

    public void write(Writer out, Document doc, int pos, int len)
            throws IOException, BadLocationException {
        AolRtfDocument aolrtfdoc = (AolRtfDocument) doc;
        AolRtfWriter writer = new AolRtfWriter(out, aolrtfdoc, pos, len);
        writer.write();
    }

    private class AolRtfTranslatingParser extends HTMLEditorKit.Parser {
        private final Parser realParser = new ParserDelegator();

        public void parse(Reader r, ParserCallback cb, boolean ignoreCharSet)
                throws IOException {
            DefensiveTools.checkNull(cb, "cb");

            realParser.parse(r, new MyParserCallback(cb), ignoreCharSet);
        }

        private class MyParserCallback extends ParserCallback {
            private final ParserCallback realCallback;

            public MyParserCallback(ParserCallback realCallback) {
                DefensiveTools.checkNull(realCallback, "realCallback");

                this.realCallback = realCallback;
            }

            public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                if (!isHandledComplexTag(t)) return;

                SimpleAttributeSet nattr = new SimpleAttributeSet();
                if (t == HTML.Tag.BODY) {
                    copyAttribute(a, nattr, HTML.Attribute.BGCOLOR);
                    realCallback.handleStartTag(t, nattr, pos);

                } else if (isBypassedComplexTag(t)) {
                    realCallback.handleStartTag(t, nattr, pos);

                } else if (t == HTML.Tag.FONT) {
                    copyAttribute(a, nattr, HTML.Attribute.FACE);
                    copyAttribute(a, nattr, HTML.Attribute.COLOR);

                    StyleSheet sheet = getStyleSheet();
                    Object sizeVal = a.getAttribute(HTML.Attribute.SIZE);
                    if (sizeVal != null) {
                        String sizeStr = sizeVal.toString();
                        String translated = translateFontSize(sizeStr);
                        System.out.println("translated: " + translated);
                        if (translated == null) translated = sizeStr;
                        System.out.println("setting font size to " + translated);
                        sheet.addCSSAttribute(nattr,
                                CSS.Attribute.FONT_SIZE,
                                translated);
                    }

                    Object bgVal = a.getAttribute("back");
                    if (bgVal != null) {
                        sheet.addCSSAttributeFromHTML(nattr,
                                CSS.Attribute.BACKGROUND_COLOR,
                                bgVal.toString());
                    }

                    realCallback.handleStartTag(t, nattr, pos);

                } else if (t == HTML.Tag.A) {
                    copyAttribute(a, nattr, HTML.Attribute.HREF);
                    copyAttribute(a, nattr, HTML.Attribute.TARGET);

                    realCallback.handleStartTag(t, nattr, pos);

                } else if (t == HTML.Tag.P || t == HTML.Tag.IMPLIED) {
                    realCallback.handleStartTag(t, nattr, pos);

                } else {
                    assert false : "Tag " + t + " is marked as handled, but is "
                            + "not handled";
                }
            }

            private boolean isHandledComplexTag(HTML.Tag t) {
                return isBypassedComplexTag(t) || t == HTML.Tag.BODY
                        || t == HTML.Tag.FONT || t == HTML.Tag.A
                        || t == HTML.Tag.P || t == HTML.Tag.IMPLIED;
            }

            private String translateFontSize(String size) {
                FontSizeTranslator translator = getFontSizeTranslator();
                if (translator == null) return null;
                else return translator.getRealFromHtmlSize(size).toString();
            }

            private void copyAttribute(AttributeSet from,
                    MutableAttributeSet to, Object attrname) {
                Object bgc = from.getAttribute(attrname);
                if (bgc != null) {
                    to.addAttribute(attrname, bgc);
                }
            }

            private boolean isBypassedComplexTag(HTML.Tag t) {
                return t == HTML.Tag.B || t == HTML.Tag.I || t == HTML.Tag.U
                        || t == HTML.Tag.S || t == HTML.Tag.STRONG
                        || t == HTML.Tag.EM || t == HTML.Tag.STRIKE
                        || t == HTML.Tag.HTML || t == HTML.Tag.HEAD;
            }

            public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a,
                    int pos) {
                if (t == HTML.Tag.BR || t == HTML.Tag.HR) {
                    realCallback.handleSimpleTag(t, new SimpleAttributeSet(),
                            pos);
                }
                //TODO: handle images
            }

            public void handleEndTag(HTML.Tag t, int pos) {
                if (isHandledComplexTag(t)) {
                    realCallback.handleEndTag(t, pos);
                }
            }

            public void handleText(char[] data, int pos) {
                realCallback.handleText(data, pos);
            }

            public void handleEndOfLineString(String eol) {
                realCallback.handleEndOfLineString(eol);
            }

            public void handleError(String errorMsg, int pos) {
                realCallback.handleError(errorMsg, pos);
            }

            public void flush() throws BadLocationException {
                realCallback.flush();
            }
        }
    }

    private class AolRtfWriter extends HTMLWriter {
        public AolRtfWriter(Writer out, AolRtfDocument htmldoc, int pos, int len) {
            super(out, htmldoc, pos, len);
        }

        protected int getIndentSpace() {
            return 0;
        }

        protected boolean getCanWrapLines() {
            return false;
        }

        protected void writeAttributes(AttributeSet attr) throws IOException {
            Map fixed = new HashMap();
            writeStyleConstantsAttrs(attr, fixed);
            writeHtmlAttrs(attr, fixed);
            writeCssAttrs(attr, fixed);
            for (Iterator it = fixed.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();

                String key = (String) entry.getKey();
                String val = (String) entry.getValue();

                write(' ');
                write(key);
                write("=\"");
                output(val);
                write("\"");
            }
        }

        protected void startTag(Element elem) throws IOException,
                BadLocationException {
            if (getName(elem) == HTML.Tag.BODY) {
                String bg = (String) elem.getAttributes().getAttribute(HTML.Attribute.BGCOLOR);
                if (bg != null) {
                    write("<body bgcolor=\"");
                    output(bg);
                    write("\">");
                }
            }
        }

        protected void comment(Element elem) throws BadLocationException,
                IOException {
            // do nothing
        }

        protected void writeLineSeparator() throws IOException {
            setCurrentLineLength(0);
        }

        protected void emptyTag(Element elem) throws BadLocationException,
                IOException {
            Object name = getName(elem);
            if (name == HTML.Tag.BR || name == HTML.Tag.HR
                    || name == HTML.Tag.CONTENT) {
                super.emptyTag(elem);
            }
        }

        private Object getName(Element elem) {
            return elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
        }

        protected void endTag(Element elem) throws IOException {
            // do nothing
        }

        private void writeCssAttrs(AttributeSet attr, Map fixed) {
            Object fontFamily = attr.getAttribute(CSS.Attribute.FONT_FAMILY);
            if (fontFamily != null) fixed.put("face", fontFamily.toString());

            Object size = attr.getAttribute(CSS.Attribute.FONT_SIZE);
            if (size != null) fixed.put("size", convertFontSizeFromCss(size.toString()));

            Object fg = attr.getAttribute(CSS.Attribute.COLOR);
            if (fg != null) fixed.put("color", fg.toString());

            Object bg = attr.getAttribute(CSS.Attribute.BACKGROUND_COLOR);
            if (bg == null) {
                bg = attr.getAttribute(CSS.Attribute.BACKGROUND);
            }
            if (bg != null) fixed.put("back", bg.toString());
        }

        private String convertFontSizeFromCss(String s) {
            FontSizeTranslator trans = getFontSizeTranslator();
            if (trans == null) return s;
            else return trans.getHtmlFontSizeFromCss(s);
        }

        private void writeHtmlAttrs(AttributeSet attr, Map fixed) {
            String fontFamily = (String) attr.getAttribute(HTML.Attribute.FACE);
            if (fontFamily != null) fixed.put("face", fontFamily);

            String size = (String) attr.getAttribute(HTML.Attribute.SIZE);
            if (size != null) fixed.put("size", size);

            String fg = (String) attr.getAttribute(HTML.Attribute.COLOR);
            if (fg != null) fixed.put("color", fg);
        }

        private void writeStyleConstantsAttrs(AttributeSet attr, Map fixed) {
            String fontFamily = (String) attr.getAttribute(StyleConstants.FontFamily);
            if (fontFamily != null) fixed.put("face", fontFamily);

            Integer size = (Integer) attr.getAttribute(StyleConstants.FontSize);
            if (size != null) fixed.put("size", size);

            Color fg = (Color) attr.getAttribute(StyleConstants.Foreground);
            if (fg != null) fixed.put("color", colorToHex(fg));

            Color bg = (Color) attr.getAttribute(StyleConstants.Background);
            if (bg != null) fixed.put("back", colorToHex(bg));
        }

        private void output(String str) throws IOException {
            char[] chars = str.toCharArray();
            outputEscaped(chars);
        }

        /**
         * This method does not escape newlines as BR tags
         */
        private void outputEscaped(char[] chars) throws IOException {
            super.output(chars, 0, chars.length);
        }

        /**
         * This method escapes newlines as BR tags
         */
        protected void output(char[] chars, int start, int length)
                throws IOException {
            int off = start;
            for (int i = start; i < length; i++) {
                char aChar = chars[i];
                if (aChar == '\n') {
                    super.output(chars, off, i-off);
                    off = i + 1;
                    write("<br>");
                    System.err.println("escaping BR: ");
                    new Throwable().printStackTrace();
                }
            }
            super.output(chars, off, length - (off - start));
        }

        private String colorToHex(Color color) {
            StringBuffer buf = new StringBuffer(7);
            buf.append('#');

            appendHexValue(buf, color.getRed());
            appendHexValue(buf, color.getGreen());
            appendHexValue(buf, color.getBlue());

            return buf.toString();
        }

        private void appendHexValue(StringBuffer buf, int value) {
            String str = Integer.toHexString(value);
            int len = str.length();
            if (len > 2) str = str.substring(0, 2);
            if (len < 2) buf.append('0');
            buf.append(str);
        }
    }
}
