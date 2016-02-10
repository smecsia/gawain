package ru.qatools.gawain

import groovy.transform.CompileStatic

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class Filter {
    Closure<String> callback

    boolean filter(event) {
        event.with(callback)
    }
}