package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * NOTE: This test accesses the file system by creating a temp file
 * @author Alex Rass
 * @since Mar 25, 2006
 */
public class TestTransferredFileImpl extends TestCase {
  private static final String CONTENTS = "blah!";
  private File file;
  private TransferredFile tfi;

  public void setUp() throws Exception {
    file = File.createTempFile("TestTransferredFileImpl", ".txt");
    FileOutputStream fos = new FileOutputStream(file);
    fos.write(CONTENTS.getBytes());
    fos.close();
    file.deleteOnExit();
  }

  public void tearDown() throws Exception {
    if (tfi != null) tfi.close();
    file.delete();
  }

  public void testCloseProperly1() throws Exception {
    tfi = new TransferredFileImpl(new RandomAccessFile(file, "rw"), 10, file,
        "name", 0);
    ByteBuffer dst = ByteBuffer.allocate(10);
    tfi.getChannel().read(dst);
    tfi.close();
    file.delete();
    assertFalse(file.exists());
  }

  public void testCloseProperly2() throws Exception {
    tfi = new TransferredFileImpl(file, "name", "rw");
    ByteBuffer dst = ByteBuffer.allocate(10);
    tfi.getChannel().read(dst);
    tfi.close();
    file.delete();
    assertFalse(file.exists());
  }

  public void testGetSize() throws Exception {
    tfi = new TransferredFileImpl(file, "name", "rw");
    assertEquals("Reporting wrong file size.", file.length(), tfi.getSize());
  }
}
