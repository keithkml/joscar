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

package net.kano.aimcrypto;

import net.kano.aimcrypto.config.LocalPreferencesManager;
import net.kano.aimcrypto.connection.AimConnection;
import net.kano.aimcrypto.connection.AimConnectionProperties;
import net.kano.aimcrypto.connection.State;
import net.kano.aimcrypto.connection.StateEvent;
import net.kano.aimcrypto.connection.StateInfo;
import net.kano.aimcrypto.connection.StateListener;
import net.kano.aimcrypto.connection.oscar.service.icbm.Conversation;
import net.kano.aimcrypto.connection.oscar.service.icbm.IcbmBuddyInfo;
import net.kano.aimcrypto.connection.oscar.service.icbm.IcbmListener;
import net.kano.aimcrypto.connection.oscar.service.icbm.IcbmService;
import net.kano.aimcrypto.connection.oscar.service.icbm.ImConversation;
import net.kano.aimcrypto.forms.DummyOnlineWindow;
import net.kano.aimcrypto.forms.ImBox;
import net.kano.aimcrypto.forms.SignonProgressWindow;
import net.kano.aimcrypto.forms.SignonWindow;
import net.kano.aimcrypto.forms.prefs.TrustPrefsPane;
import net.kano.aimcrypto.forms.prefs.AccountPrefsWindow;
import net.kano.aimcrypto.forms.prefs.LocalCertificatesPrefsPane;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GuiSession {
    private final AppSession appSession;
    private SignonWindow signonWindow = null;
    private SignonProgressWindow signonProgressWindow = null;
    private DummyOnlineWindow dummyOnlineWindow = null;

    private AimSession aimSession;
    private AimConnection conn;

    private CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();

    private boolean open = false;
    private boolean closed = false;
    private ConnStateListener connStateListener = new ConnStateListener();

    public GuiSession(AppSession appSession) {
        DefensiveTools.checkNull(appSession, "appSession");
        this.appSession = appSession;
    }

    public void addListener(GuiSessionListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeListener(GuiSessionListener l) {
        listeners.remove(l);
    }

    public void open() {
        synchronized(this) {
            if (open) return;
            open = true;
        }
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            GuiSessionListener listener = (GuiSessionListener) it.next();
            listener.opened(this);
        }
        openSignonWindow();
    }

    private void openSignonWindow() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                initSignonWindow();
                signonWindow.setVisible(true);
            }
        });
    }

    public void signon(Screenname screenname, String pass) {
        signon(new AimConnectionProperties(screenname, pass));
    }

    private void signon(AimConnectionProperties props) {
        Screenname sn = new Screenname(signonWindow.getScreenname());
        aimSession = appSession.openAimSession(sn);

        conn = aimSession.openConnection(props);
        conn.addStateListener(connStateListener);
        conn.connect();

        initSignonProgressWindow();
        signonProgressWindow.updateSession();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                signonWindow.setVisible(false);
                signonProgressWindow.pack();
                signonProgressWindow.setSize(signonProgressWindow.getPreferredSize());
                signonProgressWindow.setVisible(true);
            }
        });
    }

    private synchronized void initDummyOnlineWindow() {
        if (dummyOnlineWindow == null) {
            dummyOnlineWindow = new DummyOnlineWindow(this);
            dummyOnlineWindow.pack();
            dummyOnlineWindow.setSize(dummyOnlineWindow.getPreferredSize());
        }
    }

    private synchronized void initSignonWindow() {
        if (signonWindow == null) {
            signonWindow = new SignonWindow(this);
            signonWindow.pack();
            signonWindow.setSize(signonWindow.getPreferredSize());
        }
    }

    public void close() {
        synchronized(this) {
            if (!open || closed) return;
            closed = true;
        }

        disconnect();
        if (aimSession != null) aimSession.close();
        if (signonWindow != null) signonWindow.dispose();
        if (signonProgressWindow != null) signonProgressWindow.dispose();

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            GuiSessionListener listener = (GuiSessionListener) it.next();
            listener.closed(this);
        }
    }

    public void disconnect() {
        if (conn != null) conn.disconnect();
    }

    private synchronized void initSignonProgressWindow() {
        if (signonProgressWindow == null) {
            signonProgressWindow = new SignonProgressWindow(this);
        }
    }

    public AimSession getAimSession() { return aimSession; }

    public synchronized AimConnection getAimConnection() { return conn; }

    private Map imBoxes = new HashMap();

    public synchronized ImBox openImBox(Screenname sn) {
        ImBox box = (ImBox) imBoxes.get(sn);
        if (box == null) {
            IcbmService service = conn.getIcbmService();

            box = createBox(sn);

            ImConversation imConversation = service.getImConversation(sn);
            box.handleConversation(imConversation);

            box.pack();
            box.setSize(box.getPreferredSize());
            box.setVisible(true);
        }
        final ImBox fbox = box;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fbox.requestFocus();
            }
        });
        return box;
    }

    private synchronized ImBox createBox(Screenname sn) {
        DefensiveTools.checkNull(sn, "sn");
        if (imBoxes.containsKey(sn)) {
            throw new IllegalArgumentException("box for " + sn + " already exists");
        }
        ImBox box = new ImBox(this, sn);
        box.setSize(410, 280);
        imBoxes.put(sn, box);
        return box;
    }

    private void handleNewConversation(Conversation conv) {
        openImBox(conv.getBuddy()).handleConversation(conv);
    }

    public void showPrefsWindow(Screenname sn) {
        AccountPrefsWindow frame = new AccountPrefsWindow(appSession, sn);
        frame.addPrefsPane(new LocalCertificatesPrefsPane(appSession, sn));
        frame.addPrefsPane(new TrustPrefsPane(appSession, sn));
        frame.setSize(640, 480);
        frame.setVisible(true);
    }

    public AppSession getAppSession() {
        return appSession;
    }

    private class ConnStateListener implements StateListener {
        public void handleStateChange(StateEvent event) {
            AimConnection conn = event.getAimConnection();
            if (conn != aimSession.getConnection()) {
                // we can ignore this event since we're using a new connection
                // now
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
                        signonProgressWindow.setVisible(false);
                        initDummyOnlineWindow();
                        dummyOnlineWindow.setVisible(true);
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
                        signonWindow.setFailureInfo(sinfo);
                        if (signonProgressWindow != null) {
                            signonProgressWindow.setVisible(false);
                        }
                        if (dummyOnlineWindow != null) {
                            dummyOnlineWindow.setVisible(false);
                        }
                        openSignonWindow();
                    }
                });
            }
        }
    }
}
