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

}
