package net.kano.joscar.ratelim;

public interface FutureEventQueue {
    /**
     * Flushes any "ready" events out of the queue. Returns the amount of time
     * that we must wait before the next event is ready.
     * <br><br>
     * Returns -1 if nothing is in any of the queues and there is no
     * minimum wait time.
     * <br><br>
     * This method will never return 0. If any waiting is needed, it will
     * return a number >= 1.
     */
    long flushQueues();

    boolean hasQueues();

    void registerQueueRunner(QueueRunner runner);

    void unregisterQueueRunner(QueueRunner runner);
}
