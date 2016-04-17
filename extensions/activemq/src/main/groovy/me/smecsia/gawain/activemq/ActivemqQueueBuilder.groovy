package me.smecsia.gawain.activemq
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import me.smecsia.gawain.GawainQueue
import me.smecsia.gawain.Opts
import me.smecsia.gawain.builders.QueueBuilder
/**
 * @author Ilya Sadykov
 */
@CompileStatic
@InheritConstructors
class ActivemqQueueBuilder extends AbstractActivemqBuilder implements QueueBuilder {

    @Override
    GawainQueue build(String name, int maxSize) {
        new ActivemqQueue(name, connectionFactory, new Opts(maxQueueSize: maxSize))
    }
}
