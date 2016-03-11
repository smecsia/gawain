package ru.qatools.gawain

import groovy.transform.CompileStatic

/**
 * @author Ilya Sadykov
 */
@CompileStatic
interface AggregationStrategy<T> {

    void process(Map state, T event)
}