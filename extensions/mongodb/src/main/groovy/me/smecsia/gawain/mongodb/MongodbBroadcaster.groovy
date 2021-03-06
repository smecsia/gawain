package me.smecsia.gawain.mongodb
import com.mongodb.MongoClient
import groovy.transform.CompileStatic
import org.bson.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import me.smecsia.gawain.Broadcaster
import me.smecsia.gawain.Gawain
import me.smecsia.gawain.Opts
import ru.qatools.mongodb.MongoTailableQueue

import static java.util.concurrent.Executors.newFixedThreadPool
import static ru.qatools.mongodb.util.SerializeUtil.objectFromBytes
import static ru.qatools.mongodb.util.SerializeUtil.objectToBytes
/**
 * @author Ilya Sadykov
 */
@CompileStatic
class MongodbBroadcaster<T> implements Broadcaster<T> {
    final Logger LOGGER = LoggerFactory.getLogger(getClass())
    MongoClient mongoClient
    String consumerName
    Gawain router
    MongoTailableQueue<T> queue

    MongodbBroadcaster(MongoClient mongoClient, String dbName,
                       String consumerName, Gawain router,
                       Opts opts = new Opts(maxSize: 100L, consumers: 1)) {
        this.mongoClient = mongoClient
        this.consumerName = consumerName
        this.router = router

        queue = new MongoTailableQueue<>(T, mongoClient, dbName,
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
