/*
 *  Copyright (c) 2005, The Joust Project
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
 */

package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joscar.rvproto.ft.FileTransferChecksum;

import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.IOException;

public class Checksummer implements ProgressStatusOwner {
    private volatile long position = 0;
    private boolean summed = false;

    private final FileChannel channel;
    private final long end;

    public Checksummer(FileChannel channel, long length) {
        this.channel = channel;
        this.end = length;
    }

    public long compute() throws IOException, IllegalStateException {
        synchronized(this) {
            if (summed) {
                throw new IllegalStateException("already summing or summed");
            }
            summed = true;
        }
        long sum;
        long origOffset = -1;
        try {
            synchronized (this) {
                origOffset = channel.position();
                channel.position(0);
            }
            FileTransferChecksum summer = new FileTransferChecksum();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            long remaining = end;
            while (remaining > 0) {
                buffer.rewind();
                buffer.limit((int) Math.min(remaining, buffer.capacity()));
                int count = channel.read(buffer);
                if (count == -1) break;
                buffer.flip();
                remaining -= buffer.limit();
                summer.update(buffer.array(), buffer.arrayOffset(), buffer.limit());
                long newPos = end - remaining;
                setPosition(newPos);
            }
            if (remaining > 0) {
                throw new IOException("could not get checksum for entire file; "
                        + remaining + " failed of " + end);
            }

            sum = summer.getValue();

        } finally {
            synchronized (this) {
                setPosition(end);
                if (origOffset != -1) channel.position(origOffset);
            }
        }
        return sum;
    }

    private void setPosition(long newPos) {
        position = newPos;
    }

    public long getStartPosition() {
        return 0;
    }

    public long getPosition() {
        return position;
    }

    public long getEnd() { return end; }
}
