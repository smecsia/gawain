package ru.qatools.gawain

import groovy.transform.CompileStatic

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
     * Returns key set
     */
    abstract Set<String> keys()

    /**
     * Returns values map
     */
    abstract Map<String, Map> values();

    /**
     * Locks key and returns the value
     */
    abstract Map lockAndGet(String key)

    /**
     * Perform operation against each state (performing lockAndGet and putAndUnlock then)
     */
    def withEach(StateClosure closure) {
        values().each { k, s -> with(k, closure) }
    }

    /**
     * Perform operation against state for key (performing lockAndGet and putAndUnlock then)
     */
    Map with(String key, Repository.StateClosure closure) {
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
    abstract void lock(String key)

    /**
     * Unlocking key
     */
    abstract void unlock(String key)

    /**
     * Putting key to map and unlocking it
     */
    abstract Map putAndUnlock(String key, Map value)

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