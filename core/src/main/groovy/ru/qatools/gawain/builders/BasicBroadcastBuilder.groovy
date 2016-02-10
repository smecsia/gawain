package ru.qatools.gawain.builders
import groovy.transform.CompileStatic
import ru.qatools.gawain.BasicBroadcaster
import ru.qatools.gawain.Broadcaster
import ru.qatools.gawain.Gawain
import ru.qatools.gawain.Opts

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class BasicBroadcastBuilder implements BroadcastBuilder {

    @Override
    Broadcaster build(String target, Gawain gawain, Opts opts) {
        new BasicBroadcaster(target: target, gawain: gawain)
    }

}