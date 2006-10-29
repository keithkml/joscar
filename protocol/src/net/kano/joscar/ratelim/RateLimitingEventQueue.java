/*
 *  Copyright (c) 2006, The Joust Project
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
 */

package net.kano.joscar.ratelim;

import net.kano.joscar.CopyOnWriteArraySet;
import net.kano.joscar.DefensiveTools;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class RateLimitingEventQueue extends AbstractFutureEventQueue {
    private final Set<RateQueue> queues = new CopyOnWriteArraySet<RateQueue>();

    public long flushQueues() {
        if (queues.isEmpty()) return -1;

        long minimumWaitTime = -1;
        for (RateQueue queue : queues) {
            boolean queueEmpty = queue.sendAndDequeueReadyRequestsIfPossible();
            if (queueEmpty) continue;

            long waitTime = queue.getOptimalWaitTime();

            if (minimumWaitTime == -1 || minimumWaitTime > waitTime) {
                minimumWaitTime = waitTime;
            }
        }

        // if minimumWaitTime is zero, we'd like to return 1, because that's
        // what our javadoc says we do
        if (minimumWaitTime == 0) {
            minimumWaitTime = 1;
        }
        return minimumWaitTime;
    }

    public boolean hasQueues() {
        return !queues.isEmpty();
    }

    public void addQueue(RateQueue queue) {
        DefensiveTools.checkNull(queue, "queue");

        queues.add(queue);

        updateQueueRunners();
    }

    public void addQueues(Collection<RateQueue> rateQueues) {
        // we need to copy these, because the elements may be set to null
        // between a null check and the addAll
        List<RateQueue> safeRateQueues =
                DefensiveTools.getSafeNonnullListCopy(rateQueues, "rateQueues");
        queues.addAll(safeRateQueues);
        updateQueueRunners();
    }

    public void removeQueue(RateQueue queue) {
        DefensiveTools.checkNull(queue, "queue");

        queues.remove(queue);
        updateQueueRunners();
    }

    public void removeQueues(Collection<RateQueue> rateQueues) {
        DefensiveTools.checkNull(rateQueues, "rateQueues");

        queues.removeAll(rateQueues);
        updateQueueRunners();
    }

	public String toString() {
        return "RateLimitingEventQueue: "
		+ "queues=" + queues;
    }
}
