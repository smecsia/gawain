package me.smecsia.gawain.mongodb

import org.junit.Test
import me.smecsia.gawain.Gawain

import static com.jayway.awaitility.Awaitility.await
import static java.util.concurrent.TimeUnit.SECONDS
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat

/**
 * @author Ilya Sadykov
 */
class MongodbBroadcasterTest extends AbstractMongoTest {

    @Test
    public void testMongoDbBroadcasting() throws Exception {
        def gawain1 = Gawain.run {
            useBroadcastBuilder(mongoBroadcastBuilder())
            processor('input', { it }).broadcast('users')
        }
        def gawain2 = Gawain.run {
            useBroadcastBuilder(mongoBroadcastBuilder())
            aggregator 'users', key { 'all' }, aggregate { state, evt ->
                state.users = (state.users ?: []) + [evt]
            }
        }
        gawain1.to('input', [name: 'Vasya', lastName: 'Fedorov'])
        gawain1.to('input', [name: 'Petya', lastName: 'Makarov'])
        gawain1.to('input', [name: 'Sergey', lastName: 'Vasilyev'])
        await().atMost(5, SECONDS).until({ gawain2.repo('users')['all']?.users?.size() }, equalTo(3))
        assertThat(gawain2.repo('users')['all']
                .users.collect({ it.name }), containsInAnyOrder('Vasya', 'Petya', 'Sergey'))
    }
}
