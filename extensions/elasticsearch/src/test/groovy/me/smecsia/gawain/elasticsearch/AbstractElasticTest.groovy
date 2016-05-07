package me.smecsia.gawain.elasticsearch

import groovy.transform.CompileStatic
import me.smecsia.gawain.elasticsearch.util.EmbeddedESService
import org.junit.BeforeClass

import static me.smecsia.gawain.util.SocketUtil.findFreePort
/**
 * @author Ilya Sadykov
 */
@CompileStatic
abstract class AbstractElasticTest {
    static EmbeddedESService service
    static ElasticClient client
    static ElasticPessimisticLocking locking
    static ElasticRepoBuilder builder

    @BeforeClass
    public static void setUp() throws Exception {
        String port = "${findFreePort()}"
        service = new EmbeddedESService(
                'cluster.name': 'local',
                'transport.tcp.port': port,
                'transport.host': 'localhost'
        )
        client = new ElasticClient('local', "localhost:${port}")
        client.start()
        locking = new ElasticPessimisticLocking(client.client(), 'gawain', 'locks')
        builder = new ElasticRepoBuilder(client.client(), 'users')
    }
}
