package me.smecsia.gawain.jdbc

import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import me.smecsia.gawain.error.InvalidLockOwnerException
import me.smecsia.gawain.error.LockWaitTimeoutException
import me.smecsia.gawain.jdbc.dialect.BasicDialect
import me.smecsia.gawain.jdbc.dialect.Dialect

import java.sql.Connection
import java.sql.SQLException

import static java.lang.System.currentTimeMillis

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class JDBCPessimisticLocking {
    final static Logger LOGGER = LoggerFactory.getLogger(JDBCPessimisticLocking)
    final String tableName
    final Connection connection
    final Dialect dialect
    final long lockPollIntervalMs

    JDBCPessimisticLocking(String tableName, Connection connection, long lockPollIntervalMs = 10,
                           Dialect dialect = new BasicDialect()) {
        this.tableName = tableName
        this.connection = connection
        this.dialect = dialect
        this.lockPollIntervalMs = lockPollIntervalMs
        dialect.createLocksTableIfNotExists(tableName, connection)
    }

    void tryLock(String key, long timeoutMs) throws LockWaitTimeoutException {
        def lockStarted = currentTimeMillis()
        LOGGER.debug("Trying to lock key '${key}'")
        while (currentTimeMillis() - lockStarted < timeoutMs) {
            try {
                if (dialect.isLockedByMe(tableName, key, connection)) {
                    LOGGER.debug("key '${key}' is already locked by me")
                    return
                }
                dialect.tryLock(tableName, key, connection)
                LOGGER.debug("Successfully locked key '${key}'")
                return
            } catch (SQLException e) {
                LOGGER.trace("Lock trial was unsuccessful for key ${key}", e)
            }
            LOGGER.trace("Still waiting for lock '${key}'")
            sleep(lockPollIntervalMs)
        }
        throw new LockWaitTimeoutException("Failed to lock key '${key}' within ${timeoutMs}ms")
    }

    void unlock(String key) throws InvalidLockOwnerException {
        try {
            LOGGER.trace("Trying to unlock key ${key}")
            dialect.tryUnlock(tableName, key, connection)
        } catch (SQLException e) {
            LOGGER.debug("Failed to unlock key ${key}", e)
            throw new InvalidLockOwnerException("Failed to unlock key '${key}'", e)
        }
    }

    boolean isLocked(String key) {
        dialect.isLocked(tableName, key, connection)
    }

    boolean isLockedByMe(String key) {
        dialect.isLockedByMe(tableName, key, connection)
    }
}
