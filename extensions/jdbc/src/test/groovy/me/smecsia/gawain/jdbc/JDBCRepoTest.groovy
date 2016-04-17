package me.smecsia.gawain.jdbc

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import me.smecsia.gawain.Gawain

import static com.jayway.awaitility.Awaitility.await
import static java.util.concurrent.TimeUnit.SECONDS
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat

/**
 * @author Ilya Sadykov
 */
@RunWith(Parameterized)
class JDBCRepoTest extends BasicDBRepoTest {

    @Test
    public void testJDBCRepo() throws Exception {
        def gawain = Gawain.run {
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
    }
}
