package me.smecsia.gawain.activemq

import groovy.transform.CompileStatic
import me.smecsia.gawain.Opts
import me.smecsia.gawain.serialize.ToBytesMessageSerializer
import org.apache.activemq.ActiveMQConnection
import org.apache.activemq.ActiveMQConnectionFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.jms.DeliveryMode
import javax.jms.Destination
import javax.jms.MessageProducer
import javax.jms.Session

import static me.smecsia.gawain.activemq.ActivemqUtil.ensureBytesSerializer

/**
 * @author Ilya Sadykov
 */
@CompileStatic
abstract class AbstractActivemqConsumer<T> {
    final ToBytesMessageSerializer<T> serializer
    final Logger LOGGER = LoggerFactory.getLogger(getClass())
    final String name
    final Opts opts
    final ActiveMQConnectionFactory factory

    protected MessageProducer producer

    protected Session session
    protected Destination destination

    AbstractActivemqConsumer(String name, ActiveMQConnectionFactory factory, Opts opts) {
        this.name = name
        this.opts = opts
        this.factory = factory
        this.serializer = ensureBytesSerializer(opts.messageSerializer)
        LOGGER.debug("Creating producer connection to ${factory.brokerURL}...")
        destination = initDestination(session = newSession(factory), name)
        producer = session.createProducer(this.destination)
        producer.setDeliveryMode(DeliveryMode.PERSISTENT)
        LOGGER.debug("Destination ${destination} initialized for ${factory.brokerURL}")
    }

    protected abstract Destination initDestination(Session session, String name);

    protected void produce(T event) {
        def message = session.createBytesMessage()
        message.writeBytes(serializer.serialize(event))
        producer.send(message)
    }

    protected ActivemqConsumer<T> newConsumer() {
        Session session = newSession(factory)
        new ActivemqConsumer<T>(session.createConsumer(initDestination(session, name)), opts)
    }

    protected Session newSession(ActiveMQConnectionFactory factory) {
        LOGGER.debug("Creating consumer connection to ${factory.brokerURL}...")
        ActiveMQConnection cConnection = factory.createConnection() as ActiveMQConnection
        cConnection.start()
        cConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    }


}
