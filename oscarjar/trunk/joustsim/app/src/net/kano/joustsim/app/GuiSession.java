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

import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.AimConnectionProperties;
import net.kano.joustsim.oscar.State;
import net.kano.joustsim.oscar.StateEvent;
import net.kano.joustsim.oscar.StateInfo;
import net.kano.joustsim.oscar.StateListener;
import net.kano.joustsim.oscar.AimSession;
import net.kano.joustsim.oscar.oscar.service.icbm.Conversation;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmBuddyInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmListener;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmService;
import net.kano.joustsim.app.forms.DummyOnlineWindow;
import net.kano.joustsim.app.forms.ImBox;
import net.kano.joustsim.app.forms.SignonBox;
import net.kano.joustsim.app.forms.SignonProgressBox;
import net.kano.joustsim.app.forms.SignonWindow;
import net.kano.joustsim.app.forms.prefs.AccountPrefsWindow;
import net.kano.joustsim.app.forms.prefs.LocalCertificatesPrefsPane;
import net.kano.joustsim.app.forms.prefs.TrustPrefsPane;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.DefensiveTools;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GuiSession {
    private final JoustsimSession appSession;

    private SignonWindow signonWin = null;
    private SignonBox signonBox = null;
    private SignonProgressBox signonProgressBox = null;
    private DummyOnlineWindow dummyOnlineWindow = null;

    private AimSession aimSession;
    private AimConnection conn;

    private CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();

    private boolean open = false;
    private boolean closed = false;
    private ConnStateListener connStateListener = new ConnStateListener();

    private Map imBoxes = new HashMap();
    private Map prefsWindows = new HashMap();

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

    public void close() {
        AimSession aimSession;
        SignonWindow signonWin;
        synchronized(this) {
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

        if (aimSession != null) aimSession.closeConnection();
        if (signonWin != null) signonWin.dispose();

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            GuiSessionListener listener = (GuiSessionListener) it.next();
            listener.closed(this);
        }
    }

    public void signon(net.kano.joustsim.Screenname screenname, String pass) {
        signon(new AimConnectionProperties(screenname, pass));
    }

    public void signoff() {
        AimConnection conn = getAimConnection();
        if (conn != null) conn.disconnect();
    }

    public synchronized ImBox openImBox(net.kano.joustsim.Screenname sn) {
        ImBox box = (ImBox) imBoxes.get(sn);
        if (box == null) {
            IcbmService service = conn.getIcbmService();

            box = createBox(sn);

            box.handleConversation(service.getImConversation(sn));
            box.handleConversation(service.getSecureAimConversation(sn));
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

    public synchronized AccountPrefsWindow openPrefsWindow(net.kano.joustsim.Screenname sn) {
        AccountPrefsWindow frame = (AccountPrefsWindow) prefsWindows.get(sn);
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
                fframe.setVisible(true);
                fframe.requestFocus();
            }
        });
        return frame;
    }

    private void signon(AimConnectionProperties props) {
        if (closed) return;

        net.kano.joustsim.Screenname sn = new net.kano.joustsim.Screenname(signonBox.getScreenname());
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

        conn = aimSession.openConnection(props);
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

    private synchronized void initSignonWin() {
        if (closed) return;
        if (signonWin == null) {
            signonWin = new SignonWindow(this);
        }
    }

    private synchronized void initSignonBox() {
        if (closed) return;
        initSignonWin();
        if (signonBox == null) {
            signonBox = new SignonBox(this);
        }
    }

    private synchronized void initSignonProgressWindow() {
        if (closed) return;
        initSignonWin();
        if (signonProgressBox == null) {
            signonProgressBox = new SignonProgressBox(this);
        }
    }

    private void openSignonWindow() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame();
                JList list = new JList();
                list.setForeground(Color.RED);
                DefaultListModel model = new DefaultListModel();
                list.setModel(model);
                list.setCellRenderer(new DefaultListCellRenderer() {
                    public Component getListCellRendererComponent(JList list, Object value,
                            int index, boolean isSelected, boolean cellHasFocus) {
                        return super.getListCellRendererComponent(list, value, index,
                                isSelected, cellHasFocus);
                    }
                });
                model.addElement("<HTML><B>test<br>test2<br>");
                model.addElement("test test");
                frame.getContentPane().add(list);
                frame.setSize(300, 300);
                frame.setVisible(true);
                initSignonBox();
                signonWin.setToSignonBox();
                System.out.println("setting visible because of openSignonWindow call");
                signonWin.setVisible(true);
            }
        });
    }

    private synchronized ImBox createBox(net.kano.joustsim.Screenname sn) {
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

    private synchronized void resetStuff() {
        for (Iterator it = imBoxes.values().iterator(); it.hasNext();) {
            ImBox box = (ImBox) it.next();
            box.setInvalidBox();
            box.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        }
        imBoxes.clear();
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

                    public void buddyInfoUpdated(IcbmService service, net.kano.joustsim.Screenname buddy,
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
                resetStuff();
            }
        }
    }
}
