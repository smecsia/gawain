package me.smecsia.gawain.jdbc.dialect

import groovy.transform.CompileStatic

import java.sql.Connection
import java.sql.SQLException

import static me.smecsia.gawain.jdbc.util.ThreadUtil.threadId

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class BasicDialect implements Dialect {
    private int maxObjectSize = 30720 // 30Kb by default

    @Override
    void createLocksTableIfNotExists(String tableName, Connection conn) throws SQLException {
        conn.createStatement().execute(createLocksSQL(tableName));
    }

    @Override
    void createRepoTableIfNotExists(String tableName, Connection conn) throws SQLException {
        conn.createStatement().execute(createRepoSQL(tableName));
    }

    @Override
    void tryLock(String tableName, String key, Connection conn) throws SQLException {
        conn.createStatement().execute(insertLockSQL(tableName, key));
    }

    @Override
    void tryUnlock(String tableName, String key, Connection conn) throws SQLException {
        conn.createStatement().execute(removeLockSQL(tableName, key));
    }

    @Override
    boolean isLocked(String tableName, String key, Connection conn) throws SQLException {
        def statement = conn.createStatement()
        statement.execute("SELECT * FROM ${table(tableName)} WHERE ${field("key")}='${key}'")
        statement.resultSet.next()
    }

    @Override
    boolean isLockedByMe(String tableName, String key, Connection conn) throws SQLException {
        def statement = conn.createStatement()
        statement.execute("SELECT * FROM ${table(tableName)} WHERE ${field("key")}='${key}' " +
                "AND ${field("thread_id")} ='${threadId()}'")
        statement.resultSet.next()
    }

    @Override
    void put(String tableName, String key, Connection conn, byte[] bytes) throws SQLException {
        def statement = conn.prepareStatement(upsertSQL(tableName))
        statement.setString(1, key)
        statement.setBytes(2, bytes)
        statement.executeUpdate();
    }


    @Override
    void remove(String tableName, String key, Connection conn) throws SQLException {
        def statement = conn.createStatement()
        statement.executeUpdate("DELETE FROM ${table(tableName)} WHERE ${field("key")} = '${key}'")
    }

    @Override
    byte[] get(String tableName, String key, Connection conn) throws SQLException {
        def statement = conn.createStatement()
        statement.execute("SELECT object FROM ${table(tableName)} WHERE ${field("key")} = '${key}'")
        (statement.resultSet.next()) ? statement.resultSet.getBytes('object') : null
    }

    @Override
    Collection<String> keys(String tableName, Connection conn) throws SQLException {
        def statement = conn.createStatement()
        statement.execute("SELECT ${field("key")} FROM ${table(tableName)}")
        def res = []
        while (statement.resultSet.next()) {
            res << statement.resultSet.getString('key')
        }
        res
    }

    @Override
    Map<String, byte[]> valuesMap(String tableName, Connection conn) throws SQLException {
        def statement = conn.createStatement()
        statement.execute("SELECT ${field("key")}, ${field("object")} FROM ${table(tableName)}")
        def res = [:]
        while (statement.resultSet.next()) {
            res[statement.resultSet.getString('key')] = statement.resultSet.getBytes('object')
        }
        res
    }

    // SQL which should be overriden

    protected String table(String name) {
        "${name}"
    }

    protected String field(String name) {
        "${name}"
    }

    protected String upsertSQL(String tableName) {
        """
             MERGE INTO ${table(tableName)}(${field("key")}, ${field("object")}) VALUES (?, ?)
        """
    }

    protected String insertLockSQL(String tableName, String key) {
        """
            INSERT INTO ${table(tableName)} (${field("key")}, ${field("locked_date")}, ${field("thread_id")})
                    VALUES ('${key}', current_date() - 100, '${threadId()}')
        """
    }


    protected String createLocksSQL(String tableName) {
        """
            CREATE TABLE IF NOT EXISTS ${table(tableName)} (
              ${field("key")} VARCHAR(512),
              ${field("locked_date")} DATE,
              ${field("thread_id")} VARCHAR(256),
              PRIMARY KEY (${field("key")})
            )
        """
    }

    protected String createRepoSQL(String tableName) {
        """
            CREATE TABLE IF NOT EXISTS ${table(tableName)} (
              ${field("key")} VARCHAR(512),
              ${field("object")} VARBINARY(${maxObjectSize}),
              PRIMARY KEY (${field("key")})
            )
        """
    }

    protected String removeLockSQL(String tableName, String key) {
        """
            DELETE FROM ${table(tableName)} WHERE ${field("key")}='${key}'
        """
    }
}
