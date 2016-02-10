package ru.qatools.gawain.impl

import groovy.transform.CompileStatic
import ru.qatools.gawain.AggregationKey

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class AggregationKeyImpl<T> implements AggregationKey<T> {
    Closure<String> callback

    @Override
    String calculate(T event) {
        event.with(callback)
    }
}