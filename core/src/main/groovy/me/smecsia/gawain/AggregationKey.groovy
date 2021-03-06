package me.smecsia.gawain

import groovy.transform.CompileStatic

/**
 * @author Ilya Sadykov
 */
@CompileStatic
interface AggregationKey<T> {
    String calculate(T event)
}