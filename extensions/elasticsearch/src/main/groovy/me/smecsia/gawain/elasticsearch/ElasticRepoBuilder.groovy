package me.smecsia.gawain.elasticsearch

import groovy.transform.CompileStatic
import me.smecsia.gawain.Opts
import me.smecsia.gawain.Repository
import me.smecsia.gawain.builders.RepoBuilder
import me.smecsia.gawain.error.InitializationException
import me.smecsia.gawain.serialize.ToJsonStateSerializer
import org.elasticsearch.client.Client

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class ElasticRepoBuilder implements RepoBuilder {
    public static final String LOCKS_SUFFIX = '__locks'
    final Client client
    final Opts opts
    final String indexName, locksSuffix

    ElasticRepoBuilder(Client client, String indexName, Opts opts = new Opts()) {
        this(client, indexName, LOCKS_SUFFIX, opts)
    }

    ElasticRepoBuilder(Client client, String indexName, String locksSuffix, Opts opts = new Opts()) {
        this.client = client
        this.opts = opts
        this.indexName = indexName
        this.locksSuffix = locksSuffix
    }

    @Override
    Repository build(String name, Opts opts) {
        if (!(opts.stateSerializer instanceof ToJsonStateSerializer)) {
            throw new InitializationException("Cannot use ${opts.stateSerializer}! " +
                    "ElasticSearch supports only serialization to json")
        }
        def locking = new ElasticPessimisticLocking(client, indexName, "${name}${locksSuffix}", opts.lockPollIntervalMs)
        new ElasticRepo(indexName, name, locking, opts.stateSerializer as ToJsonStateSerializer, opts.maxLockWaitMs)
    }
}
