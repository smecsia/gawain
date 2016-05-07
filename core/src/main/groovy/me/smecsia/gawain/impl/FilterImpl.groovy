package me.smecsia.gawain.impl

import groovy.transform.CompileStatic
import me.smecsia.gawain.Filter

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class FilterImpl<E> implements Filter<E> {
    Closure<String> callback

    @Override
    boolean filter(E event) {
        callback.call(event)
    }
}
