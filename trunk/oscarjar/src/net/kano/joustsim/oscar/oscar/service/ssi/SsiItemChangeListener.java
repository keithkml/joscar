package net.kano.joustsim.oscar.oscar.service.ssi;

import net.kano.joscar.snaccmd.ssi.SsiItem;

public interface SsiItemChangeListener {
    void handleItemCreated(SsiItem item);

    void handleItemModified(SsiItem item);

    void handleItemDeleted(SsiItem item);
}
