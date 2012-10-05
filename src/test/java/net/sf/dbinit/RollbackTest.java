package net.sf.dbinit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link DBInit}.
 * @author Damien Coraboeuf
 */
public class RollbackTest {

	private static final String DIR_DB = "target/dbinit/rollback";

	private static final String FILE_DB = DIR_DB + "/rollback";

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
		db.setResourceInitialization("/dbinit/rollback/init.sql");
		db.setResourceUpdate("/dbinit/rollback/update.{0}.sql");
		db.setVersionTable("VERSION");
		db.setVersionColumnName("value");
		db.setVersionColumnTimestamp("value_date");
	}

	private void init() throws SQLException {
		db.setVersion(0);
		db.run();
	}

	@Test
	public void patch_with_rollback() throws SQLException {
		init();
		db.setVersion(1);
		db.run();
		// Checks
		Connection c = DriverManager.getConnection(JDBC_URL, "SA", "");
		try {
			// Checks the version has stayed 0
			{
				PreparedStatement ps = c.prepareStatement("select * from VERSION");
				try {
					ResultSet rs = ps.executeQuery();
					try {
						assertTrue(rs.next());
						assertEquals(0, rs.getInt("value"));
						assertFalse(rs.next());
					} finally {
						rs.close();
					}
				} finally {
					ps.close();
				}
			}
			// Checks the table A & B have been deleted
			{
				ResultSet rs = c.getMetaData().getTables(null, null, "XTABLE_%", null);
				try {
					assertFalse (rs.next());
				} finally {
					rs.close();
				}
			}
		} finally {
			c.close();
		}
	}

}
