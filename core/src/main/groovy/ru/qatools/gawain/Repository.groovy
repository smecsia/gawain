package ru.qatools.gawain

import groovy.transform.CompileStatic
import ru.qatools.gawain.error.LockWaitTimeoutException

/**
 * @author Ilya Sadykov
 */
@CompileStatic
trait Repository {
    /**
     * Gets value without locking
     */
    abstract Map get(String key)

    /**
     * Gets value without locking
     */
    Map getAt(String key) {
        get(key)
    }

    /**
     * Returns true if key is locked by current thread
     */
    abstract boolean isLockedByMe(String key);

    /**
     * Returns key set
     */
    abstract Set<String> keys()

    /**
     * Returns values map
     */
    abstract Map<String, Map> values();

    /**
     * Locks key and returns the value
     * This method blocks until timeout if lock is not available
     * @throws LockWaitTimeoutException if lock wait timeout is elapsed
     */
    abstract Map lockAndGet(String key) throws LockWaitTimeoutException

    /**
     * Perform operation against each state (performing lockAndGet and putAndUnlock then)
     * @throws LockWaitTimeoutException if lock wait timeout is elapsed
     */
    def withEach(StateClosure closure) throws LockWaitTimeoutException {
        values().each { k, s -> with(k, closure) }
    }

    /**
     * Perform operation against state for key (performing lockAndGet and putAndUnlock then)
     * @throws LockWaitTimeoutException if lock wait timeout is elapsed
     */
    Map with(String key, Repository.StateClosure closure) throws LockWaitTimeoutException {
        def state = lockAndGet(key) ?: [:]
        try {
            closure.forState(key, state)
            putAndUnlock(key, state)
        } catch (Exception e) {
            unlock(key)
            throw e
        }
    }

    /**
     * Locks key without getting value
     */
    abstract void lock(String key) throws LockWaitTimeoutException

    /**
     * Tries to lock key within given timeout.
     * Does not throw an exception
     * @return true if lock is acquired successfully, false otherwise
     */
    abstract boolean tryLock(String key)

    /**
     * Unlocking key
     */
    abstract void unlock(String key)

    /**
     * Putting key to map and unlocking it
     */
    abstract Map putAndUnlock(String key, Map value)

    /**
     * Putting key to map without unlocking it
     */
    abstract Map put(String key, Map value)

    /**
     * Deleting key and unlocking it
     */
    abstract def deleteAndUnlock(String key)

    /**
     * Helper interface to perform some actions on state with proper locking
     */
    static interface StateClosure {
        void forState(String key, Map state)
    }
}