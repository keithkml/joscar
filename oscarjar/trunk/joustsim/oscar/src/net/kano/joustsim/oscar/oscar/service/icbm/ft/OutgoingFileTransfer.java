package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joscar.rvcmd.InvitationMessage;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface OutgoingFileTransfer extends FileTransfer, OutgoingRvConnection {
    void sendRequest(InvitationMessage msg);

    void setFile(File file);
    void setFiles(String folderName, List<File> files);
    List<File> getFiles();
    String getFolderName();

    Map<File, String> getNameMappings();
    void mapName(File file, String name);
    String getMappedName(File file);

    FileChecksummer getChecksummer();
}
