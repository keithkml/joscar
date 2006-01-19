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

import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.io.IOException;
import java.util.logging.Logger;

public abstract class Receiver implements ProgressStatusProvider {
  private static final Logger LOGGER = Logger
      .getLogger(Receiver.class.getName());

  protected final long offset;
  protected final long length;
  private volatile long position = 0;

  public Receiver(long offset, long length) {
    this.offset = offset;
    this.length = length;
  }

  public int receive(SocketChannel socketIn) throws IOException {
    Selector selector = Selector.open();
    boolean wasBlocking = socketIn.isBlocking();
    try {
      if (wasBlocking) socketIn.configureBlocking(false);
      socketIn.register(selector, SelectionKey.OP_READ);

      setPosition(offset);
      int downloaded = 0;
      while (true) {
        if (downloaded >= length) {
          LOGGER.severe("downloaded too much: " + downloaded
              + " >= length " + length);
          break;
        }
        boolean waited = waitIfPaused();
        if (waited) continue;

        long remaining = length - downloaded;
        selector.select(50);
        long transferred = transfer(socketIn, downloaded, remaining);

        if (transferred == -1) {
          LOGGER.severe("transferFrom returned -1");
          break;
        }

        downloaded += transferred;
        setPosition(offset + downloaded);
        if (isCancelled()) {
          LOGGER.fine("Someone said to cancel receiving");
          break;
        }
      }
      return downloaded;

    } finally {
      selector.close();
      if (wasBlocking) socketIn.configureBlocking(true);
    }
  }

  protected abstract boolean isCancelled();

  protected abstract boolean waitIfPaused();

  protected abstract long transfer(SocketChannel socketIn, int downloaded,
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
