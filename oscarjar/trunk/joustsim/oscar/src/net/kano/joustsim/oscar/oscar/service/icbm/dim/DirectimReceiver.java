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

import net.kano.joscar.DynAsciiCharSequence;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.rvproto.directim.DirectImHeader;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.AbstractTransferrer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.PauseHelper;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirectimReceiver extends AbstractTransferrer<ReadableByteChannel> {
  private static final Logger LOGGER = Logger
      .getLogger(DirectimReceiver.class.getName());

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
  private boolean autoResponse;

  public static boolean isAutoResponse(DirectImHeader header) {
    return (header.getFlags() & DirectImHeader.FLAG_AUTORESPONSE) != 0;
  }

  private final String charset;
  private final AttachmentSaver saver;
  private final EventPost eventPost;
  private final @Nullable PauseHelper pauseHelper;
  private final @Nullable Cancellable cancellable;

  private ByteBuffer buffer;
  private DynAsciiCharSequence chars;
  {
    resizeBuffer(1024);
  }

  private ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();

  private @Nullable Selector selector;
  private Mode mode = Mode.MESSAGE;
  private String lastid = null;
  private AttachmentDestination lastDestination = null;
  private long lastAttachmentReceived = 0;
  private Long lastAttachmentSize = null;
  private boolean checkbuffer = false;
  private WritableByteChannel destchannel = null;

  public DirectimReceiver(StreamInfo stream,
      EventPost eventPost, @Nullable PauseHelper pauseHelper,
      AttachmentSaver saver, @Nullable Cancellable cancellable,
      String charset, long datalen, boolean autoResponse) {
    this(eventPost, pauseHelper, saver, cancellable, charset, datalen,
        stream.getSocketChannel(), stream.getSocketChannel(), autoResponse);
  }

  public DirectimReceiver(EventPost eventPost, AttachmentSaver saver,
      String charset, long datalen, ReadableByteChannel readable,
      boolean autoResponse) {
    this(eventPost, null, saver, null, charset, datalen, readable, null,
        autoResponse);
  }

  public DirectimReceiver(
      EventPost eventPost, @Nullable PauseHelper pauseHelper,
      AttachmentSaver saver, @Nullable Cancellable cancellable,
      String charset, long datalen, ReadableByteChannel readable,
      @Nullable SelectableChannel selectable, boolean autoResponse) {
    super(readable, selectable, 0, datalen);

    this.cancellable = cancellable;
    this.charset = charset;
    this.saver = saver;
    this.eventPost = eventPost;
    this.pauseHelper = pauseHelper;
    this.autoResponse = autoResponse;
  }

  public void resizeBuffer(int size) {
    buffer = ByteBuffer.allocate(size);
    chars = new DynAsciiCharSequence(ByteBlock.wrap(buffer.array()));
  }

  protected boolean isCancelled() {
    return cancellable != null && cancellable.isCancelled();
  }

  protected boolean waitIfPaused() {
    return pauseHelper != null && pauseHelper.waitUntilUnpause();
  }

  protected void waitUntilReady() throws IOException {
    // Sometimes extra data is left in the buffer, and transfer() should be
    // called again immediately, to continue processing the buffer.
    if (checkbuffer) return;

    if (mode == Mode.DATA && selector != null) {
      selector.select(50);
    }
    if (buffer.position() == 0) {
      // we only want to call the super waitUntilReady if the buffer is empty
      // and we read some data last time
      super.waitUntilReady();
    }
  }

  protected long transfer(ReadableByteChannel channel, long transferred,
      long remaining) throws IOException {
    int origpos = buffer.position();
    if (!checkbuffer && ((mode == Mode.MESSAGE || mode == Mode.TAG) && buffer.remaining() == 0)) {
      LOGGER.warning("DIM buffer full; entering drain mode from " + mode);
      eventPost.fireEvent(new EnteringDrainModeEvent(remaining));
      mode = Mode.DRAIN;
      checkbuffer = false;
      buffer.rewind();
      buffer.limit(buffer.capacity());
      return origpos;
    }
    buffer.limit((int) Math.min(buffer.capacity(), buffer.position()
        + remaining));
    int read = channel.read(buffer);
    int actuallyRead = Math.max(read, 0);
    if (!checkbuffer && (read == -1 || mode == Mode.DRAIN)) {
      int skipped = buffer.position();
      buffer.rewind();
      buffer.limit(buffer.capacity());
      long progress = transferred + actuallyRead;
      long total = transferred + remaining;
      eventPost.fireEvent(new DrainingEvent(progress, total));
      checkbuffer = false;
      return skipped;
    }
    checkbuffer = (read > 0);
    chars.setLength(buffer.position());
    if (mode == Mode.MESSAGE || mode == Mode.TAG || mode == Mode.DRAIN) {
      if (mode == Mode.MESSAGE) {
        int binaryPos = chars.indexOf("<BINARY>");
        if (binaryPos != -1) {
          int firstDataTag = binaryPos + "<BINARY>".length();
          // write the last part of the message to the msg buffer
          msgBuffer.write(buffer.array(), 0, binaryPos);
          buffer.position(firstDataTag);
          buffer.compact();
          String message = new String(msgBuffer.toByteArray(), charset);

          mode = Mode.TAG;
          checkbuffer = true;
          msgBuffer = null;

          eventPost.fireEvent(new ReceivedMessageEvent(message,
              autoResponse));
          return firstDataTag;

        } else {
          int writelen;
          if (remaining - actuallyRead <= 7) {
            writelen = buffer.position();
          } else {
            writelen = Math.max(0, buffer.position() - 7);
          }
          if (writelen > 0) {
            msgBuffer.write(buffer.array(), 0, writelen);
            String message = new String(msgBuffer.toByteArray(), charset);
            eventPost.fireEvent(new ReceivingMessageEvent(msgBuffer.size(),
                transferred + remaining, message));
          }
          int endpos = buffer.position();
          buffer.position(writelen);
          buffer.limit(endpos);
          buffer.compact();
          return writelen;
        }

      } else if (mode == Mode.TAG) {
        int closeBracket = chars.indexOf(">");
        if (closeBracket != -1) {
          // the data starts after the ">"
          CharSequence tag = chars.subSequence(0, closeBracket + 1);
          Matcher sizem = PATTERN_SIZE.matcher(tag);
          if (sizem.find()) {
            try {
              lastAttachmentSize = Long.parseLong(sizem.group(1));
            } catch (NumberFormatException e) {
              // who cares, we'll look for it later
            }
          }
          Matcher idm = PATTERN_ID.matcher(tag);
          if (idm.find()) {
            lastid = idm.group(1);
          }
          if (lastAttachmentSize != null && lastid != null) {
            mode = Mode.DATA;

            lastDestination = saver.createChannel(lastid, lastAttachmentSize);
            destchannel = lastDestination.getWritable();
            SelectableChannel destinationSel = lastDestination.getSelectable();
            if (destinationSel != null) {
              selector = Selector.open();
              destinationSel.register(selector, SelectionKey.OP_WRITE);
            } else {
              selector = null;
            }
            checkbuffer = true;
          }
          buffer.limit(buffer.position());
          buffer.position(closeBracket + 1);
          buffer.compact();
        }/*
        if (buffer.position() >= remaining) {
          // we're at the end of the stream, there aren't any tags left, let's
          // bail out
          return remaining;
        }*/
      }
      // if we read some data from the socket, but we buffered it and it's in
      // the compacted part of the buffer, it hasn't been processed yet
      int transferredNow = actuallyRead + origpos - buffer.position();
      return transferredNow;

    } else if (mode == Mode.DATA) {
      buffer.flip();
      int origLimit = buffer.limit();
      if (lastAttachmentReceived + buffer.remaining() > lastAttachmentSize) {
        buffer.limit((int) (lastAttachmentSize - lastAttachmentReceived));
      }
      int wrote = destchannel.write(buffer);
      if (wrote == -1) {
        return -1;
      }
      lastAttachmentReceived += wrote;
      eventPost.fireEvent(new ReceivingAttachmentEvent(transferred + wrote,
          transferred + remaining, lastAttachmentReceived, lastDestination));
      if (lastAttachmentReceived >= lastAttachmentSize) {
        eventPost.fireEvent(new ReceivedAttachmentEvent(lastid,
            lastAttachmentSize, lastDestination));
        mode = Mode.TAG;
        lastid = null;
        lastAttachmentReceived = 0;
        lastAttachmentSize = null;
        lastDestination = null;
        try {
          destchannel.close();
        } catch (IOException e) {
          LOGGER.log(Level.WARNING, "Error closing attachment saver", e);
        }
        destchannel = null;
        if (selector != null) {
          try {
            selector.close();
          } catch (IOException e) {
            LOGGER.log(Level.SEVERE,
                "Error closing attachment NIO selector", e);
          }
        }
        checkbuffer = true;
      }
      if (wrote != read) checkbuffer = true;
      buffer.limit(origLimit);
      buffer.compact();
      return wrote;

    } else {
      throw new IllegalStateException("Unknown mode " + mode);
    }
  }

  protected void cleanUp() throws IOException {
    if (mode == Mode.MESSAGE) {
      // we must have never hit a <BINARY> part, and so never fired a
      // message event
      msgBuffer.write(buffer.array(), 0, buffer.position());
      String msg = new String(msgBuffer.toByteArray(), charset);
      eventPost.fireEvent(new ReceivedMessageEvent(msg,
          autoResponse));
    } else if (mode == Mode.DATA) {

    }
    if (selector != null) {
      selector.close();
    }
  }

  protected int getSelectionKey() {
    return SelectionKey.OP_READ;
  }

  private static enum Mode { MESSAGE, TAG, DATA, DRAIN }
}
