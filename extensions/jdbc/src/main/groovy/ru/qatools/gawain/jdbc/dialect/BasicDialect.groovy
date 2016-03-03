package ru.qatools.gawain.jdbc.dialect

import groovy.transform.CompileStatic

import java.sql.Connection
import java.sql.SQLException

import static ru.qatools.gawain.jdbc.util.ThreadUtil.threadId

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class BasicDialect implements Dialect {

    @Override
    void createLocksTableIfNotExists(String tableName, Connection conn) throws SQLException {
        conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS ${tableName} (
                      key VARCHAR,
                      locked_date DATE,
                      thread_id VARCHAR,
                      PRIMARY KEY (key)
                    )
        """);
    }

    @Override
    void createRepoTableIfNotExists(String tableName, Connection conn) throws SQLException {
        conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS ${tableName} (
                      key VARCHAR,
                      object VARBINARY,
                      PRIMARY KEY (key)
                    )
        """);
    }

    @Override
    void tryLock(String tableName, String key, Connection conn) throws SQLException {
        conn.createStatement().execute("""
                INSERT INTO ${tableName} (key, locked_date, thread_id)
                        VALUES ('${key}', current_date() - 100, '${threadId()}')
        """);
    }


    @Override
    void tryUnlock(String tableName, String key, Connection conn) throws SQLException {
        conn.createStatement().execute("""
                DELETE FROM ${tableName} WHERE key='${key}'
        """);
    }

    @Override
    boolean isLocked(String tableName, String key, Connection conn) throws SQLException {
        def statement = conn.createStatement()
        statement.execute("SELECT * FROM ${tableName} WHERE key='${key}'")
        statement.resultSet.next()
    }

    @Override
    boolean isLockedByMe(String tableName, String key, Connection conn) throws SQLException {
        def statement = conn.createStatement()
        statement.execute("SELECT * FROM ${tableName} WHERE key='${key}' AND thread_id='${threadId()}'")
        statement.resultSet.next()
    }

    @Override
    void put(String tableName, String key, Connection conn, byte[] bytes) throws SQLException {
        def statement = conn.prepareStatement("""
             MERGE INTO ${tableName}(key, object) VALUES (?, ?)
        """)
        statement.setString(1, key)
        statement.setBytes(2, bytes)
        statement.executeUpdate();
    }

    @Override
    void remove(String tableName, String key, Connection conn) throws SQLException {
        def statement = conn.createStatement()
        statement.executeUpdate("""
            DELETE FROM ${tableName} WHERE key = '${key}'
        """)
    }

    @Override
    byte[] get(String tableName, String key, Connection conn) throws SQLException {
        def statement = conn.createStatement()
        statement.execute("SELECT object FROM ${tableName} WHERE key='${key}'")
        (statement.resultSet.next()) ? statement.resultSet.getBytes('object') : null
    }

    @Override
    Collection<String> keys(String tableName, Connection conn) throws SQLException {
        def statement = conn.createStatement()
        statement.execute("SELECT key FROM ${tableName}")
        def res = []
        while (statement.resultSet.next()) {
            res << statement.resultSet.getString('key')
        }
        res
    }

    @Override
    Map<String, byte[]> valuesMap(String tableName, Connection conn) throws SQLException {
        def statement = conn.createStatement()
        statement.execute("SELECT key, object FROM ${tableName}")
        def res = [:]
        while (statement.resultSet.next()) {
            res[statement.resultSet.getString('key')] = statement.resultSet.getBytes('object')
        }
        res
    }
}
