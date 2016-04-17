package me.smecsia.gawain.impl

import groovy.transform.CompileStatic
import me.smecsia.gawain.AggregationStrategy

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class AggregationStrategyImpl<T> extends ProcessingStrategyImpl<T> implements AggregationStrategy<T> {

    @Override
    void process(Map state, event) {
        callback.call(state, event)
    }
}