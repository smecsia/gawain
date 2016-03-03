package ru.qatools.gawain.jdbc.dialect
import groovy.transform.CompileStatic
/**
 * @author Ilya Sadykov
 */
@CompileStatic
class MysqlDialect extends BasicDialect {

    @Override
    protected String field(String name) {
        "`${name}`"
    }

    @Override
    protected String table(String name) {
        "`${name}`"
    }

    @Override
    protected String upsertSQL(String tableName) {
        """
            INSERT INTO ${table(tableName)} (${field("key")}, ${field("object")})
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE
              ${field("key")} = VALUES(${field("key")}),
              ${field("object")} = VALUES(${field("object")})
        """
    }

    @Override
    protected String createLocksSQL(String tableName) {
        """
            CREATE TABLE IF NOT EXISTS ${table(tableName)} (
              ${field("key")} VARCHAR(512),
              ${field("locked_date")} DATETIME,
              ${field("thread_id")} VARCHAR(256),
              PRIMARY KEY (${field("key")})
            )
        """
    }
}
