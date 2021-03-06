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
 *  File created by keith @ Mar 6, 2003
 *
 */

package net.kano.joscar.flap;

/**
 * Provides an interface for listening for -- and optionally halting further
 * processing of -- FLAP packets received on a <code>FlapProcessor</code>.
 */
public interface VetoableFlapPacketListener {
    /**
     * An enumeration of possible actions that a vetoing packet listener can
     * trigger.
     */
    enum VetoResult {
        /**
         * Tells the <code>FlapProcessor</code> to stop processing other vetoable
         * and non-vetoable listeners, but to continue its own internal processing.
         * <br>
         * <br>
         * Note that as of this writing there is no internal processing; until
         * such processing exists this value is functionally equivalent to
         * <code>STOP_PROCESSING_ALL</code>. It is suggested, however, to use this
         * value instead of <code>STOP_PROCESSING_ALL</code> to allow for further
         * expansion of <code>FlapProcessor</code>'s processing code.
         */
        STOP_PROCESSING_LISTENERS,

        /**
         * Tells the <code>FlapProcessor</code> to continue processing as usual;
         * that is, this value signifies that this packet has not been vetoed.
         */
        CONTINUE_PROCESSING,

        /**
         * Tells the <code>FlapProcessor</code> to halt all further processing of
         * this command whatsoever. This value may or may not be actually honored
         * in the future: currently this value is functionally equivalent to
         * <code>STOP_PROCESSING_LISTENERS</code> as no fruther processing is done
         * on FLAP packets by the <code>FlapProcessor</code>.
         */
        STOP_PROCESSING_ALL,
    }
    /**
     * Processes a single packet received by a <code>FlapProcessor</code>.
     *
     * @param event information on the packet's receipt
     * @return an object representing what action should be taken by the
     *         <code>FlapProcessor</code>
     */
    VetoResult handlePacket(FlapPacketEvent event);
}
