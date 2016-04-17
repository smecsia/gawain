package me.smecsia.gawain.activemq

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import me.smecsia.gawain.Broadcaster
import me.smecsia.gawain.Gawain
import me.smecsia.gawain.Opts
import me.smecsia.gawain.builders.BroadcastBuilder

/**
 * @author Ilya Sadykov
 */
@CompileStatic
@InheritConstructors
class ActivemqBroadcastBuilder extends AbstractActivemqBuilder implements BroadcastBuilder {

    @Override
    Broadcaster build(String consumer, Gawain router, Opts opts) {
        new ActivemqBroadcaster(consumer, connectionFactory, router, opts)
    }
}
