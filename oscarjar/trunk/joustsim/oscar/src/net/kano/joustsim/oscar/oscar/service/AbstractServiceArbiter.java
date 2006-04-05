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

package net.kano.joustsim.oscar.oscar.service;

import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.OscarConnListener;
import net.kano.joustsim.oscar.oscar.OscarConnStateEvent;
import net.kano.joustsim.JavaTools;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public abstract class AbstractServiceArbiter<S extends MutableService>
    implements ServiceArbiter<S> {
  private static final Logger LOGGER = Logger
      .getLogger(AbstractServiceArbiter.class.getName());

  protected final ServiceArbitrationManager manager;
  protected final Set<ServiceArbiterRequest> requests
      = new LinkedHashSet<ServiceArbiterRequest>();
  protected S currentService = null;

  public AbstractServiceArbiter(ServiceArbitrationManager manager) {
    this.manager = manager;
  }

  public abstract int getSnacFamily();

  public synchronized boolean shouldKeepAlive() {
    return !requests.isEmpty();
  }

  private void dequeueRequests(S service) {
    List<ServiceArbiterRequest> requests;
    synchronized (this) {
      requests = new ArrayList<ServiceArbiterRequest>(this.requests);
    }
    for (ServiceArbiterRequest request : requests) {
      processRequest(service, request);
    }
    handleRequestsDequeuedEvent(service);
  }

  /**
   * This is called after all requests have been dequeued due to a new service
   * coming online. Most arbiters do not need to imlpement this method.
   */
  protected abstract void handleRequestsDequeuedEvent(S service);

  protected abstract void processRequest(S service,
      ServiceArbiterRequest request);

  public final S createService(AimConnection aimConnection,
      OscarConnection conn) {
    final S service = createServiceInstance(aimConnection, conn);
    conn.addOscarListener(new OscarConnListener() {
      public void registeredSnacFamilies(OscarConnection conn) {
      }

      public void connStateChanged(OscarConnection conn,
          OscarConnStateEvent event) {
        LOGGER.info("Conn state changed for " + AbstractServiceArbiter.this + ": "
            + event.getClientConnEvent().getNewState());
      }

      public void allFamiliesReady(OscarConnection conn) {
        LOGGER.fine("Dequeueing requests upon connecting to " + service);
        dequeueRequests(service);
      }
    });
    service.addServiceListener(new ServiceListener() {
      public void handleServiceReady(Service s) {
        synchronized (AbstractServiceArbiter.this) {
          currentService = service;
        }
      }

      public void handleServiceFinished(Service s) {
        synchronized (AbstractServiceArbiter.this) {
          if (currentService == service) {
            currentService = null;
          }
        }
      }
    });
    return service;
  }

  protected abstract S createServiceInstance(AimConnection aimConnection,
      OscarConnection conn);

  protected final void addRequest(ServiceArbiterRequest req) {
    addRequestImpl(req, null);
  }

  private <R extends ServiceArbiterRequest> void addRequestImpl(R req,
      @Nullable Class<R> unique) {
    S service;
    synchronized (this) {
      if (unique != null) {
        for (Iterator<ServiceArbiterRequest> it = requests.iterator();
            it.hasNext();) {
          if (unique.isInstance(it.next())) it.remove();
        }
      }
      requests.add(req);
      service = currentService;
    }
    if (service == null) {
      manager.openService(this);
    } else {
      processRequest(service, req);
    }
  }

  protected <R extends ServiceArbiterRequest> void addUniqueRequest(R req,
      Class<R> unique) {
    if (unique == null) {
      throw new IllegalArgumentException("class cannot be null");
    }
    addRequestImpl(req, unique);
  }

  protected synchronized @Nullable <E> E getRequest(Class<E> cls) {
    E found = null;
    for (ServiceArbiterRequest request : requests) {
      if (cls.isInstance(request)) {
        if (found == null) {
          found = JavaTools.cast(cls, request);
        } else {
          throw new IllegalArgumentException("Multiple instances of "
              + cls + " in requests: " + requests);
        }
      }
    }
    return found;
  }

  protected synchronized void removeRequest(ServiceArbiterRequest req) {
    //noinspection StatementWithEmptyBody
    while (requests.remove(req)) ;
  }

  protected synchronized void removeRequests(RequestProcessor processor) {
    for (Iterator it = requests.iterator(); it.hasNext();) {
      ServiceArbiterRequest request = (ServiceArbiterRequest) it.next();
      if (processor.shouldRemove(request)) it.remove();
    }
  }

  protected interface RequestProcessor {
    boolean shouldRemove(ServiceArbiterRequest request);
  }
}
