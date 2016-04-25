package me.smecsia.gawain.jdbc

import groovy.transform.CompileStatic
import me.smecsia.gawain.Repository
import me.smecsia.gawain.serialize.ToBytesStateSerializer
import me.smecsia.gawain.error.LockWaitTimeoutException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static me.smecsia.gawain.serialize.Serializer.DEFAULT_STATE_SERIALIZER
/**
 * @author Ilya Sadykov
 */
@CompileStatic
class JDBCRepo implements Repository {
    final static Logger LOGGER = LoggerFactory.getLogger(JDBCRepo)
    final String tableName
    final JDBCPessimisticLocking locking
    final ToBytesStateSerializer serializer
    final int maxLockWaitMs

    JDBCRepo(String tableName, JDBCPessimisticLocking locking, int maxLockWaitMs = 5000,
             ToBytesStateSerializer serializer = DEFAULT_STATE_SERIALIZER) {
        this.tableName = tableName
        this.locking = locking
        this.maxLockWaitMs = maxLockWaitMs
        this.serializer =  serializer
        locking.dialect.createRepoTableIfNotExists(tableName, locking.connection)
    }

    @Override
    Map get(String key) {
        byte[] data = locking.dialect.get(tableName, key, locking.connection)
        (data != null) ? serializer.deserialize(data) : null
    }

    @Override
    boolean isLockedByMe(String key) {
        locking.isLockedByMe(key)
    }

    @Override
    Set<String> keys() {
        locking.dialect.keys(tableName, locking.connection)
        [] as Set
    }

    @Override
    Map<String, Map> values() {
        locking.dialect.valuesMap(tableName, locking.connection).inject([:])
                { res, Map.Entry<String, byte[]> e ->
                    res[e.key] = (e.value != null) ? serializer.deserialize(e.value) : null
                } as Map<String, Map>
    }

    @Override
    Map lockAndGet(String key) throws LockWaitTimeoutException {
        locking.tryLock(key, maxLockWaitMs)
        def bytes = locking.dialect.get(tableName, key, locking.connection)
        (bytes != null) ? serializer.deserialize(bytes) : null
    }

    @Override
    void lock(String key) throws LockWaitTimeoutException {
        locking.tryLock(key, maxLockWaitMs)
    }

    @Override
    boolean tryLock(String key) {
        try {
            locking.tryLock(key, maxLockWaitMs)
            return true
        } catch (LockWaitTimeoutException e) {
            LOGGER.debug("Failed to lock key '${key}' within ${maxLockWaitMs}ms", e)
            return false
        }
    }

    @Override
    void unlock(String key) {
        locking.unlock(key)
    }

    @Override
    Map putAndUnlock(String key, Map value) {
        locking.dialect.put(tableName, key, locking.connection, serializer.serialize(value))
        locking.unlock(key)
        value
    }

    @Override
    Map put(String key, Map value) {
        locking.dialect.put(tableName, key, locking.connection, serializer.serialize(value))
        value
    }

    @Override
    def deleteAndUnlock(String key) {
        locking.dialect.remove(tableName, key, locking.connection)
    }
}
