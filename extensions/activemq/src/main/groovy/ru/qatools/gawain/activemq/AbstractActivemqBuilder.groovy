package ru.qatools.gawain.activemq
import groovy.transform.CompileStatic
import org.apache.activemq.ActiveMQConnectionFactory
/**
 * @author Ilya Sadykov
 */
@CompileStatic
abstract class AbstractActivemqBuilder {
    final ActiveMQConnectionFactory connectionFactory

    AbstractActivemqBuilder(ActiveMQConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory
    }
}
