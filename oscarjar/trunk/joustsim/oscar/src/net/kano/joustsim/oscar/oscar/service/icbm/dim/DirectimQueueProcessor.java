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
 * Date: Feb 20, 2006
 * Time: 4:25:39 PM
 */

package net.kano.joustsim.oscar.oscar.service.icbm.dim;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.ImEncodedString;
import net.kano.joscar.ImEncodingParams;
import net.kano.joscar.rvproto.directim.DirectImHeader;
import net.kano.joustsim.oscar.oscar.service.icbm.DirectMessage;
import net.kano.joustsim.oscar.oscar.service.icbm.Message;
import net.kano.joustsim.oscar.oscar.service.icbm.TypingState;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.Initiator;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvSessionConnectionInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class DirectimQueueProcessor {
  private static final Logger LOGGER = Logger
      .getLogger(DirectimQueueProcessor.class.getName());

  private static final ByteBlock TAG_BINARY
      = ByteBlock.wrap(BinaryTools.getAsciiBytes("<BINARY>"));
  private static final ByteBlock TAG_SBINARY
      = ByteBlock.wrap(BinaryTools.getAsciiBytes("</BINARY>"));
  public static final ByteBlock TAG_SDATA
      = ByteBlock.wrap(BinaryTools.getAsciiBytes("</DATA>"));
  public static final Object INIT = new Object();

  private final Cancellable cancellable;
  private final RvConnection connection;
  private final StreamInfo stream;

  public DirectimQueueProcessor(Cancellable cancellable,
      RvConnection connection, StreamInfo stream) {
    this.cancellable = cancellable;
    this.connection = connection;
    this.stream = stream;
  }

  protected void processItem(Object item) throws IOException {
    if (item == INIT) {
      RvSessionConnectionInfo rvinfo = connection.getRvSessionInfo();
      if (rvinfo.getInitiator() == Initiator.BUDDY) {
        DirectImHeader header = new DirectImHeader();
        header.setDefaults();
        header.setScreenname(connection.getMyScreenname().getFormatted());
        header.setFlags(DirectImHeader.FLAG_CONFIRMATION
            | DirectImHeader.FLAG_CONFIRMATION_UNKNOWN);
        header.setMessageId(rvinfo
            .getRvSession().getRvSessionId());
        header.setEncoding(new ImEncodingParams(
            ImEncodingParams.CHARSET_ASCII));

        OutputStream out = stream.getOutputStream();
        header.write(out);
      }

    } else if (item instanceof Message) {
      reallySendMessage((Message) item);

    } else if (item instanceof TypingState) {
      reallySendTypingState((TypingState) item);

    } else {
      LOGGER.warning("I don't understand what to do with " + item
          + " in directim queue");
    }
  }

  private void reallySendMessage(Message message) throws IOException {
    ImEncodedString str = ImEncodedString
        .encodeString(message.getMessageBody());
    DirectImHeader header = DirectImHeader.createMessageHeader(str,
        message.isAutoResponse());
    header.setScreenname(connection.getMyScreenname().getFormatted());
    List<AttachmentInfo> attachmentInfos
        = new ArrayList<AttachmentInfo>();
    if (message instanceof DirectMessage) {
      DirectMessage msg = (DirectMessage) message;
      Set<Attachment> attachments = msg.getAttachments();
      if (!attachments.isEmpty()) {
        long length = header.getDataLength();
        for (Attachment attachment : attachments) {
          String id = attachment.getId();
          ByteBlock prefix = ByteBlock.wrap(BinaryTools.getAsciiBytes(
              "<DATA ID=\"" + id + "\" SIZE=\""
                  + attachment.getLength() + "\">"));
          ByteBlock suffix = TAG_SDATA;
          attachmentInfos.add(new AttachmentInfo(id, prefix, suffix, attachment));
          length += TAG_BINARY.getLength();
          length += TAG_SBINARY.getLength();
          length += prefix.getLength() + suffix.getLength() + attachment.getLength();
        }
        header.setDataLength(length);
      }
    }
    OutputStream out = stream.getOutputStream();
    header.write(out);
    ByteBuffer msgData = ByteBuffer.wrap(str.getBytes());
    SelectableChannel chan = stream.getSelectableChannel();
    WritableByteChannel writable = stream.getWritableChannel();
    @Nullable Selector selector;
    if (chan == null) {
      selector = null;
    } else {
      selector = Selector.open();
      chan.register(selector, SelectionKey.OP_WRITE);
    }
    EventPost post = connection.getEventPost();
    int length = msgData.limit();
    while (msgData.hasRemaining() && (selector == null || selector.isOpen())
        && writable.isOpen()) {
      if (selector != null) {
        selector.select(50);
      }
      post.fireEvent(new SendingMessageEvent(msgData.position(), length));
      int i = writable.write(msgData);
      if (i == -1) {
        throw new IOException("Failed to write to channel");
      }
    }
    post.fireEvent(new SentMessageEvent(length));
    if (!attachmentInfos.isEmpty()) {
      int attachno = 0;
      int numattachments = attachmentInfos.size();
      for (AttachmentInfo attachmentInfo : attachmentInfos) {
        Attachment attachment = attachmentInfo.attachment;
        AttachmentSender sender = new AttachmentSender(stream, attachment, post,
            attachmentInfo.id, attachno, numattachments, cancellable);
        setupAttachmentSender(sender);
        TAG_BINARY.write(out);
        attachmentInfo.prefix.write(out);
        long transferred = sender.transfer();
        attachmentInfo.suffix.write(out);
        TAG_SBINARY.write(out);
        if (transferred != attachment.getLength()) {
          throw new IOException("Transferred " + transferred
              + ", should have transferred " + attachment.getLength());
        }
        attachno++;
      }
    }
    post.fireEvent(new SentCompletePacketEvent());
  }

  protected void setupAttachmentSender(AttachmentSender sender) {
  }

  private void reallySendTypingState(TypingState state) throws IOException {
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

  private static class AttachmentInfo {
    public final String id;
    public final ByteBlock prefix;
    public final ByteBlock suffix;
    public final Attachment attachment;

    public AttachmentInfo(String id, ByteBlock prefix, ByteBlock suffix,
        Attachment data) {
      this.prefix = prefix;
      this.id = id;
      this.suffix = suffix;
      this.attachment = data;
    }
  }

}
