package me.smecsia.gawain.activemq

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.apache.activemq.ActiveMQConnectionFactory
import me.smecsia.gawain.Broadcaster
import me.smecsia.gawain.Gawain
import me.smecsia.gawain.Opts

import javax.jms.Destination
import javax.jms.Session

import static java.util.concurrent.Executors.newFixedThreadPool

/**
 * @author Ilya Sadykov
 */
@CompileStatic
@InheritConstructors
class ActivemqBroadcaster<T extends Serializable> extends AbstractActivemqConsumer<T> implements Broadcaster<T> {
    final Gawain<T> router

    ActivemqBroadcaster(String destName, ActiveMQConnectionFactory factory, Gawain router, Opts opts = new Opts()) {
        super(destName, factory, opts)
        this.router = router
        LOGGER.debug("[${router.name}][${destName}] Starting ${opts.bcConsumers} broadcaster consumers...")
        def tp = newFixedThreadPool(opts.bcConsumers)
        (opts.bcConsumers as Integer).times { idx ->
            LOGGER.debug("[${router.name}][${destName}#${idx}] Starting broadcaster consumer...")
            ActivemqConsumer<T> consumer = newConsumer()
            tp.submit({
                while (true) {
                    try {
                        final T msg = consumer.consume()
                        LOGGER.debug("[${router.name}][${destName}#${idx}] got event ${msg}")
                        router.to(destName, msg)
                    } catch (Exception e) {
                        LOGGER.error("[${router.name}][${destName}#${idx}] failed to consume message", e)

                    }
                }
            })
        }
    }

    @Override
    def broadcast(T event) {
        produce(event)
    }

    @Override
    protected Destination initDestination(Session session, String name) {
        session.createTopic(name)
    }
}
