/*
 *  Copyright (c) 2002, The Joust Project
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
 *  File created by keith @ Apr 25, 2003
 *
 */

package net.kano.joscar.rvcmd.sendfile;

import net.kano.joscar.Writable;

public class FileInfoHeader  {
    /*


wHdrType bCookie wEncryption wCompression
wTotalNumFiles wNumFilesLeft wTotalNumParts wNumPartsLeft dwTotalFilesize
dwFilesize dwModifiedTime dwChecksum dwResForkRecvdChecksum dwResForkSize
dwCreationTime dwResForkChecksum dwNumRecvd dwRecvdChecksum bIDstring bFlags
bListNameOffset bListSizeOffset bDummy bMacFileInfo wNameEncoding wNameLanguage
bName

Sa8SSSSSSIIIIIIIIIIa32ccca69a16SSa*

S - wHdrType
a8 - bCookie
SSSSSS -
IIIIIIIIII
a32
ccc
a69
a16
SS
a*

    -- umm:

    rv cookie: 43 43 32 37 36 31 43 00
    4f 46 54 32  01 02 01 01 - cookie (CC27 61C[null])
    00 00 - encrypt
    00 00 - compress
    00 00 - totfiles
    00 00 - filesleft
    00 00 - totparts
    00 00 - partsleft
    00 01 00 01 - totsize
    00 01 00 01 - size
    00 00 00 41 - modtime
    00 00 00 41
    3e a8 69 9a 3d 93 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 4f 46 54 5f 57 69 6e 64 6f 77 73 20 49 43 42 4d 46 54 20 56 31 2e 31 20 33 32 00 00 00 00 00 00 20 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 31 32 33 34 35 36 37 38 31 32 33 34 35 36 37 38 31 32 33 34 35 36 37 38 31 32 33 34 35 36 37 38 31 32 33 34 35 36 37 38 31 32 33 34 35 36 37 38 31 32 33 34 35 36 37 38 31 32 33 34 35 36 37 38 31 00

    -- let's try again:

    4f 46 54 32 - "OFT2"
    01 02 - total header length, starting with that OFT2
    01 01 - header type 
    00 00  00 00 00 00 - cookie
    00 00 - encrypt
    00 00 - compress
    00 01 - totfiles
    00 01 - filesleft
    00 01 - totparts
    00 01 - partsleft
    00 00 00 41 - totsize
    00 00 00 41 - size
    3e a8 69 9a - modtime
    3d 93 00 00 - checksum
    00 00 00 00 - rfrcsum
    00 00 00 00 - rfsize
    00 00 00 00 - cretime
    00 00 00 00 - rfcsum
    00 00 00 00 - nrecvd
    00 00 00 00 - nrecvsum
    4f 46 54 5f 57 69 6e 64 6f 77 73 20 49 43 42 4d - OFT_Windows ICBM
    46 54 20 56 31 2e 31 20 33 32 00 00 00 00 00 00 - FT V1.1 32[nulls]
    20 - flags
    00 - listnameoffset
    00 - listsizeoffset
    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 - nulls...
    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
    00 00 00 00 00
    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 - mac file info
    00 00 - encoding
    00 00 - subencoding
    [filename][null]

    aimbs_getrawbuf(bs, fh->bcookie, 8);
    fh->encrypt = aimbs_get16(bs);
    fh->compress = aimbs_get16(bs);
    fh->totfiles = aimbs_get16(bs);
    fh->filesleft = aimbs_get16(bs);
    fh->totparts = aimbs_get16(bs);
    fh->partsleft = aimbs_get16(bs);
    fh->totsize = aimbs_get32(bs);
    fh->size = aimbs_get32(bs);
    fh->modtime = aimbs_get32(bs);
    fh->checksum = aimbs_get32(bs);
    fh->rfrcsum = aimbs_get32(bs);
    fh->rfsize = aimbs_get32(bs);
    fh->cretime = aimbs_get32(bs);
    fh->rfcsum = aimbs_get32(bs);
    fh->nrecvd = aimbs_get32(bs);
    fh->recvcsum = aimbs_get32(bs);
    aimbs_getrawbuf(bs, fh->idstring, 32);
    fh->flags = aimbs_get8(bs);
    fh->lnameoffset = aimbs_get8(bs);
    fh->lsizeoffset = aimbs_get8(bs);
    aimbs_getrawbuf(bs, fh->dummy, 69);
    aimbs_getrawbuf(bs, fh->macfileinfo, 16);
    fh->nencode = aimbs_get16(bs);
    fh->nlanguage = aimbs_get16(bs);
    aimbs_getrawbuf(bs, fh->name, 64);
    */

    private long icbmCookie;
    private int encryption;
    private int compression;
    private int fileCount;
    private int filesLeft;
    private int partCount;
    private int partsLeft;
    private long totalSize;
    private long fileSize;
    private long lastmod;
    private long checksum;
    private long rfrcsum;
    private long rfsize;
    private long created;
    private long rfcsum;
    private long nrecvd;
    private long nrecvsum;
    private byte[] idstring;
    private int flags;
    private int nameoffset;
    private int sizeoffset;
    private byte[] dummy;
    private byte[] macFileInfo;
    private int nencode;
    private int nlanguage;
    private String filename;
}
