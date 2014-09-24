package net.sf.dbinit;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Unit test for {@link DBInit}.
 *
 * @author Damien Coraboeuf
 */
public class DBInitTest {

    private static final String DIR_DB = "target/dbinit/test";

    private static final String FILE_DB = DIR_DB + "/db";

    private static final String JDBC_URL = "jdbc:h2:file:" + FILE_DB;

    private DBInit db;

    @Before
    public void before() throws IOException {
        // Clean-up
        File dir = new File(DIR_DB);
        if (dir.exists()) {
            FileUtils.forceDelete(dir);
        }
        // General initialisation
        db = new DBInit();
        db.setJdbcDriver("org.h2.Driver");
        db.setJdbcUser("SA");
        db.setJdbcPassword("");
        db.setJdbcURL(JDBC_URL);
        db.setResourceInitialization("/dbinit/test/init.sql");
        db.setResourceUpdate("/dbinit/test/update.{0}.sql");
        db.setVersionTable("VERSION");
        db.setVersionColumnName("value");
        db.setVersionColumnTimestamp("value_date");
    }

    @Test(expected = DBInitDriverNotFoundException.class)
    public void driver_not_found() {
        db.setJdbcDriver("my.dummy.Driver");
        db.run();
    }

    @Test(expected = DBInitCannotGetResourceException.class)
    public void resource_not_found() {
        db.setResourceInitialization("/dbinit/test/none.sql");
        db.run();
    }

    @Test
    public void init() throws SQLException {
        db.setVersion(0);
        db.run();
        // Checks the table
        Connection c = DriverManager.getConnection(JDBC_URL, "SA", "");
        try {
            {
                PreparedStatement ps = c.prepareStatement("insert into PROJECT (name) values (?)");
                try {
                    ps.setString(1, "My project");
                    int count = ps.executeUpdate();
                    assertEquals(1, count);
                } finally {
                    ps.close();
                }
            }
            {
                PreparedStatement ps = c.prepareStatement("select * from PROJECT  where id = ?");
                try {
                    ps.setInt(1, 1);
                    ResultSet rs = ps.executeQuery();
                    try {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt("id"));
                        assertEquals("My project", rs.getString("name"));
                    } finally {
                        rs.close();
                    }
                } finally {
                    ps.close();
                }
            }
        } finally {
            c.close();
        }
    }

    @Test
    public void patch() throws SQLException {
        init();
        db.setVersion(1);
        db.run();
        // Checks the table
        Connection c = DriverManager.getConnection(JDBC_URL, "SA", "");
        try {
            {
                PreparedStatement ps = c.prepareStatement("select * from PROJECT  where id = ?");
                try {
                    ps.setInt(1, 1);
                    ResultSet rs = ps.executeQuery();
                    try {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt("id"));
                        assertEquals("My project", rs.getString("name"));
                        assertNull(rs.getString("url"));
                    } finally {
                        rs.close();
                    }
                } finally {
                    ps.close();
                }
            }
        } finally {
            c.close();
        }
    }

    @Test
    public void patchActionNotApplied() throws SQLException {
        init();
        db.setVersion(1);
        TestDBPatchAction patchAction = new TestDBPatchAction();
        db.setPatchActions(Arrays.asList(patchAction));
        db.run();
        // The patch must not have been applied
        assertFalse(patchAction.isApplied());
    }

    @Test
    public void patchAction() throws SQLException {
        init();
        db.setVersion(2);
        TestDBPatchAction patchAction = new TestDBPatchAction();
        db.setPatchActions(Arrays.asList(patchAction));
        db.run();
        // The patch must have been applied
        assertTrue(patchAction.isApplied());
        // Checks that URLs have been filled in
        Connection c = DriverManager.getConnection(JDBC_URL, "SA", "");
        try {
            {
                PreparedStatement ps = c.prepareStatement("select * from PROJECT  where id = ?");
                try {
                    ps.setInt(1, 1);
                    ResultSet rs = ps.executeQuery();
                    try {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt("id"));
                        assertEquals("My project", rs.getString("name"));
                        assertEquals("uri:My project", rs.getString("url"));
                    } finally {
                        rs.close();
                    }
                } finally {
                    ps.close();
                }
            }
        } finally {
            c.close();
        }
    }

    @Test
    public void up_to_date() throws SQLException {
        patch();
        db.setVersion(1);
        db.run();
    }

    @Test(expected = DBInitVersionException.class)
    public void version_mismatch() throws SQLException {
        patch();
        db.setVersion(0);
        db.run();
    }

}
