package ru.qatools.gawain.jdbc

import org.junit.Test
import ru.qatools.gawain.Opts

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

/**
 * @author Ilya Sadykov
 */
class JDBCRepoBuilderTest extends AbstractJDBCTest {

    @Test
    public void testLockSaveAndGet() throws Exception {
        def repo = new JDBCRepoBuilder(createConnection())
                .build('repo1', new Opts(maxLockWaitMs: 500))
        repo.put('user', [name: 'Vasya'])
        assertThat(repo.get('user'), equalTo([name: 'Vasya'] as Map))

        def user = repo.lockAndGet('user')
        Thread.start { repo.with('user') { k, u -> u.name += '+Petya' } }
        sleep(100)
        user.name = 'Masha'
        sleep(100)
        repo.putAndUnlock('user', user)
        sleep(300)
        assertThat(repo.get('user'), equalTo([name: 'Masha+Petya'] as Map))
    }
}
