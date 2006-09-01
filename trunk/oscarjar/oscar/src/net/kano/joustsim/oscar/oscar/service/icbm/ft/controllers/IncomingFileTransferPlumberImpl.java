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
 * Date: Jan 26, 2006
 * Time: 5:50:03 PM
 */

package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import net.kano.joscar.rvcmd.SegmentedFilename;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.rvproto.ft.FileTransferHeader;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileMapper;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.IncomingFileTransfer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class IncomingFileTransferPlumberImpl
    extends FileTransferPlumberImpl implements IncomingFileTransferPlumber {
  private final IncomingFileTransfer transfer;
  private ReceiveFileController controller;

  public IncomingFileTransferPlumberImpl(IncomingFileTransfer transfer,
      ReceiveFileController controller) {
    super(controller.getStream(), transfer);
    this.transfer = transfer;
    this.controller = controller;
  }

  public TransferredFile getNativeFile(SegmentedFilename segName)
      throws IOException {
    return getNativeFile(segName, FileTransferHeader.MACFILEINFO_DEFAULT);
  }

  public TransferredFile getNativeFile(SegmentedFilename segName,
      ByteBlock macFileInfo) throws IOException {
    List<String> parts = segName.getSegments();
    File destFile;
    FileMapper fileMapper = transfer.getFileMapper();
    if (parts.size() > 0) {
      destFile = fileMapper.getDestinationFile(segName);
    } else {
      destFile = fileMapper.getUnspecifiedFilename();
    }
    TransferredFileImpl tfile = new TransferredFileImpl(destFile,
        segName.toNativeFilename(), "rw");
    tfile.setMacFileInfo(macFileInfo);
    return tfile;
  }

  public Transferrer createTransferrer(TransferredFile file, long startedAt,
      long toDownload) {
    return new FileReceiver(controller, file.getChannel(),
        startedAt, toDownload);
  }

  public boolean shouldAttemptResume(TransferredFile file) {
    return file.getRealFile().exists();
  }
}
