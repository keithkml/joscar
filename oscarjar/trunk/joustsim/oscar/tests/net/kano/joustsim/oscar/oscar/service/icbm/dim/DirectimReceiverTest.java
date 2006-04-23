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
import net.kano.joustsim.TestTools;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.List;

public class DirectimReceiverTest extends DirectimTest {
  public void testShortMessages() throws IOException {
    for (int i = 0; i < 20; i++) {
      String str = makeString(i);
      System.out.println("Running test with " + str);
      runTestWithPlainMessage(str, 1024, str);
    }
  }

  public void testGarbageAfterBinaryTag() throws IOException {
    String str = "hi";
    runTestWithAttachment(
        str + "<BINARY><DATA ID=\"1\" SIZE=\"1\">a</DATA></BINARY> ", 1024, str, "a");
  }

  public void testMessagesNearBufferLength() throws IOException {
    int bufferSize = 30;
    for (int i = bufferSize -10; i < bufferSize +20; i++) {
      System.out.println("Running test for " + i + " chars");
      String str = makeString(i);
      runTestWithPlainMessage(str, bufferSize, str);
    }
  }

  public void testShortMessagesWithFakeAttachment() throws IOException {
    for (int i = 0; i < 20; i++) {
      String str = makeString(i);
      System.out.println("Running test with " + str);
      runTestWithPlainMessage(str + "<BINARY><DATA ID=\"\">", 1024, str);
      runTestWithPlainMessage(str + "<BINARY><DATA SIZE=\"\">", 1024, str);
    }
  }

  public void testMessagesNearBufferLengthWithFakeAttachment() throws IOException {
    for (int i = 0; i < 80; i++) {
      String str = makeString(i);
      System.out.println("Running test with " + i + " chars");
      runTestWithPlainMessage(str + "<BINARY><DATA ID=\"\">", 30, str);
      runTestWithPlainMessage(str + "<BINARY><DATA SIZE=\"\">", 30, str);
    }
  }

  public void testShortMessagesWithAttachment() throws IOException {
    for (int i = 0; i < 20; i++) {
      String str = makeString(i);
      System.out.println("Running test with " + str);
      for (int alen = 0; alen < 20; alen++) {
        String attch = makeString(alen);
        runTestWithAttachment(str + "<BINARY><DATA ID=\"a\" SIZE=\""
            + alen + "\">" + attch + "</DATA></BINARY>", 1024, str, attch);
      }
    }
  }

  public void testAttachmentOver64k() throws IOException {
    int alen = 65 * 1024;
    String attch = makeString(alen);
    runTestWithAttachment("<BINARY><DATA ID=\"a\" SIZE=\""
        + alen + "\">" + attch + "</DATA></BINARY>", 1024, "", attch);
  }

  public void testMessagesNearBufferSizeWithAttachment() throws IOException {
    for (int i = 0; i < 80; i++) {
      String str = makeString(i);
      System.out.println("Running test with " + str);
      for (int alen = 0; alen < 80; alen++) {
        String attch = makeString(alen);
        runTestWithAttachment(str + "<BINARY><DATA ID=\"a\" SIZE=\""
            + alen + "\">" + attch + "</DATA></BINARY>", 30, str, attch);
      }
    }
  }

  public void testMessagesNearBufferSizeWithAttachments() throws IOException {
    for (int i = 0; i < 60; i++) {
      String str = makeString(i);
      System.out.println("Running test with " + str);
      for (int alen = 0; alen < 60; alen++) {
        String attch = makeString(alen);
        for (int alen2 = 0; alen2 < 60; alen2++) {
          String attch2 = makeString(alen2);
          runTestWithAttachment(str + "<BINARY><DATA ID=\"a\" SIZE=\""
              + alen + "\">" + attch + "</DATA><DATA ID=\"b\" SIZE=\""
              + alen2 + "\">" + attch2 + "</DATA></BINARY>", 25, str, attch, attch2);
        }
      }
    }
  }

  private void runTestWithPlainMessage(String sent, int bufferSize,
      String expect) throws IOException {
    CollectingEventPost events = runTest(sent, bufferSize);
    String recvd = TestTools.findOnlyInstance(events.getEvents(),
        ReceivedMessageEvent.class).getMessage();
    // to make the output more useful we check for length first if it would be
    // hard to determine string difference
    if (expect.length() > 10 || recvd.length() > 10) {
//      assertEquals(expect.length(), recvd.length());
    }
    assertEquals(expect, recvd);
  }

  private void runTestWithAttachment(String sent, int bufferSize,
      String expect, String... expectAttachAscii) throws IOException {
    CollectingEventPost events = runTest(sent, bufferSize);
    String recvd = TestTools.findOnlyInstance(events.getEvents(),
        ReceivedMessageEvent.class).getMessage();
    assertTrue(expect.equals(recvd));
    int attcount = 0;
    for (RvConnectionEvent event : events.getEvents()) {
      if (event instanceof ReceivedAttachmentEvent) {
        ReceivedAttachmentEvent attchev = (ReceivedAttachmentEvent) event;
        ByteBlock attchbuf = ((MemoryAttachment) attchev.getAttachment())
            .getBuffer();
        String recvdattach = BinaryTools.getAsciiString(attchbuf);
        assertEquals(expectAttachAscii[attcount], recvdattach);
        attcount++;
      }
    }
    assertEquals("Wrong number of attachment events",
        expectAttachAscii.length, attcount);
  }

  private CollectingEventPost runTest(String msg,
      int bufferSize) throws IOException {
    byte[] bytes = msg.getBytes("US-ASCII");
    ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
    CollectingEventPost events = new CollectingEventPost();
    DirectimReceiver receiver = new DirectimReceiver(events,
        new MemorySaver(), "US-ASCII", bytes.length, Channels.newChannel(bin),
        false);
    receiver.resizeBuffer(bufferSize);
    receiver.transfer();
    return events;
  }

  private static class CollectingEventPost implements EventPost {
    private List<RvConnectionEvent> events = new ArrayList<RvConnectionEvent>();

    public void fireEvent(RvConnectionEvent event) {
      events.add(event);
    }

    @SuppressWarnings({"ReturnOfCollectionOrArrayField"})
    public List<RvConnectionEvent> getEvents() {
      return events;
    }
  }

  private static class MemorySaver implements AttachmentSaver {
    public Attachment createChannel(String id, long length) {
      return new IncomingMemoryAttachment(id, length);
    }
  }
}
