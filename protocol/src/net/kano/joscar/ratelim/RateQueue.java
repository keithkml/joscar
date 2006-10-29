/*
 *  Copyright (c) 2003, The Joust Project
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
 *  File created by keith @ May 25, 2003
 *
 */

package net.kano.joscar.ratelim;

import net.kano.joscar.DefensiveTools;
import net.kano.joscar.logging.Logger;
import net.kano.joscar.logging.LoggingSystem;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snac.SnacRequestAdapter;
import net.kano.joscar.snac.SnacRequestSentEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * Manages a single queue for a single rate class of a SNAC connection. This
 * class basically wraps a plain old queue, and its existence is thus rather
 * questionable. However, functionality might be added later, so this class
 * stays.
 */
public class RateQueue {
    /** A logger to log rate-related events. */
    private static final Logger logger
            = LoggingSystem.getLogger("net.kano.joscar.ratelim");

    private final ConnectionQueueMgr connectionManager;

    /** The actual request queue. */
    private final LinkedList<SnacRequest> queue = new LinkedList<SnacRequest>();

    /** The rate class monitor for this rate queue. */
    private final RateClassMonitor rateMonitor;

    /**
     * Requests which have been dequeued but not yet sent.
     */
    private final Set<SnacRequest> pendingRequests = new HashSet<SnacRequest>();

    private final SnacRequestSender requestSender;

    RateQueue(ConnectionQueueMgr connectionManager, RateClassMonitor monitor,
              SnacRequestSender requestSender) {
        DefensiveTools.checkNull(connectionManager, "connectionManager");
        DefensiveTools.checkNull(monitor, "monitor");
        DefensiveTools.checkNull(requestSender, "requestSender");

        this.connectionManager = connectionManager;
        this.rateMonitor = monitor;
        this.requestSender = requestSender;
    }

    /**
     * Returns the rate class monitor associated with this rate queue.
     *
     * @return this rate queue's associated rate class monitor
     */
    public RateClassMonitor getRateClassMonitor() { return rateMonitor; }

    /**
     * Returns the number of requests currently waiting in this queue.
     *
     * @return the number of requests currently waiting in this queue
     */
    public synchronized int getQueueSize() { return queue.size(); }

    /**
     * Returns whether any requests are waiting in this queue.
     *
     * @return whether any requests are currently in this queue
     */
    public synchronized boolean hasRequests() { return !queue.isEmpty(); }

    /**
     * Adds a request to this queue.
     *
     * @param req the request to enqueue
     */
    synchronized void enqueue(SnacRequest req) {
        DefensiveTools.checkNull(req, "req");

        if (logger.logFineEnabled()) {
            logger.logFine("Enqueuing " + req.getCommand() + " within ratequeue " +
                    "(class " + rateMonitor.getRateInfo().getRateClass()
                    + ")...");
        }

        queue.add(req);
    }

    /**
     * Removes the oldest request from this queue.
     *
     * @return the request that was removed
     */
    synchronized SnacRequest dequeueNextRequest() throws NoSuchElementException {
        SnacRequest request = queue.removeFirst();

        if (logger.logFineEnabled()) {
            logger.logFine("Dequeueing " + request.getCommand()
                    + " from ratequeue (class "
                    + rateMonitor.getRateInfo().getRateClass() + ")...");
        }

        return request;
    }

    /**
     * Dequeues all requests in this queue, adding them in order from oldest
     * to newest to the given collection.
     *
     * @param dest the collection to which the dequeued requests should be added
     */
    synchronized void dequeueAll(Collection<? super SnacRequest> dest) {
        dest.addAll(queue);
        queue.clear();
    }

    /**
     * Removes all requests from this queue.
     */
    synchronized void clear() {
        queue.clear();
    }

    public boolean isOpen() {
        return hasRequests() && !connectionManager.isPaused();
    }

    /**
     * Returns whether the queue is "ready" to send the next request. (A
     * rate queue is "ready" if its {@linkplain #getOptimalWaitTime() wait
     * time} is zero.
     */
    public boolean isReady() {
        return rateMonitor.getPossibleCmdCount() > 0;
    }

    /**
     * Returns the {@linkplain RateClassMonitor#getOptimalWaitTime()
     * "optimal wait time"} for this queue.
     */
    public long getOptimalWaitTime() {
        return rateMonitor.getOptimalWaitTime();
    }

    /**
     * Dequeues all "ready" requests. Returns whether the queue has been
     * cleared; returns false if there are still commands left in the queue.
     */
    public synchronized boolean sendAndDequeueReadyRequestsIfPossible() {
        if (!isOpen()) {
            return true;
        }
        if (isReady()) {
            sendAndDequeueReadyRequests();
        }

        return !hasRequests();
    }

    private void sendAndDequeueReadyRequests() {
        List<SnacRequest> toSend = dequeueReadyRequests();

        if (toSend.isEmpty()) {
            return;
        }
        requestSender.sendRequests(toSend);
    }

  /**
   * Dequeues as many requests as possible without being rate limited. A
   * {@linkplain #pendingRequests list of unsent dequeued requests} is kept to 
   * prevent the case where {@code sendAndDequeueReadyRequests} is called once,
   * then called again by another thread before the dequeued requests from the
   * first call are actually sent. This would result in sending twice as many
   * requests as desired, and would probably end up rate-limiting the user (see
   * example below).
   * <br><br>
   * When the request is recorded as "sent" by the SNAC processor, it is
   * {@linkplain #removePending removed} from the unsent dequeued requests list.
   * <br><br>
   * An example: assume there are 5 SNAC commands queued.
   * <ol>
   * <li>Thread 1 calls {@code sendAndDequeueReadyRequests}
   * <ol>
   * <li>The rate monitor says 2 SNAC commands can be sent before being rate
   *     limited</li>
   * <li>Two commands are removed from the queue and passed to the SNAC
   *     processor to be sent</li>
   * </ol>
   * </li>
   * <li>Thread 2 calls {@code sendAndDequeueReadyRequests}
   * <ol>
   * <li>The rate monitor has not yet seen the 2 commands that Thread 1 passed
   *     to the SNAC processor, because the SNAC processor has not sent them
   *     yet. So, it still says that 2 SNAC commands can be sent before being
   *     rate limited.</li>
   * <li>Two more commands are removed from the queue and passed to the SNAC
   *     processor to be sent</li>
   * </ol>
   * </li>
   * <li>The SNAC processor sends the first of the 2 commands passed to it from
   *     Thread 1</li>
   * <li>The SNAC processor sends the second of the 2 commands passed to it from
   *     Thread 1</li>
   * <li>The SNAC processor sends the first of the 2 commands passed to it from
   *     Thread 2</li>
   * <li>The user is now rate-limited!</li>
   * </ol>
   */
    private synchronized List<SnacRequest> dequeueReadyRequests() {
        List<SnacRequest> toSend = new ArrayList<SnacRequest>(queue.size());
        int max = rateMonitor.getPossibleCmdCount() - pendingRequests.size();
        for (int i = 0; i < max && hasRequests(); i++) {
            SnacRequest request = dequeueNextRequest();

            request.addListener(new SnacRequestAdapter() {
                public void handleSent(SnacRequestSentEvent e) {
                    removePending(e.getRequest());
                }
            });
            toSend.add(request);
        }
        pendingRequests.addAll(toSend);
        return toSend;
    }

    // package-private for testing
    synchronized void removePending(SnacRequest request) {
        pendingRequests.remove(request);
    }

    public String toString() {
        return "RateQueue: rateMonitor=" + rateMonitor
            + ", queued: " + queue.size();
    }
}
