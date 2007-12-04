package net.kano.joustsim.oscar.oscar.service.ssi;

import net.kano.joustsim.*;

/**
 * @author Damian Minkov
 */
public interface BuddyAuthorizationListener
{
    void authorizationDenied(Screenname screenname, String reason);
    void authorizationAccepted(Screenname screenname, String reason);
    void authorizationRequestReceived(Screenname screenname, String reason);
    // return whether to create buddy as awaiting authorization
    boolean authorizationRequired(Screenname screenname);

    void futureAuthorizationGranted(Screenname screenname, String reason);
    void youWereAdded(Screenname screenname);
}
