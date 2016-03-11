package ru.qatools.gawain.builders

import groovy.transform.CompileStatic
import ru.qatools.gawain.GawainBlockingQueue
import ru.qatools.gawain.GawainQueue

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class BasicQueueBuilder implements QueueBuilder {
    @Override
    GawainQueue build(String name, int maxSize) {
        new GawainBlockingQueue(maxSize)
    }
}
