package me.smecsia.gawain.builders

import groovy.transform.CompileStatic
import me.smecsia.gawain.GawainBlockingQueue

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
