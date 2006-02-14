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
import net.kano.joscar.ImEncodedString;
import net.kano.joscar.ImEncodingParams;
import net.kano.joscar.rvproto.directim.DirectImHeader;
import net.kano.joustsim.oscar.oscar.service.icbm.DirectMessage;
import net.kano.joustsim.oscar.oscar.service.icbm.Message;
import net.kano.joustsim.oscar.oscar.service.icbm.TypingState;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.ConnectionType;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.Initiator;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvSessionConnectionInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.AbstractTransferrer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.PausableController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.PauseHelper;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.PauseHelperImpl;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.TimeoutableController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectedEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectionTimedOutEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirectimController
    extends StateController
    implements PausableController, Cancellable, TimeoutableController {
  private static final Logger LOGGER = Logger
      .getLogger(DirectimController.class.getName());

  private static final ByteBlock TAG_BINARY
      = ByteBlock.wrap(BinaryTools.getAsciiBytes("<BINARY>"));
  private static final ByteBlock TAG_SBINARY
      = ByteBlock.wrap(BinaryTools.getAsciiBytes("</BINARY>"));
  public static final ByteBlock TAG_SDATA
      = ByteBlock.wrap(BinaryTools.getAsciiBytes("</DATA>"));

  private DirectimConnection transfer;
  private StreamInfo stream;
  private Thread recvThread;
  private Thread sendThread;
  private volatile boolean cancelled = false;
  private PauseHelper pauseHelper = new PauseHelperImpl();
  private final List<Object> queue = new ArrayList<Object>();
  private static final Object INIT = new Object();

  private final Object icbmIdLock = new Object();
  private boolean icbmIdConfirmed = false;

  public void start(RvConnection conn, StateController last) {
    this.transfer = (DirectimConnection) conn;

    if (transfer.getRvSessionInfo().getInitiator() == Initiator.BUDDY) {
      setIcbmIdConfirmed();
    }
    stream = (StreamInfo) last.getEndStateInfo();
    transfer.getTimeoutHandler().startTimeout(this);

    recvThread = new Thread(new Runnable() {
      public void run() {
        try {
          receiveInThread();
        } catch (Exception e) {
          fireFailed(e);
        }
      }
    }, "Direct IM reader");
    recvThread.setDaemon(true);
    recvThread.start();

    queue.add(INIT);
    sendThread = new Thread(new Runnable() {
      public void run() {
        while (true) {
          if (!waitForIcbmIdConfirmation()) {
            return;
          }
          List<Object> newItems;
          synchronized(queue) {
            if (queue.isEmpty()) {
              try {
                queue.wait();
              } catch (InterruptedException e) {
                break;
              }
            }
            newItems = new ArrayList<Object>(queue);
            queue.clear();
          }
          for (Object item : newItems) {
            try {
              if (item == INIT) {
                RvSessionConnectionInfo rvinfo = transfer.getRvSessionInfo();
                if (rvinfo.getInitiator() == Initiator.BUDDY) {
                  DirectImHeader header = new DirectImHeader();
                  header.setDefaults();
                  header.setScreenname(transfer.getMyScreenname().getFormatted());
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

            } catch (IOException e) {
              fireFailed(e);

            } catch (Exception e) {
              LOGGER.log(Level.SEVERE, "Error while processing DIM queue", e);
            }
          }
        }
      }

      private void reallySendMessage(Message message) throws IOException {
        ImEncodedString str = ImEncodedString.encodeString(message.getMessageBody());
        DirectImHeader header = DirectImHeader.createMessageHeader(str,
            message.isAutoResponse());
        header.setScreenname(transfer.getMyScreenname().getFormatted());
        List<AttachmentInfo> attachmentInfos = new ArrayList<AttachmentInfo>();
        if (message instanceof DirectMessage) {
          DirectMessage msg = (DirectMessage) message;
          Map<String,AttachmentDestination> attachments = msg.getAttachments();
          if (!attachments.isEmpty()) {
            long length = header.getDataLength();
            length += TAG_BINARY.getLength();
            length += TAG_SBINARY.getLength();
            for (Map.Entry<String,AttachmentDestination> entry : attachments.entrySet()) {
              AttachmentDestination data = entry.getValue();
              String id = entry.getKey();
              ByteBlock prefix = ByteBlock.wrap(BinaryTools.getAsciiBytes(
                  "<DATA ID=\"" + id + "\" SIZE=\""
                  + data.getLength() + "\">"));
              ByteBlock suffix = TAG_SDATA;
              attachmentInfos.add(new AttachmentInfo(id, prefix, suffix, data));
              length += prefix.getLength() + suffix.getLength() + data.getLength();
            }
            header.setDataLength(length);
          }
        }
        OutputStream out = stream.getOutputStream();
        header.write(out);
        ByteBuffer msgData = ByteBuffer.wrap(str.getBytes());
        SocketChannel chan = stream.getSocketChannel();
        Selector selector = Selector.open();
        chan.register(selector, SelectionKey.OP_WRITE);
        EventPost post = transfer.getEventPost();
        int length = msgData.limit();
        while (msgData.hasRemaining() && selector.isOpen() && chan.isOpen()) {
          selector.select(50);
          post.fireEvent(new SendingMessageEvent(msgData.position(), length));
          int i = chan.write(msgData);
          if (i == -1) {
            fireFailed(new UnknownErrorEvent());
            return;
          }
        }
        post.fireEvent(new SentMessageEvent(length));
        if (!attachmentInfos.isEmpty()) {
          TAG_BINARY.write(out);
          int attachno = 0;
          int numattachments = attachmentInfos.size();
          for (AttachmentInfo attachment : attachmentInfos) {
            AttachmentDestination data = attachment.data;
            AttachmentSender sender = new AttachmentSender(chan, data, post,
                attachment.id, attachno, numattachments);
            if (sender.getPosition() != data.getLength()) {
              fireFailed(new UnknownErrorEvent());
            }
            attachno++;
          }
          TAG_SBINARY.write(out);
        }
        post.fireEvent(new SentCompletePacketEvent());
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
    }, "Direct IM queue");
    sendThread.setDaemon(true);
    sendThread.start();
  }

  @Nullable public ConnectionType getTimeoutType() {
    return null;
  }

  public void cancelIfNotFruitful(long timeout) {
    if (!isIcbmIdConfirmed()) {
      fireFailed(new ConnectionTimedOutEvent(timeout));
    }
  }

  private class AttachmentInfo {
    public final String id;
    public final ByteBlock prefix;
    public final ByteBlock suffix;
    public final AttachmentDestination data;

    public AttachmentInfo(String id, ByteBlock prefix, ByteBlock suffix,
        AttachmentDestination data) {
      this.prefix = prefix;
      this.id = id;
      this.suffix = suffix;
      this.data = data;
    }
  }

  public void pauseTransfer() {
    pauseHelper.setPaused(true);
  }

  public void unpauseTransfer() {
    pauseHelper.setPaused(false);
  }

  private void receiveInThread() throws IOException {
    transfer.getEventPost().fireEvent(new ConnectedEvent());

    InputStream is = stream.getInputStream();
    while (true) {
      DirectImHeader header = DirectImHeader.readDirectIMHeader(is);
      if (header == null) {
        fireFailed(new UnknownErrorEvent());
        break;
      }

      long datalen = header.getDataLength();
      long flags = header.getFlags();
      EventPost eventPost = transfer.getEventPost();
      if ((flags & DirectImHeader.FLAG_TYPINGPACKET) != 0) {
        eventPost.fireEvent(new BuddyTypingEvent(getTypingState(flags)));
      }
      RvSessionConnectionInfo rvinfo = transfer.getRvSessionInfo();
      if (!isIcbmIdConfirmed() && rvinfo.getInitiator() == Initiator.ME) {
        long realid = rvinfo.getRvSession().getRvSessionId();
        if ((flags & DirectImHeader.FLAG_CONFIRMATION) != 0
            && header.getMessageId() == realid) {
          setIcbmIdConfirmed();

        } else {
          LOGGER.warning("Buddy sent wrong confirmation ICBM ID: "
              + header.getMessageId() + " should be " + realid);
          fireFailed(new UnknownErrorEvent());
          return;
        }
      }
      if (datalen > 0) {
        LOGGER.fine("Read header; reading packet of " + datalen + " bytes");
        String charset = header.getEncoding().toCharsetName();
        if (charset == null) charset = "US-ASCII";

        DirectimReceiver receiver = new DirectimReceiver(stream, eventPost,
            getPauseHelper(), transfer.getAttachmentSaver(), this, charset,
            datalen, DirectimReceiver.isAutoResponse(header));
        long transferred = receiver.transfer();

        if (transferred != datalen) {
          LOGGER.info("Position at end was " + transferred + ", expected "
              + datalen);
          fireFailed(new UnknownErrorEvent());
          break;
        }

        eventPost.fireEvent(new DoneReceivingEvent());
      }
    }
  }

  private void setIcbmIdConfirmed() {
    synchronized (icbmIdLock) {
      if (icbmIdConfirmed) return;

      icbmIdConfirmed = true;
      icbmIdLock.notifyAll();
    }
  }

  private boolean isIcbmIdConfirmed() {
    synchronized (icbmIdLock) {
      return icbmIdConfirmed;
    }
  }

  private boolean waitForIcbmIdConfirmation() {
    synchronized(icbmIdLock) {
      while (!icbmIdConfirmed) {
        try {
          icbmIdLock.wait();
        } catch (InterruptedException e) {
          return false;
        }
      }
      return icbmIdConfirmed;
    }
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

  public void setTypingState(TypingState state) {
    enqueue(state);
  }

  private void enqueue(Object o) {
    synchronized(queue) {
      queue.add(o);
      queue.notifyAll();
    }
  }

  public void sendMessage(Message message) {
    if (message instanceof DirectMessage) {
      DirectMessage dim = (DirectMessage) message;
      for (String id : dim.getAttachments().keySet()) {
        if (id.matches(".*(\"|<|>).*")) {
          throw new IllegalArgumentException("Attachment ID " + id
              + " is invalid: illegal character \"<>");
        }
      }
    }
    enqueue(message);
  }

  public void stop() {
    LOGGER.info("Stopping directim controller");
    cancelled = true;
    if (recvThread != null) recvThread.interrupt();
    if (sendThread != null) sendThread.interrupt();
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public PauseHelper getPauseHelper() {
    return pauseHelper;
  }

  private static class AttachmentSender extends AbstractTransferrer<SocketChannel> {
    private ByteBuffer buf;
    private ReadableByteChannel inchan;
    private final AttachmentDestination data;
    private final EventPost post;
    private final String id;
    private final int attachno;
    private final int numattachments;

    public AttachmentSender(SocketChannel chan, AttachmentDestination data,
        EventPost post, String id, int attachno, int numattachments)
        throws FileNotFoundException {
      super(chan, chan, 0, data.getLength());
      this.data = data;
      this.post = post;
      this.id = id;
      this.attachno = attachno;
      this.numattachments = numattachments;
      buf = ByteBuffer.allocate(1024);
      inchan = data.openForReading();
    }

    protected int getSelectionKey() {
      return SelectionKey.OP_WRITE;
    }

    protected boolean isCancelled() {
      return false;
    }

    protected boolean waitIfPaused() {
      return false;
    }

    protected long transfer(SocketChannel channel, long transferred,
        long remaining) throws IOException {
      post.fireEvent(new SendingAttachmentEvent(id, transferred,
          data.getLength(), attachno, numattachments));
      buf.rewind();
      buf.limit(buf.capacity());
      inchan.read(buf);
      buf.flip();
      return channel.write(buf);
    }
  }

}
