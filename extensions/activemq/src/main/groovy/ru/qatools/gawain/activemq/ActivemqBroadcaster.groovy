package ru.qatools.gawain.activemq

import groovy.transform.CompileStatic
import ru.qatools.gawain.Broadcaster
import ru.qatools.gawain.Gawain
import ru.qatools.gawain.Opts

import javax.jms.Session

import static java.util.concurrent.Executors.newFixedThreadPool

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class ActivemqBroadcaster<T extends Serializable> extends AbstractActivemqRouter<T> implements Broadcaster<T> {

    @SuppressWarnings("GroovyInfiniteLoopStatement")
    ActivemqBroadcaster(String destName, Session session, Gawain router, Opts opts = new Opts()) {
        super(destName, session, session.createTopic(destName), opts)
        LOGGER.debug("[${router.name}][${destName}] Starting ${opts.bcConsumers} broadcaster consumers...")
        def tp = newFixedThreadPool(opts.bcConsumers)
        (opts.bcConsumers as Integer).times { idx ->
            LOGGER.debug("[${router.name}][${destName}#${idx}] Starting broadcaster consumer...")
            tp.submit({
                while (true) {
                    try {
                        def msg = consume()
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
}
