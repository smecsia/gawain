package ru.qatools.gawain.jdbc
import org.junit.Test
import ru.qatools.gawain.error.LockWaitTimeoutException

import java.util.concurrent.atomic.AtomicBoolean

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat
/**
 * @author Ilya Sadykov
 */
class JDBCPessimisticLockingTest extends AbstractJDBCTest {

    @Test
    public void testLockUnlock() throws Exception {
        def locking = new JDBCPessimisticLocking('locks', createConnection(), 10)
        def key = 'key1'
        locking.tryLock(key, 300)
        locking.tryLock(key, 300)
        assertThat(locking.isLockedByMe(key), equalTo(true))
        def failedToLock = new AtomicBoolean(false)
        Thread.start {
            try {
                locking.tryLock(key, 200)
            } catch (LockWaitTimeoutException e) {
                failedToLock.set(true)
            }
        }
        sleep(300)
        assertThat(failedToLock.get(), equalTo(true))
        failedToLock.set(false)
        def lockedByThread = new AtomicBoolean(false)
        locking.unlock(key)
        Thread.start {
            try {
                locking.tryLock(key, 200)
                lockedByThread.set(locking.isLockedByMe(key))
            } catch (LockWaitTimeoutException e) {
                failedToLock.set(true)
            }
        }
        sleep(300)
        assertThat(locking.isLocked(key), equalTo(true))
        assertThat(failedToLock.get(), equalTo(false))
        assertThat(lockedByThread.get(), equalTo(true))
    }
}
