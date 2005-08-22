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

import net.kano.joscar.ByteBlock;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.Writable;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joscar.snaccmd.icon.IconCommand;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.ServiceArbiter;
import net.kano.joustsim.oscar.oscar.service.ServiceArbitrationManager;
import net.kano.joustsim.oscar.oscar.service.ServiceListener;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class IconServiceArbiter
        implements ServiceArbiter<IconService>, IconRequestHandler {
    private static final Logger LOGGER = Logger
            .getLogger(IconServiceArbiter.class.getName());

    private Set<RequestedIconInfo> requestedIconInfos
            = new LinkedHashSet<RequestedIconInfo>();
    private IconService currentService = null;
    private CopyOnWriteArrayList<IconRequestListener> listeners
            = new CopyOnWriteArrayList<IconRequestListener>();
    private ServiceArbitrationManager manager;
    private Writable iconToSet = null;

    public IconServiceArbiter(ServiceArbitrationManager manager) {
        this.manager = manager;
    }

    public int getSnacFamily() {
        return IconCommand.FAMILY_ICON;
    }

    public synchronized boolean shouldKeepAlive() {
        return !requestedIconInfos.isEmpty() || iconToSet != null;
    }

    public void addIconRequestListener(IconRequestListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeIconRequestListener(IconRequestListener listener) {
        listeners.remove(listener);
    }

    public void requestIcon(Screenname sn, ExtraInfoData hashBlock) {
        IconService service;
        synchronized (this) {
            requestedIconInfos.add(new RequestedIconInfo(sn, hashBlock));
            service = currentService;
        }
        if (service == null) {
            LOGGER.finer("Icon service is down; requesting for it to be opened");
            manager.openService(this);
        } else {
            service.requestIcon(sn, hashBlock);
        }
    }

    public void uploadIcon(Writable data) {
        IconService service;
        synchronized (this) {
            iconToSet = data;
            service = currentService;
        }

        if (service == null) {
            manager.openService(this);
        } else {
            uploadIcon(service, data);
        }
    }

    private void dequeueRequests(IconService service) {
        List<RequestedIconInfo> infos;
        Writable icon;
        synchronized (this) {
            infos = new ArrayList<RequestedIconInfo>(requestedIconInfos);
            icon = iconToSet;
        }
        for (RequestedIconInfo info : infos) {
            service.requestIcon(info.getScreenname(),
                    info.getIconHash());
        }
        if (icon != null) {
            uploadIcon(service, icon);
        }
    }

    private void uploadIcon(IconService service, Writable icon) {
        service.uploadIcon(icon, new IconSetListener() {
            public void handleIconSet(IconService service, Writable data,
                    boolean succeeded) {
                synchronized(this) {
                    if (data == iconToSet) {
                        iconToSet = null;
                    }
                }
            }
        });
    }

    public IconService createService(AimConnection aimConnection,
            OscarConnection conn) {
        final IconService service = new IconService(aimConnection, conn);
        service.addIconRequestListener(new IconRequestListener() {
            public void buddyIconCleared(IconService service,
                    Screenname screenname, ExtraInfoData data) {
//                synchronized(IconServiceArbiter.this) {
//                    if (service == currentService) {
//                    }
//                }
                for (IconRequestListener listener : listeners) {
                    listener.buddyIconCleared(service, screenname, data);
                }
            }

            public void buddyIconUpdated(IconService service,
                    Screenname screenname, ExtraInfoData hash, ByteBlock iconData) {
                synchronized(IconServiceArbiter.this) {
                    requestedIconInfos.remove(
                            new RequestedIconInfo(screenname, hash));
                }
                for (IconRequestListener listener : listeners) {
                    listener.buddyIconUpdated(service, screenname, hash,
                            iconData);
                }
            }
        });
        service.addServiceListener(new ServiceListener() {
            public void handleServiceReady(Service s) {
                synchronized(IconServiceArbiter.this) {
                    currentService = service;
                }
                dequeueRequests(service);
            }

            public void handleServiceFinished(Service s) {
                synchronized(IconServiceArbiter.this) {
                    if (currentService == service) {
                        currentService = null;
                    }
                }
            }
        });
        return service;
    }

    private static class RequestedIconInfo {
        private final Screenname screenname;
        private final ExtraInfoData iconHash;

        public RequestedIconInfo(Screenname screenname, ExtraInfoData iconHash) {
            this.iconHash = iconHash;
            this.screenname = screenname;
        }

        public ExtraInfoData getIconHash() {
            return iconHash;
        }

        public Screenname getScreenname() {
            return screenname;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final RequestedIconInfo that = (RequestedIconInfo) o;

            if (!iconHash.equals(that.iconHash)) return false;
            if (!screenname.equals(that.screenname)) return false;

            return true;
        }

        public int hashCode() {
            int result = screenname.hashCode();
            result = 29 * result + iconHash.hashCode();
            return result;
        }
    }
}
