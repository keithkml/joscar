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

import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.ConnectionRequestRvCmd;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousSessionHandler;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.IncomingRvConnectionImpl;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionState;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvSessionConnectionInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.AbstractIncomingRvSessionHandler;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectionCompleteEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;
import net.kano.joustsim.oscar.proxy.AimProxyInfo;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public class IncomingDirectimConnectionImpl
    extends IncomingRvConnectionImpl implements DirectimConnection {
  private static final Logger LOGGER = Logger
      .getLogger(IncomingDirectimConnectionImpl.class.getName());

  private AttachmentSaver attachmentSaver = new SizeBasedAttachmentSaver();

  public IncomingDirectimConnectionImpl(
      AimProxyInfo proxy, Screenname screenname,
      RvSessionConnectionInfo rvsessioninfo) {
    super(proxy, screenname, rvsessioninfo);
  }

  public IncomingDirectimConnectionImpl(AimProxyInfo proxy,
      Screenname screenname, RvSession session) {
    this(proxy, screenname, new MutableSessionConnectionInfo(session));
    ((MutableSessionConnectionInfo) getRvSessionInfo()).setMaker(new DirectimRequestMaker(this));
  }

  protected StateController getNextStateControllerFromSuccessState(
      StateController oldController, StateInfo oldStateInfo) {
    if (oldController instanceof DirectimController) {
      LOGGER.fine("Changing from success of receive controller to "
          + "completed");
      queueStateChange(RvConnectionState.FINISHED,
          new ConnectionCompleteEvent());
      return null;

    } else if (oldStateInfo instanceof StreamInfo) {
      LOGGER.fine("Got stream info; starting directim controller");
      return new DirectimController();

    } else {
      throw new IllegalStateException("Trying to change from success "
          + "state; unknown previous controller/state "
          + oldController + "/" + oldStateInfo);
    }
  }

  protected StateController getNextStateFromErrorWithUnknownController(
      StateController oldController, StateInfo oldState,
      RvConnectionEvent event) {
    if (oldController instanceof DirectimController) {
      queueStateChange(RvConnectionState.FAILED, event);
      return null;

    } else {
      throw new IllegalStateException("Trying to change from error "
          + "state; unknown previous controller/state "
          + oldController + "/" + oldState);
    }
  }

  protected RendezvousSessionHandler createSessionHandler() {
    return new AbstractIncomingRvSessionHandler(this) {
      protected void handleFirstRequest(ConnectionRequestRvCmd reqCmd) {
        // we're okay. there's no dim-specific stuff in the request.
      }
    };
  }

  @Nullable public DirectimController getDirectimController() {
    return DirectimTools.getDirectimStateController(this);
  }

  public AttachmentSaver getAttachmentSaver() {
    return attachmentSaver;
  }

  public void setAttachmentSaver(AttachmentSaver attachmentSaver) {
    this.attachmentSaver = attachmentSaver;
  }

}
