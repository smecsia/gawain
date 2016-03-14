package ru.qatools.gawain.builders

import groovy.transform.CompileStatic
import ru.qatools.gawain.GawainBlockingQueue

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class BasicQueueBuilder implements QueueBuilder<GawainBlockingQueue> {
    @Override
    GawainBlockingQueue build(String name, int maxSize) {
        new GawainBlockingQueue(maxSize)
    }
}
