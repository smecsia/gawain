package me.smecsia.gawain.elasticsearch

import me.smecsia.gawain.Opts
import me.smecsia.gawain.jackson.JacksonStateSerializer
import org.junit.Test

import static org.hamcrest.Matchers.hasEntry
import static org.junit.Assert.assertThat
/**
 * @author Ilya Sadykov
 */
class ElasticRepoBuilderTest extends AbstractElasticTest {

    @Test
    public void testLockSaveAndGet() throws Exception {
        def repo = builder.build('repo1', new Opts(maxLockWaitMs: 1000, stateSerializer: new JacksonStateSerializer()))
        repo.put('user', [name: 'Vasya'])
        assertThat(repo.get('user'), hasEntry('name', 'Vasya'))

        def user = repo.lockAndGet('user')
        Thread.start { repo.with('user') { k, u -> u.name += '+Petya' } }
        sleep(100)
        user.name = 'Masha'
        sleep(100)
        repo.putAndUnlock('user', user)
        sleep(300)
        assertThat(repo.get('user'), hasEntry('name', 'Masha+Petya'))
    }
}
