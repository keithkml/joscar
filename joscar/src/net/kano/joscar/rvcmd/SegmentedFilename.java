/*
 *  Copyright (c) 2003, The Joust Project
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
 *  File created by Keith @ 4:13:07 PM
 *
 */

package net.kano.joscar.rvcmd;

import net.kano.joscar.DefensiveTools;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public final class SegmentedFilename {
    private static final String FILESEP_FT = "\001";
    private static final String FILESEP_NATIVE
            = System.getProperty("file.separator");

    private static SegmentedFilename createFromString(String nativeFilename,
            String sep) {
        StringTokenizer strtok = new StringTokenizer(nativeFilename, sep);

        List parts = new LinkedList();
        while (strtok.hasMoreTokens()) {
            String part = strtok.nextToken();
            parts.add(part);
        }

        return new SegmentedFilename((String[]) parts.toArray(new String[0]));
    }

    public static SegmentedFilename createFromNativeFilename(
            String nativeFilename) {
        return createFromString(nativeFilename, FILESEP_NATIVE);
    }

    public static SegmentedFilename createFromFTFilename(String ftFilename) {
        return createFromString(ftFilename, FILESEP_FT);
    }

/*
    public static SegmentedFilename createFromFTFilename(
            ByteBlock filenameBlock, int charset, int charsubset) {
        // note that we loop *past* the end of the byte block so we can more
        // easily handle the end of the block (by treating it as an ^A).
        int fblen = filenameBlock.getLength();
        List fileParts = new LinkedList();
        for (int i = 0, wordstart = 0; i <= fblen; i++) {
            if (i == fblen || filenameBlock.get(i) == FT_FILE_SEPARATOR) {
                // we found the end of a filename part
                ByteBlock filenamePartBlock
                        = filenameBlock.subBlock(wordstart, i);

                // convert it to a string
                String filenamePart = ImEncodedString.readImEncodedString(
                        charset, charsubset, filenamePartBlock);

                fileParts.add(filenamePart);

                // and mark the character after this as where the next "word" or
                // filename part starts
                wordstart = i + 1;
            }
        }

        // and convert the list we made to an array of strings
        String[] partArray = (String[]) fileParts.toArray(new String[0]);

        return new SegmentedFilename(partArray);
    }

*/
    private final String[] parts;

    public SegmentedFilename(SegmentedFilename parent, String file) {
        this(parent, new SegmentedFilename(new String[] { file }));
    }

    public SegmentedFilename(SegmentedFilename parent,
            SegmentedFilename file) {
        DefensiveTools.checkNull(file, "file");

        if (parent == null) {
            parts = file.parts;
        } else {
            parts = new String[parent.parts.length + file.parts.length];
            System.arraycopy(parent.parts, 0, parts, 0, parent.parts.length);
            System.arraycopy(file.parts, 0, parts, parent.parts.length,
                    file.parts.length);
        }
    }


    public SegmentedFilename(String[] parts) {
        DefensiveTools.checkNull(parts, "parts");

        for (int i = 0; i < parts.length; i++) {
            DefensiveTools.checkNull(parts[i], "parts elements");
        }

        this.parts = (String[]) parts.clone();
    }

    public final String[] getFilenameParts() {
        return (String[]) parts.clone();
    }

    private final String toFilename(String sep) {
        // we'll estimate the length as 16 letters per file/dir name
        StringBuffer buffer = new StringBuffer(parts.length*16);

        for (int i = 0; i < parts.length; i++) {
            if (i != 0) buffer.append(sep);

            buffer.append(parts[i]);
        }

        return buffer.toString();
    }

    public final String toNativeFilename() {
        return toFilename(FILESEP_NATIVE);
    }

    public final String toFTFilename() {
        return toFilename(FILESEP_FT);
    }

    public String toString() {
        return "SegmentedFilename: " + Arrays.asList(parts) + " ("
                + toNativeFilename() + ")";
    }
}