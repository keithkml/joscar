/*
 *  Copyright (c) 2002, The Joust Project
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions 
 *  are met:
 *
 *  - Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer. 
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution. 
 *  - Neither the name of the Joust Project nor the names of its
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 *  File created by keith @ Mar 6, 2003
 *
 */

package net.kano.joscar.flap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Provides a slightly more developer-friendly interface to a TCP-based outgoing
 * FLAP connection than <code>FlapProcessor</code>. Provides a concept of
 * connection state as well as a dedicated FLAP reading thread to continuously
 * process incoming commands (as opposed to having to call
 * <code>readNextFlap</code> manually).
 * <br>
 * <br>
 * One shortcoming of this class is that it does not by default support a
 * proxy of any kind. While I'm not sure proxy support is in the scope of this
 * class and/or this package, it is obviously a popular feature, and will
 * probably be added in some form to the package at a later date.
 * <br>
 * <br>
 * Possible state transitions and meanings:
 * <dl>
 *
 * <dt><code>STATE_NOT_CONNECTED</code> -&gt; <code>STATE_INITING</code> -&gt;
 * <code>STATE_FAILED</code></dt>
 * <dd>Creating the connection / FLAP processing thread failed</dd>
 *
 * <dt><code>STATE_NOT_CONNECTED</code> -&gt; <code>STATE_INITING</code> -&gt;
 * <code>STATE_RESOLVING</code> -&gt; <code>STATE_FAILED</code></dt>
 * <dd>Looking up the specified hostname failed (maybe the host does not
 * exist)</dd>
 *
 * <dt><code>STATE_NOT_CONNECTED</code> -&gt; <code>STATE_INITING</code> -&gt;
 * <i>[<code>STATE_RESOLVING</code> (optional)]</i> -&gt;
 * <code>STATE_CONNECTING</code> -&gt; <code>STATE_FAILED</code></dt>
 * <dd>Making a TCP connection to the given server on the given port failed</dd>
 *
 * <dt><code>STATE_NOT_CONNECTED</code> -&gt; <code>STATE_INITING</code> -&gt;
 * <i>[<code>STATE_RESOLVING</code> (optional)]</i> -&gt;
 * <code>STATE_CONNECTING</code> -&gt; <code>STATE_CONNECTED</code> -&gt;
 * <code>STATE_NOT_CONNECTED</code></dt>
 * <dd>This is the normal progression of state, though I hope for your sake that
 * a good amount of time passes between the last two states :)</dd>
 * </dl>
 * Just to make things more confusing, if you call <code>disconnect</code>
 * during a connection, the state will revert to
 * <code>STATE_NOT_CONNECTED</code> no matter what state it's currently in.
 * <br>
 * <br>
 * Note that this class has various means of setting both a hostname and an
 * IP address to use for connecting. More importantly, note that
 * <code>connect</code> will <i>fail</i> if both of these are set and if neither
 * of these is set. So what does this mean? Yes, it means only one of these
 * values can be non-<code>null</code> when <code>connect</code> is called.
 */
public class ClientFlapConn extends FlapProcessor {
    /**
     * A state indicating that this FLAP client is not connected to a server.
     */
    public static final Object STATE_NOT_CONNECTED = "NOT_CONNECTED";

    /**
     * A state indicating that this FLAP client is preparing to connect. This
     * state normally does not last for more than a few milliseconds.
     */
    public static final Object STATE_INITING = "INITING";

    /**
     * A state indicating that the given hostname is being resolved to an IP
     * address before connecting.
     */
    public static final Object STATE_RESOLVING = "RESOLVING";

    /**
     * A state indicating that a TCP connection attempt is being made to the
     * given server on the given port.
     */
    public static final Object STATE_CONNECTING = "CONNECTING";

    /**
     * A state indicating that a TCP connection has succeeded and is currently
     * open.
     */
    public static final Object STATE_CONNECTED = "CONNECTED";

    /**
     * A state indicating that some stage of the connection failed. See
     * {@link ClientFlapConn} documentation for details on state transitions
     * and meanings.
     */
    public static final Object STATE_FAILED = "FAILED";

    /**
     * A reason indicating that the reason for a state change to
     * <code>NOT_CONNECTED</code> was that <code>disconnect</code> was called.
     */
    public static final Object REASON_ON_PURPOSE = "ON_PURPOSE";

    /**
     * The current state of the connection.
     */
    private Object state = STATE_NOT_CONNECTED;

    /**
     * The TCP socket on which this FLAP connection is being held, if any.
     */
    private Socket socket = null;

    /**
     * The hostname we are supposed to connect to.
     */
    private String host = null;

    /**
     * The IP address we are supposed to connect to.
     */
    private InetAddress ip = null;

    /**
     * The port we are supposed to connect to.
     */
    private int port = -1;

    /**
     * A list of connection listeners (state change listeners).
     */
    private List connListeners = new ArrayList();

    /**
     * The current connection thread.
     */
    private ConnectionThread connThread = null;

    /**
     * Creates a client FLAP connection with the default FLAP command factory
     * and no host, IP, or port to connect to yet.
     */
    public ClientFlapConn() { }

    /**
     * Creates a client FLAP connection with the default FLAP command factory
     * and the given hostname and port. The given hostname and port will be be
     * used to connect to when <code>connect</code> is called.
     *
     * @param host the hostname to connect to when <code>connect</code> is
     *        called
     * @param port the port to connect to when <code>connect</code> is called
     */
    public ClientFlapConn(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Creates a client FLAP connection with the default FLAP command factory
     * and the given IP and port. The given IP and port will be used to connect
     * to when <code>connect</code> is called.
     *
     * @param ip the IP address to connect to when <code>connect</code> is
     *        called
     * @param port the port to connect to when <code>connect</code> is called
     */
    public ClientFlapConn(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    /**
     * Adds a connection listener to this connection.
     *
     * @param l the listener to add
     */
    public synchronized final void addConnListener(FlapConnListener l) {
        if (!connListeners.contains(l)) connListeners.add(l);
    }

    /**
     * Removes a connection listener from this connection.
     *
     * @param l the listener to remove
     */
    public synchronized final void removeConnListener(FlapConnListener l) {
        connListeners.remove(l);
    }

    /**
     * Returns the hostname associated with this connection.
     *
     * @return the hostname associated with this connection
     */
    public synchronized final String getHost() {
        return host;
    }

    /**
     * Sets the hostname associated with this connection.
     *
     * @param host the hostname to associate with this connection
     */
    public synchronized final void setHost(String host) {
        this.host = host;
    }

    /**
     * Returns the IP address associated with this connection.
     *
     * @return the IP address associated with this connection
     */
    public synchronized final InetAddress getIP() {
        return ip;
    }

    /**
     * Sets the IP address associated with this connection.
     *
     * @param ip the IP address associated with this connection
     */
    public synchronized final void setIP(InetAddress ip) {
        this.ip = ip;
    }

    /**
     * Returns the TCP port associated with this connection.
     *
     * @return the TCP port associated with this connection
     */
    public synchronized final int getPort() {
        return port;
    }

    /**
     * Sets the TCP port associated with this connection.
     *
     * @param port the TCP port associated with this connection
     */
    public synchronized final void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns the current connection state. This will be one of {@link
     * #STATE_NOT_CONNECTED}, {@link #STATE_INITING}, {@link #STATE_RESOLVING},
     * {@link #STATE_CONNECTING}, {@link #STATE_CONNECTED}, or {@link
     * #STATE_FAILED}; see each value's individual documentation for details.
     *
     * @return the current state of this connection
     */
    public synchronized final Object getState() {
        return state;
    }

    /**
     * Sets the state of the connection to the given value, providing the given
     * reason to state listeners. Must be one of {@link
     * #STATE_NOT_CONNECTED}, {@link #STATE_INITING}, {@link #STATE_RESOLVING},
     * {@link #STATE_CONNECTING}, {@link #STATE_CONNECTED}, or {@link
     * #STATE_FAILED}.
     *
     * @param state the new connection state
     * @param reason a "reason" or description of this state change to provide
     *        to state listeners
     */
    private synchronized final void setState(Object state, Object reason) {
        if (this.state == state || (this.state == STATE_FAILED
                && state == STATE_NOT_CONNECTED)) return;

        Object oldState = this.state;
        this.state = state;

        FlapConnEvent event = new FlapConnEvent(this, oldState, this.state,
                reason);

        for (Iterator it = connListeners.iterator(); it.hasNext();) {
            FlapConnListener listener = (FlapConnListener) it.next();

            listener.stateChanged(event);
        }
    }

    /**
     * Sets the socket associated with this connection.
     *
     * @param socket the socket to associate with this connection
     */
    private synchronized final void setSocket(Socket socket) {
        this.socket = socket;
    }

    /**
     * Attempts to connect using the values of {@linkplain #setHost host} or
     * {@linkplain #setIP IP address} and {@linkplain #setPort TCP port} which
     * were, presumably, set before this method was called. Upon successful
     * connection, a FLAP reading loop will be started and FLAP processing
     * will begin.
     *
     * @throws IllegalStateException if a connection attempt is already being
     *         made; both IP and hostname are both set; neither IP or hostname
     *         is set and/or TCP port is not set
     */
    public synchronized final void connect() throws IllegalStateException {
        if (state != STATE_NOT_CONNECTED && state != STATE_FAILED) {
            throw new IllegalStateException("I am already " +
                    "connected/connecting");
        }
        if (host == null && ip == null) {
            throw new IllegalStateException("either host or ip must be " +
                    "non-null");
        }
        if (host != null && ip != null) {
            throw new IllegalStateException("host and ip may not both be " +
                    "non-null");
        }
        if (port == -1) {
            throw new IllegalStateException("port must not be -1");
        }

        setState(STATE_INITING, null);
        try {
            connThread = new ConnectionThread();
            new Thread(connThread, "FLAP connection to "
                    + (host == null ? (Object) ip : (Object) host) + ":"
                    + port).start();
        } catch (Throwable t) {
            setState(STATE_FAILED, t);
        }
    }

    /**
     * If not already disconnected, this disconnects the TCP socket associated
     * with this connection and sets the connection state to
     * <code>STATE_NOT_CONNECTED</code>. Note that if the connection state is
     * already <CODE>STATE_NOT_CONNECTED</code> or <code>STATE_FAILED</code>
     * no state change will take place.
     */
    public synchronized void disconnect() {
        if (state == STATE_NOT_CONNECTED || state == STATE_FAILED) return;

        detach();
        connThread.cancel();
        setState(STATE_NOT_CONNECTED, REASON_ON_PURPOSE);
        try { socket.close(); } catch (IOException ignored) { }
    }

    /**
     * A thread to resolve a hostname (if necessary), initiate a TCP connection,
     * and run a FLAP reading loop on the connection indefinitely.
     */
    private class ConnectionThread implements Runnable {
        /**
         * Whether this connection attempt has been cancelled.
         */
        private boolean cancelled = false;

        /**
         * Cancels this connection attempt as immediately as possible.
         */
        public void cancel() {
            cancelled = true;
        }

        /**
         * Returns whether this connection attempt has been cancelled. Note that
         * this synchronizes on the parent ClientFlapConn to ensure that
         * cancellation is recognized as soon as possible. I think that makes
         * sense.
         *
         * @return whether this connection attempt has been cancelled
         */
        private boolean isCancelled() {
            synchronized(ClientFlapConn.this) {
                return cancelled;
            }
        }

        /**
         * Starts the connection / FLAP read thread.
         */
        public void run() {
            InetAddress ip = ClientFlapConn.this.ip;

            if (ip == null) {
                setState(STATE_RESOLVING, null);
                try {
                    ip = InetAddress.getByName(host);
                } catch (UnknownHostException e) {
                    if (isCancelled()) return;

                    setState(STATE_FAILED, e);
                    return;
                }
                if (isCancelled()) return;
            }

            setState(STATE_CONNECTING, null);

            Socket socket;
            try {
                socket = new Socket(ip, port);
                if (isCancelled()) return;
                attachToSocket(socket);
                if (isCancelled()) return;
            } catch (IOException e) {
                if (isCancelled()) return;
                setState(STATE_FAILED, e);
                return;
            }

            if (isCancelled()) return;
            setSocket(socket);

            if (isCancelled()) return;
            setState(STATE_CONNECTED, null);

            try {
                runFlapLoop();
            } catch (IOException e) {
                if (isCancelled()) return;
                setState(STATE_NOT_CONNECTED, e);
                try { socket.close(); } catch (IOException ignored) { }
                return;
            }
        }
    }
}