package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joscar.rvcmd.InvitationMessage;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.TransferredFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface OutgoingFileTransfer
    extends FileTransfer, OutgoingRvConnection {
  void sendRequest(InvitationMessage msg);

  void setFile(File file) throws IOException;

  void setFile(File file, String pseudonym) throws IOException;

  void setFiles(String folderName, List<File> files) throws IOException;

  void setFilesWithDetails(String folderName, List<TransferredFile> files);

  List<TransferredFile> getFiles();

  String getFolderName();

  FileChecksummer getChecksummer();
}
