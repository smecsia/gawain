package me.smecsia.gawain

import groovy.transform.CompileStatic

/**
 * @author Ilya Sadykov
 */
@CompileStatic
interface Filter<E> {
    boolean filter(E event)
}