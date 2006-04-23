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

import junit.framework.TestCase;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.TransferredFile;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.TransferredFileImpl;

import java.io.File;
import java.io.IOException;

public class DefaultTransferredFileFactoryTest extends TestCase {
  private DefaultTransferredFileFactory factory;
  private static final String SLASH = File.separator;

  public void testDirectChild() throws IOException {
    TransferredFile file = factory.getTransferredFileFromRoot(
        new File("x" + SLASH + "y"),
        new File("x"), null);
    assertEquals("y", file.getTransferredName());
  }

  public void testDirectChildWithFolderName() throws IOException {
    TransferredFile file = factory.getTransferredFileFromRoot(
        new File("x" + SLASH + "y"),
        new File("x"), "d/c");
    assertEquals("d/c/y", file.getTransferredName());
  }

  public void testDirectChildWithFolderNameTrailingSlash() throws IOException {
    TransferredFile file = factory.getTransferredFileFromRoot(
        new File("x" + SLASH + "y"),
        new File("x"), "d/c/");
    assertEquals("d/c/y", file.getTransferredName());
  }

  public void testSecondChild() throws IOException {
    TransferredFile file = factory.getTransferredFileFromRoot(
        new File("x" + SLASH + "y" + SLASH + "z"),
        new File("x"), null);
    assertEquals("y" + SLASH + "z", file.getTransferredName());
  }

  public void testNotChild() throws IOException {
    assertFails(new File("z"), new File("x"));
  }

  private void assertFails(File file, File root) throws IOException {
    try {
      TransferredFile tfile = factory
          .getTransferredFileFromRoot(file, root, null);
      fail("Should fail, but got name " + tfile.getTransferredName());
    } catch (IllegalArgumentException e) {
      // cool
    }
  }

  public void testFileIsRoot() throws IOException {
    assertFails(new File("x"), new File("x"));
  }

  protected void setUp() throws Exception {
    factory = new DefaultTransferredFileFactory() {
      protected TransferredFileImpl createInitialTransferredFile(File file,
          String name) throws IOException {
        return new TransferredFileImpl(null, 0, file, name, 0) {

        };
      }
    };
  }
}
