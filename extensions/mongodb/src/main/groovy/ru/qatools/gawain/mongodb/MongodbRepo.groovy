package ru.qatools.gawain.mongodb

import com.mongodb.BasicDBObject
import com.mongodb.MongoClient
import com.mongodb.util.JSON
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.bson.Document
import ru.qatools.gawain.Opts
import ru.qatools.gawain.Repository
import ru.qatools.gawain.error.LockWaitTimeoutException
import ru.qatools.mongodb.MongoPessimisticLocking
import ru.qatools.mongodb.MongoPessimisticRepo

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class MongodbRepo implements Repository {
    private static final JsonSlurper jsonParser = new JsonSlurper()
    long maxLockWaitMs
    MongoPessimisticLocking locking
    MongoPessimisticRepo<Map> repo

    MongodbRepo(MongoClient client, String dbName, String colName, Opts opts = new Opts()) {
        locking = new MongoPessimisticLocking(client, dbName, colName, (opts['pollIntMs'] ?: 10L) as long)
        repo = new MongoPessimisticRepo<>(locking, Map)
        maxLockWaitMs = opts.maxLockWaitMs
        repo.setSerializer({ state ->
            (BasicDBObject) JSON.parse(JsonOutput.toJson(state))
        })
        repo.setDeserializer({ Document input, clazz ->
            jsonParser.parseText(input.toJson())
        })
    }

    @Override
    Map get(String key) {
        repo.get(key)
    }

    @Override
    boolean isLockedByMe(String key) {
        repo.lock.isLockedByMe(key)
    }

    @Override
    Set<String> keys() {
        repo.keySet()
    }

    @Override
    Map<String, Map> values() {
        repo.keyValueMap()
    }

    @Override
    Map lockAndGet(String key) throws LockWaitTimeoutException {
        try {
            repo.tryLockAndGet(key, maxLockWaitMs)
        } catch (ru.qatools.mongodb.error.LockWaitTimeoutException e) {
            throw new LockWaitTimeoutException("Failed to wait until lock is available", e)
        }
    }

    @Override
    void lock(String key) throws LockWaitTimeoutException {
        try {
            repo.lock.tryLock(key, maxLockWaitMs)
        } catch (ru.qatools.mongodb.error.LockWaitTimeoutException e) {
            throw new LockWaitTimeoutException("Failed to wait until lock is available", e)
        }
    }

    @Override
    boolean tryLock(String key) {
        try {
            repo.lock.tryLock(key, maxLockWaitMs)
            return true
        } catch (ru.qatools.mongodb.error.LockWaitTimeoutException e) {
            return false
        }
    }

    @Override
    void unlock(String key) {
        repo.getLock().forceUnlock(key)
    }

    @Override
    Map putAndUnlock(String key, Map value) {
        repo.putAndUnlock(key, value); value
    }

    @Override
    Map put(String key, Map value) {
        repo.put(key, value); value
    }

    @Override
    def deleteAndUnlock(String key) {
        repo.removeAndUnlock(key)
    }
}
