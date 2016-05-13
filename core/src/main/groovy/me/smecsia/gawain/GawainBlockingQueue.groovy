package me.smecsia.gawain

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
/**
 * @author Ilya Sadykov
 */
@Canonical
@CompileStatic
@Slf4j('LOGGER')
class GawainBlockingQueue<T> implements GawainQueue<T> {
    public static final int DEFAULT_MAX_SIZE = 100000
    protected final BlockingQueue queue

    GawainBlockingQueue(int maxSize) {
        def setMaxSize = (maxSize ?: DEFAULT_MAX_SIZE) as int
        LOGGER.info("Initializing the array blocking queue with max size ${setMaxSize}")
        queue = new ArrayBlockingQueue(setMaxSize)
    }

    @Override
    def add(T event) {
        queue.put(event)
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
