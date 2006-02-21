package net.kano.joscar.snaccmd.icbm;

public interface SingleBuddyRequest {
    /**
     * Returns the screenname to whom this message is to be sent.
     *
     * @return the screenname of the recipient of this message
     */
    String getScreenname();
}
