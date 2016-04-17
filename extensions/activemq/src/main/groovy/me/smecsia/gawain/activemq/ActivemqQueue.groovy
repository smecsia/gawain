package me.smecsia.gawain.activemq
import groovy.transform.CompileStatic
import org.apache.activemq.ActiveMQConnectionFactory
import me.smecsia.gawain.GawainQueue
import me.smecsia.gawain.Opts

import javax.jms.Destination
import javax.jms.Session
/**
 * @author Ilya Sadykov
 */
@CompileStatic
class ActivemqQueue<T extends Serializable> extends AbstractActivemqConsumer<T> implements GawainQueue<T> {

    ActivemqQueue(String destName, ActiveMQConnectionFactory factory, Opts opts = new Opts()) {
        super(destName, factory, opts)
    }

    @Override
    def add(T event) {
        produce(event)
    }

    @Override
    ActivemqConsumer<T> buildConsumer() {
        super.newConsumer()
    }

    @Override
    protected Destination initDestination(Session session, String name) {
        session.createQueue(name)
    }
}