package me.smecsia.gawain.elasticsearch

import me.smecsia.gawain.Gawain
import me.smecsia.gawain.jackson.JacksonStateSerializer
import org.junit.Test

import static com.jayway.awaitility.Awaitility.await
import static java.util.concurrent.TimeUnit.SECONDS
import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat
/**
 * @author Ilya Sadykov
 */
class ElasticRepoTest extends AbstractElasticTest {

    @Test
    public void testElasticRepo() throws Exception {
        def gawain = Gawain.run {
            defaultOpts(stateSerializer: new JacksonStateSerializer())
            useRepoBuilder(builder)
            processor('input', { it }).to('users')
            aggregator 'users', key { 'all' }, aggregate { state, evt ->
                state.users = (state.users ?: []) + [evt]
            }
        }
        gawain.to('input', [name: 'Vasya', lastName: 'Fedorov'])
        gawain.to('input', [name: 'Petya', lastName: 'Makarov'])
        gawain.to('input', [name: 'Sergey', lastName: 'Vasilyev'])
        await().atMost(5, SECONDS).until({ gawain.repo('users')['all']?.users?.size() }, equalTo(3))
        assertThat(gawain.repo('users')['all']
                .users.collect({ it.name }), containsInAnyOrder('Vasya', 'Petya', 'Sergey'))
        gawain.repo('users').clear()
        assertThat(gawain.repo('users').keys(), is(empty()))
    }
}
