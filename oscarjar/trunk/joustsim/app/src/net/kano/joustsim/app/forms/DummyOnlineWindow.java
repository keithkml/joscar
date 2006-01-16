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

package net.kano.joustsim.app.forms;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.FileWritable;
import net.kano.joscar.rvcmd.sendfile.FileSendBlock;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.app.GuiSession;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.BuddyInfo;
import net.kano.joustsim.oscar.BuddyInfoChangeListener;
import net.kano.joustsim.oscar.MyBuddyIconManager;
import net.kano.joustsim.oscar.oscar.service.bos.MainBosService;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionManager;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionManagerListener;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.IncomingFileTransfer;
import net.kano.joustsim.oscar.oscar.service.info.InfoService;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;

public class DummyOnlineWindow extends JFrame {
    private JPanel mainPanel;
    private JButton disconnectButton;
    private JTextField snBox;
    private JButton openButton;
    private JLabel onlineLabel;
    private JButton prefsButton;
    private JProgressBar memoryUseBar;

    private final GuiSession guiSession;
    private AimConnection conn = null;

    private OpenImAction openImAction = new OpenImAction();
    private DisconnectAction disconnectAction = new DisconnectAction();

    private Timer memoryUseTimer = new Timer(5000, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            updateMemoryUse();
        }
    });
    private BuddyListBox buddyListBox;
    private JButton awayButton;
    private JButton idleButton;
    private JButton statusButton;
    private JCheckBox visibleCheckbox;
    private JButton changeIconButton;
    private JLabel myIconLabel;

    {
        getContentPane().add(mainPanel);
        openButton.setAction(openImAction);
        disconnectButton.setAction(disconnectAction);
        snBox.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                changed();
            }

            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            private void changed() {
                updateButtons();
            }
        });
        memoryUseTimer.setInitialDelay(0);
        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                memoryUseTimer.start();
            }

            public void windowClosing(WindowEvent e) {
                conn.disconnect();
                memoryUseTimer.stop();
            }
        });

        prefsButton.setAction(new ShowPrefsAction());

        setIconImage(new ImageIcon(getClass().getClassLoader()
                .getResource("icons/buddy-list-tiny.png")).getImage());
        awayButton.setAction(new AbstractAction() {
            {
                putValue(NAME, "Away");
            }
            public void actionPerformed(ActionEvent e) {
                String msg = JOptionPane.showInputDialog(DummyOnlineWindow.this,
                        "Away message (leave empty to set un-away):",
                        "doing something cool");

                if (msg == null) return;
                InfoService infoService = conn.getInfoService();
                if (msg.equals("")) {
                    infoService.setAwayMessage(null);
                } else {
                    infoService.setAwayMessage(msg);
                }
            }
        });
        idleButton.setAction(new AbstractAction() {
            {
                putValue(NAME, "Idle");
            }
            public void actionPerformed(ActionEvent e) {
                String msg = JOptionPane.showInputDialog(DummyOnlineWindow.this,
                        "Idle time, in minutes (0 for un-idle):");

                if (msg == null) return;
                int mins = Integer.parseInt(msg);
                MainBosService service = conn.getBosService();
                if (mins == 0) {
                    service.setUnidle();
                } else {
                    service.setIdleSince(new Date(System.currentTimeMillis()-(mins*60*1000)));
                }
            }
        });
        statusButton.setAction(new AbstractAction() {
            {
                putValue(NAME, "Status");
            }

            public void actionPerformed(ActionEvent e) {
                String msg = JOptionPane.showInputDialog(DummyOnlineWindow.this,
                        "Status message:");
                if (msg == null) return;

                conn.getBosService().setStatusMessage(msg);
            }
        });
        visibleCheckbox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                boolean visible = visibleCheckbox.isSelected();
                conn.getBosService().setVisibleStatus(visible);
            }
        });
        changeIconButton.setAction(new AbstractAction() {
            {
                putValue(NAME, "Change...");
            }

            private JLabel accessory = new JLabel();
            private JFileChooser chooser = new JFileChooser();
            {
                chooser.setAcceptAllFileFilterUsed(true);
                chooser.setApproveButtonText("Choose");
                chooser.setDialogTitle("Choose Buddy Icon");
                chooser.setAccessory(accessory);
                chooser.addChoosableFileFilter(new FileFilter() {
                    public boolean accept(File f) {
                        if (f.isDirectory()) return true;

                        String name = f.getName();
                        int dot = name.lastIndexOf('.');
                        if (dot == -1) return false;
                        String ext = name.substring(dot + 1).toLowerCase();
                        return Arrays.asList("png", "jpg", "gif").contains(ext);
                    }

                    public String getDescription() {
                        return "Image files (*.png, *.jpg, *.gif)";
                    }
                });
                accessory.setSize(50, 50);
                chooser.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        Image icon = getSelectedImage();
                        accessory.setIcon(icon == null ? null : new ImageIcon(icon));
                    }
                });
            }

            private Image getSelectedImage() {
                File file = chooser.getSelectedFile();
                Image icon = null;
                if (file != null) {
                    try {
                        icon = ImageIO.read(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return icon;
            }

            public void actionPerformed(ActionEvent e) {
                int result = chooser.showOpenDialog(DummyOnlineWindow.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    MyBuddyIconManager mgr = guiSession.getAimConnection().getMyBuddyIconManager();
                    FileWritable writable = new FileWritable(
                            chooser.getSelectedFile().getAbsolutePath());
                    mgr.requestSetIcon(ByteBlock.createByteBlock(writable));
                }
            }
        });
    }

    private void updateMemoryUse() {
        assert SwingUtilities.isEventDispatchThread();

        Runtime runtime = Runtime.getRuntime();
        int totalkb = (int) (runtime.totalMemory()/1024);
        int usedkb = (int) ((runtime.totalMemory() - runtime.freeMemory())/1024);
        memoryUseBar.setMaximum(totalkb);
        memoryUseBar.setValue(usedkb);
        NumberFormat formatter = NumberFormat.getNumberInstance();
        formatter.setMaximumFractionDigits(1);
        memoryUseBar.setString(formatter.format(usedkb/1024.0) + " MB of "
                + formatter.format(totalkb/1024.0) + " MB");
    }

    public DummyOnlineWindow(GuiSession session) {
        DefensiveTools.checkNull(session, "session");

        this.guiSession = session;
    }

    public void updateSession() {
        synchronized(this) {
            this.conn = guiSession.getAimConnection();
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String sn = conn.getScreenname().getFormatted();
                setTitle(sn);
                onlineLabel.setText("You are online as " + sn);
                buddyListBox.updateSession(guiSession);
                snBox.setText("");
                updateButtons();
            }
        });
        RvConnectionManager ftm = conn.getIcbmService().getFileTransferManager();
        ftm.addFileTransferListener(new RvConnectionManagerListener() {
            public void handleNewIncomingFileTransfer(RvConnectionManager manager,
                    IncomingFileTransfer transfer) {
                FileSendBlock fileInfo = transfer.getFileInfo();
                int choice = JOptionPane.showConfirmDialog(DummyOnlineWindow.this,
                        transfer.getBuddyScreenname() + " wants to send you "
                                + fileInfo.getFileCount() + " files \""
                                + fileInfo.getFilename() + "\"", "File Transfer",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    watchTransfer(transfer);
                    transfer.accept();
                } else {
                    transfer.decline();
                }
            }
        });
        BuddyInfo myInfo = conn.getBuddyInfoManager()
                .getBuddyInfo(conn.getScreenname());
        myInfo.addPropertyListener(new BuddyInfoChangeListener() {
            public void receivedBuddyStatusUpdate(BuddyInfo info) {
            }

            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if (propertyName.equals(BuddyInfo.PROP_ICON_DATA)) {
                    ByteBlock data = (ByteBlock) evt.getNewValue();
                    ImageIcon image = new ImageIcon(data.toByteArray());
                    myIconLabel.setIcon(image);
                }
            }
        });
    }

    //TODO: make communication between buddylistbox and dummyonline window more MVC
    public void watchTransfer(final FileTransfer transfer) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                FileTransferDialog dialog = new FileTransferDialog(transfer);
                dialog.pack();
                dialog.setVisible(true);
            }
        });
    }

    private void updateButtons() {
        assert SwingUtilities.isEventDispatchThread();

        openImAction.setEnabled(snBox.getDocument().getLength() != 0);
    }

    private class OpenImAction extends AbstractAction {
        public OpenImAction() {
            super("IM");

            putValue(SHORT_DESCRIPTION, "Open an IM window with this buddy");
        }

        public void actionPerformed(ActionEvent e) {
            Screenname sn = new Screenname(snBox.getText());
            snBox.setText("");
            guiSession.openImBox(sn);
        }
    }

    private class DisconnectAction extends AbstractAction {
        public DisconnectAction() {
            super("Disconnect");

            putValue(MNEMONIC_KEY, KeyEvent.VK_D);
            putValue(SHORT_DESCRIPTION, "Disconnect from AIM");
        }

        public void actionPerformed(ActionEvent e) {
            guiSession.signoff();
        }
    }

    private class ShowPrefsAction extends AbstractAction {
        public ShowPrefsAction() {
            super("Preferences");

            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
            putValue(SHORT_DESCRIPTION, "View this screenname's account "
                    + "preferences");
        }

        public void actionPerformed(ActionEvent e) {
            guiSession.openPrefsWindow(conn.getScreenname());
        }
    }
}
