package me.smecsia.gawain.jdbc

import groovy.transform.CompileStatic
import me.smecsia.gawain.Opts
import me.smecsia.gawain.Repository
import me.smecsia.gawain.builders.RepoBuilder
import me.smecsia.gawain.jdbc.dialect.BasicDialect
import me.smecsia.gawain.jdbc.dialect.Dialect

import java.sql.Connection

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class JDBCRepoBuilder implements RepoBuilder {
    public static final String DEFAULT_TABLE = '__locks__'
    final Connection connection
    final Opts opts
    final Dialect dialect
    final JDBCPessimisticLocking locking

    JDBCRepoBuilder(Connection connection, Dialect dialect, Opts opts = new Opts()) {
        this(connection, DEFAULT_TABLE, opts, dialect)
    }

    JDBCRepoBuilder(Connection connection, String locksTable = DEFAULT_TABLE,
                    Opts opts = new Opts(), Dialect dialect = new BasicDialect()) {
        this.connection = connection
        this.opts = opts
        this.dialect = dialect
        this.locking = new JDBCPessimisticLocking(locksTable, connection, opts.lockPollIntervalMs, dialect)
    }

    @Override
    Repository build(String name, Opts opts) {
        new JDBCRepo(name, locking, opts.maxLockWaitMs, opts.serializer)
    }
}
