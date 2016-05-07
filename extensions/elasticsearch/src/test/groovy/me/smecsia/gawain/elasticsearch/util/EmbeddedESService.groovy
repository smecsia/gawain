package me.smecsia.gawain.elasticsearch.util

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.elasticsearch.client.Client
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.node.Node
import org.elasticsearch.script.groovy.GroovyPlugin

import java.nio.file.Path

import static java.nio.file.Files.createTempDirectory
import static org.apache.commons.io.FileUtils.deleteQuietly
import static org.elasticsearch.common.settings.Settings.settingsBuilder

/**
 * @author Ilya Sadykov
 */
@Canonical
@CompileStatic
class EmbeddedESService {
    public static final int INIT_TIMEOUT = 10000
    public static final String BIND_HOST = 'localhost'
    private final Path dataDirectory
    private Node node

    public EmbeddedESService(Map<String, String> settings = [:]) {
        Settings.Builder builder = settingsBuilder()
        this.dataDirectory = createTempDirectory('embeddedES')

        def dataDir = this.dataDirectory.resolve('data')
        def logDir = this.dataDirectory.resolve('logs')

        builder.put(
                'path.home': dataDirectory.toString(),
                'http.enabled': 'true',
                'threadpool.bulk.queue_size': '5000',
                'path.data': dataDir.toString(),
                'path.logs': logDir.toString(),
                'network.bind_host': BIND_HOST,
                'network.publish_host': BIND_HOST,
                'network.host': BIND_HOST,
                'script.inline': 'true',
                'script.indexed': 'true',
                'script.update': 'true',
                'script.search': 'true',
                'script.plugin': 'true',
                'script.aggs': 'true',
        )

        builder.put(settings)
        node = new EmbeddedESNode(builder.build(), [GroovyPlugin])
        node.start()
        node.client().admin().cluster().prepareHealth().setWaitForYellowStatus().execute()
                .actionGet(INIT_TIMEOUT);
    }

    public void stop() {
        if (node != null) {
            node.close();
            node = null;
            deleteQuietly(dataDirectory.toFile())
        }
    }

    Client client() {
        node.client()
    }
}
