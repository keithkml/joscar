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
 *  File created by Keith @ 8:29:48 PM
 *
 */

package net.kano.joscar.snaccmd.icbm;

public class ImEncoding {
    /** A charset code indicating US-ASCII encoding. */
    public static final int CHARSET_ASCII = 0x0000;
    /** A charset code indicating ISO-8859-1 encoding. */
    public static final int CHARSET_ISO = 0x0003;
    /** A charset code indicating UCS-2BE, or UTF-16BE. */
    public static final int CHARSET_UTF16 = 0x0002;

    /** A charset "subcode" that is sent by default. */
    public static final int CHARSUBSET_DEFAULT = 0x0000;

    private final int charsetCode;
    private final int charsetSubcode;

    public ImEncoding(int charsetCode, int charsetSubcode) {
        this.charsetCode = charsetCode;
        this.charsetSubcode = charsetSubcode;
    }

    public final int getCharsetCode() { return charsetCode; }

    public final int getCharsetSubcode() { return charsetSubcode; }

    public final String toCharsetName() {
        if (charsetCode == CHARSET_ASCII) return "US-ASCII";
        else if (charsetCode == CHARSET_ISO) return "ISO-8859-1";
        else if (charsetCode == CHARSET_UTF16) return "UTF-16BE";
        else return null;
    }

    public String toString() {
        return "ImEncoding: code=" + charsetCode + ", subcode=" + charsetSubcode
                + " (charset guess: " + toCharsetName() + ")";
    }
}