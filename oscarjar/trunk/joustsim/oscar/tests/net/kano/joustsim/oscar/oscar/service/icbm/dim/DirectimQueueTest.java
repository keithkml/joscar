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

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.rvproto.directim.DirectImHeader;
import net.kano.joustsim.oscar.oscar.service.icbm.DirectMessage;
import net.kano.joustsim.oscar.oscar.service.icbm.MockOutgoingRvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.AbstractStreamInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;

public class DirectimQueueTest extends DirectimTest {
  private MyDirectimQueueProcessor processor;
  private ByteArrayOutputStream bout;

  protected void setUp() throws Exception {
    bout = new ByteArrayOutputStream();
    processor = new MyDirectimQueueProcessor();
  }

  public void testMessage() throws IOException {
    processor.processItem(new DirectMessage("hi", false));
    byte[] buf = bout.toByteArray();
    ByteBlock block = ByteBlock.wrap(buf);
    DirectImHeader header = DirectImHeader.readDirectIMHeader(
        new ByteArrayInputStream(buf));
    assertData("hi", block, header);
  }

  private void assertData(String expected, ByteBlock block,
      DirectImHeader header) {
    assertEquals(expected,
        BinaryTools.getAsciiString(block.subBlock(header.getHeaderSize())));
    assertEquals(expected.length(), header.getDataLength());
    assertEquals(header.getHeaderSize() + expected.length(), block.getLength());
  }

  private OutgoingMemoryAttachment makeAttachment(String id, String str)
      throws UnsupportedEncodingException {
    return new OutgoingMemoryAttachment(id, ByteBlock.wrap(str.getBytes("US-ASCII")));
  }

  public void testSendingMessageWithAttachment() throws IOException {
    processor.processItem(new DirectMessage("hi", false,
        makeAttachment("a", "hey")));
    byte[] buf = bout.toByteArray();
    ByteBlock block = ByteBlock.wrap(buf);
    DirectImHeader header = DirectImHeader.readDirectIMHeader(
        new ByteArrayInputStream(buf));
    assertData("hi<BINARY><DATA ID=\"a\" SIZE=\"3\">hey</DATA></BINARY>", block, header);
  }

  public void testMessageWithAttachments() throws IOException {
    processor.processItem(new DirectMessage("hi", false,
        makeAttachment("a", "hey"), makeAttachment("b", "heya")));
    byte[] buf = bout.toByteArray();
    ByteBlock block = ByteBlock.wrap(buf);
    DirectImHeader header = DirectImHeader.readDirectIMHeader(
        new ByteArrayInputStream(buf));
    assertData("hi<BINARY><DATA ID=\"a\" SIZE=\"3\">hey</DATA></BINARY>"
        + "<BINARY><DATA ID=\"b\" SIZE=\"4\">heya</DATA></BINARY>", block, header);
  }

  public void testMessageNearBufferSize() throws IOException {
    processor.setBufsize(30);
    for (int i = 10; i < 50; i++) {
      bout = new ByteArrayOutputStream();
      String str = makeString(i);
      processor.processItem(new DirectMessage(str, false));
      byte[] buf = bout.toByteArray();
      ByteBlock block = ByteBlock.wrap(buf);
      DirectImHeader header = DirectImHeader.readDirectIMHeader(
          new ByteArrayInputStream(buf));
      assertData(str, block, header);
    }
  }

  public void testMessageWithAttachmentNearBufferSize() throws IOException {
    processor.setBufsize(30);
    for (int i = 10; i < 50; i++) {
      String msg = makeString(i);
      for (int j = 10; j < 50; j++) {
        bout = new ByteArrayOutputStream();
        String attch = makeString(j);
        processor.processItem(new DirectMessage(msg, false, makeAttachment("1", attch)));
        byte[] buf = bout.toByteArray();
        ByteBlock block = ByteBlock.wrap(buf);
        DirectImHeader header = DirectImHeader.readDirectIMHeader(
            new ByteArrayInputStream(buf));
        assertData(msg + "<BINARY><DATA ID=\"1\" SIZE=\"" + attch.length()
            + "\">" + attch + "</DATA></BINARY>", block, header);
      }
    }
  }

  public void testMessageWithAttachmentsNearBufferSize() throws IOException {
    processor.setBufsize(30);
    for (int i = 10; i < 50; i++) {
      System.out.println("Trying length " + i);
      String msg = makeString(i);
      for (int j = 10; j < 50; j++) {
        String attch = makeString(j);
        for (int k = 10; k < 50; k++) {
          bout = new ByteArrayOutputStream();
          String attch2 = makeString(k);
          processor.processItem(new DirectMessage(msg, false,
              makeAttachment("1", attch), makeAttachment("2", attch2)));
          byte[] buf = bout.toByteArray();
          ByteBlock block = ByteBlock.wrap(buf);
          DirectImHeader header = DirectImHeader.readDirectIMHeader(
              new ByteArrayInputStream(buf));
          assertData(msg + makeBinarySection("1", attch)
              + makeBinarySection("2", attch2), block, header);
        }
      }
    }
  }

  private String makeBinarySection(String id, String attch) {
    return "<BINARY><DATA ID=\"" + id + "\" SIZE=\"" + attch.length()
        + "\">" + attch + "</DATA></BINARY>";
  }

  private class MyDirectimQueueProcessor extends DirectimQueueProcessor {
    private int bufsize = 1024;

    public MyDirectimQueueProcessor() {
      super(new Cancellable() {
        public boolean isCancelled() {
          return false;
        }
      }, new MockOutgoingRvConnection(), new AbstractStreamInfo() {
        public SelectableChannel getSelectableChannel() {
          return null;
        }

        public WritableByteChannel getWritableChannel() {
          return Channels.newChannel(DirectimQueueTest.this.bout);
        }

        public ReadableByteChannel getReadableChannel() {
          return null;
        }
      });
    }

    public void setBufsize(int bufsize) {
      this.bufsize = bufsize;
    }

    protected void setupAttachmentSender(AttachmentSender sender) {
      sender.resizeBuffer(bufsize);
    }
  }
}
