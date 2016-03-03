package ru.qatools.gawain.jdbc

import ru.qatools.gawain.Opts
import ru.qatools.gawain.Repository
import ru.qatools.gawain.builders.RepoBuilder
import ru.qatools.gawain.jdbc.dialect.BasicDialect
import ru.qatools.gawain.jdbc.dialect.Dialect

import java.sql.Connection

/**
 * @author Ilya Sadykov
 */
class JDBCRepoBuilder implements RepoBuilder {
    final Connection connection
    final Opts opts
    final Dialect dialect
    final JDBCPessimisticLocking locking

    JDBCRepoBuilder(Connection connection, String locksTable = '__locks__',
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
