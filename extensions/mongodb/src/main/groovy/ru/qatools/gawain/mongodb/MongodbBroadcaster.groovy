package ru.qatools.gawain.mongodb
import com.mongodb.MongoClient
import groovy.transform.CompileStatic
import org.bson.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.qatools.gawain.Broadcaster
import ru.qatools.gawain.Gawain
import ru.qatools.gawain.Opts
import ru.qatools.mongodb.MongoTailableQueue

import static java.util.concurrent.Executors.newFixedThreadPool
import static ru.qatools.mongodb.util.SerializeUtil.objectFromBytes
import static ru.qatools.mongodb.util.SerializeUtil.objectToBytes
/**
 * @author Ilya Sadykov
 */
@CompileStatic
class MongodbBroadcaster implements Broadcaster {
    final Logger LOGGER = LoggerFactory.getLogger(getClass())
    MongoClient mongoClient
    String consumerName
    Gawain router
    MongoTailableQueue queue

    MongodbBroadcaster(MongoClient mongoClient, String dbName,
                       String consumerName, Gawain router,
                       Opts opts = new Opts(maxSize: 100L, consumers: 1)) {
        this.mongoClient = mongoClient
        this.consumerName = consumerName
        this.router = router

        queue = new MongoTailableQueue<>(this.class, mongoClient, dbName,
                colName(consumerName), (opts['maxSize'] ?: 100L) as long)
        queue.setSerializer({ o -> objectToBytes(o) })
        queue.setDeserializer({ Document input, Class clazz -> objectFromBytes(input, clazz) })
        queue.init()

        def tp = newFixedThreadPool(opts.bcConsumers)
        LOGGER.debug("[${router.name}][${consumerName}] Starting ${opts.bcConsumers} consumers...")
        (opts.bcConsumers as Integer).times { idx ->
            LOGGER.debug("[${router.name}][${consumerName}#${idx}] Starting consumer...")
            tp.submit({ queue.poll({
                LOGGER.debug("[${router.name}][${consumerName}#${idx}] got event ${it}")
                router.to(consumerName, it)
            }) })
        }
    }

    private static String colName(String consumerName) {
        "broadcast_to_${consumerName}"
    }

    @Override
    def broadcast(event) {
        queue.add(event)
    }
}
