package ru.qatools.gawain.impl

import groovy.transform.CompileStatic
import ru.qatools.gawain.Filter

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class FilterImpl<E> implements Filter<E> {
    Closure<String> callback

    @Override
    boolean filter(E event) {
        event.with(callback)
    }
}
