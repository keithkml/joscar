package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joscar.rvcmd.InvitationMessage;

import java.io.File;
import java.util.List;

public interface OutgoingFileTransfer extends FileTransfer {
    boolean isTrustingProxyRedirects();

    void setTrustProxyRedirects(boolean trustingProxyRedirects);

    boolean isOnlyUsingProxy();

    void setOnlyUsingProxy(boolean onlyUsingProxy);

    void makeRequest(InvitationMessage msg);

    List<File> getFiles();

    void setFiles(List<File> files);
}
