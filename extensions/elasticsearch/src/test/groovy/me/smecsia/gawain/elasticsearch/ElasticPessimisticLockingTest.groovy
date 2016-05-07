package me.smecsia.gawain.elasticsearch

import me.smecsia.gawain.error.LockWaitTimeoutException
import org.junit.Test

import java.util.concurrent.atomic.AtomicBoolean

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.Matchers.contains
import static org.junit.Assert.assertThat
/**
 * @author Ilya Sadykov
 */
class ElasticPessimisticLockingTest extends AbstractElasticTest {

    @Test
    public void testLockUnlock() throws Exception {
        def key = 'key1'
        locking.tryLock(key, 300)
        locking.tryLock(key, 300)
        assertThat(locking.isLockedByMe(key), equalTo(true))
        Flags flags
        tryLockInThread(key, flags = new Flags())
        sleep(300)
        assertThat([flags.locked, flags.lockedByMe, flags.lockTimeout], contains(true, false, true))
        def lockedByThread = new AtomicBoolean(false)
        locking.unlock(key)
        tryLockInThread(key, flags = new Flags()) { lockedByThread.set(true) }
        sleep(300)
        assertThat(locking.isLocked(key), equalTo(true))
        assertThat([flags.locked, flags.lockedByMe, flags.lockTimeout], contains(false, false, false))
        assertThat(lockedByThread.get(), equalTo(true))
    }

    protected Thread tryLockInThread(String key, Flags flags, Closure call = {}) {
        Thread.start {
            try {
                flags.locked = locking.isLocked(key)
                flags.lockedByMe = locking.isLockedByMe(key)
                locking.tryLock(key, 200)
                call.call()
            } catch (LockWaitTimeoutException e) {
                flags.lockTimeout = true
            }
        }
    }

    class Flags {
        boolean locked, lockedByMe, lockTimeout
    }
}
