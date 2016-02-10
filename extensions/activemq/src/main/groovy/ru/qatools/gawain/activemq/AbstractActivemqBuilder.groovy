package ru.qatools.gawain.activemq

import org.apache.activemq.ActiveMQConnection

import javax.jms.Session

/**
 * @author Ilya Sadykov
 */
abstract class AbstractActivemqBuilder {
    final ActiveMQConnection connection
    final Session session

    AbstractActivemqBuilder(ActiveMQConnection connection) {
        this.connection = connection
        if (!connection.isStarted()) {
            connection.start()
        }
        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    }
}
