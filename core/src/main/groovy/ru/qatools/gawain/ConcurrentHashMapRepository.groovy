package ru.qatools.gawain

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import static java.util.concurrent.TimeUnit.SECONDS

/**
 * @author Ilya Sadykov
 */
class ConcurrentHashMapRepository implements Repository {
    private final Map<String, Map> map = new ConcurrentHashMap<>()
    private final Map<String, Lock> locks = new ConcurrentHashMap<>();
    private maxLockWaitSec = 5

    @Override
    Map get(String key) {
        map.get(key)
    }

    @Override
    Map lockAndGet(String key) {
        getLock(key).tryLock(maxLockWaitSec, SECONDS)
        get(key)
    }

    @Override
    void lock(String key) {
        getLock(key).tryLock(maxLockWaitSec, SECONDS)
    }

    @Override
    void unlock(String key) {
        getLock(key).unlock()
    }

    @Override
    Map putAndUnlock(String key, Map value) {
        map.put(key, value)
        unlock(key)
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
        map.remove(key)
        unlock(key)
    }

    private synchronized Lock getLock(String key) {
        if (!locks.containsKey(key)) {
            locks.put(key, new ReentrantLock())
        }
        locks.get(key)
    }
}
