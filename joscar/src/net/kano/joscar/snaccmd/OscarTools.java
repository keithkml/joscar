/*
 *  Copyright (c) 2002-2003, The Joust Project
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
 *  File created by keith @ Feb 21, 2003
 *
 */

package net.kano.joscar.snaccmd;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides a set of tools for performing OSCAR-specific functions.
 */
public final class OscarTools {
    /**
     * A private constructor that is never called ensures that this class cannot
     * be instantiated.
     */
    private OscarTools() { }

    /**
     * Reads a screenname which is preceded by a single byte describing the
     * screenname's length from the given block of data. Returns
     * <code>null</code> if no such valid object can be read.
     *
     * @param data the block containing a single-byte length followed by a
     *        screen name in US-ASCII format
     * @return an object containing the screenname and the total number of bytes
     *         read
     */
    public static StringBlock readScreenname(ByteBlock data) {
        if (data.getLength() < 1) return null;

        int length = BinaryTools.getUByte(data, 0);

        if (length > data.getLength() - 1) return null;

        String sn = BinaryTools.getAsciiString(data.subBlock(1, length));

        return new StringBlock(sn, length + 1);
    }

    /**
     * Writes the given screenname to the given stream, preceded by a single
     * byte containing the screenname's length in bytes.
     *
     * @param out the stream to write to
     * @param sn the screenname to write to the given stream
     * @throws IOException if an I/O error occurs
     */
    public static void writeScreenname(OutputStream out, String sn)
            throws IOException {
        byte[] bytes = BinaryTools.getAsciiBytes(sn);

        BinaryTools.writeUByte(out, bytes.length);
        out.write(bytes);
    }

    /**
     * A regular expression for processing AIM info content type strings. See
     * {@link #parseContentTypeString parseContentTypeString} for details.
     */
    private static final Pattern typePattern = Pattern.compile(
            "[;=\\s]*+" + // any leading semicolons, equals signs, and space
            "(\\S+?)" + // the key name, without spaces (like "charset")
            "\\s*" + // any whitespace after the key name
            "(?:=\\s*" + // an equals sign, followed by possible whitespace
            "(?:" +
            "\"(.*?)\"" + // a value after the equals sign, in quotes
            "|(\\S*?)" + // a single-word value not in quotes
            ")" +
            "\\s*" + // any more whitespace after the value
            ")?" +
            "(?:" +
            "[=\\s]*;[=\\s]*" + // a semicolon surrounded by whitespace or
                                // stray equals signs
            "|\\z" + // or the end of the input
            ")");

    /**
     * Converts a string like <code>text/x-aolrtf;
     * charset=us-ascii</code> to a <code>Map</code> with two keys:
     * <code>text/x-aolrtf</code> (value <code>null</code>) and
     * <code>charset</code> (value <code>us-ascii</code>).
     *
     * @param  str the content type string
     * @return     a map with keys and values extracted from the given string
     */
    public static Map parseContentTypeString(String str) {
        // create a fun matcher
        Matcher matcher = typePattern.matcher(str);

        // and a map to store keys/values in
        Map entries = new HashMap();

        // and get all the matches..
        while (matcher.find()) {
            // the first group is \S+?, or the first nonwhitespace string before
            // an equals sign or a semicolon or the end of the string. so "key"
            // in "key=value;"
            String key = matcher.group(1);

            // the second group is .*?, or anything inside quotes. note that
            // this does not allow for anything like backslashed quotes.
            String value = matcher.group(2);

            if (value == null) {
                // there was no quoted value, so look for an unquoted value.
                // if this is null we don't care, because it means they're both
                // null and there's no value for this key.
                value = matcher.group(3);
            }

            // and put the key & value into the map!
            entries.put(key, value);
        }

        return entries;
    }

    /** A regular expression that only matches valid names for charsets. */
    private static final Pattern charsetPattern
            = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-.:_]*");

    /**
     * Returns <code>true</code> if the given charset name is a valid charset
     * name. Note that this method does not check to see whether the given
     * charset is available; rather it just checks that the name is in the
     * correct format.
     *
     * @param charset the name of the charset to check
     * @return whether the given charset is in a valid charset name format
     */
    private static boolean isValidCharset(String charset) {
        return charsetPattern.matcher(charset).matches();
    }

    /** A regular expression that matches the name of a UCS-2 charset. */
    private static final Pattern ucsPattern = Pattern.compile("ucs-2([bl]e)?");

    /**
     * Returns a valid, hopefully compatible charset name from the given charset
     * name. For example, attempts to convert such names as "unicode-2.0" to
     * "UTF-16BE". The returned charset name is guaranteed to be supported by
     * the JVM; that is, the returned charset can always be used to encode
     * without further processing.
     *
     * @param charset the possibly invalid charset, or <code>null</code>
     * @return a valid charset derived from the given charset name
     */
    private static String fixCharset(String charset) {
        if (charset == null) return "US-ASCII";

        // sigh. ok, first attempt to hax0r unicode 2.0, since java doesn't
        // support it yet, and iChat, well, does.
        String lower = charset.toLowerCase();
        if (lower.equals("unicode-2-0")) {
            // we think unicode-2.0's default encoding might just be UTF-16.
            return "UTF-16BE";
        }
        if (lower.startsWith("unicode-2-0-")) {
            // the charset is "unicode-2-0-SOMECHARSET", so let's extract
            // SOMECHARSET and hope it works
            String newCharset = charset.substring(12);

            if (isValidCharset(newCharset) && Charset.isSupported(newCharset)) {
                // this is a valid charset!
                return newCharset;
            }
        }

        // okay, none of those were true. check for UCS-2, which is just UTF-16
        Matcher matcher = ucsPattern.matcher(charset);
        if (matcher.matches()) {
            // it's UCS-2! get the type, LE or BE (or null if neither)
            String type = matcher.group(1);

            // and build the corresponding UTF-16 type
            String newCharset = "utf-16";
            if (type != null) newCharset += type;

            // and return it
            return newCharset;
        }

        // okay. none of those worked. let's use ascii. :/
        return "US-ASCII";
    }

    /**
     * Returns a string given its binary representation and one of AIM's
     * <code>text/x-aolrtf; charset=us-ascii</code> content-type strings.
     *
     * @param infoData the binary representation of the string
     * @param infoType an AIM content-type string
     * @return a string decoded from the given byte block and the charset
     *         that might be specified in the given content-type string
     */
    public static String getInfoString(ByteBlock infoData, String infoType) {
        // declare this up here
        String charset;

        if (infoType != null) {
            // get a content type map
            Map type = parseContentTypeString(infoType);

            // extract the charset, if there is one
            charset = (String) type.get("charset");

            charset = getValidCharset(charset);
        } else {
            // there's no encoding given! so just use ASCII.
            charset = "US-ASCII";
        }

        try {
            // okay, finally, decode the data
            return ByteBlock.createString(infoData, charset);
        } catch (UnsupportedEncodingException impossible) { return null; }
    }

    /**
     * Returns the given charset if it is supported by the JVM; if it is not
     * supported, attempts to fix it and returns the "fixed" version. This
     * method will always return the name of a charset that can be used within
     * this JVM. Note that if <code>charset</code> is <code>null</code>,
     * <code>"US-ASCII"</code> will be returned.
     *
     * @param charset the charset name to "fix"
     * @return either the given charset name or a valid charset name derived
     *         from the given name
     */
    public static String getValidCharset(String charset) {
        // use US-ASCII if there's no charset or if the name isn't a valid
        // charset name according to the isSupported javadoc (if there's a
        // method like isValidCharsetName(), please someone email me, but
        // I'm pretty sure there isn't)
        if (charset == null || !isValidCharset(charset)) {
            charset = fixCharset(charset);
        } else {
            try {
                if (!Charset.isSupported(charset)) {
                    // if this character set isn't supported, try some other
                    // stuff
                    charset = fixCharset(charset);
                }
            } catch (IllegalCharsetNameException e) {
                // this shouldn't happen, so be very loud and angry about it
                System.err.println("charset=" + charset);
                e.printStackTrace(System.err);

                // and default to ASCII
                charset = fixCharset(charset);
            }
        }
        return charset;
    }

    /**
     * Creates a <code>String</code> from the given block of data and the given
     * charset. Note that this will <i>never</i> return <code>null</code>, even
     * if the given <code>charset</code> is <code>null</code>. This method will
     * do its best to produce a <code>String</code> from the given data using
     * {@link #getValidCharset getValidCharset}.
     *
     * @param data a block of data containing a string
     * @param charset the name of a charset to use to extract a string from the
     *        given data, or <code>null</code> for US-ASCII
     * @return a <code>String</code> decoded from the given block of data
     */
    public static String getString(ByteBlock data, String charset) {
        charset = getValidCharset(charset);

        try {
            return ByteBlock.createString(data, charset);
        } catch (UnsupportedEncodingException impossible) { return null; }
    }

    /**
     * Returns a "normalized" version of the given string by removing all spaces
     * and converting to lowercase. Several aspects of the AIM protocol are
     * "normalized": an IM to "joustacular" is the same as an IM to "Joust
     * Acular". Similarly, joining the chat room "JoUsTeRrIfIc" is equivalent
     * to joining "Jousterrific".
     *
     * @param str the string to normalize
     * @return a normalized version of the given string
     */
    public static String normalize(final String str) {
        final StringBuffer buffer = new StringBuffer(str.length());

        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);

            if (c != ' ') buffer.append(Character.toLowerCase(c));
        }

        return buffer.toString();
    }

    /** A poor regular expression to match an HTML tag. */
    private static final Pattern htmlRE = Pattern.compile("<[^>]*>");

    /**
     * Uses a poorly conceived method to remove HTML from a string. "Not for
     * production use."
     *
     * @param str the string from which to strip HTML tags
     * @return the given string with HTML tags removed
     */
    public static String stripHtml(String str) {
        return htmlRE.matcher(str).replaceAll("");
    }
}
