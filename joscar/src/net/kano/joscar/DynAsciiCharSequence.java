/*
 *  Copyright (c) 2006, The Joust Project
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
 *  File created by keith @ Jan 18, 2006
 *
 */

package net.kano.joscar;

/**
 * A <code>CharSequence</code> that represents the contents of a block of binary
 * data as a sequence of single-byte characters. <br> <br> One should note that
 * each byte read is converted to a char via a simple cast, as in the following
 * code: <code>(char) data[i]</code>. There are probably charset issues, but it
 * should be safe to use when US-ASCII encoding is assumed for the data block.
 */
public class DynAsciiCharSequence implements CharSequence {
    /** The block of binary data that this character sequence represents. */
    private final ByteBlock data;
    private int length;

    /**
     * Creates a new character sequence representing the given block of binary
     * data.
     *
     * @param data the data that this character sequence should represent
     */
    public DynAsciiCharSequence(ByteBlock data) {
        DefensiveTools.checkNull(data, "data");

        this.data = data;
        length = data.getLength();
    }

    public void setLength(int length) {
        this.length = length;
    }

    public char charAt(int index) { return (char) data.get(index); }

    public int length() { return length; }

    public DynAsciiCharSequence subSequence(int start, int end) {
        return new DynAsciiCharSequence(data.subBlock(start, end - start));
    }

    public String toString() { return BinaryTools.getAsciiString(data); }

    public boolean contains(CharSequence target) {
        return indexOf(target) != -1;
    }

    public int indexOf(CharSequence target) {
        return indexOf(target, 0);
    }

    /**
     * @param target    the characters being searched for
     * @param fromIndex the index to begin searching from
     */
    // This code was adapted from a package-private method in String.java in
    // the JDK.
    public int indexOf(CharSequence target, int fromIndex) {
        DefensiveTools.checkRange(fromIndex, "fromIndex", 0);
        if (fromIndex >= length()) {
            return (target.length() == 0 ? length() : -1);
        }
        if (target.length() == 0) {
            return fromIndex;
        }

        char first = target.charAt(0);
        int max = length();

        for (int i = fromIndex; i <= max; i++) {
            // Look for first character.
            if (charAt(i) != first) {
                while (++i <= max && charAt(i) != first) ;
            }

            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + target.length() - 1;
                for (int k = 1; j < end && charAt(j) ==
                        target.charAt(k); j++, k++) {
                    // the loop parameters do it all
                }

                if (j == end) {
                    // Found whole string.
                    return i;
                }
            }
        }
        return -1;
    }
}
