package me.smecsia.gawain.jdbc.dialect

import groovy.transform.CompileStatic

import java.sql.Connection
import java.sql.SQLException

/**
 * @author Ilya Sadykov
 */
@CompileStatic
interface Dialect {
    void createLocksTableIfNotExists(String tableName, Connection conn) throws SQLException

    void createRepoTableIfNotExists(String tableName, Connection conn) throws SQLException

    void tryLock(String tableName, String key, Connection conn) throws SQLException

    void tryUnlock(String tableName, String key, Connection conn) throws SQLException

    boolean isLocked(String tableName, String key, Connection conn) throws SQLException

    boolean isLockedByMe(String tableName, String key, Connection conn) throws SQLException

    void put(String tableName, String key, Connection conn, byte[] bytes) throws SQLException

    void remove(String tableName, String key, Connection conn) throws SQLException

    byte[] get(String tableName, String key, Connection conn) throws SQLException

    Collection<String> keys(String tableName, Connection conn) throws SQLException

    Map<String, byte[]> valuesMap(String tableName, Connection conn) throws SQLException
}