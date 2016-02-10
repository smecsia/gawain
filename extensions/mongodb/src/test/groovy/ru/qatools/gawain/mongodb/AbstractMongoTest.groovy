package ru.qatools.gawain.mongodb

import org.junit.BeforeClass
import ru.yandex.qatools.embed.service.MongoEmbeddedService

import static de.flapdoodle.embed.mongo.distribution.Version.Main.PRODUCTION
import static java.lang.Runtime.getRuntime
import static ru.qatools.gawain.mongodb.util.SocketUtil.findFreePort

/**
 * @author Ilya Sadykov
 */
abstract class AbstractMongoTest {

    static final String RS_NAME = "local"
    static final String DB = "dbname"
    static final String USER = "user"
    static final String PASS = "password"
    static final int INIT_TIMEOUT = 10000
    protected static MongoEmbeddedService mongo
    public static GString URL

    @BeforeClass
    public static synchronized void setUpClass() throws Exception {
        if (mongo == null) {
            mongo = new MongoEmbeddedService(
                    "localhost:" + findFreePort(), DB, USER,
                    PASS, RS_NAME, null, true, INIT_TIMEOUT)
                    .useVersion(PRODUCTION).useWiredTiger();
            mongo.start();
            URL = "mongodb://${USER}:${PASS}@${mongo.getHost()}:" +
                    "${mongo.getPort()}/${DB}?replicaSet=${RS_NAME}&w=majority"
            getRuntime().addShutdownHook(new Thread({ mongo.stop() }));
        }
    }

    protected static MongodbBroadcastBuilder mongoBroadcastBuilder() {
        new MongodbBroadcastBuilder(URL, DB)
    }

    protected static MongodbRepoBuilder mongoRepoBuilder() {
        new MongodbRepoBuilder(URL, DB)
    }
}
