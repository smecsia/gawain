package ru.qatools.gawain

import groovy.transform.Canonical
import groovy.transform.CompileStatic

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @author Ilya Sadykov
 */
@Canonical
@CompileStatic
class GawainBlockingQueue<T> implements GawainQueue<T> {
    public static final int DEFAULT_MAX_SIZE = 100000
    protected final BlockingQueue queue

    GawainBlockingQueue(int maxSize) {
        queue = new ArrayBlockingQueue((maxSize ?: DEFAULT_MAX_SIZE) as int)
    }

    @Override
    def add(T event) {
        queue.add(event)
    }

    @Override
    Consumer<T> buildConsumer() {
        new Consumer(queue)
    }

    @CompileStatic
    static class Consumer<T> implements GawainQueueConsumer<T> {
        final BlockingQueue queue

        Consumer(BlockingQueue queue) {
            this.queue = queue
        }

        @Override
        T consume() {
            queue.take()
        }
    }
}
