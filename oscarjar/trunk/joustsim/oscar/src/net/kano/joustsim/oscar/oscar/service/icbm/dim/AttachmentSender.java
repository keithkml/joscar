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

import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.AbstractTransferrer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;

class AttachmentSender extends AbstractTransferrer {
  private final Attachment data;
  private final EventPost post;
  private final String id;
  private final int attachno;
  private final int numattachments;
  private Cancellable cancellable;

  private ByteBuffer buf;
  private ReadableByteChannel inchan;

  public AttachmentSender(StreamInfo stream, Attachment data,
      EventPost post, String id, int attachno, int numattachments,
      Cancellable cancellable) throws IOException {
    super(stream, 0, data.getLength());
    this.data = data;
    this.post = post;
    this.id = id;
    this.attachno = attachno;
    this.numattachments = numattachments;
    resizeBuffer(1024);
    inchan = data.openForReading();
    this.cancellable = cancellable;
  }

  public void resizeBuffer(int size) {
    buf = ByteBuffer.allocate(size);
    buf.limit(0);
  }

  protected int getSelectionKey() {
    return SelectionKey.OP_WRITE;
  }

  protected boolean isCancelled() {
    return cancellable.isCancelled();
  }

  protected boolean waitIfPaused() {
    return false;
  }

  protected long transferChunk(ReadableByteChannel readable,
      WritableByteChannel writable, long transferred, long remaining)
      throws IOException {
    assert buf.position() == 0 : buf.position();
    int leftoverBytes = buf.limit();
    buf.position(leftoverBytes);
    buf.limit(Math.max(0, (int) Math.min(buf.capacity(),
        remaining - leftoverBytes)));
    int read = inchan.read(buf);
    if (read == -1 && leftoverBytes == 0) {
      // it looks like we need to fill the rest of the stream with nulls,
      // because the attachment stream didn't give us as many bytes as we were
      // told to transfer
      buf.position(0);
      buf.limit((int) Math.min(buf.capacity(), remaining));
      post.fireEvent(new SendingAttachmentNullPaddingEvent(id, transferred,
          data.getLength(), attachno, numattachments));

    } else {
      post.fireEvent(new SendingAttachmentDataEvent(id, transferred,
          data.getLength(), attachno, numattachments));
      buf.flip();
    }
    int wrote = writable.write(buf);
    buf.compact();
    buf.flip();
    return wrote;
  }
}
