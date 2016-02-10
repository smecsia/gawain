package ru.qatools.gawain.impl

import groovy.transform.CompileStatic
import ru.qatools.gawain.AggregationStrategy

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class AggregationStrategyImpl<T> extends ProcessingStrategyImpl<T> implements AggregationStrategy<T> {

    @Override
    Map process(Map state, event) {
        callback.call(state, event)
        state
    }
}