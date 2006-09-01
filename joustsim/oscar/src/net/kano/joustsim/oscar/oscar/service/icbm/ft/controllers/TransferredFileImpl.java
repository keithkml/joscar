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

import net.kano.joscar.ByteBlock;
import net.kano.joscar.rvproto.ft.FileTransferHeader;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

public class TransferredFileImpl implements TransferredFile {
  private static final Logger LOGGER
      = Logger.getLogger(TransferredFileImpl.class.getName());

  private final @Nullable RandomAccessFile raf;
  private final long size;
  private final File file;
  private final String name;
  private final long lastmod;
  private ByteBlock macFileInfo = FileTransferHeader.MACFILEINFO_DEFAULT;

  protected TransferredFileImpl(@Nullable RandomAccessFile raf,
      long size, File file, String name, long lastmod) {
    this.raf = raf;
    this.size = size;
    this.file = file;
    this.name = name;
    this.lastmod = lastmod;
  }

  public TransferredFileImpl(File file, String name, String fileMode)
      throws IOException {
    this.file = file;
    this.name = name;
    raf = new RandomAccessFile(file, fileMode);
    size = raf.length();
    lastmod = file.lastModified();
  }

  public long getSize() {
    return size;
  }

  public void close() throws IOException {
    if (raf == null) {
      LOGGER.fine("Couldn't close " + file + " because there's no " +
          "RandomAccessFile set");
    } else {
      LOGGER.fine("Closing RandomAccessFile for " + file);
      raf.close();
    }
  }

  public String getTransferredName() {
    return name;
  }

  public File getRealFile() {
    return file;
  }

  public long getLastModifiedMillis() {
    return lastmod;
  }

  public FileChannel getChannel() {
    if (raf == null) {
      throw new IllegalStateException("This file does not have a "
          + "RandomAccessFile. It was probably created for unit testing.");
    }
    return raf.getChannel();
  }

  public ByteBlock getMacFileInfo() {
    return macFileInfo;
  }

  public void setMacFileInfo(ByteBlock macFileInfo) {
    this.macFileInfo = macFileInfo;
  }
}
