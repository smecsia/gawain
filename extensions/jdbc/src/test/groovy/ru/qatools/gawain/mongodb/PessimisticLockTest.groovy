package ru.qatools.gawain.mongodb

import groovy.transform.CompileStatic
import org.h2.jdbc.JdbcSQLException
import org.junit.Before
import org.junit.Test

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

import static org.junit.Assert.fail

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class PessimisticLockTest {

    @Before
    public void setUp() {
        try {
            createTables(getConnection())
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Test(expected = JdbcSQLException.class)
    public void pessimistic_locking_exercise() throws SQLException {
        // first, jack bauer inserts a field agent report
        Connection connJack = getConnection()
        connJack.setAutoCommit(false);
        def key = 'CTU Field'
        connJack.createStatement().execute("insert into items (name, release_date) values ('${key}', current_date() - 100)");
        connJack.commit();
        connJack.setAutoCommit(false);
        // later on jack wants to update the field report and make
        // sure noone else can access the rows at the same time!
        connJack.createStatement().execute("select * from items where name = '${key}' for update");
        // TODO update the row, etc.
        System.out.println("Jack Bauer locked the row for any other update");

        // then habib shows up and tries to update the row, but
        // cannot. An Exception is being thrown
        Connection habibConn = getConnection()
        habibConn.setAutoCommit(false);
        habibConn.createStatement()
                .executeUpdate("update items set release_date = current_date() + 10  where name = '${key}'");

        fail("We should never be able to get to this line, because an exception is thrown");
    }

    // simply what we did in OpenConnectionExerciseJava6/7.java
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:exercise_db;DB_CLOSE_DELAY=-1");
    }


    private void createTables(Connection conn) {
        try {
            conn.createStatement().execute("create table items (id identity, release_date date, name VARCHAR)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
