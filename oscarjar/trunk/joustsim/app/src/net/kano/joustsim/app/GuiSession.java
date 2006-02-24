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
 *  File created by keith @ Jan 18, 2004
 *
 */

package net.kano.joustsim.app;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.app.forms.DummyOnlineWindow;
import net.kano.joustsim.app.forms.ImBox;
import net.kano.joustsim.app.forms.SignonBox;
import net.kano.joustsim.app.forms.SignonProgressBox;
import net.kano.joustsim.app.forms.SignonWindow;
import net.kano.joustsim.app.forms.prefs.AccountPrefsWindow;
import net.kano.joustsim.app.forms.prefs.LocalCertificatesPrefsPane;
import net.kano.joustsim.app.forms.prefs.TrustPrefsPane;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.AimConnectionProperties;
import net.kano.joustsim.oscar.AimSession;
import net.kano.joustsim.oscar.State;
import net.kano.joustsim.oscar.StateEvent;
import net.kano.joustsim.oscar.StateInfo;
import net.kano.joustsim.oscar.StateListener;
import net.kano.joustsim.oscar.oscar.service.chatrooms.ChatInvitation;
import net.kano.joustsim.oscar.oscar.service.chatrooms.ChatMessage;
import net.kano.joustsim.oscar.oscar.service.chatrooms.ChatRoomManager;
import net.kano.joustsim.oscar.oscar.service.chatrooms.ChatRoomManagerListener;
import net.kano.joustsim.oscar.oscar.service.chatrooms.ChatRoomSession;
import net.kano.joustsim.oscar.oscar.service.chatrooms.ChatRoomSessionListener;
import net.kano.joustsim.oscar.oscar.service.chatrooms.ChatRoomUser;
import net.kano.joustsim.oscar.oscar.service.chatrooms.ChatSessionState;
import net.kano.joustsim.oscar.oscar.service.icbm.Conversation;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmBuddyInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmListener;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmService;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GuiSession {
  private final JoustsimSession appSession;

  private SignonWindow signonWin = null;
  private SignonBox signonBox = null;
  private SignonProgressBox signonProgressBox = null;
  private DummyOnlineWindow dummyOnlineWindow = null;

  private AimSession aimSession;
  private AimConnection conn;

  private CopyOnWriteArrayList<GuiSessionListener> listeners
      = new CopyOnWriteArrayList<GuiSessionListener>();

  private boolean open = false;
  private boolean closed = false;
  private ConnStateListener connStateListener = new ConnStateListener();

  private Map<Screenname, ImBox> imBoxes = new HashMap<Screenname, ImBox>();
  private Map<Screenname, AccountPrefsWindow> prefsWindows
      = new HashMap<Screenname, AccountPrefsWindow>();

  public GuiSession(JoustsimSession appSession) {
    DefensiveTools.checkNull(appSession, "appSession");

    this.appSession = appSession;
  }

  public JoustsimSession getAppSession() { return appSession; }

  public AimSession getAimSession() { return aimSession; }

  public synchronized AimConnection getAimConnection() { return conn; }

  public synchronized SignonBox getSignonBox() {
    return signonBox;
  }

  public synchronized SignonProgressBox getSignonProgressBox() {
    return signonProgressBox;
  }

  public void addListener(GuiSessionListener l) {
    listeners.addIfAbsent(l);
  }

  public void removeListener(GuiSessionListener l) {
    listeners.remove(l);
  }

  public void open() {
    synchronized (this) {
      if (open) return;
      open = true;
    }
    for (GuiSessionListener listener : listeners) {
      listener.opened(this);
    }
    openSignonWindow();
  }

  public void close() {
    AimSession aimSession;
    SignonWindow signonWin;
    synchronized (this) {
      if (!open || closed) return;
      closed = true;

      aimSession = this.aimSession;
      signonWin = this.signonWin;

      if (aimSession != null) this.aimSession = null;
      if (signonWin != null) this.signonWin = null;
      if (signonBox != null) signonBox = null;
      if (signonProgressBox != null) signonProgressBox = null;
      imBoxes.clear();
      prefsWindows.clear();
    }
    signoff();

    // we want to kill these things outside the lock so we're not in the
    // lock for too long
    if (aimSession != null) aimSession.closeConnection();
    if (signonWin != null) signonWin.dispose();

    for (GuiSessionListener listener : listeners) {
      listener.closed(this);
    }
  }

  public void signon(Screenname screenname, String pass) {
    signon(new AimConnectionProperties(screenname, pass));
  }

  public void signoff() {
    AimConnection conn = getAimConnection();
    if (conn != null) conn.disconnect();
  }

  public ImBox openImBox(Screenname sn) {
    ImBox box;
    synchronized (this) {
      box = imBoxes.get(sn);
      if (box == null) {
        IcbmService service = conn.getIcbmService();

        box = createBox(sn);

        box.handleConversation(service.getImConversation(sn));
        box.handleConversation(service.getSecureAimConversation(sn));
        box.handleConversation(service.getDirectimConversation(sn));
      }
    }
    final ImBox fbox = box;
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        fbox.setVisible(true);
        fbox.requestFocus();
      }
    });
    return box;
  }

  public synchronized AccountPrefsWindow openPrefsWindow(Screenname sn) {
    AccountPrefsWindow frame = prefsWindows.get(sn);
    if (frame == null) {
      frame = new AccountPrefsWindow(appSession, sn);
      frame.addPrefsPane(new LocalCertificatesPrefsPane(appSession, sn));
      frame.addPrefsPane(new TrustPrefsPane(appSession, sn));
      frame.setSize(640, 480);
      prefsWindows.put(sn, frame);
    }

    final AccountPrefsWindow fframe = frame;
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        fframe.setState(JFrame.NORMAL);
        fframe.setVisible(true);
        fframe.requestFocus();
      }
    });
    return frame;
  }

  private void signon(AimConnectionProperties props) {
    if (closed) return;

    Screenname sn = new Screenname(signonBox.getScreenname());
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        System.out.println("setting visible because of signon() call");

        initSignonProgressWindow();
        signonProgressBox.updateSession();
        signonWin.setToSignonProgressBox();
        signonWin.setVisible(true);
      }
    });
    aimSession = appSession.openAimSession(sn);
    //TODO: dispose of other buddies' prefs windows

    conn = aimSession.openConnection(props);
//    conn.setProxy(AimProxyInfo.forSocks5("kano.net", 10800, "test", "test"));
    conn.addStateListener(connStateListener);
    conn.connect();
  }

  private synchronized void initDummyOnlineWindow() {
    if (closed) return;
    if (dummyOnlineWindow == null) {
      dummyOnlineWindow = new DummyOnlineWindow(this);
      dummyOnlineWindow.pack();
    }
    dummyOnlineWindow.updateSession();
  }

  private synchronized void initSignonWindow() {
    if (closed) return;
    if (signonWin == null) {
      signonWin = new SignonWindow(this);
    }
  }

  private synchronized void initSignonBox() {
    if (closed) return;
    initSignonWindow();
    if (signonBox == null) {
      signonBox = new SignonBox(this);
    }
  }

  private synchronized void initSignonProgressWindow() {
    if (closed) return;
    initSignonWindow();
    if (signonProgressBox == null) {
      signonProgressBox = new SignonProgressBox(this);
    }
  }

  private void openSignonWindow() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        initSignonBox();
        signonWin.setToSignonBox();
        System.out.println("setting visible because of openSignonWindow call");
        signonWin.setVisible(true);
      }
    });
  }

  private synchronized ImBox createBox(Screenname sn) {
    DefensiveTools.checkNull(sn, "sn");
    if (imBoxes.containsKey(sn)) {
      throw new IllegalArgumentException("IM box for " + sn
          + " already exists");
    }
    ImBox box = new ImBox(this, conn, sn);
    box.setSize(410, 280);
    imBoxes.put(sn, box);
    return box;
  }

  private void handleNewConversation(Conversation conv) {
    ImBox box = openImBox(conv.getBuddy());
    box.handleConversation(conv);
  }

  private synchronized void resetForDisconnection() {
    for (ImBox box : imBoxes.values()) {
      box.setInvalidBox();
      box.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
    imBoxes.clear();
  }

  public DummyOnlineWindow getDummyOnlineWindow() {
    return dummyOnlineWindow;
  }

  private class ConnStateListener implements StateListener {
    public void handleStateChange(StateEvent event) {
      AimConnection conn = event.getAimConnection();
      if (aimSession == null || conn != aimSession.getConnection()) {
        // we can ignore this event since we're using a new connection
        // now
        System.out.println("ignoring event for " + conn + ": " + event);
        return;
      }
      State state = event.getNewState();
      if (state == State.ONLINE) {
        IcbmService icbmservice = conn.getIcbmService();
        icbmservice.addIcbmListener(new IcbmListener() {
          public void newConversation(IcbmService service, Conversation conv) {
            handleNewConversation(conv);
          }

          public void buddyInfoUpdated(IcbmService service, Screenname buddy,
              IcbmBuddyInfo info) {
          }
        });
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            System.out.println("online, setting invisible");
            signonWin.setVisible(false);
            initDummyOnlineWindow();
            dummyOnlineWindow.setVisible(true);
          }
        });
        conn.getChatRoomManager().addListener(new ChatRoomManagerListener() {
          public void handleInvitation(ChatRoomManager chatRoomManager,
              ChatInvitation inv) {
            int result = JOptionPane.showOptionDialog(dummyOnlineWindow,
                inv.getScreenname() + " has invited you to " + inv.getRoomName(),
                "Invitation", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null,
                new String[]{"Join Room", "Ignore"}, "Join Room");
            if (result == JOptionPane.YES_OPTION) {
              ChatRoomSession session = inv.accept();

              session.addListener(new ChatRoomSessionListener() {
                public void handleStateChange(ChatRoomSession room, ChatSessionState oldState,
                    ChatSessionState state) {

                }

                public void handleUsersJoined(ChatRoomSession room,
                    Set<ChatRoomUser> joined) {
                }

                public void handleUsersLeft(ChatRoomSession room, Set<ChatRoomUser> left) {
                }

                public void handleIncomingMessage(ChatRoomSession room, ChatRoomUser user,
                    ChatMessage message) {

                }
              });
            } else {
              inv.reject();
            }
          }
        });

      } else if (state == State.DISCONNECTED
          || state == State.FAILED) {
        AimConnection oldconn = event.getAimConnection();
        if (oldconn != null) {
          oldconn.removeStateListener(connStateListener);
        }
        final StateInfo sinfo = event.getNewStateInfo();
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            signonBox.setFailureInfo(sinfo);
            if (dummyOnlineWindow != null) {
              dummyOnlineWindow.setVisible(false);
            }
            openSignonWindow();
          }
        });
        resetForDisconnection();
      }
    }
  }
}
