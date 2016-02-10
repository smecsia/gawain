package ru.qatools.gawain

/**
 * @author Ilya Sadykov
 */
class BasicBroadcaster implements Broadcaster {
    private String target
    private Gawain gawain

    @Override
    def broadcast(event) {
        gawain.to(target, event)
    }
}
