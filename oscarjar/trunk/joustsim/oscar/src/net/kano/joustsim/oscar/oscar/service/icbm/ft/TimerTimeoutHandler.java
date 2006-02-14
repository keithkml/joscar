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

package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.TimeoutableController;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

public class TimerTimeoutHandler implements TimeoutHandler {
  private final Timer timer = new Timer(true);
  private final RvConnectionImpl conn;
  private Map<TimeoutableController, TimerInfo> tasks
      = new IdentityHashMap<TimeoutableController, TimerInfo>();

  public TimerTimeoutHandler(RvConnectionImpl conn) {
    this.conn = conn;
  }

  public void startTimeout(TimeoutableController controller) {
    ConnectionType type = controller.getTimeoutType();
    RvConnectionSettings settings = conn.getSettings();
    long timeout = type == null
        ? settings.getDefaultPerConnectionTimeout() 
        : settings.getPerConnectionTimeout(type);
    TimerInfo task = new TimerInfo(controller, timeout);
    storeTimer(controller, task);
    task.start();
  }

  private synchronized void storeTimer(TimeoutableController controller,
      TimerInfo task) {
    tasks.put(controller, task);
  }

  private @Nullable synchronized TimerInfo getTimer(TimeoutableController controller) {
    return tasks.get(controller);
  }

  public void pauseTimeout(TimeoutableController controller) {
    TimerInfo task = getTimer(controller);
    if (task != null) {
      task.pause();
    }
  }

  public void unpauseTimeout(TimeoutableController controller) {
    TimerInfo task = getTimer(controller);
    if (task != null) {
      task.unpause();
    }
  }

  private class TimerInfo {
    private long started = -1;
    private final TimeoutableController controller;
    private final long timeout;
    private long pausedSince = -1;
    private long pausedTotal = 0;
    private TimerTask lastTask = null;

    public TimerInfo(TimeoutableController controller, long timeout) {
      this.controller = controller;
      this.timeout = timeout;
    }

    public void start() {
      started = System.currentTimeMillis();
      assert !Thread.holdsLock(this);

      TimerTask task = makeTask();
      Date date;
      synchronized (this) {
        lastTask = task;
        date = getNextDate();
      }
      timer.schedule(task, date);
    }

    private synchronized Date getNextDate() {
      return new Date(started + timeout + pausedTotal);
    }

    private TimerTask makeTask() {
      return new TimerTask() {
        public void run() {
          synchronized (TimerInfo.this) {
            if (this != lastTask) return;
          }
          controller.cancelIfNotFruitful(timeout);
        }
      };
    }

    public void pause() {
      TimerTask lastTask;
      synchronized (this) {
        if (pausedSince != -1) return;
        lastTask = this.lastTask;
        this.lastTask = null;
        pausedSince = System.currentTimeMillis();
      }
      if (lastTask != null) lastTask.cancel();
    }

    public synchronized void unpause() {
      TimerTask task = makeTask();
      Date date;
      synchronized (this) {
        if (pausedSince == -1) return;
        pausedTotal += System.currentTimeMillis() - pausedSince;
        pausedSince = -1;
        date = getNextDate();
      }
      timer.schedule(task, date);
    }
  }
}
