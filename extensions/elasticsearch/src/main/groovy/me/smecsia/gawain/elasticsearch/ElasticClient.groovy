package me.smecsia.gawain.elasticsearch

import groovy.transform.CompileStatic
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.action.NoShardAvailableActionException
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.shard.IllegalIndexShardStateException
import org.elasticsearch.index.shard.ShardNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.Callable

import static org.elasticsearch.common.settings.Settings.settingsBuilder

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class ElasticClient {
    final static Logger LOGGER = LoggerFactory.getLogger(ElasticClient)
    private TransportClient client
    private volatile started = false
    private final Map<String, String> settings
    private final String clusterName
    private final String clientHosts

    public ElasticClient(Map<String, String> settings = [:], String clusterName, String clientHosts) {
        this.settings = settings
        this.clusterName = clusterName
        this.clientHosts = clientHosts
        addShutdownHook { stop() }
    }

    TransportClient client() {
        return client
    }

    public start() {
        Settings.Builder builder = settingsBuilder()
        builder.put(settings)
        builder.put('cluster.name': clusterName);
        def clientBuilder = new TransportClient.Builder()
        clientBuilder.settings(builder.build())
        client = clientBuilder.build()
        for (String host : clientHosts.split(',')) {
            String[] hostPort = host.split(':');
            client.addTransportAddress(
                    new InetSocketTransportAddress(
                            InetAddress.getByName(hostPort[0]),
                            Integer.valueOf(hostPort[1])
                    ));
        }
    }

    public stop() {
        if (client != null && started) {
            client.close()
            started = false
        }
    }

    static <T> T ignoreNoIndex(T defaultRes, Callable<T> query = { null }) {
        try {
            query.call() as T
        } catch (Exception e) {
            LOGGER.trace("Exception during index query ${e.getMessage()}", e)
            assertElasticException(e, [
                    ShardNotFoundException,
                    IndexNotFoundException,
                    NoShardAvailableActionException,
                    IllegalIndexShardStateException
            ], defaultRes)
        }
    }

    static <T> T assertElasticException(Exception e, Class<? extends ElasticsearchException> excClass,
                                        T defaultRes = null) {
        assertElasticException(e, [excClass], defaultRes)
    }

    static <T> T assertElasticException(Exception e, Collection<Class<? extends ElasticsearchException>> excClasses = [],
                                        T defaultRes = null) {
        if (!excClasses.any { it.isInstance(toElasticException(e)) }) {
            throw e;
        }
        defaultRes
    }

    static Throwable toElasticException(Exception e) {
        (e.getCause()?.getCause()?.getCause()?.getCause()) ?:
                (e.getCause()?.getCause()?.getCause() ?: e.getCause()?.getCause())
    }
}
