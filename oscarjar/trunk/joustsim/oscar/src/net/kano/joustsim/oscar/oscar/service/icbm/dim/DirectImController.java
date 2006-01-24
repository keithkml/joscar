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

import net.kano.joscar.ByteBlock;
import net.kano.joscar.DynAsciiCharSequence;
import net.kano.joscar.ImEncodedString;
import net.kano.joscar.rvproto.directim.DirectImHeader;
import net.kano.joustsim.oscar.oscar.service.icbm.TypingState;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.Receiver;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.PausableController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.PauseHelper;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirectImController
    extends StateController implements PausableController {
  private static final Logger LOGGER = Logger
      .getLogger(DirectImController.class.getName());

  /**
   * Matches ID=abc123 case-insensitively with any combination of single or
   * double quotes around the value. I've made a learning computer.
   */
  private static final Pattern PATTERN_ID
      = Pattern.compile("ID=['\"]*(\\w+)[\"']*", Pattern.CASE_INSENSITIVE);
  /**
   * Matches SIZE=abc123 case-insensitively with any combination of single or
   * double quotes around the value.
   */
  private static final Pattern PATTERN_SIZE
      = Pattern.compile("SIZE=['\"]*(\\w+)[\"']*", Pattern.CASE_INSENSITIVE);

  private RvConnection transfer;
  private StreamInfo stream;
  private Thread thread;
  private volatile boolean cancelled = false;
  private PauseHelper pauseHelper = new PauseHelper();

  public void start(RvConnection transfer, StateController last) {
    this.transfer = transfer;
    stream = (StreamInfo) last.getEndStateInfo();
    thread = new Thread(new Runnable() {
      public void run() {
        try {
          runConnectionInThread();
        } catch (IOException e) {
          fireFailed(e);
        }
      }
    }, "Direct IM controller");
    thread.start();
  }

  public void pauseTransfer() {
    pauseHelper.setPaused(true);
  }

  public void unpauseTransfer() {
    pauseHelper.setPaused(false);
  }

  private enum Mode { MESSAGE, TAG, DATA }

  private void runConnectionInThread() throws IOException {
    SocketChannel socketChan = stream.getSocketChannel();
    socketChan.configureBlocking(true);

    InputStream is = stream.getInputStream();
    while (true) {
      DirectImHeader header = DirectImHeader.readDirectIMHeader(is);
      if (header == null) {
        fireFailed(new UnknownErrorEvent());
        break;
      }

      int datalen = (int) header.getDataLength();
      long flags = header.getFlags();
      EventPost eventPost = transfer.getEventPost();
      if ((flags & DirectImHeader.FLAG_TYPINGPACKET) != 0) {
        eventPost.fireEvent(new BuddyTypingEvent(getTypingState(flags)));
      }
      if (datalen > 0) {
        String charset = header.getEncoding().toCharsetName();
        if (charset == null) charset = "US-ASCII";
        ByteBuffer bytes = ByteBuffer.allocate(datalen);

        DirectImReceiver receiver = new DirectImReceiver(header, charset, bytes,
            datalen, eventPost);
        receiver.receive(socketChan);

        if (bytes.position() != datalen) {
          LOGGER.info("Position at end was " + bytes.position() + ", expected "
              + datalen);
          fireFailed(new UnknownErrorEvent());
          break;
        }

        if (receiver.getLastMode() == Mode.MESSAGE) {
          // we must have never hit a <BINARY> part
          String msg = new String(bytes.array(), charset);
          eventPost.fireEvent(new ReceivedMessageEvent(msg,
              isAutoResponse(header)));
        }
      }
    }
  }

  private boolean isAutoResponse(DirectImHeader header) {
    return (header.getFlags() & DirectImHeader.FLAG_AUTORESPONSE) != 0;
  }

  private static TypingState getTypingState(long flags) {
    TypingState state;
    if ((flags & DirectImHeader.FLAG_TYPED) != 0) {
      state = TypingState.PAUSED;
    } else if ((flags & DirectImHeader.FLAG_TYPING) != 0) {
      state = TypingState.TYPING;
    } else {
      state = TypingState.NO_TEXT;
    }
    return state;
  }

  public void setTypingState(TypingState state) throws IOException {
    DirectImHeader header;
    if (state == TypingState.PAUSED) {
      header = DirectImHeader.createTypedHeader();
    } else if (state == TypingState.TYPING) {
      header = DirectImHeader.createTypingHeader();
    } else if (state == TypingState.NO_TEXT) {
      header = DirectImHeader.createTypingErasedHeader();
    } else {
      throw new IllegalStateException("Unknown typing state: " + state);
    }
    header.write(stream.getOutputStream());
  }

  public void sendMessage(String msg) {
    sendMessage(msg, false);
  }

  public void sendMessage(String msg, boolean autoresponse) {
    DirectImHeader header = DirectImHeader.createMessageHeader(
        ImEncodedString.encodeString(msg), autoresponse);
    //TODO: this must be done in blocking mode, or (preferably) rewritten to use NIO
    try {
      header.write(stream.getOutputStream());
    } catch (IOException e) {
      thread.interrupt();
      fireFailed(e);
    }
  }

  public void stop() {
    LOGGER.info("Stopping directim controller");
    cancelled = true;
    thread.interrupt();
  }

  private class DirectImReceiver extends Receiver {
    private DynAsciiCharSequence chars;
    private Mode mode;
    private String lastid;
    private int lastdata;
    private int lastdatalen;
    private ByteBlock lastbytes;
    private int lasttag;
    private DirectImHeader header;
    private final String charset;
    private final ByteBuffer bytes;
    private final int datalen;
    private final EventPost eventPost;

    public DirectImReceiver(DirectImHeader header, String charset,
        ByteBuffer bytes, int datalen, EventPost eventPost) {
      super(0, datalen);
      this.header = header;
      this.charset = charset;
      this.bytes = bytes;
      this.datalen = datalen;
      this.eventPost = eventPost;
      chars = new DynAsciiCharSequence(ByteBlock.wrap(bytes.array()));
      mode = Mode.MESSAGE;
      lastid = null;
      lastdata = -1;
      lastdatalen = -1;
      lastbytes = null;
      lasttag = -1;
    }

    protected boolean isCancelled() {
      return cancelled;
    }

    protected boolean waitIfPaused() {
      return pauseHelper.waitUntilUnpause();
    }

    protected long transfer(SocketChannel socketIn, int downloaded,
        long remaining) throws IOException {
      bytes.position((int) (offset + downloaded));
      bytes.limit(bytes.position() + (int) Math.min(1024, remaining));
      int count = socketIn.read(bytes);
      if (count != -1) {
        // make the charsequence include all characters that have been
        // received
        chars.setLength(bytes.position());

        // figure out what to do with the data
        if (mode == Mode.MESSAGE) {
          int idx = chars.indexOf("<BINARY>");
          if (idx != -1) {
            lasttag = idx + "<BINARY>".length();
            mode = Mode.TAG;
            String message = new String(bytes.array(), 0, idx, charset);
            eventPost.fireEvent(new ReceivedMessageEvent(message,
                isAutoResponse(header)));
          }
        }

        if (mode == Mode.TAG) {
          int idx = chars.indexOf(">", lasttag);
          if (idx != -1) {
            // the data starts after the ">"
            DynAsciiCharSequence tag = chars.subSequence(lasttag, idx + 1);
            Matcher sizem = PATTERN_SIZE.matcher(tag);
            Matcher idm = PATTERN_ID.matcher(tag);
            if (sizem.find() && idm.find()) {
              lasttag = -1;
              lastid = idm.group(1);
              lastdata = idx + 1;
              lastdatalen = Integer.parseInt(sizem.group(1));
              lastbytes = ByteBlock.wrap(bytes.array(), lastdata,
                  lastdatalen);
              mode = Mode.DATA;
            }
          }
        }

        if (mode == Mode.DATA) {
          int attachmentPos = bytes.position() - lastdata;
          if (attachmentPos < lastdatalen) {
            eventPost.fireEvent(new ReceivingAttachmentEvent(
                bytes.position(), datalen, lastid, attachmentPos,
                lastdatalen, lastbytes));

          } else {
            // we got all of the data!
            eventPost.fireEvent(new ReceivedAttachmentEvent(lastid, lastbytes));
            lastid = null;
            lastdata = -1;
            lastdatalen = -1;
            lastbytes = null;
            mode = Mode.TAG;
          }
        }
      }
      return count;
    }

    public Mode getLastMode() { return mode; }
  }
}
