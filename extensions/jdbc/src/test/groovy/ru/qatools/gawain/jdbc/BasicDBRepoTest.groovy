package ru.qatools.gawain.jdbc
import com.wix.mysql.EmbeddedMysql
import com.wix.mysql.config.Charset
import com.wix.mysql.distribution.Version
import org.junit.runners.Parameterized
import ru.qatools.gawain.jdbc.dialect.MysqlDialect
import ru.qatools.gawain.jdbc.dialect.PostgresDialect
import ru.yandex.qatools.embed.service.PostgresEmbeddedService

import static com.wix.mysql.EmbeddedMysql.anEmbeddedMysql
import static com.wix.mysql.config.MysqldConfig.aMysqldConfig
import static java.sql.DriverManager.getConnection
import static ru.qatools.gawain.util.SocketUtil.findFreePort
/**
 * @author Ilya Sadykov
 */
class BasicDBRepoTest {
    private static final EmbeddedMysql mysql
    private static final PostgresEmbeddedService postgres
    static final String MYSQLURL
    static final String PGURL
    static final String H2URL
    public static final String DB = "test"
    public static final String USER = 'username'
    public static final String PASSWORD = 'password'

    static {
        def mysqlBuilder = anEmbeddedMysql(aMysqldConfig(Version.v5_7_latest).withCharset(Charset.UTF8)
                .withUser(USER, PASSWORD)
                .withPort(findFreePort()).build())
                .addSchema(DB)
        def pgPort = findFreePort()
        postgres = new PostgresEmbeddedService("localhost", pgPort, USER, PASSWORD, DB)
        postgres.start()
        mysql = mysqlBuilder.start()
        addShutdownHook {
            mysql.stop();
            postgres.stop()
        }
        MYSQLURL = "jdbc:mysql://localhost:${mysql.config.port}/${DB}?useSSL=false" +
                "&user=${USER}&password=${PASSWORD}"
        PGURL = "jdbc:postgresql://localhost:${pgPort}/${DB}?user=${USER}&password=${PASSWORD}"
        H2URL = "jdbc:h2:mem:exercise_db;DB_CLOSE_DELAY=-1"
    }

    @Parameterized.Parameter
    public JDBCRepoBuilder builder

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        [
                [new JDBCRepoBuilder(getConnection(H2URL))],
                [new JDBCRepoBuilder(getConnection(MYSQLURL), new MysqlDialect())],
                [new JDBCRepoBuilder(getConnection(PGURL), new PostgresDialect())]
        ].collect { it.toArray() }
    }

}
