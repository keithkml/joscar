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
 *  File created by keith @ Apr 14, 2003
 *
 */

package net.kano.joscar.snac;

import net.kano.joscar.DefensiveTools;

/**
 * An interface for managing a "SNAC queue," which controls when individual
 * SNAC commands are sent on a <code>SnacProcessor</code>. This is useful for,
 * for example, implementing an automatic rate limiting mechanism.
 */
public abstract class SnacQueueManager {
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
    public abstract void queueSnac(SnacProcessor processor,
            SnacRequest request);

    /**
     * Clears all pending (queued) SNAC commands in this SNAC manager for the
     * given SNAC processor. This is normally only called when the given SNAC
     * processor has been disconnected.
     *
     * @param processor the SNAC processor for which pending SNACs should be
     *        erased
     */
    public abstract void  clearQueue(SnacProcessor processor);

    /**
     * Sends the given SNAC request over the given SNAC connection. The given
     * SNAC must not have already been sent.
     *
     * @param processor the SNAC connection on which to send the given SNAC
     * @param request the SNAC request to send
     */
    protected final void sendSnac(SnacProcessor processor,
            SnacRequest request) {
        DefensiveTools.checkNull(request, "request");
        
        processor.reallySendSnac(request);
    }
}
