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

import java.io.FilterReader;
import java.io.Reader;
import java.io.IOException;

public class RecordingFilterReader extends FilterReader {
    private int started = 0;
    private final StringBuffer buffer = new StringBuffer(100);

    public RecordingFilterReader(Reader r) {
        super(r);
    }

    public int read() throws IOException {
        int ch = super.read();
        if (ch != -1) buffer.append(ch);
        return ch;
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        int chs = super.read(cbuf, off, len);
        if (chs > 0) buffer.append(cbuf, off, chs);
        return chs;
    }

    public String getBufferFrom(int pos) {
        return buffer.substring(getOffsetFromPosition(pos));
    }

    public int getIndexOfEarliestSpaceBefore(int pos) {
        int off = getOffsetFromPosition(pos);

        assert off <= buffer.length() : "off=" + off + ", len=" + buffer.length();

        while (off > 0) {
            char ch = buffer.charAt(off-1);
            if (ch != ' ' && ch != '\n') break;
            off--;
        }
        return off + this.started;
    }

    public int getIndexOfLatestSpaceStartingWith(int pos) {
        int off = getOffsetFromPosition(pos);

        boolean changed = false;
        int len = buffer.length();
        while (off < len) {
            char ch = buffer.charAt(off);
            if (ch != ' ' && ch != '\n') break;
            off++;
            changed = true;
        }
        if (!changed) return -1;

        return off - 1 + this.started;
    }

    public void clearUntil(int pos) {
        int off = getOffsetFromPosition(pos);

        assert off <= buffer.length() : "off=" + off + ", len=" + buffer.length();

        started = pos;
        buffer.delete(0, off);
    }

    private int getOffsetFromPosition(int pos) {
        return pos - started;
    }

    public char getCharAt(int pos) {
        return buffer.charAt(getOffsetFromPosition(pos));
    }

    public String substring(int from, int to) {
        return buffer.substring(getOffsetFromPosition(from),
                getOffsetFromPosition(to));
    }
}
