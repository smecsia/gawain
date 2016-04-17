package me.smecsia.gawain.activemq
import org.junit.Test
import me.smecsia.gawain.Gawain

import static com.jayway.awaitility.Awaitility.await
import static java.util.concurrent.TimeUnit.SECONDS
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat
import static me.smecsia.gawain.Gawain.*
import static me.smecsia.gawain.Opts.opts
/**
 * @author Ilya Sadykov
 */
class ActivemqQueueBuilderTest extends AbstractActivemqTest {

    @Test
    public void testActivemqQueue() throws Exception {
        def gawain = Gawain.run {
            useQueueBuilder(activemqQueueBuilder())
            processor('input', { it }, consumers: 10).to('users')
            aggregator('users', key { 'all' }, aggregate { state, evt ->
                state.users = (state.users ?: []) + [evt]
            }, consumers: 10)
        }
        gawain.to('input', [name: 'Vasya', lastName: 'Fedorov'])
        gawain.to('input', [name: 'Petya', lastName: 'Makarov'])
        gawain.to('input', [name: 'Sergey', lastName: 'Vasilyev'])
        await().atMost(5, SECONDS).until({ gawain.repo('users')['all']?.users?.size() }, equalTo(3))
        assertThat(gawain.repo('users')['all']
                .users.collect({ it.name }), containsInAnyOrder('Vasya', 'Petya', 'Sergey'))
    }

    @Test
    public void testSingleRunWithResult() throws Exception {
        Collection<Integer> events = (0..1000).collect { new Random().nextInt(100) }
        def results = doAggregation(
                events, key { "${it}" },
                aggregate { s, e ->
                    s.count = (s.count ?: 0) + 1
                },
                opts(consumers: 100, processors: 1, maxQueueSize: 1000000, benchmark: true),
                { it.useQueueBuilder(activemqQueueBuilder()) }
        )
        println(results)
    }
}
