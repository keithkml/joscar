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
import java.util.HashSet;
import java.util.Set;
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

    public static boolean deleteDir(File dir) {
        // to see if this directory is actually a symbolic link to a directory,
        // we want to get its canonical path - that is, we follow the link to
        // the file it's actually linked to
        File candir;
        try {
            candir = dir.getCanonicalFile();
        } catch (IOException e) {
            return false;
        }

        // a symbolic link has a different canonical path than its actual path,
        // unless it's a link to itself
        if (!candir.equals(dir.getAbsoluteFile())) {
            // this file is a symbolic link, and there's no reason for us to
            // follow it, because then we might be deleting something outside of
            // the directory we were told to delete
            return false;
        }

        // now we go through all of the files and subdirectories in the
        // directory and delete them one by one
        File[] files = candir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];

                // in case this directory is actually a symbolic link, or it's
                // empty, we want to try to delete the link before we try
                // anything
                boolean deleted = !deleteFile(file);
                if (deleted) {
                    // deleting the file failed, so maybe it's a non-empty
                    // directory
                    if (file.isDirectory()) deleteDir(file);

                    // otherwise, there's nothing else we can do
                }
            }
        }

        // now that we tried to clear the directory out, we can try to delete it
        // again
        return deleteFile(dir);
    }

    private static boolean deleteFile(File file) {
        return file.delete();
    }
}
