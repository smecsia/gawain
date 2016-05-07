package me.smecsia.gawain.mongodb

import me.smecsia.gawain.Gawain
import me.smecsia.gawain.jackson.JacksonStateSerializer
import org.junit.Test

import static com.jayway.awaitility.Awaitility.await
import static java.util.concurrent.TimeUnit.SECONDS
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.nullValue
import static org.junit.Assert.assertThat

/**
 * @author Ilya Sadykov
 */
class MongodbRepoTest extends AbstractMongoTest {

    @Test
    public void testMongoDbRepoWithJackson() throws Exception {
        def gawain = Gawain.run {
            defaultOpts(stateSerializer: new JacksonStateSerializer())
            useRepoBuilder(mongoRepoBuilder())
            mainRoute(it)
        }
        performTest(gawain)
        gawain.repo('users').put('key', null)
        assertThat(gawain.repo('users').get('key'), is(nullValue()))
    }

    @Test
    public void testMongoDbRepoWithBytes() throws Exception {
        def gawain = Gawain.run {
            useRepoBuilder(mongoRepoBuilder())
            mainRoute(it)
        }
        performTest(gawain)
    }

    static def mainRoute(Gawain g) {
        g.processor('input', { it }).to('users')
        g.aggregator 'users', g.key { 'all' }, g.aggregate { state, evt ->
            state.users = (state.users ?: []) + [evt]
        }
    }

    static def performTest(Gawain gawain) {
        gawain.to('input', [name: 'Vasya', lastName: 'Fedorov'])
        gawain.to('input', [name: 'Petya', lastName: 'Makarov'])
        gawain.to('input', [name: 'Sergey', lastName: 'Vasilyev'])
        await().atMost(400, SECONDS).until({ gawain.repo('users')['all']?.users?.size() }, equalTo(3))
        assertThat(gawain.repo('users')['all']
                .users.collect({ it.name }), containsInAnyOrder('Vasya', 'Petya', 'Sergey'))
        gawain.repo('users').clear()
        assertThat(gawain.repo('users').keys(), is(empty()))
    }
}
