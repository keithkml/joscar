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
 *  File created by keith @ Feb 5, 2004
 *
 */

package net.kano.joustsim.app.forms.prefs;

import net.kano.joustsim.oscar.AppSession;
import net.kano.joustsim.Screenname;
import net.kano.joscar.DefensiveTools;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

public class AccountPrefsWindow extends JFrame {
    private JViewport currentPaneHolder;
    private JList sectionsList;
    private JLabel mainLabel;
    private JPanel mainPanel;
    private JSplitPane splitPane;
    private JScrollPane sectionsScrollPane;
    private JButton closeButton;

    private final AppSession session;
    private final net.kano.joustsim.Screenname sn;

    private PrefsPane currentPane = null;

    private DefaultListModel sectionsModel = new DefaultListModel();

    {
        getContentPane().add(mainPanel);

        EmptyBorder empty = new EmptyBorder(0, 0, 0, 0);
        splitPane.setBorder(empty);
//        currentPaneHolder.setBorder(empty);

        sectionsList.setModel(sectionsModel);
        sectionsList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof PrefsPane) {
                    PrefsPane pane = (PrefsPane) value;
                    super.getListCellRendererComponent(list,
                            pane.getPrefsName(), index, isSelected,
                            cellHasFocus);
                    setIcon(pane.getSmallPrefsIcon());

                } else {
                    super.getListCellRendererComponent(list, value, index,
                            isSelected, cellHasFocus);
                }

                return this;
            }
        });
        ListSelectionModel selm = sectionsList.getSelectionModel();
        selm.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updatePane();
            }
        });

        currentPaneHolder.setLayout(new BorderLayout());

        Font font = mainLabel.getFont();
        mainLabel.setFont(font.deriveFont(Font.BOLD, font.getSize() + 4));
        addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent e) {
                PrefsPane pane = getCurrentPane();
                if (pane != null) pane.prefsWindowFocused();
            }

            public void windowLostFocus(WindowEvent e) {
                PrefsPane pane = getCurrentPane();
                if (pane != null) pane.prefsWindowFocusLost();
            }
        });

        closeButton.setAction(new CloseAction());
    }

    public AccountPrefsWindow(AppSession session, net.kano.joustsim.Screenname sn) {
        DefensiveTools.checkNull(session, "session");
        DefensiveTools.checkNull(sn, "sn");

        this.sn = sn;
        this.session = session;
        setTitle("Account Preferences");
        mainLabel.setText("Account preferences for " + sn.getFormatted());
    }

    public void addPrefsPane(final PrefsPane pane) {
        DefensiveTools.checkNull(pane, "pane");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                sectionsModel.addElement(pane);
            }
        });
    }

    private void updatePane() {
        assert EventQueue.isDispatchThread();

        final PrefsPane current = (PrefsPane) sectionsList.getSelectedValue();
        final PrefsPane old;
        synchronized(this) {
            old = currentPane;
            if (old == current) return;
            currentPane = current;
        }
        if (old != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    old.prefsPaneHidden();
                }
            });
        }
        Component view = current == null ? null : current.getPrefsComponent();
        if (current != null) {
            currentPaneHolder.setView(null);
            current.prefsPaneToBeShown();
        }
        currentPaneHolder.setView(view);
        currentPaneHolder.repaint();
        if (current != null) {
            SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                System.out.println("prefs pane shown");
                current.prefsPaneShown();
            }
        });
        }
    }

    private synchronized PrefsPane getCurrentPane() {
        return currentPane;
    }

    private class CloseAction extends AbstractAction {
        public CloseAction() {
            super("Close");

            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        }

        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }
}
