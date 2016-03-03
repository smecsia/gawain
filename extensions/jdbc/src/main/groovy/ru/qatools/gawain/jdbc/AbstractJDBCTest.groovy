package ru.qatools.gawain.jdbc

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * @author Ilya Sadykov
 */
class AbstractJDBCTest {
    // simply what we did in OpenConnectionExerciseJava6/7.java
    protected Connection createConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:exercise_db;DB_CLOSE_DELAY=-1");
    }
}
