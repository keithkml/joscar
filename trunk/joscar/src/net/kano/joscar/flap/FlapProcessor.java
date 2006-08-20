package net.kano.joscar.flap;

import net.kano.joscar.net.ConnProcessor;

import java.io.IOException;

public interface FlapProcessor extends ConnProcessor {
    /**
     * Adds a "vetoable packet listener." A vetoable packet listener has the
     * ability to halt the processing of a given FLAP.
     *
     * @param listener the listener to add
     */
    void addVetoablePacketListener(VetoableFlapPacketListener listener);

    /**
     * Removes the given vetoable packet listener from this FLAP processor's
     * list of vetoable packet listeners.
     *
     * @param listener the listener to remove
     */
    void removeVetoablePacketListener(VetoableFlapPacketListener listener);

    /**
     * Adds a FLAP packet listener to this FLAP processor.
     *
     * @param listener the listener to add
     */
    void addPacketListener(FlapPacketListener listener);

    /**
     * Removes a FLAP packet listener from this FLAP processor.
     *
     * @param listener the listener to remove
     */
    void removePacketListener(FlapPacketListener listener);

    /**
     * Sets the FLAP command factory to use for generating
     * <code>FlapCommand</code>s from FLAP packets. This can be
     * <code>null</code>, disabling the generation of <code>FlapCommand</code>s.
     *
     * @param factory the new factory to use, or <code>null</code> to disable
     *        the generation of <code>FlapCommand</code>s on this connection
     */
    void setFlapCmdFactory(FlapCommandFactory factory);

    /**
     * Sends the given FLAP command on this FLAP processor's attached output
     * stream. Note that <i>if this processor is not currently attached to
     * an output stream or socket, this method will <b>return silently</b></i>.
     *
     * @param command the command to send
     */
    void sendFlap(FlapCommand command);

    void runFlapLoop() throws IOException;
}
