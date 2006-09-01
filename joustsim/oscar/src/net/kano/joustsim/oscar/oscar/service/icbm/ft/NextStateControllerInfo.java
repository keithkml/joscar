/*
 * Created by IntelliJ IDEA.
 * User: keithkml
 * Date: Mar 1, 2006
 * Time: 5:04:01 PM
 */

package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.StateController;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.RvConnectionEvent;
import org.jetbrains.annotations.Nullable;

public class NextStateControllerInfo {
  private final StateController controller;
  private RvConnectionState state;
  private RvConnectionEvent event;

  public NextStateControllerInfo(StateController value) {
    this(value, null, null);
  }

  public NextStateControllerInfo(StateController controller,
      RvConnectionState state, RvConnectionEvent event) {
    this.controller = controller;
    this.state = state;
    this.event = event;
  }

  public NextStateControllerInfo(RvConnectionState state,
      RvConnectionEvent event) {
    this(null, state, event);
  }

  public NextStateControllerInfo(StateController controller,
      RvConnectionEvent event) {
    this(controller, null, event);
  }

  public @Nullable StateController getController() {
    return controller;
  }

  public @Nullable RvConnectionState getState() {
    return state;
  }

  public @Nullable RvConnectionEvent getEvent() {
    return event;
  }
}
