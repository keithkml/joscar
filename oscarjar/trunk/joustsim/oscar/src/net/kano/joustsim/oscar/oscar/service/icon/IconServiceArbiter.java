/*
 *  Copyright (c) 2005, The Joust Project
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *  - Neither the name of the Joust Project nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 */

package net.kano.joustsim.oscar.oscar.service.icon;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.Writable;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joscar.snaccmd.icon.IconCommand;
import net.kano.joustsim.JavaTools;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.AbstractServiceArbiter;
import net.kano.joustsim.oscar.oscar.service.ServiceArbiterRequest;
import net.kano.joustsim.oscar.oscar.service.ServiceArbitrationManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public class IconServiceArbiter
    extends AbstractServiceArbiter<IconServiceImpl>
    implements IconRequestHandler {
  private CopyOnWriteArrayList<IconRequestListener> listeners
      = new CopyOnWriteArrayList<IconRequestListener>();
  private IconRequestListener delegatingListener
      = JavaTools.getDelegatingProxy(listeners, IconRequestListener.class);

  public IconServiceArbiter(ServiceArbitrationManager manager) {
    super(manager);
  }

  public int getSnacFamily() {
    return IconCommand.FAMILY_ICON;
  }

  protected boolean shouldKeepAliveSub() { return false; }

  public void addIconRequestListener(IconRequestListener listener) {
    listeners.addIfAbsent(listener);
  }

  public void removeIconRequestListener(IconRequestListener listener) {
    listeners.remove(listener);
  }

  public void requestIcon(Screenname sn, ExtraInfoData hashBlock) {
	  if (sn != null) {
		  addRequest(new RequestedIconInfo(sn, hashBlock));
	  } else {
		  Logger LOGGER = Logger
		  .getLogger(IconServiceArbiter.class.getName());

		  LOGGER.warning("Arbiter "
						 + this + "was told to requestIcon with no screenname; hashBlock is"
						 + hashBlock);  
	  }
  }

  public void uploadIcon(Writable data) {
    addUniqueRequest(new UploadIconRequest(data), UploadIconRequest.class);
  }

  protected void handleRequestsDequeuedEvent(IconServiceImpl service) {
  }

  protected void processRequest(IconServiceImpl service,
      ServiceArbiterRequest request) {
    if (request instanceof RequestedIconInfo) {
      RequestedIconInfo iconInfo = (RequestedIconInfo) request;
      service.requestIcon(iconInfo.screenname,
          iconInfo.iconHash);

    } else if (request instanceof UploadIconRequest) {
      UploadIconRequest uploadReq = (UploadIconRequest) request;

      service.uploadIcon(uploadReq.data, new IconSetListener() {
        public void handleIconSet(IconService service, Writable data,
            boolean succeeded) {
          synchronized (this) {
            UploadIconRequest req = getRequest(UploadIconRequest.class);
            if (req != null && req.data == data) {
              removeRequest(req);
            }
          }
        }
      });
    }
  }

  protected IconServiceImpl createServiceInstance(AimConnection aimConnection,
      OscarConnection conn) {
    IconServiceImpl service = new IconServiceImpl(aimConnection, conn);
    service.addIconRequestListener(delegatingListener);
    return service;
  }

  private static class RequestedIconInfo implements ServiceArbiterRequest {
    public final Screenname screenname;
    public final ExtraInfoData iconHash;

    public RequestedIconInfo(Screenname screenname, ExtraInfoData iconHash) {
      this.iconHash = iconHash;
      this.screenname = screenname;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      RequestedIconInfo that = (RequestedIconInfo) o;

      if (!iconHash.equals(that.iconHash)) return false;
      if (!screenname.equals(that.screenname)) return false;

      return true;
    }

    public int hashCode() {
		if (screenname != null) {
			int result = screenname.hashCode();
			result = 29 * result + iconHash.hashCode();
			return result;
		} else {
			Logger LOGGER = Logger
			.getLogger(IconServiceArbiter.class.getName());
			
			LOGGER.warning("Arbiter "
						   + this + "was asked for a hashCode with no screenname.");			
			
			return 0;
		}
    }
  }

  private static class UploadIconRequest implements ServiceArbiterRequest {
    public final Writable data;

    public UploadIconRequest(Writable data) {
      this.data = data;
    }
  }
}
