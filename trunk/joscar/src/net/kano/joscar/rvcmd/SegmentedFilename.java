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
import java.util.Collection;
import java.util.ArrayList;

/**
 * A data structure containing a series of directory and file names ("segments")
 * that form a system-independent representation of a "file path." This
 * structure is used in file transfers to accommodate for sharing files between
 * operating systems.
 */
public final class SegmentedFilename {
    /** The file separator used in AIM file transfer. */
    private static final String FILESEP_FT = "\001";
    /** The file separator used by the current platform. */
    public static final String FILESEP_NATIVE
            = System.getProperty("file.separator");

    public SegmentedFilename(String... strings) {
        this(Arrays.asList(strings));
    }

    /**
     * Creates a <code>SegmentedFilename</code> from the given string whose
     * filenames are separated by the given separator string.
     *
     * @param path a path string whose path elements are separated by the given
     *        separator string
     * @param separator a separator string that separates the individual
     *        directory and file names in the given path
     * @return a <code>SegmentedFilename</code> generated from parsing the given
     *         path
     */
    private static SegmentedFilename createFromString(String path,
            String separator) {
        DefensiveTools.checkNull(path, "path");
        DefensiveTools.checkNull(separator, "separator");

        StringTokenizer strtok = new StringTokenizer(path, separator);

        List<String> parts = new LinkedList<String>();
        while (strtok.hasMoreTokens()) {
            String part = strtok.nextToken();
            parts.add(part);
        }

        return new SegmentedFilename(parts);
    }

    /**
     * Creates a <code>SegmentedFilename</code> from the given <i>native</i>
     * filename. Note that this method <i>will only produce a valid
     * <code>SegmentedFilename</code> for filenames on the platform on which
     * the JVM is currently running</i>, as it uses the JVM's
     * <code>file.separator</code> system propery.
     *
     * @param nativeFilename a native filename to be converted to a
     *        <code>SegmentedFilename</code>
     * @return a <code>SegmentedFilename</code> generated from the given native
     *         filename
     */
    public static SegmentedFilename fromNativeFilename(String nativeFilename) {
        return createFromString(nativeFilename, FILESEP_NATIVE);
    }

    /**
     * Creates a <code>SegmentedFilename</code> from the given "file transfer
     * filename." A "file transfer filename" is simply a filename in the format
     * <code>"\001home\001keith\001dev\001joust\001Joust.ipr"</code>, where
     * <code>\001</code> is the ASCII character with the value
     * <code>0x01</code>.
     *
     * @param ftFilename a "file transfer filename" to convert to a
     *        <code>SegmentedFilename</code>
     * @return a <code>SegmentedFilename</code> generated from the given "file
     *         transfer filename"
     */
    public static SegmentedFilename fromFTFilename(String ftFilename) {
        return createFromString(ftFilename, FILESEP_FT);
    }

    /**
     * The component "parts" or "segments" of this segmented filename object.
     */
    private final List<String> parts;

    /**
     * Creates a new <code>SegmentedFilename</code> with the given segmented
     * filename as its "parent" and the given file or directory name as the last
     * "segment." Using this constructor is equivalent to using
     * {@link #SegmentedFilename(SegmentedFilename, SegmentedFilename) new
     * SegmentedFilename(parent, new SegmentedFilename(new String[] { file }))}.
     * <br>
     * <br>
     * Note that if <code>parent</code> is <code>null</code>, this method is
     * equivalent to using {@link #SegmentedFilename(Collection<String>) new
     * SegmentedFilename(new String[] { file })}. Also note that
     * <code>file</code> cannot be <code>null</code>.
     *
     * @param parent the "parent" <code>SegmentedFilename</code>
     * @param file a filename to be the last segment of the created segmented
     *        filename
     */
    public SegmentedFilename(SegmentedFilename parent, String file) {
        this(parent, new SegmentedFilename(Arrays.asList(file)));
    }

    /**
     * Creates a new <code>SegmentedFilename</code> that consists of the
     * segments in <code>parent</code> followed by the segments in
     * <code>child</code>. Note that if <code>parent</code> is
     * <code>null</code>, the created segmented filename will simply be a clone
     * of <code>child</code>.
     *
     * @param parent the "parent" segmented filename, or <code>null</code> for
     *        none
     * @param child the "child" segmented filename
     */
    public SegmentedFilename(SegmentedFilename parent,
            SegmentedFilename child) {
        DefensiveTools.checkNull(child, "file");

        if (parent == null) {
            parts = child.parts;
        } else {
            List<String> list = new ArrayList<String>(parent.parts.size()
                    + child.parts.size());
            list.addAll(parent.parts);
            list.addAll(child.parts);
            parts = DefensiveTools.getUnmodifiable(list);
        }
    }

    /**
     * Creates a <code>SegmentedFilename</code> containing the given list of
     * segments.
     *
     * @param parts the list of filename "segments" of which the created
     *        segmented filename should consist
     */
    public SegmentedFilename(Collection<String> parts) {
        this.parts = DefensiveTools.getSafeNonnullListCopy(parts, "parts");
    }

    /**
     * Returns an array containing the "segments" of which this segmented
     * filename consists.
     *
     * @return an array containing this segmented filename's component
     *         "segments"
     */
    public final List<String> getSegments() {
        return parts;
    }

    public final String getLastSegment() {
        return parts.isEmpty() ? null : parts.get(0);
    }

    /**
     * Converts this segmented filename to a string, separating segments in the
     * returned string with the given separator string.
     *
     * @param sep a string by which the individual segments should be separated
     *        in the returned string
     * @return a <code>String</code> containing the list of segments separated
     *         by the given separator
     */
    private String toFilename(String sep) {
        DefensiveTools.checkNull(sep, "sep");

        // we'll estimate the length as 16 letters per file/dir name
        StringBuffer buffer = new StringBuffer(parts.size()*16);

        boolean first = true;
        for (String part : parts) {
            if (part.length() == 0) continue;

            if (first) first = false;
            else buffer.append(sep);

            buffer.append(part);
        }

        return buffer.toString();
    }

    /**
     * Converts this segmented filename to a string, separating segments in the
     * returned string with the system's native file separator. This method will
     * always return a relative filename (that is, a filename which does not
     * start with a "/" or a "\").
     *
     * @return a <code>String</code> containing the filename as a relative native
     *         filename
     */
    public final String toNativeFilename() {
        return toFilename(FILESEP_NATIVE);
    }

    /**
     * Converts this segmented filename to a string, separating segments in the
     * returned string with the "file transfer filename" separator (character
     * <code>\001</code>).
     *
     * @return a <code>String</code> containing the filename as a "file transfer
     *         filename"
     */ 
    public final String toFTFilename() {
        return toFilename(FILESEP_FT);
    }

    public int hashCode() {
        int code = 0;
        for (String part : parts) code += 29 * part.hashCode();

        return code;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SegmentedFilename)) return false;

        return parts.equals(((SegmentedFilename) obj).parts);
    }

    public String toString() {
        return "SegmentedFilename: " + parts + " ("
                + toNativeFilename() + ")";
    }
}