package me.smecsia.gawain.jdbc.dialect
import groovy.transform.CompileStatic

import java.sql.Connection
import java.sql.SQLException

import static me.smecsia.gawain.jdbc.util.ThreadUtil.threadId

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class PostgresDialect extends BasicDialect {

    @Override
    protected String createRepoSQL(String tableName) {
        """
            CREATE TABLE IF NOT EXISTS ${table(tableName)} (
              ${field("key")} VARCHAR(512),
              ${field("object")} BYTEA,
              PRIMARY KEY (${field("key")})
            )
        """
    }

    @Override
    protected String insertLockSQL(String tableName, String key) {
        """
            INSERT INTO ${table(tableName)} (${field("key")}, ${field("thread_id")})
                    VALUES ('${key}', '${threadId()}')
        """
    }

    @Override
    protected String upsertSQL(String tableName) {
        """
            INSERT INTO ${table(tableName)} (${field("key")}, ${field("object")})
            VALUES (?, ?)
            ON CONFLICT (${field("key")}) DO UPDATE
                SET ${field("key")} = ?, ${field("object")} = ?
        """
    }

    @Override
    void put(String tableName, String key, Connection conn, byte[] bytes) throws SQLException {
        def statement = conn.prepareStatement(upsertSQL(tableName))
        statement.setString(1, key)
        statement.setBytes(2, bytes)
        statement.setString(3, key)
        statement.setBytes(4, bytes)
        statement.executeUpdate();
    }
}
