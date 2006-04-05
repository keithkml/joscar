/*
 *  Copyright (c) 2004, The Joust Project
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
 *  File created by keith @ Jan 14, 2004
 *
 */

package net.kano.joustsim.oscar;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.auth.AuthCommand;
import net.kano.joscar.snaccmd.buddy.BuddyCommand;
import net.kano.joscar.snaccmd.conn.ConnCommand;
import net.kano.joscar.snaccmd.icbm.IcbmCommand;
import net.kano.joscar.snaccmd.loc.LocCommand;
import net.kano.joscar.snaccmd.ssi.SsiCommand;
import net.kano.joustsim.JavaTools;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.BasicConnection;
import net.kano.joustsim.oscar.oscar.LoginConnection;
import net.kano.joustsim.oscar.oscar.OscarConnListener;
import net.kano.joustsim.oscar.oscar.OscarConnStateEvent;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.ServiceArbiter;
import net.kano.joustsim.oscar.oscar.service.bos.MainBosService;
import net.kano.joustsim.oscar.oscar.service.buddy.BuddyService;
import net.kano.joustsim.oscar.oscar.service.chatrooms.ChatRoomManager;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmService;
import net.kano.joustsim.oscar.oscar.service.info.BuddyTrustManager;
import net.kano.joustsim.oscar.oscar.service.info.CertificateInfoTrustManager;
import net.kano.joustsim.oscar.oscar.service.info.InfoService;
import net.kano.joustsim.oscar.oscar.service.login.LoginService;
import net.kano.joustsim.oscar.oscar.service.ssi.SsiService;
import net.kano.joustsim.oscar.proxy.AimProxyInfo;
import net.kano.joustsim.trust.CertificateTrustManager;
import net.kano.joustsim.trust.SignerTrustManager;
import net.kano.joustsim.trust.TrustPreferences;
import net.kano.joustsim.trust.TrustedCertificatesTracker;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AimConnection {
  private static final Logger LOGGER
      = Logger.getLogger(AimConnection.class.getName());

  private final AppSession appSession;
  private final AimSession aimSession;

  private final Screenname screenname;

  private final Map<Integer, List<Service>> snacfamilies
      = new HashMap<Integer, List<Service>>();

  private final CopyOnWriteArrayList<OpenedServiceListener> serviceListeners
      = new CopyOnWriteArrayList<OpenedServiceListener>();

  private final BuddyInfoManager buddyInfoManager;
  private final BuddyInfoTracker buddyInfoTracker;
  private final BuddyIconTracker buddyIconTracker;
  private final MyBuddyIconManager myBuddyIconManager;

  private final CertificateInfoTrustManager certificateInfoTrustManager;
  private final TrustedCertificatesTracker trustedCertificatesTracker;
  private final BuddyTrustManager buddyTrustManager;
  private final CapabilityManager capabilityManager;

  private final TrustPreferences localPrefs;
  private final ExternalServiceManager externalServiceMgr;
  private final ChatRoomManager chatRoomManager;
  private final ConnectionManager connectionManager;
  private volatile AimProxyInfo proxy = AimProxyInfo.forNoProxy();

  public AimConnection(Screenname screenname, String password) {
    this(new AimConnectionProperties(screenname, password));
  }

  public AimConnection(AimConnectionProperties props) {
    this(new DefaultAimSession(props.getScreenname()), props);
  }

  public AimConnection(AimSession aimSession, AimConnectionProperties props) {
    this(aimSession, aimSession.getTrustPreferences(), props);
  }

  public AimConnection(AimSession aimSession,
      TrustPreferences prefs, AimConnectionProperties props)
      throws IllegalArgumentException {
    Screenname sn = aimSession.getScreenname();
    if (!props.getScreenname().equals(sn)) {
      throw new IllegalArgumentException("connection properties object "
          + "is for screenname " + props.getScreenname() + ", but "
          + "this connection is for " + sn);
    }
    DefensiveTools.checkNull(aimSession, "aimSession");
    DefensiveTools.checkNull(props, "props");

    if (!props.isComplete()) {
      throw new IllegalArgumentException("connection properties are "
          + "incomplete (props.isComplete() == false): " + props);
    }

    this.appSession = aimSession.getAppSession();
    this.aimSession = aimSession;

    this.screenname = sn;

    this.localPrefs = prefs;


    connectionManager = new ConnectionManager(this, props);
    connectionManager.addConnectionPreparer(new OscarConnectionPreparer() {
      public void prepareMainBosConnection(ConnectionManager mgr,
          BasicConnection conn) {
        listenForSnacFamilies(conn);
      }

      public void prepareLoginConnection(ConnectionManager mgr,
          LoginConnection conn) {
        listenForSnacFamilies(conn);
      }
    });
    connectionManager.addStateListener(new StateListener() {
      public void handleStateChange(StateEvent event) {
        if (event.getNewState().isFinished()) {
          List<Service> services = DefensiveTools.getUnmodifiable(
              getLocalServices());
          for (OpenedServiceListener listener : serviceListeners) {
            listener.closedServices(AimConnection.this, services);
          }
        }
      }
    });

    this.buddyInfoManager = new BuddyInfoManager(this);
    this.buddyInfoTracker = new BuddyInfoTracker(this);
    this.buddyIconTracker = new BuddyIconTracker(this);
    this.myBuddyIconManager = new MyBuddyIconManager(this);

    this.trustedCertificatesTracker = createTrustedCertificatesTracker(prefs);
    this.certificateInfoTrustManager
        = new CertificateInfoTrustManager(trustedCertificatesTracker);
    this.buddyTrustManager = new BuddyTrustManager(this);

    this.capabilityManager = createCapabilityManager();
    this.externalServiceMgr = new ExternalServiceManager(this);
    this.chatRoomManager = new ChatRoomManager(this);
  }

  private void listenForSnacFamilies(OscarConnection conn) {
    conn.addOscarListener(new OscarConnListener() {
      public void registeredSnacFamilies(OscarConnection conn) {
        recordSnacFamilies(conn);
      }

      public void connStateChanged(OscarConnection conn,
          OscarConnStateEvent event) {
      }

      public void allFamiliesReady(OscarConnection conn) {
      }
    });
  }

  private synchronized List<Service> getLocalServices() {
    List<Service> list = new ArrayList<Service>();
    for (Map.Entry<Integer,List<Service>> entry : snacfamilies.entrySet()) {
      list.addAll(entry.getValue());
    }
    return list;
  }

  private CapabilityManager createCapabilityManager() {
    CapabilityManager mgr = new CapabilityManager(this);
    mgr.setCapabilityHandler(CapabilityBlock.BLOCK_ENCRYPTION,
        new SecurityEnabledHandler(this));
    mgr.setCapabilityHandler(CapabilityBlock.BLOCK_ICQCOMPATIBLE,
        new DefaultEnabledCapabilityHandler());
    mgr.setCapabilityHandler(CapabilityBlock.BLOCK_SHORTCAPS,
        new DefaultEnabledCapabilityHandler());
    return mgr;
  }

  private TrustedCertificatesTracker createTrustedCertificatesTracker(
      TrustPreferences prefs) {
    CertificateTrustManager certMgr;
    SignerTrustManager signerMgr;
    if (prefs != null) {
      certMgr = prefs.getCertificateTrustManager();
      signerMgr = prefs.getSignerTrustManager();

    } else {
      LOGGER.warning("Warning: this AIM connection's certificate "
          + "and signer managers will not be set because the trust "
          + "manager is null");
      certMgr = null;
      signerMgr = null;
    }
    return new TrustedCertificatesTracker(certMgr, signerMgr);
  }

  public final AppSession getAppSession() { return appSession; }

  public final AimSession getAimSession() { return aimSession; }

  public final Screenname getScreenname() { return screenname; }

  public BuddyInfoManager getBuddyInfoManager() { return buddyInfoManager; }

  public BuddyIconTracker getBuddyIconTracker() { return buddyIconTracker; }

  public MyBuddyIconManager getMyBuddyIconManager() {
    return myBuddyIconManager;
  }

  public CertificateInfoTrustManager getCertificateInfoTrustManager() {
    return certificateInfoTrustManager;
  }

  public TrustedCertificatesTracker getTrustedCertificatesTracker() {
    return trustedCertificatesTracker;
  }

  public TrustPreferences getLocalPrefs() { return localPrefs; }

  public BuddyTrustManager getBuddyTrustManager() { return buddyTrustManager; }

  public CapabilityManager getCapabilityManager() { return capabilityManager; }

  public BuddyInfoTracker getBuddyInfoTracker() { return buddyInfoTracker; }

  public void addOpenedServiceListener(OpenedServiceListener l) {
    serviceListeners.addIfAbsent(l);
  }

  public void removeOpenedServiceListener(OpenedServiceListener l) {
    serviceListeners.remove(l);
  }

  public @Nullable synchronized Service getService(int family) {
    return getMutableService(family);
  }

  private Service getMutableService(int family) {
    List<Service> list = getServiceListIfExists(family);
    return list == null ? null : list.get(0);
  }

  private synchronized List<Service> getServiceListIfExists(int family) {
    List<Service> list = snacfamilies.get(family);
    return list == null || list.isEmpty() ? null : list;
  }

  public LoginService getLoginService() {
    return getServiceOfType(AuthCommand.FAMILY_AUTH, LoginService.class);
  }

  public MainBosService getBosService() {
    return getServiceOfType(ConnCommand.FAMILY_CONN, MainBosService.class);
  }

  public IcbmService getIcbmService() {
    return getServiceOfType(IcbmCommand.FAMILY_ICBM, IcbmService.class);
  }

  public BuddyService getBuddyService() {
    return getServiceOfType(BuddyCommand.FAMILY_BUDDY, BuddyService.class);
  }

  public InfoService getInfoService() {
    return getServiceOfType(LocCommand.FAMILY_LOC, InfoService.class);
  }

  public SsiService getSsiService() {
    return getServiceOfType(SsiCommand.FAMILY_SSI, SsiService.class);
  }

  private @Nullable <E extends Service> E getServiceOfType(int fam,
      Class<E> cls) {
    Service service = getService(fam);
    if (cls.isInstance(service)) {
      return JavaTools.cast(cls, service);
    } else {
      return null;
    }
  }

  public void sendSnac(SnacCommand snac) {
    int family = snac.getFamily();
    Service service = getMutableService(family);
    if (service == null) {
      ServiceArbiter<?> arbiter = externalServiceMgr.getServiceArbiter(family);
      if (arbiter != null) {
        throw new IllegalStateException("can't send SNAC because the family "
            + "is controlled by an arbiter: " + snac);
      }
    } else {
      service.getOscarConnection().sendSnac(snac);
    }
  }

  public ExternalServiceManager getExternalServiceManager() {
    return externalServiceMgr;
  }

  public ChatRoomManager getChatRoomManager() { return chatRoomManager; }

  public void addStateListener(StateListener l) {
    connectionManager.addStateListener(l);
  }

  public void removeStateListener(StateListener l) {
    connectionManager.removeStateListener(l);
  }

  public State getState() { return connectionManager.getState(); }

  public StateInfo getStateInfo() { return connectionManager.getStateInfo(); }

  public boolean isOnline() { return getState() == State.ONLINE; }

  public boolean connect() { return connectionManager.connect(); }

  /**
   * Returns whether {@link #connect()} has been called.
   */
  public boolean triedConnecting() {
    return connectionManager.triedConnecting();
  }

  /**
   * Disconnects {@linkplain #wantedDisconnect() on purpose}.
   */
  public void disconnect() { connectionManager.disconnect(true); }

  public void disconnect(boolean onPurpose) {
    connectionManager.disconnect(onPurpose);
  }

  /**
   * Returns whether this connection was disconnected on purpose. If the
   * connection has not disconnected, this method will return {@code false}.
   */
  public boolean wantedDisconnect() {
    return connectionManager.wantedDisconnect();
  }

  private void recordSnacFamilies(OscarConnection conn) {
    List<Service> added = new ArrayList<Service>();
    synchronized (this) {
      for (int family : conn.getSnacFamilies()) {
        List<Service> services = getServiceList(family);
        Service service = conn.getService(family);
        if (service == null) {
          LOGGER.finer("Could not find service handler for family 0x"
              + Integer.toHexString(family));
        } else {
          services.add(service);
          added.add(service);
        }
      }
    }

    List<Service> addedClone = DefensiveTools.getUnmodifiable(added);

    for (OpenedServiceListener listener : serviceListeners) {
      listener.openedServices(this, addedClone);
    }
  }

  private synchronized List<Service> getServiceList(int family) {
    List<Service> list = snacfamilies.get(family);
    if (list == null) {
      list = new ArrayList<Service>();
      snacfamilies.put(family, list);
    }
    return list;
  }

  public AimProxyInfo getProxy() { return proxy; }

  public void setProxy(AimProxyInfo proxy) {
    DefensiveTools.checkNull(proxy, "proxy");
    
    this.proxy = proxy;
  }
}