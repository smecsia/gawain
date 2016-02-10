package ru.qatools.gawain.impl

import groovy.transform.CompileStatic
import ru.qatools.gawain.ProcessingStrategy

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class ProcessingStrategyImpl<T> implements ProcessingStrategy<T> {
    Closure callback

    @Override
    T process(T event) {
        event.with(callback)
        event
    }
}