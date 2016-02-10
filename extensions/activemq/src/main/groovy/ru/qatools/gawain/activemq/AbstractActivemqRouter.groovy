package ru.qatools.gawain.activemq
import groovy.transform.CompileStatic
import org.nustaq.serialization.FSTConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.qatools.gawain.Opts

import javax.jms.*
/**
 * @author Ilya Sadykov
 */
@CompileStatic
class AbstractActivemqRouter<T> {
    static final FSTConfiguration serializer = FSTConfiguration.createDefaultConfiguration()
    final Logger LOGGER = LoggerFactory.getLogger(getClass())
    final String name
    final MessageProducer producer
    final MessageConsumer consumer
    final Session session
    final Destination destination
    final Opts opts

    AbstractActivemqRouter(String name, Session session, Destination destination, Opts opts) {
        this.name = name
        this.session = session
        this.destination = destination
        this.opts = opts
        producer = session.createProducer(this.destination)
        consumer = session.createConsumer(this.destination)
        producer.setDeliveryMode(DeliveryMode.PERSISTENT)
    }

    protected void produce(T event){
        def message = session.createBytesMessage()
        message.writeBytes(serializer.asByteArray(event))
        producer.send(message)
    }

    protected T consume() {
        def message = ((opts.maxQueueWaitSec > 0) ?
                consumer.receive(opts.maxQueueWaitSec) : consumer.receive()) as BytesMessage
        def bytes = new byte[message.getBodyLength()]
        message.readBytes(bytes)
        serializer.asObject(bytes) as T
    }

}
