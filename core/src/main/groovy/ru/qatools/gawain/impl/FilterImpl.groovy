package ru.qatools.gawain.impl

import ru.qatools.gawain.Filter

/**
 * @author Ilya Sadykov
 */
class FilterImpl<E> implements Filter<E> {
    Closure<String> callback

    @Override
    boolean filter(E event) {
        event.with(callback)
    }
}
