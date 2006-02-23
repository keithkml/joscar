package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.MiscTools;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousSessionHandler;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ControllerListener;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.OutgoingConnectionController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.ConnectedController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ChecksummingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectedEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ConnectingToProxyEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.FileCompleteEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.LocallyCancelledEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ResolvingProxyEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.StartedControllerEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.StartingControllerEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.StoppingControllerEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.TransferringFileEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.WaitingForConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.FailedStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.SuccessfulStateInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StateInfo;
import net.kano.joustsim.oscar.proxy.AimProxyInfo;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public abstract class RvConnectionImpl
    implements RvConnection, StateBasedRvConnection {
  private static final Logger LOGGER = Logger
      .getLogger(FileTransferHelper.class.getName());

  private final RendezvousSessionHandler rvSessionHandler;
  private final CopyOnWriteArrayList<RvConnectionEventListener> listeners
      = new CopyOnWriteArrayList<RvConnectionEventListener>();
  private final EventPost eventPost = new EventPostImpl();
  private final ControllerListener controllerListener = new ControllerListener() {
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
  private final List<StateChangeEvent> eventQueue
      = new CopyOnWriteArrayList<StateChangeEvent>();
  private final RvConnectionSettings settings = new RvConnectionSettings();
  private final RvSessionConnectionInfo sessionInfo;
  private final Screenname screenname;

  private StateController controller = null;
  private RvConnectionState state = RvConnectionState.WAITING;
  private boolean done = false;
  private volatile TimeoutHandler timeoutHandler = new TimerTimeoutHandler(this);
  private static final List<RvConnectionState> COOL_STATES
      = Arrays.asList(RvConnectionState.WAITING, RvConnectionState.PREPARING,
      RvConnectionState.CONNECTING, RvConnectionState.CONNECTED);

  protected RvConnectionImpl(AimProxyInfo proxy,
      Screenname myScreenname, RvSessionConnectionInfo rvsessioninfo) {
    rvSessionHandler = createSessionHandler();
    settings.setProxyInfo(proxy);
    settings.setPerConnectionTimeout(ConnectionType.LAN, 2000);
    sessionInfo = rvsessioninfo;
    this.screenname = myScreenname;
  }

  protected void fireEvent(RvConnectionEvent event) {
    assert !Thread.holdsLock(this);

    for (RvConnectionEventListener listener : listeners) {
      listener.handleEvent(this, event);
    }
  }

  protected void fireStateChange(RvConnectionState newState,
                                 RvConnectionEvent event) {
    assert !Thread.holdsLock(this);

    for (RvConnectionEventListener listener : listeners) {
      listener.handleEventWithStateChange(this, newState, event);
    }
  }

  public synchronized RvConnectionState getState() { return state; }

  public void setTimeoutHandler(TimeoutHandler timeoutHandler) {
    this.timeoutHandler = timeoutHandler;
  }

  public TimeoutHandler getTimeoutHandler() { return timeoutHandler; }

  protected boolean startStateController(StateController controller) {
    return changeStateController(controller);
  }

  protected boolean changeStateController(StateController controller) {
    StateController last;
    synchronized (this) {
      StateController old = this.controller;
      if (!isValidNextController(old, controller)) return false;
      last = storeNextController(controller);
      assert last == old;
    }
    stopThenStart(last, controller);
    return true;
  }

  protected boolean isValidNextController(StateController oldController,
      StateController newController) {
    if (done) {
      LOGGER.warning("Someone tried changing controller for " + this
          + " to " + newController + ", but we are done so it is being ignored");
      return false;
    }

    if (isConnectedController(oldController)) {
      ConnectedController conn = (ConnectedController) oldController;
      if (conn.isConnected()) {
        if (!canInterruptConnectedController(conn, newController)) {
          return false;
        }
      }
    }

    return true;
  }

  private void stopThenStart(StateController last,
      StateController controller) {
    if (last != null) {
      fireEvent(new StoppingControllerEvent(last));
      last.stop();
    }
    if (controller != null) {
      fireEvent(new StartingControllerEvent(controller));
      controller.start(this, last);
      fireEvent(new StartedControllerEvent(controller));
    }
  }

  protected boolean changeStateControllerFrom(StateController oldController) {
    LOGGER.finer("Changing state controller from " + oldController);
    StateController next;
    synchronized (this) {
      if (this.controller == oldController) {
        next = getNextStateController();
        if (!isValidNextController(oldController, next)) return false;
        storeNextController(next);

      } else {
        next = null;
      }
    }
    flushEventQueue();
    stopThenStart(oldController, next);
    return true;
  }

  private synchronized StateController storeNextController(
      StateController controller) {
    LOGGER.info("Transfer " + this + " changing to state controller "
        + controller);
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

  public RendezvousSessionHandler getRvSessionHandler() {
    return rvSessionHandler;
  }

  public boolean close() {
    setState(RvConnectionState.FAILED, new LocallyCancelledEvent());
    return true;
  }

  public void close(RvConnectionEvent error) {
    setState(RvConnectionState.FAILED, error);
  }

  public boolean setState(RvConnectionState state, RvConnectionEvent event) {
    assert !Thread.holdsLock(this);

    StateController controller;
    synchronized (this) {
      if (done) return false;

      assert isValidChange(this.state, state);

      this.state = state;
      if (state == RvConnectionState.FAILED
          || state == RvConnectionState.FINISHED) {
        done = true;
      }
      controller = this.controller;
    }
    if (state == RvConnectionState.FAILED) {
      sessionInfo.getRequestMaker().sendRvReject();
    }
    if (controller != null && (state == RvConnectionState.FAILED
        || state == RvConnectionState.FINISHED)) {
      controller.stop();
    }
    fireStateChange(state, event);
    return true;
  }

  private boolean isValidChange(RvConnectionState old,
      RvConnectionState state) {
    if (old.equals(state)) return true;

    int oidx = COOL_STATES.indexOf(old);
    int nidx = COOL_STATES.indexOf(state);
    if (nidx != -1 && oidx != -1) {
      // if the states are in the cool states list, they must be in order (but
      // skipping states is okay)
      return nidx >= oidx;
    }
    if (state == RvConnectionState.FINISHED) {
      if (old == RvConnectionState.FAILED) return false;
    }
    if (state == RvConnectionState.FAILED) {
      if (old == RvConnectionState.FINISHED) return false;
    }
    return true;
  }

  public void addEventListener(RvConnectionEventListener listener) {
    listeners.addIfAbsent(listener);
  }

  public void removeEventListener(RvConnectionEventListener listener) {
    listeners.remove(listener);
  }

  public EventPost getEventPost() {
    return eventPost;
  }

  public Screenname getBuddyScreenname() {
    return new Screenname(sessionInfo.getRvSession().getScreenname());
  }

  protected synchronized void queueEvent(RvConnectionEvent event) {
    eventQueue.add(new StateChangeEvent(null, event));
  }

  protected synchronized void queueStateChange(
      RvConnectionState rvConnectionState,
      RvConnectionEvent event) {
    eventQueue.add(new StateChangeEvent(rvConnectionState, event));
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

  protected abstract RendezvousSessionHandler createSessionHandler();

  public RvConnectionSettings getSettings() { return settings; }

  public Screenname getMyScreenname() {
    return screenname;
  }

  public RvSessionConnectionInfo getRvSessionInfo() {
    return sessionInfo;
  }

  public String toString() {
    return MiscTools.getClassName(this) + " with " + getBuddyScreenname();
  }

  public static boolean isLanController(StateController oldController) {
    return oldController instanceof OutgoingConnectionController
        && ((OutgoingConnectionController) oldController).getTimeoutType()
        == ConnectionType.LAN;
  }

  public static boolean isInternetController(StateController oldController) {
    return oldController instanceof OutgoingConnectionController
        && ((OutgoingConnectionController) oldController).getTimeoutType()
        == ConnectionType.INTERNET;
  }

  public synchronized StateController getNextStateController() {
    StateController oldController = getStateController();
    StateInfo endState = oldController.getEndStateInfo();
    if (endState instanceof SuccessfulStateInfo) {
      if (isSomeConnectionController(oldController)) {
        return createConnectedController(endState);

      } else {
        return getNextControllerFromSuccess(oldController, endState);
      }

    } else if (endState instanceof FailedStateInfo) {
      return getNextControllerFromError(oldController, endState);

    } else {
      throw new IllegalStateException("Unknown previous state " + endState);
    }
  }

  public boolean isOpen() {
    return getState().isOpen();
  }

  protected abstract StateController getNextControllerFromError(
      StateController oldController, StateInfo endState);

  protected abstract StateController getNextControllerFromSuccess(
      StateController oldController, StateInfo endState);

  protected abstract ConnectedController createConnectedController(
      StateInfo endState);

  protected abstract boolean isSomeConnectionController(StateController oldController);

  protected boolean canInterruptConnectedController(
      ConnectedController connected, StateController newController) {
    return newController == null;
  }

  protected abstract boolean isConnectedController(StateController controller);

  protected static class StateChangeEvent {
    private RvConnectionState state;
    private RvConnectionEvent event;

    public StateChangeEvent(RvConnectionState state,
                            RvConnectionEvent event) {

      this.state = state;
      this.event = event;
    }

    public RvConnectionState getState() {
      return state;
    }

    public RvConnectionEvent getEvent() {
      return event;
    }
  }

  private class EventPostImpl implements EventPost {
    public void fireEvent(RvConnectionEvent event) {
      boolean fireState;
      RvConnectionState newState = null;
      synchronized (RvConnectionImpl.this) {
        RvConnectionState oldState = state;
        if (event instanceof ConnectingEvent
            || event instanceof ConnectingToProxyEvent
            || event instanceof ResolvingProxyEvent
            || event instanceof WaitingForConnectionEvent) {
          newState = RvConnectionState.CONNECTING;

        } else if (event instanceof ConnectedEvent) {
          newState = RvConnectionState.CONNECTED;

        } else if (event instanceof TransferringFileEvent
            || event instanceof FileCompleteEvent) {
          newState = FileTransferState.TRANSFERRING;

        } else if (event instanceof ChecksummingEvent
            && oldState == RvConnectionState.WAITING) {
          newState = RvConnectionState.PREPARING;
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
}
