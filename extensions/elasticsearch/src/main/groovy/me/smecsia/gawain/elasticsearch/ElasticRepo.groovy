package me.smecsia.gawain.elasticsearch

import groovy.transform.CompileStatic
import me.smecsia.gawain.Repository
import me.smecsia.gawain.error.LockWaitTimeoutException
import me.smecsia.gawain.serialize.ToJsonStateSerializer
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.Client
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static java.util.Collections.emptySet
import static me.smecsia.gawain.elasticsearch.ElasticClient.ignoreNoIndex

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class ElasticRepo implements Repository {
    final static Logger LOGGER = LoggerFactory.getLogger(ElasticRepo)
    final ElasticPessimisticLocking locking
    final String indexName
    final String typeName
    final Client client
    final ToJsonStateSerializer serializer
    final int maxLockWaitMs

    ElasticRepo(String indexName, String typeName,
                ElasticPessimisticLocking locking,
                ToJsonStateSerializer serializer, int maxLockWaitMs = 5000) {
        this.locking = locking
        this.maxLockWaitMs = maxLockWaitMs
        this.serializer = serializer
        this.client = locking.client
        this.indexName = indexName
        this.typeName = typeName
    }

    @Override
    Map get(String key) {
        ignoreNoIndex([:]) {
            serializer.deserialize(
                    client.prepareGet(indexName, typeName, key).execute().get().getSourceAsString()
            )
        }
    }

    @Override
    boolean isLockedByMe(String key) {
        locking.isLockedByMe(key)
    }

    @Override
    Set<String> keys() {
        ignoreNoIndex(emptySet()) {
            client.prepareSearch(indexName)
                    .setTypes(typeName)
                    .addField("id")
                    .execute().get().hits.toList()
                    .collect { it.id }
                    .toSet()
        }
    }

    @Override
    Map<String, Map> values() {
        ignoreNoIndex([:]) {
            client.prepareSearch(indexName)
                    .setTypes(typeName)
                    .execute().get().hits.toList()
                    .inject([:]) { r, h -> r[h.id] = serializer.deserialize(h.getSourceAsString()); r }
        }
    }

    @Override
    Map lockAndGet(String key) throws LockWaitTimeoutException {
        locking.tryLock(key, maxLockWaitMs)
        get(key)
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
        put(key, value)
        locking.unlock(key)
        value
    }

    @Override
    Map put(String key, Map value) {
        ignoreNoIndex(null as IndexResponse) {
            client.prepareIndex(indexName, typeName, key)
                    .setSource(serializer.serialize(value))
                    .execute().get()
        }
        value
    }

    @Override
    def deleteAndUnlock(String key) {
        locking.unlock(key)
        ignoreNoIndex {
            client.prepareDelete(indexName, typeName, key).execute().get()
        }
    }

    @Override
    def clear() {
        client.admin().indices().delete(new DeleteIndexRequest(indexName)).get()
    }

    @Override
    void forceUnlock(String key) {
        locking.forceUnlock(key)
    }
}
