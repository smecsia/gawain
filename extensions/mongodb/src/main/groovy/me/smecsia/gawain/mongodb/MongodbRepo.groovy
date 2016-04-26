package me.smecsia.gawain.mongodb

import com.mongodb.MongoClient
import groovy.transform.CompileStatic
import me.smecsia.gawain.Opts
import me.smecsia.gawain.Repository
import me.smecsia.gawain.error.LockWaitTimeoutException
import ru.qatools.mongodb.MongoPessimisticLocking
import ru.qatools.mongodb.MongoPessimisticRepo

import static ru.qatools.mongodb.MongoPessimisticRepo.COLL_SUFFIX

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class MongodbRepo implements Repository {
    final long maxLockWaitMs
    final MongoPessimisticLocking locking
    final MongoPessimisticRepo<Map> repo
    final MongoClient mongo
    final String dbName
    final String colName

    MongodbRepo(MongoClient mongo, String dbName, String colName, Opts opts = new Opts()) {
        this.locking = new MongoPessimisticLocking(mongo, dbName, colName, (opts['pollIntMs'] ?: 10L) as long)
        this.repo = new MongoPessimisticRepo<>(locking, Map)
        this.maxLockWaitMs = opts.maxLockWaitMs
        def serializer = new MongodbSerializer(opts)
        this.repo.setSerializer(serializer)
        this.repo.setDeserializer(serializer)
        this.mongo = mongo
        this.dbName = dbName
        this.colName = colName
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

    @Override
    def clear() {
        mongo.getDatabase(dbName).getCollection("${colName}${COLL_SUFFIX}").drop()
    }
}
