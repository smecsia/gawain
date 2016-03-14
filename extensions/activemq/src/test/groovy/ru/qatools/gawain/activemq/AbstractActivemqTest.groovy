package ru.qatools.gawain.activemq

import groovy.transform.CompileStatic
import org.apache.activemq.ActiveMQConnectionFactory
import org.junit.Before
import ru.qatools.gawain.activemq.util.ActivemqEmbeddedService

import static java.lang.Runtime.getRuntime
import static ru.qatools.gawain.util.SocketUtil.findFreePort

/**
 * @author Ilya Sadykov
 */
@CompileStatic
abstract class AbstractActivemqTest {

    protected ActivemqEmbeddedService activemq
    public final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory()

    @Before
    public synchronized void setUp() throws Exception {
        if (activemq == null) {
            def brokerURL = "tcp://localhost:${findFreePort()}"
            activemq = new ActivemqEmbeddedService(brokerURL, "testActivemq")
            activemq.start();
            getRuntime().addShutdownHook({ activemq.stop() });
            factory.brokerURL = brokerURL
        }
    }

    protected ActivemqQueueBuilder activemqQueueBuilder() {
        new ActivemqQueueBuilder(factory)
    }

    protected ActivemqBroadcastBuilder activemqBroadcastBuilder() {
        new ActivemqBroadcastBuilder(factory)
    }
}
