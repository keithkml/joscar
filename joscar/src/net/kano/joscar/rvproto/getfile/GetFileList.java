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
 *  File created by Keith @ 9:14:11 PM
 *
 */

package net.kano.joscar.rvproto.getfile;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.LiveWritable;
import net.kano.joscar.tlv.ImmutableTlvChain;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

public class GetFileList implements LiveWritable {
    public static final String GFLISTVERSION_DEFAULT = "Lst1";

    private final String gfListVersion;
    private final GetFileEntry[] files;

    public static GetFileList readGetFileList(ByteBlock block) {
        DefensiveTools.checkNull(block, "block");

        if (block.getLength() < 4) return null;

        String version = BinaryTools.getAsciiString(block.subBlock(0, 4));

        System.out.println("version: " + version);

        ByteBlock rest = block.subBlock(4);
        TlvChain chain = ImmutableTlvChain.readChain(rest);

        Tlv[] tlvs = chain.getTlvs();
        System.out.println("entry tlvs: " + tlvs.length);

        List entries = new LinkedList();

        for (int i = 0; i < tlvs.length;) {
            GetFileEntry entry
                    = GetFileEntry.readGetFileEntry(tlvs, i);

            System.out.println("read entry: " + entry);

            if (entry == null) break;

            entries.add(entry);

            i += entry.getTotalTlvCount();
        }

        GetFileEntry[] entryArray
                = (GetFileEntry[]) entries.toArray(new GetFileEntry[0]);
        return new GetFileList(version, entryArray);
    }

    public GetFileList(GetFileEntry[] files) {
        this(GFLISTVERSION_DEFAULT, files);
    }

    public GetFileList(String gfListVersion, GetFileEntry[] files) {
        DefensiveTools.checkNull(gfListVersion, "gfListVersion");
        DefensiveTools.checkNull(files, "files");

        for (int i = 0; i < files.length; i++) {
            DefensiveTools.checkNull(files[i], "files[] elements");
        }

        this.gfListVersion = gfListVersion;
        this.files = files;
    }

    public final String getGfListVersion() { return gfListVersion; }

    public final GetFileEntry[] getFileEntries() {
        return (GetFileEntry[]) files.clone();
    }

    public final void write(OutputStream out) throws IOException {
        out.write(BinaryTools.getAsciiBytes(gfListVersion));
        for (int i = 0; i < files.length; i++) {
            files[i].write(out);
        }
    }
}