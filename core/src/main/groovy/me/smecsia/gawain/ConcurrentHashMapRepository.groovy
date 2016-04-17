package me.smecsia.gawain

import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import me.smecsia.gawain.error.IllegalLockOwnerException
import me.smecsia.gawain.error.LockWaitTimeoutException

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

import static java.lang.Thread.currentThread
import static java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class ConcurrentHashMapRepository implements Repository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentHashMapRepository.class)
    private final Map<String, Map> map = new ConcurrentHashMap<>()
    private final Map<String, Thread> owners = new ConcurrentHashMap<>()
    private final Map<String, Semaphore> locks = new ConcurrentHashMap<>();
    private int maxLockWaitMs = 5000

    @Override
    Map get(String key) {
        map.get(key)
    }

    @Override
    boolean isLockedByMe(String key) {
        currentThread() == owners.get(key)
    }

    @Override
    Map lockAndGet(String key) throws LockWaitTimeoutException {
        lock(key)
        get(key)
    }

    @Override
    void lock(String key) {
        if (!tryLock(key)) {
            throw new LockWaitTimeoutException("Failed to lock key ${key} within timeout of ${maxLockWaitMs}ms")
        }
    }

    @Override
    boolean tryLock(String key) {
        LOGGER.trace("Trying to lock key '${key}'")
        if (isLockedByMe(key) || getLock(key).tryAcquire(maxLockWaitMs, MILLISECONDS)) {
            LOGGER.trace("Key has been locked successfully '${key}'")
            owners.put(key, currentThread())
            return true
        }
        return false
    }

    @Override
    void unlock(String key) {
        try {
            LOGGER.trace("Unlocking key '${key}'")
            getLock(key).release()
            LOGGER.trace("Removing owners for key '${key}'")
            owners.remove(key)
        } catch (Exception e) {
            LOGGER.warn("Failed to unlock key '${key}'", e)
        }
    }

    @Override
    Map putAndUnlock(String key, Map value) {
        LOGGER.trace("putAndUnlock key '${key}' value '${value}")
        map.put(key, value)
        unlock(key)
        value
    }

    @Override
    Map put(String key, Map value) {
        LOGGER.trace("put key '${key}' value '${value}")
        map.put(key, value)
        value
    }

    @Override
    Set<String> keys() {
        map.keySet()
    }

    @Override
    Map<String, Map> values() {
        map
    }

    @Override
    def deleteAndUnlock(String key) {
        if (!isLockedByMe(key)) {
            throw new IllegalLockOwnerException("Failed to delete entry for key '${key}'")
        }
        map.remove(key)
        unlock(key)
    }

    private synchronized Semaphore getLock(String key) {
        if (!locks.containsKey(key)) {
            locks.put(key, new Semaphore(1))
        }
        locks.get(key)
    }
}
