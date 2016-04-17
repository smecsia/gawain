package me.smecsia.gawain.builders
import groovy.transform.CompileStatic
import me.smecsia.gawain.BasicBroadcaster
import me.smecsia.gawain.Broadcaster
import me.smecsia.gawain.Gawain
import me.smecsia.gawain.Opts

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