/*
 * Copyright (c) 2006, The Joust Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 * - Neither the name of the Joust Project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * File created by keithkml
 */

package net.kano.joustsim.oscar.oscar.service.icbm.dim;

import java.io.InputStream;
import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.ByteBuffer;

public class SelectorInputStream extends InputStream {
  private final Selector selector;
  private final ReadableByteChannel readable;
  private final SelectableChannel selectable;

  private SelectorInputStream(Channel channel) throws IOException {
    selector = Selector.open();
    readable = (ReadableByteChannel) channel;
    selectable = (SelectableChannel) channel;
    selectable.configureBlocking(false);
    selectable.register(selector, SelectionKey.OP_READ);
  }

  public static <C extends SelectableChannel & ReadableByteChannel>
  SelectorInputStream getInstance(C channel) throws IOException {
    return new SelectorInputStream(channel);
  }

  public void close() throws IOException {
    readable.close();
  }

  public int read(byte[] b, int off, int len) throws IOException {
    if (len == 0) return 0;

    ByteBuffer buf = ByteBuffer.wrap(b, off, len);
    assert buf.position() == off;
    while (buf.hasRemaining() && selector.isOpen() && readable.isOpen()) {
      selector.select(50);

      int ct = readable.read(buf);
      if (ct == -1) {
        if (buf.position() == off) {
          // if we're at the beginning of the buffer, let's return -1
          return -1;
        }
      }
      if (ct == -1 || ct > 0) {
        // we either failed or read something; either way we should return
        break;
      }
    }
    return buf.position() - off;
  }

  public int read() throws IOException {
    ByteBuffer buf = ByteBuffer.allocate(1);
    while (true) {
      selector.select(50);

      int ct = readable.read(buf);
      if (ct == -1) return -1;
      if (ct > 0) return buf.get(0);
    }
  }
}
