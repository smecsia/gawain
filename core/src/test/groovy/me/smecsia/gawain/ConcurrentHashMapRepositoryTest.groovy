package me.smecsia.gawain

import me.smecsia.gawain.builders.BasicRepoBuilder
import me.smecsia.gawain.error.LockWaitTimeoutException
import org.junit.Test

import java.util.concurrent.atomic.AtomicInteger

import static java.util.concurrent.Executors.newSingleThreadExecutor
import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat
import static org.junit.Assert.fail
/**
 * @author Ilya Sadykov
 */
class ConcurrentHashMapRepositoryTest {

    @Test
    public void testLockUnlock() throws Exception {
        def repo = new BasicRepoBuilder().build('repo', new Opts(maxLockWaitMs: 100))
        def key = 'key1'
        repo.lock(key)
        repo.put(key, [value: 1])
        def counter = new AtomicInteger()
        newSingleThreadExecutor().submit {
            while (true) {
                try {
                    repo.lock(key)
                    counter.incrementAndGet()
                } catch (LockWaitTimeoutException e) {
                    println("Failed to wait for the lock: ${e.message}")
                }
                sleep(100)
            }
        }
        sleep(200)
        assertThat(counter.get(), is(0))
        assertThat(repo.isLockedByMe(key), is(true))
        repo.unlock(key)
        sleep(400)
        assertThat(counter.get(), greaterThan(0))
        assertThat(repo.isLockedByMe(key), is(false))
        try {
            repo.lock(key)
            fail("Exception must be thrown while trying to lock key ${key}")
        } catch (LockWaitTimeoutException ignored) {
        }
        assertThat(counter.get(), greaterThanOrEqualTo(1))
        assertThat(repo.get(key), hasEntry('value', 1))
        repo.clear()
        assertThat(repo.keys(), is(empty()))
    }
}
