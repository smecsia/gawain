package me.smecsia.gawain

import groovy.transform.CompileStatic

/**
 * @author Ilya Sadykov
 */
@CompileStatic
interface GawainQueue<T> {

    def add(T event)

    GawainQueueConsumer<T> buildConsumer()
}