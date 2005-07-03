package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joscar.rvcmd.InvitationMessage;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface OutgoingFileTransfer extends FileTransfer {
    void makeRequest(InvitationMessage msg);

    List<File> getFiles();

    void setFiles(String folderName, List<File> files);

    void setFile(File file);

    String getFolderName();

    Map<File, String> getNameMappings();

    void mapName(File file, String name);

    String getMappedName(File file);
}
