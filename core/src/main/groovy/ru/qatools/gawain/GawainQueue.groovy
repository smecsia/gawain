package ru.qatools.gawain

import groovy.transform.CompileStatic

/**
 * @author Ilya Sadykov
 */
@CompileStatic
interface GawainQueue<T> {

    def add(T event);

    T take();
}