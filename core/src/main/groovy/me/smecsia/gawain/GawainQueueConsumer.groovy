package me.smecsia.gawain

import groovy.transform.CompileStatic

/**
 * @author Ilya Sadykov
 */
@CompileStatic
interface GawainQueueConsumer<T> {

    /**
     * Blocks until new message is appeared in queue
     * @return new message from the queue
     */
    T consume()

}
