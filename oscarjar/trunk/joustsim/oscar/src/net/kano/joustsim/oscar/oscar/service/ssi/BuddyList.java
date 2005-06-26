package net.kano.joustsim.oscar.oscar.service.ssi;

import net.kano.joscar.snaccmd.ssi.SsiItem;

import java.util.List;

public interface BuddyList {
    void handleItemCreated(SsiItem item);

    void handleItemModified(SsiItem item);

    void handleItemDeleted(SsiItem item);

    void addLayoutListener(BuddyListLayoutListener listener);

    void removeLayoutListener(BuddyListLayoutListener listener);

    List<Group> getGroups();

    void addRetroactiveLayoutListener(
            BuddyListLayoutListener listener);
}
