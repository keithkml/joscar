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

/*
 * Created by IntelliJ IDEA.
 * User: keithkml
 * Date: Feb 12, 2006
 * Time: 12:41:42 PM
 */

package net.kano.joustsim.oscar.oscar.service.icbm.dim;

import org.jetbrains.annotations.Nullable;

import java.nio.channels.WritableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ReadableByteChannel;
import java.io.FileNotFoundException;

public abstract class AttachmentDestination {
  private final String id;
  private final long length;
  private final WritableByteChannel writable;
  private final @Nullable SelectableChannel selectable;

  public AttachmentDestination(String id, long length,
      WritableByteChannel writable, @Nullable SelectableChannel selectable) {
    this.id = id;
    this.length = length;
    this.writable = writable;
    this.selectable = selectable;
  }

  public abstract ReadableByteChannel openForReading() throws
      FileNotFoundException;

  public WritableByteChannel getWritable() {
    return writable;
  }

  public @Nullable SelectableChannel getSelectable() {
    return selectable;
  }

  public String getId() {
    return id;
  }

  public long getLength() {
    return length;
  }
}
