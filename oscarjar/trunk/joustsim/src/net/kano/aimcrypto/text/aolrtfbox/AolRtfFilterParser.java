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
 *  File created by keith @ Feb 28, 2004
 *
 */

package net.kano.aimcrypto.text.aolrtfbox;

import net.kano.joscar.DefensiveTools;

import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Users of this class cannot expect the <code>pos</code> argument of any
 * callback to be correct. {@link
 * broken }
 */
public class AolRtfFilterParser extends ParserDelegator {
    private final HTMLEditorKit.Parser realParser;

    public AolRtfFilterParser(HTMLEditorKit.Parser realParser) {
        DefensiveTools.checkNull(realParser, "realParser");

        this.realParser = realParser;
    }

    public void parse(Reader r, final HTMLEditorKit.ParserCallback cb,
            boolean ignoreCharSet) throws IOException {
        RecordingFilterReader reader = new RecordingFilterReader(r);
        AolRtfFilterCallback callback = new AolRtfFilterCallback(cb, reader);
        realParser.parse(reader, callback, ignoreCharSet);
    }

    private static class AolRtfFilterCallback
            extends HTMLEditorKit.ParserCallback {

        private final HTMLEditorKit.ParserCallback realCallback;
        private final RecordingFilterReader filterReader;
        /**
         * The position of the space up to (and including) which spaces were
         * filled in.
         */
        private int filledUntil = 0;
        private StringBuffer toPrepend = new StringBuffer(10);
        private Map implieds = new HashMap();
        private int lastPos = 0;

        public AolRtfFilterCallback(HTMLEditorKit.ParserCallback cb,
                RecordingFilterReader filterReader) {
            DefensiveTools.checkNull(cb, "cb");
            DefensiveTools.checkNull(filterReader, "filterReader");

            this.realCallback = cb;
            this.filterReader = filterReader;
        }

        public void flush() throws BadLocationException {
            flushPrepend(lastPos);
            realCallback.flush();
        }

        public void handleComment(char[] data, int pos) {
            checkSpaces(pos);
            registerLastPos(pos);
            realCallback.handleComment(data, pos);
        }

        public void handleText(char[] stripped, int pos) {
            int realpos = filterReader.getIndexOfEarliestSpaceBefore(pos);
            realpos = Math.min(pos, Math.max(realpos, filledUntil));
            registerLastPos(realpos);
            int extra = pos - realpos;
            int skip = 0;
            // the parser parses things like <br /> as "\n>", so we strip out
            // >'s if we can see that it wasn't a &gt; (by looking at the
            // original text at that position)
            if (stripped[0] == '>') {
                if (filterReader.getCharAt(pos) == '>' && realpos == pos) {
                    skip++;
                }
            }
            String real = filterReader.getBufferFrom(realpos);
            char[] realChars = real.toCharArray();

            int total = real.length() + extra;
            StringBuffer out = new StringBuffer(total);
            flushPrepend(pos);

            List newlines = null;
            int i = skip, j = skip;
            for (; i < extra; i++) {
                char ch = realChars[i];
                assert ch == ' ' || ch == '\n';
                if (ch == '\n') {
                    if (newlines == null) newlines = new ArrayList(5);
                    newlines.add(new Integer(out.length()));

                } else {
                    out.append(' ');
                }
            }
            for (; j < stripped.length; j++) {
                char s = stripped[j];

                boolean isSpace = s == ' ';
                if (isSpace) {
                    while (i < realChars.length && realChars[i] != ' ') {
                        if (realChars[i] == '\n') {
                            if (newlines == null) newlines = new ArrayList(5);
                            newlines.add(new Integer(out.length()));
                        }
                        i++;
                    }
                    while (i < realChars.length && (realChars[i] == ' '
                            || realChars[i] == '\n')) {
                        if (realChars[i] == '\n') {
                            if (newlines == null) newlines = new ArrayList(5);
                            newlines.add(new Integer(out.length()));
                        } else {
                            out.append(' ');
                        }
                        i++;
                    }
                } else {
                    out.append(s);
                }
            }

            filledUntil = realpos+i;
            filterReader.clearUntil(realpos+i);
            if (newlines != null) {
                int start = 0;
                for (Iterator it = newlines.iterator(); it.hasNext();) {
                    int off = ((Integer) it.next()).intValue();
                    if (start != off) {
                        String sub = out.substring(start, start+off);
                        char[] chars = sub.toCharArray();
                        realCallback.handleText(chars, realpos+start);
                        start = off;
                    }
                    int brpos = realpos+start;
                    synthesizeBR(brpos);
                }
                if (start != out.length()) {
                    char[] chars = out.substring(start).toCharArray();
                    realCallback.handleText(chars, realpos+start);
                }

            } else {
                realCallback.handleText(out.toString().toCharArray(), realpos);
            }
        }

        private void synthesizeBR(int pos) {
            registerLastPos(pos);
            SimpleAttributeSet attr = new SimpleAttributeSet();
            attr.addAttribute(IMPLIED, Boolean.TRUE);
            realCallback.handleSimpleTag(HTML.Tag.BR, attr, pos);
        }

        public void handleEndOfLineString(String eol) {
            realCallback.handleEndOfLineString(eol);
        }

        public void handleError(String errorMsg, int pos) {
            registerLastPos(pos);
            realCallback.handleError(errorMsg, pos);
        }

        public void handleEndTag(HTML.Tag t, int pos) {
            registerLastPos(pos);
            checkSpaces(pos);
            if (t == HTML.Tag.BODY) {
                checkFutureSpaces(pos);
            }
            if (t == HTML.Tag.BODY || t == HTML.Tag.HTML || wasLastImplied(t)) {
                checkImplied(t, null, pos);
            }
            realCallback.handleEndTag(t, pos);
        }

        private boolean wasLastImplied(HTML.Tag t) {
            return implieds.get(t) != Boolean.TRUE;
        }

        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            registerLastPos(pos);
            checkSpaces(pos);
            checkImplied(t, a, pos);
            realCallback.handleSimpleTag(t, a, pos);
        }

        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            registerLastPos(pos);
            checkSpaces(pos);
            checkImplied(t, a, pos);
            Boolean implied = (Boolean) a.getAttribute(IMPLIED);
            implied = implied == null ? Boolean.FALSE : implied;
            implieds.put(t, implied);
            realCallback.handleStartTag(t, a, pos);
        }

        private void registerLastPos(int pos) {
            lastPos = pos;
        }

        private void checkImplied(HTML.Tag t, MutableAttributeSet a, int pos) {
            if (a == null || !a.isDefined(IMPLIED)) {
                flushPrepend(pos);
            }
        }

        private void flushPrepend(int pos) {
            int length = toPrepend.length();
            if (length == 0) return;

            StringBuffer out = new StringBuffer(length);
            int start = 0;
            for (int i = 0; i < length; i++) {
                char c = toPrepend.charAt(i);
                if (c == '\n') {
                    int outlen = out.length();
                    if (outlen > 0) {
                        char[] chars = out.toString().toCharArray();
                        realCallback.handleText(chars, pos+start);
                        start += outlen;
                        out.delete(0, outlen);
                    }

                    synthesizeBR(pos+start);
                } else {
                    out.append(c);
                }
            }
            if (out.length() > 0) {
                char[] chars = out.toString().toCharArray();
                realCallback.handleText(chars, pos+start);
            }
            toPrepend.delete(0, length);
        }

        private void checkFutureSpaces(int pos) {
            int adjpos = Math.max(pos, filledUntil);
            int last = filterReader.getIndexOfLatestSpaceStartingWith(adjpos);
            if (last == -1) return;
            String sub = filterReader.substring(pos, last+1);
            toPrepend.append(sub);
        }

        private void checkSpaces(int pos) {
            if (pos > filledUntil) {
                int realpos = filterReader.getIndexOfEarliestSpaceBefore(pos);
                realpos = Math.max(realpos, filledUntil);
                filledUntil = pos;
                int extra = pos - realpos;
                if (extra == 0) return;

                toPrepend.append(filterReader.substring(realpos, pos));
            }
        }
    }
}
