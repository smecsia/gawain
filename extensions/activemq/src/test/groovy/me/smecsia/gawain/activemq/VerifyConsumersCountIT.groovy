package me.smecsia.gawain.activemq

import org.apache.activemq.ActiveMQConnectionFactory
import org.junit.Ignore
import org.junit.Test
import me.smecsia.gawain.Gawain

import java.util.concurrent.CountDownLatch

/**
 * @author Ilya Sadykov
 */
@Ignore
class VerifyConsumersCountIT {

    @Test
    public void testVerify() throws Exception {
        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory()
        factory.userName = 'admin'
        factory.password = 'admin'
        factory.brokerURL = 'failover:tcp://localhost:61616'
        def latch = new CountDownLatch(5)
        def gawain1 = Gawain.run('first') {
            useQueueBuilder(new ActivemqQueueBuilder(factory))
            useBroadcastBuilder(new ActivemqBroadcastBuilder(factory))
            aggregator('input', key { it },
                    aggregate { state, evt ->
                        state.evt = evt
                        sleep(10000)
                        latch.countDown()
                    }, consumers: 5).broadcast('events')
            processor 'events'
        }
        def gawain2 = Gawain.run('second') {
            useQueueBuilder(new ActivemqQueueBuilder(factory))
            useBroadcastBuilder(new ActivemqBroadcastBuilder(factory))
            aggregator 'events', key { 'all' }, aggregate { state, evt ->
                state.events = (state.events ?: []) + [evt]
            }
        }
        Thread.start {
            5.times {
                gawain1.to('input', it)
                sleep(10000)
            }
        }
        latch.await()
        println(gawain2.repo('events').values())
    }
}
