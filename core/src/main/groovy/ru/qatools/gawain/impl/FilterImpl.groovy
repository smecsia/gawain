package ru.qatools.gawain.impl

import ru.qatools.gawain.Filter

/**
 * @author Ilya Sadykov
 */
class FilterImpl implements Filter {
    Closure<String> callback

    @Override
    boolean filter(event) {
        event.with(callback)
    }
}
