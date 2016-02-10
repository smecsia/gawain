package ru.qatools.gawain.activemq

import groovy.transform.CompileStatic
import org.apache.activemq.ActiveMQConnection
import org.apache.activemq.ActiveMQConnectionFactory
import org.junit.BeforeClass
import ru.qatools.gawain.activemq.util.ActivemqEmbeddedService

import static java.lang.Runtime.getRuntime
import static ru.qatools.gawain.activemq.util.SocketUtil.findFreePort

/**
 * @author Ilya Sadykov
 */
@CompileStatic
abstract class AbstractActivemqTest {

    protected static ActivemqEmbeddedService activemq
    public static final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory()

    @BeforeClass
    public static synchronized void setUpClass() throws Exception {
        if (activemq == null) {
            def brokerURL = "tcp://localhost:${findFreePort()}"
            activemq = new ActivemqEmbeddedService(brokerURL, "testActivemq")
            activemq.start();
            getRuntime().addShutdownHook({ activemq.stop() });
            factory.brokerURL = brokerURL
        }
    }

    protected static ActivemqQueueBuilder activemqQueueBuilder() {
        new ActivemqQueueBuilder(
                factory.createConnection() as ActiveMQConnection
        )
    }

    protected static ActivemqBroadcastBuilder activemqBroadcastBuilder() {
        new ActivemqBroadcastBuilder(
                factory.createConnection() as ActiveMQConnection
        )
    }
}
