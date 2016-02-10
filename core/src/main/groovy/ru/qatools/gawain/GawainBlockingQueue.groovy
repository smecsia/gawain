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
class GawainBlockingQueue implements GawainQueue {
    public static final int DEFAULT_MAX_SIZE = 100000
    final BlockingQueue queue

    GawainBlockingQueue(int maxSize) {
        queue = new ArrayBlockingQueue((maxSize ?: DEFAULT_MAX_SIZE) as int)
    }

    @Override
    def add(event) {
        queue.add(event)
    }

    @Override
    Object take() {
        queue.take()
    }
}
