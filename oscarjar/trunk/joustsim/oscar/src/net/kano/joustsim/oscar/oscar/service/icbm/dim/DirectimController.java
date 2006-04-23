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

import net.kano.joscar.rvproto.directim.DirectImHeader;
import net.kano.joustsim.oscar.oscar.service.icbm.DirectMessage;
import net.kano.joustsim.oscar.oscar.service.icbm.Message;
import net.kano.joustsim.oscar.oscar.service.icbm.TypingState;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.ConnectionType;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.Initiator;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvSessionConnectionInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.PausableController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.PauseHelper;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.PauseHelperImpl;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.AbstractStateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.TimeoutableController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectedController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectedEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectionTimedOutEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.LocallyCancelledEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirectimController extends AbstractStateController
    implements PausableController, Cancellable, TimeoutableController,
    ConnectedController {
  private static final Logger LOGGER = Logger
      .getLogger(DirectimController.class.getName());

  private DirectimConnection connection;
  private StreamInfo stream;
  private Thread recvThread;
  private Thread sendThread;
  private volatile boolean cancelled = false;
  private PauseHelper pauseHelper = new PauseHelperImpl();

  private final List<Object> queue = new ArrayList<Object>();
  private DirectimQueueProcessor queueProcessor = null;

  private final Object icbmIdLock = new Object();
  private boolean icbmIdConfirmed = false;

  public void start(RvConnection conn, StateController last) {
    this.connection = (DirectimConnection) conn;

    if (connection.getRvSessionInfo().getInitiator() == Initiator.BUDDY) {
      setIcbmIdConfirmed();
    }
    stream = (StreamInfo) last.getEndStateInfo();
    connection.getTimeoutHandler().startTimeout(this);

    queueProcessor = new DirectimQueueProcessor(this, connection, stream);

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

    queue.add(DirectimQueueProcessor.INIT);
    sendThread = new Thread(new DimQueue(), "Direct IM queue");
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

  public boolean isConnected() {
    return isIcbmIdConfirmed();
  }

  public boolean didConnect() {
    return isConnected();
  }

  public void pauseTransfer() {
    pauseHelper.setPaused(true);
  }

  public void unpauseTransfer() {
    pauseHelper.setPaused(false);
  }

  private void receiveInThread() throws IOException {
    LOGGER.fine("Starting DIM receiver for " + DirectimController.this);
    connection.getEventPost().fireEvent(new ConnectedEvent());

    InputStream is = stream.getInputStream();
    while (true) {
      DirectImHeader header = DirectImHeader.readDirectIMHeader(is);
      if (header == null) {
        LOGGER.info("Could not read header in " + this);
        fireFailed(new UnknownErrorEvent());
        break;
      }

      long datalen = header.getDataLength();
      long flags = header.getFlags();
      EventPost eventPost = connection.getEventPost();
      if ((flags & DirectImHeader.FLAG_TYPINGPACKET) != 0) {
        eventPost.fireEvent(new BuddyTypingEvent(getTypingState(flags)));
      }
      RvSessionConnectionInfo rvinfo = connection.getRvSessionInfo();
      if (!isIcbmIdConfirmed() && rvinfo.getInitiator() == Initiator.ME) {
        long realid = rvinfo.getRvSession().getRvSessionId();
        if ((flags & DirectImHeader.FLAG_CONFIRMATION) != 0
            && header.getMessageId() == realid) {
          setIcbmIdConfirmed();

        } else {
          LOGGER.warning("Buddy sent wrong confirmation ICBM ID: "
              + header.getMessageId() + " should be " + realid);
          fireFailed(new UnknownErrorEvent());
          break;
        }
      }
      if (datalen > 0) {
        LOGGER.fine("Read header; reading packet of " + datalen + " bytes");
        String charset = header.getEncoding().toCharsetName();
        if (charset == null) charset = "ISO-8859-1";

        DirectimReceiver receiver = new DirectimReceiver(stream, eventPost,
            getPauseHelper(), connection.getAttachmentSaver(), this, charset,
            datalen, isAutoResponse(header));
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
    LOGGER.fine("Done with DIM receiver for " + this);
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
      for (Attachment attachment : dim.getAttachments()) {
        if (attachment.getId().matches(".*(\"|<|>).*")) {
          throw new IllegalArgumentException("Attachment ID " + attachment
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
    if (didConnect()) {
      fireSucceeded(new DirectimEndedInfo());
    } else {
      fireFailed(new LocallyCancelledEvent());
    }
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public PauseHelper getPauseHelper() {
    return pauseHelper;
  }

  public static boolean isAutoResponse(DirectImHeader header) {
    return (header.getFlags() & DirectImHeader.FLAG_AUTORESPONSE) != 0;
  }

  private class DimQueue implements Runnable {
    public void run() {
      LOGGER.fine("Starting DIM queue thread for " + DirectimController.this);
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
        for (Iterator<Object> it = newItems.iterator(); it.hasNext();) {
          Object item = it.next();
          try {
            queueProcessor.processItem(item);
            it.remove();

          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while processing DIM queue", e);
            fireItemsFailed(newItems);
            fireFailed(e);
          }
        }
      }
      synchronized(queue) {
        fireItemsFailed(queue);
      }
      LOGGER.fine("Done with DIM queue for " + DirectimController.this);
    }

    private void fireItemsFailed(List<Object> newItems) {
      for (Object failed : newItems) {
        if (failed instanceof Message) {
          Message message = (Message) failed;
          connection.getEventPost().fireEvent(
              new SendingMessageFailedEvent(message));
        }
      }
    }

    protected void processItem(Object item) throws IOException {
      queueProcessor.processItem(item);
    }
  }

}
