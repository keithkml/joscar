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

package net.kano.joustsim.app.forms;

import net.kano.joscar.DefensiveTools;
import net.kano.joustsim.app.GuiSession;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.AimSession;
import net.kano.joustsim.oscar.State;
import net.kano.joustsim.oscar.StateEvent;
import net.kano.joustsim.oscar.StateListener;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

public class SignonProgressBox extends JPanel implements SignonWindowBox {
    private JPanel mainPanel;
    private JLabel signingOnLabel;
    private JList progressList;

    private final GuiSession guiSession;
    private AimSession aimSession;
    private AimConnection conn;
    private ProgressListModel progressListModel;

    private final Icon notStartedIcon = new ImageIcon(getClass().getClassLoader()
            .getResource("icons/progress-item-not-started.png"));
    private final Icon succeededIcon = new ImageIcon(getClass().getClassLoader()
            .getResource("icons/progress-item-succeeded.png"));
    private final Icon workingIcon = new ImageIcon(getClass().getClassLoader()
            .getResource("icons/progress-item-working.png"));
    private SignonWindow signonWindow = null;

    {
        setLayout(new BorderLayout());
        add(mainPanel);

        progressList.setCellRenderer(new ProgressListRenderer());
    }

    public SignonProgressBox(GuiSession guiSession) {
        DefensiveTools.checkNull(guiSession, "guiSession");

        this.guiSession = guiSession;

        updateSession();
    }

    public Component getSignonWindowBoxComponent() {
        return this;
    }

    public void signonWindowBoxShown(SignonWindow window) {
        signonWindow = window;
        updateGui();
    }

    public void updateSession() {
        aimSession = guiSession.getAimSession();
        conn = guiSession.getAimConnection();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                updateGui();
            }
        });
    }

    private void updateGui() {
        if (progressListModel != null) progressListModel.stop();
        progressListModel = null;
        AimConnection conn = this.conn;
        if (conn != null) {
            signingOnLabel.setText("Signing on " + conn.getScreenname() + "...");
            progressListModel = new ProgressListModel(conn);
            progressList.setModel(progressListModel);
        }
        signonWindow.updateSize(this);
    }

    private static class ProgressListModel extends AbstractListModel {
        private final StateInfo[] states = new StateInfo[] {
            new StateInfo(State.CONNECTINGAUTH),
            new StateInfo(State.AUTHORIZING),
            new StateInfo(State.CONNECTING),
            new StateInfo(State.SIGNINGON),
            new StateInfo(State.ONLINE),
        };
        private final AimConnection conn;
        private StateListener stateListener = new StateListener() {
            public void handleStateChange(StateEvent event) {
                State state = event.getNewState();
                updateState(state);
            }
        };

        public ProgressListModel(AimConnection conn) {
            this.conn = conn;
            conn.addStateListener(stateListener);
            updateState(conn.getState());
        }

        private void updateState(State state) {
            int index = -1;
            for (int i = 0; i < states.length; i++) {
                StateInfo stateInfo = states[i];
                if (stateInfo.getState() == state) {
                    stateInfo.setDoing(true);
                    stateInfo.setDone(false);
                    index = i;
                    break;
                }
            }
            if (index == -1) {
                // something special?
                return;
            }
            for (int i = index - 1; i >= 0; i--) {
                StateInfo stateInfo = states[i];
                stateInfo.setDoing(false);
                stateInfo.setDone(true);
            }
            fireContentsChanged(this, 0, index);
        }

        public void stop() {
            conn.removeStateListener(stateListener);
        }

        public int getSize() {
            return states.length;
        }

        public Object getElementAt(int index) {
            return states[index];
        }

        public class StateInfo {
            private final State state;
            private boolean doing = false;
            private boolean done = false;

            public StateInfo(State state) {
                this.state = state;
            }

            public State getState() { return state; }

            public synchronized boolean isDoing() { return doing; }

            public synchronized void setDoing(boolean doing) {
                this.doing = doing;
            }

            public synchronized boolean isDone() { return done; }

            public synchronized void setDone(boolean done) {
                this.done = done;
            }
        }
    }

    private class ProgressListRenderer extends DefaultListCellRenderer {
        private Font origFont = getFont().deriveFont(Font.PLAIN);
        private Font boldFont = origFont.deriveFont(Font.BOLD);

        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof ProgressListModel.StateInfo) {
                ProgressListModel.StateInfo si
                        = (ProgressListModel.StateInfo) value;

                if (si.isDoing()) setFont(boldFont);
                else setFont(origFont);
                if (si.isDone() || si.isDoing()) setForeground(Color.BLACK);
                else setForeground(Color.GRAY);
                if (si.isDone()) setIcon(succeededIcon);
                else if (si.isDoing()) setIcon(workingIcon);
                else setIcon(notStartedIcon);

                State state = si.getState();
                String text;
                if (state == State.CONNECTINGAUTH) {
                    text = "Connecting to authorization server";
                } else if (state == State.AUTHORIZING) {
                    text = "Sending username and password";
                } else if (state == State.CONNECTING) {
                    text = "Connecting to AIM server";
                } else if (state == State.SIGNINGON) {
                    text = "Signing on";
                } else if (state == State.ONLINE) {
                    text = "Online";
                } else {
                    text = "State: " + state;
                }
                setText(text);

            } else {
                return super.getListCellRendererComponent(list, value, index,
                        isSelected, cellHasFocus);
            }
            return this;
        }
    }
}
