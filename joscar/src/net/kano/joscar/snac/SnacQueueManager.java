/*
 *  Copyright (c) 2002-2003, The Joust Project
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
 *  File created by keith @ Apr 24, 2003
 *
 */

package net.kano.joscar.snac;

/**
 * An interface for managing a "SNAC queue," which controls when individual
 * SNAC commands are sent on a <code>SnacProcessor</code>. Such a system is
 * useful for, for example, implementing an automatic rate limiting mechanism.
 *
 * @see DefaultSnacQueueManager
 */
public interface SnacQueueManager {
    /**
     * A method called to indicate that the queue should be paused until a call
     * to {@link #unpause}. This method will never be called twice without a
     * call to <code>unpause</code> in between; that is, it will never be called
     * twice in a row. Note that calls to {@link #queueSnac} can and probably
     * will be made before a call to {@link #unpause}; that is, the queue must
     * still accept the queueing of SNAC commands while it is paused.
     * <br>
     * <br>
     * Note that this will be the first method called for a given SNAC processor
     * if this queue manager is added to the processor while the processor is
     * paused.
     *
     * @param processor the SNAC processor on which to pause
     */
    void pause(SnacProcessor processor);

    /**
     * A method called to indicate that the queue can once again send SNACs.
     * This method will only be called after a call to {@link #pause}, and will
     * never be called twice without a call to {@link #pause} in between; that
     * is, essentially, it will never be called twice in a row.
     *
     * @param processor the SNAC processor on which to pause
     */
    void unpause(SnacProcessor processor);

    /**
     * Enqueues the given SNAC request that for the given SNAC processor. This
     * method can just as easily send the given request immediately (via
     * <code>sendSnac</code>) as it can enqueue it to be sent later. Note that
     * the given request will <i>not</i> be sent at all until it is sent from
     * this SNAC queue manager using <code>sendSnac</code>. It is not
     * recommended that the given request be modified before it is sent with
     * <code>sendSnac</code>.
     *
     * @param processor the SNAC processor on which the given request was
     *        created and on which it is to be sent
     * @param request the request being enqueued
     */
    void queueSnac(SnacProcessor processor, SnacRequest request);

    /**
     * Clears all pending (queued) SNAC commands in this SNAC manager for the
     * given SNAC processor. This is normally only called when the given SNAC
     * processor has been disconnected. Note that a call to this method
     * implies a call to {@link #unpause} for the given SNAC processor as well,
     * although no queued SNACs should be sent (as they would with an actual
     * call to <code>unpause</code>}.
     *
     * @param processor the SNAC processor for which pending SNACs should be
     *        erased
     */
    void clearQueue(SnacProcessor processor);
}
