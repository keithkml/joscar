package net.kano.joustsim.app.forms;

import net.kano.joscar.rvcmd.sendfile.FileSendBlock;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransferListener;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransferState;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.IncomingFileTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.OutgoingFileTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.ProgressStatusProvider;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.FileTransferEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ChecksummingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.TransferringFileEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FileTransferDialog extends JFrame {
    private JTextPane detailsLog;
    private JLabel stateLabel;
    private JLabel infoLabel;
    private JProgressBar progressBar;

    private ProgressStatusProvider progressProvider = null;

    private JPanel mainPanel;

    {
        progressBar.setMinimum(0);
        getContentPane().add(mainPanel);
        Timer timer = new Timer(200, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                long position;
                long length;
                boolean hasProvider = progressProvider != null;
                if (hasProvider) {
                    position = progressProvider.getPosition();
                    length = progressProvider.getLength();
                } else {
                    position = 0;
                    length = 0;
                }
                progressBar.setValue((int) position);
                progressBar.setMaximum((int) length);
                progressBar.setIndeterminate(!hasProvider);
            }
        });
        timer.start();
    }

    private final FileTransfer fileTransfer;

    public FileTransferDialog(FileTransfer fileTransfer) {
        this.fileTransfer = fileTransfer;
        fileTransfer.addTransferListener(new FileTransferListener() {
            public void handleEventWithStateChange(FileTransfer transfer,
                    FileTransferState state, FileTransferEvent event) {
                stateLabel.setText(state.toString());
                printEvent(event);
            }

            public void handleEvent(FileTransfer transfer, FileTransferEvent event) {
                printEvent(event);
            }

            private void printEvent(FileTransferEvent event) {
                if (event instanceof ChecksummingEvent) {
                    ChecksummingEvent event1 = (ChecksummingEvent) event;
                    progressProvider = event1.getChecksummer();
                } else if (event instanceof TransferringFileEvent) {
                    TransferringFileEvent event1 = (TransferringFileEvent) event;
                    progressProvider = event1.getProgressProvider();
                }
                Document doc = detailsLog.getDocument();
                try {
                    doc.insertString(doc.getEndPosition().getOffset(),
                            event.toString() + "\n", null);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        });
        String word;
        if (fileTransfer instanceof IncomingFileTransfer) {
            word = "Downloading";
        } else if (fileTransfer instanceof OutgoingFileTransfer) {
            word = "Sending";
        } else {
            throw new IllegalArgumentException("file transfer is not incoming "
                    + "or outgoing");
        }
        FileSendBlock fileInfo = fileTransfer.getFileInfo();
        String filesDescr;
        String filename = fileInfo.getFilename();
        int fileCount = fileInfo.getFileCount();
        if (fileCount == 1) {
            filesDescr = filename;
        } else {
            filesDescr = "folder " + filename + " (" + fileCount + " items)";
        }
        infoLabel.setText(word + " " + filesDescr);
    }

    public FileTransfer getFileTransfer() {
        return fileTransfer;
    }
}
