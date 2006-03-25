package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * @author Alex Rass
 * @since Mar 25, 2006
 */
public class UTestTransferredFileImpl extends TestCase {
    File file;
    TransferredFile tfi;
    private static final String contents = "blah!";
    public void setUp() throws Exception {
        file = File.createTempFile("UTestTransferredFileImpl", ".txt");
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(contents.getBytes());
        fos.close();
        file.deleteOnExit();
    }

    public void tearDown() throws Exception {
        if (tfi!=null) tfi.close();
    }

    public void testCloseProperly1() throws Exception {
        tfi = new TransferredFileImpl(new RandomAccessFile(file, "rw"), 10, file, "name", 0);
        ByteBuffer dst = ByteBuffer.allocateDirect(10);
        tfi.getChannel().read(dst);
        tfi.close();
        file.delete();
        assertFalse(file.exists());
    }

    public void testCloseProperly2() throws Exception {
        tfi = new TransferredFileImpl(file, "name", "rw");
        ByteBuffer dst = ByteBuffer.allocateDirect(10);
        tfi.getChannel().read(dst);
        tfi.close();
        file.delete();
        assertFalse(file.exists());
    }

    public void testGetSize() throws Exception {
        tfi = new TransferredFileImpl(file, "name", "rw");
        assertTrue("Reporting wrong file size.", file.length() == tfi.getSize());
    }

    public void testGetTransferredName() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetRealFile() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetLastModifiedMillis() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetChannel() throws Exception {
        //TODO: Test goes here...
    }

    public void testSetGetMacFileInfo() throws Exception {
        //TODO: Test goes here...
    }

}
