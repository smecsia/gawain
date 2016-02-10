package ru.qatools.gawain.builders

import ru.qatools.gawain.GawainBlockingQueue
import ru.qatools.gawain.GawainQueue

/**
 * @author Ilya Sadykov
 */
class BasicQueueBuilder implements QueueBuilder {
    @Override
    GawainQueue build(String name, int maxSize) {
        new GawainBlockingQueue(maxSize)
    }
}
