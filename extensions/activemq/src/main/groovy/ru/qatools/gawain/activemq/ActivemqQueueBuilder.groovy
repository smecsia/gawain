package ru.qatools.gawain.activemq
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import ru.qatools.gawain.GawainQueue
import ru.qatools.gawain.Opts
import ru.qatools.gawain.builders.QueueBuilder
/**
 * @author Ilya Sadykov
 */
@CompileStatic
@InheritConstructors
class ActivemqQueueBuilder extends AbstractActivemqBuilder implements QueueBuilder {

    @Override
    GawainQueue build(String name, int maxSize) {
        new ActivemqQueue(name, session, new Opts(maxQueueSize: maxSize))
    }
}
