package ru.qatools.gawain.activemq

import org.junit.Test
import ru.qatools.gawain.Gawain

import static com.jayway.awaitility.Awaitility.await
import static java.util.concurrent.TimeUnit.SECONDS
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat

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
}
