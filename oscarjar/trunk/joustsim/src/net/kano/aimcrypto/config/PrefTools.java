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
 *  File created by keith @ Feb 4, 2004
 *
 */

package net.kano.aimcrypto.config;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import org.bouncycastle.util.encoders.Base64;

import java.util.Properties;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public final class PrefTools {
    private PrefTools() { }

    public static String getBase64Decoded(String pass) {
        return BinaryTools.getAsciiString(ByteBlock.wrap(Base64.decode(pass)));
    }

    public static String getBase64Encoded(String pass) {
        byte[] encoded = Base64.encode(BinaryTools.getAsciiBytes(pass));
        return BinaryTools.getAsciiString(ByteBlock.wrap(encoded));
    }

    public static Properties loadProperties(File file) throws IOException {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            lockIfPossible(fin.getChannel(), true);
            Properties props = new Properties();
            props.load(fin);
            return props;
        } finally {
            if (fin != null) {
                try { fin.close(); } catch (IOException e) { }
            }
        }
    }

    public static void writeProperties(File prefsFile,
            Properties props, String header) throws IOException {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(prefsFile);
            lockIfPossible(fout.getChannel(), false);
            props.store(fout, header);
        } finally {
            if (fout != null) {
                try { fout.close(); } catch (IOException e) { }
            }
        }
    }

    private static void lockIfPossible(FileChannel channel, boolean val) {
        try {
            channel.lock(0L, Long.MAX_VALUE, val);
        } catch (Exception nobigdeal) {
            //TODO: file lock failed
        }
    }
}
