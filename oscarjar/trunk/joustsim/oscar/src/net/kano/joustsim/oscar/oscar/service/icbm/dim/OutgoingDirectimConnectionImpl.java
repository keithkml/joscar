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
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.OutgoingRvConnectionImpl;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionState;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvRequestMaker;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvSessionConnectionInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendOverProxyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.SendPassivelyController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectedController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectionCompleteEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.UnknownErrorEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailedStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.proxy.AimProxyInfo;
import org.jetbrains.annotations.Nullable;

public class OutgoingDirectimConnectionImpl
    extends OutgoingRvConnectionImpl implements DirectimConnection {
  private AttachmentSaver attachmentSaver = new SizeBasedAttachmentSaver();

  public OutgoingDirectimConnectionImpl(AimProxyInfo proxy,
      Screenname screenname, RvSessionConnectionInfo rvsessioninfo) {
    super(proxy, screenname, rvsessioninfo);
  }

  public OutgoingDirectimConnectionImpl(AimProxyInfo proxy,
      Screenname screenname, RvSession session) {
    this(proxy, screenname, new MutableSessionConnectionInfo(session));
    ((MutableSessionConnectionInfo) getRvSessionInfo())
        .setMaker(new DirectimRequestMaker(this));
  }

  protected StateController getNextControllerFromUnknownError(
      StateController oldController, FailedStateInfo failedStateInfo,
      RvConnectionEvent event) {
    if (oldController instanceof DirectimController) {
      //TODO: retry dim with other controllers like file receiver does
      queueStateChange(RvConnectionState.FAILED,
          event == null ? new UnknownErrorEvent() : event);
      return null;

    } else {
      throw new IllegalStateException("unknown previous controller "
          + oldController);
    }
  }

  protected StateController getNextControllerFromSuccess(
      StateController oldController, StateInfo endState) {
    if (oldController instanceof DirectimController) {
      queueStateChange(RvConnectionState.FINISHED,
          new ConnectionCompleteEvent());
      return null;

    } else {
      throw new IllegalStateException("unknown previous controller "
          + oldController);
    }
  }

  protected ConnectedController createConnectedController(StateInfo endState) {
    return new DirectimController();
  }

  protected boolean isConnectedController(StateController controller) {
    return controller instanceof DirectimController;
  }

  public void sendRequest() {
    if (getSettings().isOnlyUsingProxy()) {
      startStateController(new SendOverProxyController());
    } else {
      startStateController(new SendPassivelyController());
    }
  }

  public RvRequestMaker getRvRequestMaker() {
    return new DirectimRequestMaker(this);
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
