package ru.qatools.gawain

import groovy.transform.CompileStatic

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class BasicBroadcaster implements Broadcaster {
    private String target
    private Gawain gawain

    @Override
    def broadcast(event) {
        gawain.to(target, event)
    }
}
