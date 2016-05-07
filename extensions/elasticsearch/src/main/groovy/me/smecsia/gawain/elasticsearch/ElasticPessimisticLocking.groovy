package me.smecsia.gawain.elasticsearch

import groovy.transform.CompileStatic
import me.smecsia.gawain.error.InvalidLockOwnerException
import me.smecsia.gawain.error.LockWaitTimeoutException
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.client.Client
import org.elasticsearch.index.engine.DocumentAlreadyExistsException
import org.elasticsearch.script.Script
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static java.lang.System.currentTimeMillis
import static me.smecsia.gawain.elasticsearch.ElasticClient.assertElasticException
import static me.smecsia.gawain.elasticsearch.ElasticClient.ignoreNoIndex
import static me.smecsia.gawain.util.ThreadUtil.threadId
import static org.elasticsearch.script.ScriptService.ScriptType.INLINE

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class ElasticPessimisticLocking {
    final static Logger LOGGER = LoggerFactory.getLogger(ElasticPessimisticLocking)
    public static final int DEFAULT_POLL_INTERVAL_MS = 10
    final String indexName
    final String typeName
    final Client client
    final long lockPollIntervalMs

    ElasticPessimisticLocking(Client client, String indexName, String typeName,
                              long lockPollIntervalMs = DEFAULT_POLL_INTERVAL_MS) {
        this.indexName = indexName
        this.typeName = typeName
        this.client = client
        this.lockPollIntervalMs = lockPollIntervalMs
    }

    void tryLock(String key, long timeoutMs) throws LockWaitTimeoutException {
        def lockStarted = currentTimeMillis()
        def threadId = threadId()
        LOGGER.debug("Trying to lock key '${key}' by thread '${threadId}")
        while (currentTimeMillis() - lockStarted < timeoutMs) {
            try {
                if (isLockedByMe(key)) {
                    LOGGER.debug("key '${key}' is already locked by me (${threadId})")
                    return
                }
                client.prepareIndex(indexName, typeName, key).
                        setSource([
                                'lockedSince': currentTimeMillis(),
                                'threadId'   : threadId
                        ]).setCreate(true).execute().get()
                LOGGER.debug("Successfully locked key '${key}' by thread '${threadId}")
                return
            } catch (Exception e) {
                assertElasticException(e, DocumentAlreadyExistsException)
                LOGGER.trace("Lock trial was unsuccessful for key ${key} by thread '${threadId}'", e)
            }
            LOGGER.trace("Still waiting for lock '${key}' by thread '${threadId}'")
            sleep(lockPollIntervalMs)
        }
        throw new LockWaitTimeoutException("Failed to lock key '${key}' within ${timeoutMs}ms by thread ${threadId}")
    }

    void unlock(String key) throws InvalidLockOwnerException {
        def threadId = threadId()
        try {
            LOGGER.trace("Trying to unlock key ${key} by thread ${threadId}")
            client.prepareUpdate(indexName, typeName, key).setScript(
                    new Script("""
                                    if (ctx._source.threadId == threadId ){
                                       ctx.op = 'delete'
                                    } else {
                                       assert false
                                    }
                               """, INLINE, null, [threadId: threadId])
            ).execute().get()
        } catch (Exception e) {
            assertElasticException(e, ElasticsearchException)
            LOGGER.debug("Failed to unlock key ${key} by thread ${threadId}", e)
            throw new InvalidLockOwnerException("Failed to unlock key '${key}' by thread '${threadId}'", e)
        }
    }

    void forceUnlock(String key) {
        try {
            LOGGER.trace("Trying to forceUnlock key ${key} by thread ${threadId()}")
            client.prepareDelete(indexName, typeName, key).execute().get()
        } catch (Exception e) {
            assertElasticException(e, ElasticsearchException)
            LOGGER.debug("Failed to unlock key ${key} by thread ${threadId()}", e)
        }
    }

    boolean isLocked(String key) {
        getKey(key)?.getSource() != null
    }

    boolean isLockedByMe(String key) {
        getKey(key)?.getSource()?.get('threadId') == threadId()
    }

    protected GetResponse getKey(String key) {
        ignoreNoIndex(null as GetResponse) { client.prepareGet(indexName, typeName, key).execute().get() }
    }


}
