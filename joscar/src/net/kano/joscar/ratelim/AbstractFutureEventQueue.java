package net.kano.joscar.ratelim;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractFutureEventQueue implements FutureEventQueue {
    private final Set<QueueRunner<?>> queueRunners = new HashSet<QueueRunner<?>>();

    public synchronized void registerQueueRunner(QueueRunner runner) {
        queueRunners.add(runner);
    }

    public synchronized void unregisterQueueRunner(QueueRunner runner) {
        queueRunners.remove(runner);
    }

    protected synchronized void updateQueueRunners() {
        for (QueueRunner runner : queueRunners) {
            runner.update();
        }
    }
}
