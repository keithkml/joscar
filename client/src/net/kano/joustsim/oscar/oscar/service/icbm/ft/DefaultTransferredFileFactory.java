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

package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.TransferredFile;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.TransferredFileImpl;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class DefaultTransferredFileFactory implements TransferredFileFactory {
  public TransferredFileImpl getTransferredFile(File file, String name)
      throws IOException {
    return createTransferredFile(file, name);
  }

  private TransferredFileImpl createTransferredFile(File file, String name)
      throws IOException {
    TransferredFileImpl tfile = createInitialTransferredFile(file, name);
    initializeFile(tfile);
    return tfile;
  }

  protected TransferredFileImpl createInitialTransferredFile(File file,
      String name) throws IOException {
    return new TransferredFileImpl(file, name, "r");
  }

  public TransferredFile getTransferredFileInFolder(File file,
      String folderName) throws IOException {
    return createTransferredFile(file, folderName
        + File.separator + file.getName());
  }

  public TransferredFile getTransferredFileFromRoot(File file, File root,
      @Nullable String folderName)
      throws IOException, IllegalArgumentException {
    String path = getRelativePath(file, root, folderName);
    return createTransferredFile(file, path);
  }

  public static String getRelativePath(File file, File root,
      String folderName) {
    if (file.equals(root)) {
      throw new IllegalArgumentException("File cannot be root: " + file);
    }
    StringBuilder string = new StringBuilder(100);
    File component;
    for (component = file; component != null && !component.equals(root);
        component = component.getParentFile()) {
      string.insert(0, File.separator + component.getName());
    }
    if (component == null) {
      throw new IllegalArgumentException("File is not child of root: file="
          + file + ", root=" + root);
    }
    // remove leading slash
    string.delete(0, 1);

    // remove trailing slash
    if (folderName != null) {
      while (folderName.endsWith(File.separator)) {
        folderName = folderName.substring(0, folderName.length() - 1);
      }
      if (folderName.length() > 0) {
        string.insert(0, folderName + File.separator);
      }
    }
    String path = string.toString();
    return path;
  }

  protected void initializeFile(TransferredFileImpl tfile) {
  }
}
