package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectingToProxyEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ResolvingProxyEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.WaitingForConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectedEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.TransferringFileEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.FileCompleteEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ChecksummingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.LocallyCancelledEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ProxyRedirectDisallowedEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ControllerListener;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.SuccessfulStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailedStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousSessionHandler;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.proxy.AimProxyInfo;
import net.kano.joustsim.Screenname;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rvcmd.sendfile.FileSendRejectRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.CopyOnWriteArrayList;

import java.util.Timer;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public abstract class RvConnectionImpl
    implements RvConnection, RvConnectionPropertyHolder,
    RvSessionBasedConnection, StateBasedConnection, CachedTimerHolder {
  private static final Logger LOGGER = Logger
      .getLogger(FileTransferImpl.class.getName());
  private Timer timer = new Timer("RV connection timer", true);
  protected RendezvousSessionHandler rvSessionHandler;
  protected RvSession session;
  private StateController controller = null;
  private Map<Key<?>, Object> transferProperties
      = new HashMap<Key<?>, Object>();
  protected RvConnectionManager rvConnectionManager;
  private final CopyOnWriteArrayList<RvConnectionEventListener> listeners
      = new CopyOnWriteArrayList<RvConnectionEventListener>();
  private FileTransferState state = FileTransferState.WAITING;
  private EventPost eventPost = new EventPostImpl();
  private boolean done = false;
  private boolean onlyUsingProxy = false;
  private boolean proxyRequestTrusted = true;
  private ControllerListener controllerListener = new ControllerListener() {
    public void handleControllerSucceeded(StateController c,
                                          SuccessfulStateInfo info) {
      goNext(c);
    }

    public void handleControllerFailed(StateController c,
                                       FailedStateInfo info) {
      goNext(c);
    }

    private void goNext(StateController c) {
      c.removeControllerListener(this);
      changeStateControllerFrom(c);
    }
  };
  private List<StateChangeEvent> eventQueue
      = new CopyOnWriteArrayList<StateChangeEvent>();
  private long perConnectionTimeout = 10000;
  private Map<ConnectionType, Long> timeouts
      = new HashMap<ConnectionType, Long>();
  private int requestIndex = 1;
  protected AimProxyInfo proxyInfo;

  protected void fireEvent(RvConnectionEvent event) {
    assert !Thread.holdsLock(this);

    for (RvConnectionEventListener listener : listeners) {
      listener.handleEvent(this, event);
    }
  }

  protected void fireStateChange(FileTransferState newState,
                                 RvConnectionEvent event) {
    assert !Thread.holdsLock(this);

    for (RvConnectionEventListener listener : listeners) {
      listener.handleEventWithStateChange(this,
          newState, event);
    }
  }

  public synchronized FileTransferState getState() { return state; }

  public Timer getTimer() { return timer; }

  public synchronized int getRequestIndex() { return requestIndex; }

  public synchronized int increaseRequestIndex() {
    int requestIndex = this.requestIndex;
    requestIndex = requestIndex + 1;
    this.requestIndex = requestIndex;
    return requestIndex;
  }

  public synchronized void setRequestIndex(int requestIndex) {
    this.requestIndex = requestIndex;
  }

  protected void startStateController(StateController controller) {
//        StateController oldController = this.controller;
//        if (oldController != null) {
//            throw new IllegalStateException("Cannot start state controller: "
//                    + "controller is already set to " + oldController);
//        }
    changeStateController(controller);
  }

  protected void changeStateController(StateController controller) {
    StateController last;
    synchronized (this) {
      if (done) {
        LOGGER.warning("Someone tried changing state of " + this
            + " to " + controller + ", but we are done so it is "
            + "being ignored");
        return;
      }
      last = storeNextController(controller);
    }
    if (last != null) last.stop();
    controller.start(this, last);
  }

  protected void changeStateControllerFrom(StateController controller) {
    LOGGER.finer("Changing state controller from " + controller);
    boolean good;
    StateController next;
    synchronized (this) {
      if (done) return;

      if (this.controller == controller) {
        good = true;
        next = getNextStateController();
        storeNextController(next);
      } else {
        good = false;
        next = null;
      }
    }
    flushEventQueue();
    controller.stop();
    if (good && next != null) {
      next.start(this, controller);
    }
  }

  private synchronized StateController storeNextController(
      StateController controller) {
    LOGGER.info(
        "Transfer " + this + " changing to state controller " + controller);
    StateController last = this.controller;
    this.controller = controller;
    if (controller != null) {
      controller.addControllerListener(controllerListener);
    }
    return last;
  }

  public synchronized StateController getStateController() {
    return controller;
  }

  protected RendezvousSessionHandler getRvSessionHandler() {
    return rvSessionHandler;
  }

  public RvSession getRvSession() {
    return session;
  }

  public synchronized <V> void putTransferProperty(Key<V> key, V value) {
    LOGGER.fine("Transfer property " + key + " set to " + value + " for "
        + this);
    transferProperties.put(key, value);
  }

  @SuppressWarnings({"unchecked"})
  public synchronized <V> V getTransferProperty(Key<V> key) {
    return (V) transferProperties.get(key);
  }

  public RvConnectionManager getRvConnectionManager() {
    return rvConnectionManager;
  }

  //TODO: automatically cancel when state is set to FAILED
  public boolean cancel() {
    setState(FileTransferState.FAILED, new LocallyCancelledEvent());
    return true;
  }

  //TODO: check for valid state changes
  protected boolean setState(FileTransferState state, RvConnectionEvent event) {
    StateController controller;
    synchronized (this) {
      if (done) return false;

      this.state = state;
      if (state == FileTransferState.FAILED
          || state == FileTransferState.FINISHED) {
        done = true;
      }
      controller = this.controller;
    }
    if (state == FileTransferState.FAILED) {
      getRvSession().sendRv(new FileSendRejectRvCmd());
    }
    if (controller != null && (state == FileTransferState.FAILED
        || state == FileTransferState.FINISHED)) {
      controller.stop();
    }
    fireStateChange(state, event);
    return true;
  }

  public void addTransferListener(RvConnectionEventListener listener) {
    listeners.addIfAbsent(listener);
  }

  public void removeTransferListener(RvConnectionEventListener listener) {
    listeners.remove(listener);
  }

  public EventPost getEventPost() {
    return eventPost;
  }

  public synchronized boolean isProxyRequestTrusted() {
    return proxyRequestTrusted;
  }

  public synchronized void setProxyRequestTrusted(
      boolean trustingProxyRedirects) {
    this.proxyRequestTrusted = trustingProxyRedirects;
  }

  public synchronized boolean isOnlyUsingProxy() {
    return onlyUsingProxy;
  }

  public synchronized void setOnlyUsingProxy(boolean onlyUsingProxy) {
    this.onlyUsingProxy = onlyUsingProxy;
  }

  public synchronized void setDefaultPerConnectionTimeout(long millis) {
    perConnectionTimeout = millis;
  }

  public synchronized void setPerConnectionTimeout(ConnectionType type,
                                                   long millis) {
    timeouts.put(type, millis);
  }

  public synchronized long getDefaultPerConnectionTimeout() {
    return perConnectionTimeout;
  }

  public synchronized long getPerConnectionTimeout(ConnectionType type) {
    DefensiveTools.checkNull(type, "type");

    Long timeout = timeouts.get(type);
    if (timeout == null) return perConnectionTimeout;
    else return timeout;
  }

  public Screenname getBuddyScreenname() {
    return new Screenname(getRvSession().getScreenname());
  }

  protected synchronized void queueEvent(RvConnectionEvent event) {
    eventQueue.add(new StateChangeEvent(null, event));
  }

  protected synchronized void queueStateChange(
      FileTransferState fileTransferState,
      RvConnectionEvent event) {
    eventQueue.add(new StateChangeEvent(fileTransferState, event));
  }

  protected void flushEventQueue() {
    assert !Thread.holdsLock(this);

    Iterator<StateChangeEvent> it;
    synchronized (this) {
      it = eventQueue.iterator();
      eventQueue.clear();
    }
    while (it.hasNext()) {
      StateChangeEvent event = it.next();
      if (event.getState() == null) {
        fireEvent(event.getEvent());
      } else {
        setState(event.getState(), event.getEvent());
      }
    }
  }

  protected AimConnection getAimConnection() {
    return getRvConnectionManager().getIcbmService().getAimConnection();
  }

  protected HowToConnect processRedirect(FileSendReqRvCmd reqCmd) {
    setRequestIndex(reqCmd.getRequestIndex());
    RvConnectionInfo connInfo = reqCmd.getConnInfo();
    LOGGER.fine("Received redirect packet: " + reqCmd
        + " - to " + connInfo);
    RvConnectionEvent error = getConnectError(connInfo);
    HowToConnect how;
    if (error == null) {
      putTransferProperty(KEY_CONN_INFO, connInfo);
      LOGGER.fine("Storing connection info for redirect: " + connInfo);
      //TODO: replace KEY_REDIRECTED with KEY_DIRECTION which is enum IN or OUT or something
      if (connInfo.isProxied()) {
        LOGGER.finer("Deciding to change to proxy connect controller");
        how = HowToConnect.PROXY;
        putTransferProperty(KEY_REDIRECTED, false);
      } else {
        LOGGER.finer("Deciding to change to normal connect controller");
        how = HowToConnect.NORMAL;
        //TODO: is this redirected correct??
        putTransferProperty(KEY_REDIRECTED, true);
      }
    } else {
      //TODO: should we really fail when we get an invalid proxy redirect?
      //      we could ignore it
      setState(FileTransferState.FAILED, error);
      how = HowToConnect.DONT;
    }
    return how;
  }

  protected final RvConnectionEvent getConnectError(RvConnectionInfo connInfo) {
    RvConnectionEvent error = null;
    if (isOnlyUsingProxy()) {
      if (connInfo.isProxied() && !isProxyRequestTrusted()) {
        error = new ProxyRedirectDisallowedEvent(
            connInfo.getProxyIP());
      }
    }
    return error;
  }

  public AimProxyInfo getProxy() { return proxyInfo; }

  protected abstract RendezvousSessionHandler createSessionHandler();

  protected static class StateChangeEvent {
    private FileTransferState state;
    private RvConnectionEvent event;

    public StateChangeEvent(FileTransferState state,
                            RvConnectionEvent event) {

      this.state = state;
      this.event = event;
    }

    public FileTransferState getState() {
      return state;
    }

    public RvConnectionEvent getEvent() {
      return event;
    }
  }

  private class EventPostImpl implements EventPost {
    public void fireEvent(RvConnectionEvent event) {
      boolean fireState;
      FileTransferState newState = null;
      synchronized (RvConnectionImpl.this) {
        FileTransferState oldState = state;
        if (event instanceof ConnectingEvent
            || event instanceof ConnectingToProxyEvent
            || event instanceof ResolvingProxyEvent
            || event instanceof WaitingForConnectionEvent) {
          newState = FileTransferState.CONNECTING;

        } else if (event instanceof ConnectedEvent) {
          newState = FileTransferState.CONNECTED;

        } else if (event instanceof TransferringFileEvent
            || event instanceof FileCompleteEvent) {
          newState = FileTransferState.TRANSFERRING;

        } else if (event instanceof ChecksummingEvent
            && oldState == FileTransferState.WAITING) {
          newState = FileTransferState.PREPARING;
        }
        if (!done && newState != null && newState != oldState) {
          fireState = true;
          state = newState;
        } else {
          fireState = false;
        }
      }
      if (fireState) {
        fireStateChange(newState, event);
      } else {
        RvConnectionImpl.this.fireEvent(event);
      }
    }
  }

  protected static enum HowToConnect { DONT, PROXY, NORMAL }
}
