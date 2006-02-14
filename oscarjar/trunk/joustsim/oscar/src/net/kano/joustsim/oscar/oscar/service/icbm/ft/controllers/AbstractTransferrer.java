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

package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import net.kano.joustsim.oscar.oscar.service.icbm.ft.ProgressStatusProvider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.util.logging.Logger;
import java.util.logging.Level;

public abstract class AbstractTransferrer<C extends Channel>
    implements Transferrer, ProgressStatusProvider {
  private static final Logger LOGGER = Logger
      .getLogger(AbstractTransferrer.class.getName());

  protected final long offset;
  protected final long length;
  private volatile long position = 0;
  private final C socket;
  private @Nullable Selector selector;
  private final @Nullable SelectableChannel selectable;

  public AbstractTransferrer(C channel, @Nullable SelectableChannel selectable,
      long offset, long toTransfer) {
    this.socket = channel;
    this.selectable = selectable;
    this.offset = offset;
    this.length = toTransfer;
  }

  protected void waitUntilReady() throws IOException {
    if (selector != null) selector.select(50);
  }

  public long transfer() throws IOException {
    boolean wasBlocking;
    if (selectable != null) {
      selector = Selector.open();
      wasBlocking = selectable.isBlocking();
    } else {
      selector = null;
      wasBlocking = false;
    }
    try {
      if (selectable != null) {
        if (wasBlocking) selectable.configureBlocking(false);
        selectable.register(selector, getSelectionKey());
      }

      setPosition(offset);
      long totalTransferred = 0;
      while (true) {
        if (totalTransferred > length) {
          LOGGER.severe("downloaded too much: " + totalTransferred
              + " >= length " + length);
          break;

        } else if (totalTransferred == length) {
          break;
        }
        boolean waited = waitIfPaused();
        if (waited) continue;

        long remaining = length - totalTransferred;
        waitUntilReady();
        long transferred = transfer(socket, totalTransferred, remaining);

        if (transferred == -1) {
          LOGGER.severe("transfer returned -1");
          break;
        }

        totalTransferred += transferred;
        setPosition(offset + totalTransferred);
        if (isCancelled()) {
          LOGGER.fine("Someone said to cancel receiving");
          break;
        }
      }
      return totalTransferred;

    } finally {
      try {
        if (selector != null) selector.close();
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Couldn't close selector", e);
      }
      try {
        if (wasBlocking) selectable.configureBlocking(true);
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Couldn't reset blocking mode", e);
      }
      cleanUp();
    }
  }

  protected void cleanUp() throws IOException {
  }

  protected abstract int getSelectionKey();

  protected abstract boolean isCancelled();

  /**
   * Returns true if this method waited
   */
  protected abstract boolean waitIfPaused();

  /**
   * Returns the number of bytes transferred by this call, or -1 to cancel
   * transfer
   */
  protected abstract long transfer(C channel, long transferred,
      long remaining) throws IOException;

  public long getStartPosition() {
    return offset;
  }

  public long getPosition() {
    return position;
  }

  public long getLength() {
    return length;
  }

  private void setPosition(long position) {
    this.position = position;
  }
}
