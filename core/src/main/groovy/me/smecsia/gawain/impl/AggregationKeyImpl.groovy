package me.smecsia.gawain.impl

import groovy.transform.CompileStatic
import me.smecsia.gawain.AggregationKey

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class AggregationKeyImpl<T> implements AggregationKey<T> {
    Closure<String> callback

    @Override
    String calculate(T event) {
        callback.call(event)
    }
}