package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joscar.rvcmd.InvitationMessage;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.TransferredFile;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface OutgoingFileTransfer
    extends FileTransfer, OutgoingRvConnection {
  void sendRequest(InvitationMessage msg);

  TransferredFile setSingleFile(File file) throws IOException;

  TransferredFile addFile(File file) throws IOException;

  /**
   * {@code name} can contain a full relative path, like {@code a/b/c}
   */
  TransferredFile addFile(File file, String pseudonym) throws IOException;

  /**
   * Adds each file in {@code files} as if it's in a folder called
   * {@code folderName}, ignoring the actual folder hierarchy relationship
   * between the files. {@code folderName} may contain a full relative path,
   * like {@code x/y/z}.
   */
  List<TransferredFile> addFilesInFlatFolder(String folderName,
      List<File> files) throws IOException;

  /**
   * Adds each file in {@code files} under the given {@code root}. Each file
   * will be sent with its full relative path from the {@code root}, not
   * including the {@code root}'s name, but prefixed with the {@code folderName}.
   *
   * For example, calling this method with folderName of "cool", root
   * "/home/klea/xyz", and files "/home/klea/xyz/file1" &amp;
   * "/home/klea/xyz/dir/file2", will produce {@code TransferredFile}s with
   * paths of "cool/file1" and "cool/dir/file2".
   */
  List<TransferredFile> addFilesInHierarchy(String folderName, File root,
      List<File> files) throws IOException;

  void addFilesWithDetails(List<TransferredFile> files);

  void setDisplayName(String name);

  List<TransferredFile> getFiles();

  @Nullable String getDisplayName();

  FileChecksummer getChecksummer();

  TransferredFileFactory getTransferredFileFactory();

  void setTransferredFileFactory(TransferredFileFactory factory);
}
