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

import net.kano.joscar.BinaryTools;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.WritableByteChannel;

public class SelectorOutputStream extends OutputStream {
  private final @Nullable Selector selector;
  private final WritableByteChannel writable;
  private volatile int total = 0;

  private SelectorOutputStream(Channel chan) throws IOException {
    this((WritableByteChannel) chan, (SelectableChannel) chan);
  }

  public SelectorOutputStream(WritableByteChannel writable,
      SelectableChannel selectable) throws IOException {
    this.writable = writable;
    if (selectable != null) {
      selector = Selector.open();
      selectable.configureBlocking(false);
      selectable.register(selector, SelectionKey.OP_WRITE);
    } else {
      selector = null;
    }
  }

  public void write(int b) throws IOException {
    write(BinaryTools.getUByte(b));
  }

  public void write(byte[] b, int off, int len) throws IOException {
    ByteBuffer buf = ByteBuffer.wrap(b, off, len);
    while (buf.hasRemaining() && writable.isOpen()
        && (selector == null || selector.isOpen())) {
      if (selector != null) {
        selector.select(50);
      }

      total += writable.write(buf);
    }
  }

  public int getTotalWritten() { return total; }

  public void close() throws IOException {
    writable.close();
  }

  public static <C extends SelectableChannel & WritableByteChannel>
  SelectorOutputStream getInstance(C chan) throws IOException {
    return new SelectorOutputStream(chan);
  }
}
