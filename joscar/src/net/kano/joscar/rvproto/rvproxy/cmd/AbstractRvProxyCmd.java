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
 *  File created by keith @ Mar 6, 2003
 *
 */

package net.kano.joscar.rvproto.rvproxy.cmd;

import net.kano.joscar.rvproto.rvproxy.cmd.RvProxyCmd;

public abstract class AbstractRvProxyCmd implements RvProxyCmd {
    private final int headerVersion;
    private final int headerType;
    private final int flags;

    protected AbstractRvProxyCmd(RvProxyHeader header) {
        this(header.getHeaderVersion(), header.getHeaderType(),
                header.getFlags());
    }

    protected AbstractRvProxyCmd(int headerType) {
        this(headerType, RvProxyHeader.FLAGS_DEFAULT_FROM_CLIENT);
    }

    protected AbstractRvProxyCmd(int headerType, int flags) {
        this(RvProxyHeader.HEADERVERSION_DEFAULT, headerType, flags);
    }

    protected AbstractRvProxyCmd(int headerVersion, int headerType, int flags) {
        this.headerVersion = headerVersion;
        this.headerType = headerType;
        this.flags = flags;
    }

    public final int getHeaderVersion() { return headerVersion; }

    public final int getHeaderType() { return headerType; }

    public final int getFlags() { return flags; }
}